package com.artverse.app.artist.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.MyArtworkAdapter;
import com.artverse.app.artist.AddEditArtworkActivity;
import com.artverse.app.models.Artwork;
import com.artverse.app.models.Order;
import com.artverse.app.models.OrderItem;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.OrderStatus;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Artist's own listings. Backed by a single live Firestore listener attached
 * for the lifetime of the view, so a listing's moderation state - "In review",
 * "Update in review", "Rejected" - refreshes the moment an admin decides,
 * without the artist leaving or reopening the screen.
 */
public class MyArtworkFragment extends Fragment {

    private RecyclerView rvList;
    private View emptyState;
    private MyArtworkAdapter adapter;

    private ListenerRegistration artworksListener;
    /** Guards against a second delete tap while the order check is in flight. */
    private boolean checkingOrders = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_artwork, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvList = view.findViewById(R.id.rvList);
        emptyState = view.findViewById(R.id.emptyState);

        adapter = new MyArtworkAdapter(new MyArtworkAdapter.ActionListener() {
            @Override
            public void onEdit(Artwork artwork) {
                Intent intent = new Intent(requireContext(), AddEditArtworkActivity.class);
                intent.putExtra(Constants.EXTRA_ARTWORK_ID, artwork.id);
                intent.putExtra(Constants.EXTRA_EDIT_MODE, true);
                startActivity(intent);
            }

            @Override
            public void onDelete(Artwork artwork) {
                checkOrdersThenDelete(artwork);
            }
        });
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddEditArtworkActivity.class)));

        // Attached once here rather than in onResume: re-attaching per resume
        // stacked up duplicate listeners that were never released.
        loadArtworks();
    }

    @Override
    public void onDestroyView() {
        if (artworksListener != null) {
            artworksListener.remove();
            artworksListener = null;
        }
        super.onDestroyView();
    }

    private void loadArtworks() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        // Equality filter only, sorted client-side: keeps the query free of
        // composite-index requirements so it can never fail on a fresh
        // project (same approach as the order screens). A failing query used
        // to be swallowed here, which looked exactly like "no live updates".
        artworksListener = FirebaseUtil.artworksRef()
                .whereEqualTo("artistId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (getContext() == null) return;
                    if (error != null) {
                        Log.e("MyArtwork", "Artwork query failed", error);
                        toast("Could not load your artwork: " + error.getMessage());
                        return;
                    }
                    if (snapshot == null) return;

                    List<Artwork> artworks = new ArrayList<>();
                    for (var doc : snapshot.getDocuments()) {
                        Artwork artwork = doc.toObject(Artwork.class);
                        // A sold-and-delivered one-of-a-kind piece is retired
                        // from the dashboard; it survives in the order records
                        // and the buyer's collection, and still counts in the
                        // artist's sales and published-artwork reports.
                        if (artwork != null && !artwork.retired) {
                            artwork.id = doc.getId();
                            artworks.add(artwork);
                        }
                    }
                    artworks.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

                    adapter.submitList(artworks);
                    emptyState.setVisibility(artworks.isEmpty() ? View.VISIBLE : View.GONE);
                    rvList.setVisibility(artworks.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    /* ---------------- Delete guarded by outstanding orders ---------------- */

    /**
     * An artwork someone has ordered cannot simply vanish - the buyer is
     * waiting on it. Only orders still in flight block the delete; settled
     * ones (completed/rejected) keep their own copy of the item details in
     * the order record, so history and receipts survive the removal.
     */
    private void checkOrdersThenDelete(Artwork artwork) {
        String uid = FirebaseUtil.currentUid();
        if (uid == null || checkingOrders) return;
        checkingOrders = true;

        FirebaseUtil.ordersRef()
                .whereEqualTo("artistId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    checkingOrders = false;
                    if (getContext() == null) return;

                    int activeOrders = 0;
                    for (var doc : snapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order == null || OrderStatus.isSettled(order.status)) continue;
                        if (containsArtwork(order, artwork.id)) activeOrders++;
                    }

                    if (activeOrders > 0) {
                        showBlockedDialog(artwork, activeOrders);
                    } else {
                        confirmDelete(artwork);
                    }
                })
                .addOnFailureListener(e -> {
                    checkingOrders = false;
                    toast("Could not check orders for this artwork: " + e.getMessage());
                });
    }

    private boolean containsArtwork(Order order, String artworkId) {
        if (order.items == null || artworkId == null) return false;
        for (OrderItem item : order.items) {
            if (artworkId.equals(item.artworkId)) return true;
        }
        return false;
    }

    private void showBlockedDialog(Artwork artwork, int activeOrders) {
        String orderWord = activeOrders == 1 ? "order" : "orders";
        new AlertDialog.Builder(requireContext())
                .setTitle("Can't remove this artwork")
                .setMessage("\"" + artwork.title + "\" has " + activeOrders + " " + orderWord
                        + " still in progress. Settle " + (activeOrders == 1 ? "it" : "them")
                        + " from your Orders page first, then you can remove the listing.")
                .setPositiveButton("View orders", (dialog, which) -> openOrdersTab())
                .setNegativeButton("Close", null)
                .show();
    }

    private void openOrdersTab() {
        View nav = requireActivity().findViewById(R.id.bottomNav);
        if (nav instanceof BottomNavigationView) {
            ((BottomNavigationView) nav).setSelectedItemId(R.id.nav_artist_orders);
        }
    }

    private void confirmDelete(Artwork artwork) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove artwork")
                .setMessage("Delete \"" + artwork.title + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteArtwork(artwork))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** The live listener drops the row as soon as the delete lands. */
    private void deleteArtwork(Artwork artwork) {
        FirebaseUtil.artworksRef().document(artwork.id).delete()
                .addOnSuccessListener(v -> {
                    String uid = FirebaseUtil.currentUid();
                    if (uid != null) {
                        FirebaseUtil.artistsRef().document(uid)
                                .update("totalArtworks",
                                        com.google.firebase.firestore.FieldValue.increment(-1));
                    }
                    toast("Artwork removed");
                })
                .addOnFailureListener(e -> toast("Could not remove artwork: " + e.getMessage()));
    }

    private void toast(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
}
