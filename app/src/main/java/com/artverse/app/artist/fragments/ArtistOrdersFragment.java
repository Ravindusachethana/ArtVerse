package com.artverse.app.artist.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.artverse.app.R;
import com.artverse.app.adapters.ArtistOrderAdapter;
import com.artverse.app.models.Order;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/** Implements FR07 (artist side) - artists view and update the status of orders they receive. */
public class ArtistOrdersFragment extends Fragment {

    private RecyclerView rvList;
    private View emptyState;
    private ArtistOrderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_generic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.tvScreenTitle)).setText("Incoming Orders");
        ((ImageView) view.findViewById(R.id.ivEmptyIcon)).setImageResource(R.drawable.ic_orders);
        ((TextView) view.findViewById(R.id.tvEmptyMessage)).setText("No incoming orders yet");

        rvList = view.findViewById(R.id.rvList);
        emptyState = view.findViewById(R.id.emptyState);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setEnabled(false);

        adapter = new ArtistOrderAdapter(new ArtistOrderAdapter.OrderActionListener() {
            @Override
            public void onAccept(Order order) { updateStatus(order, Constants.STATUS_PROCESSING); }

            @Override
            public void onReject(Order order) { updateStatus(order, Constants.STATUS_REJECTED); }
        });
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.ordersRef()
                .whereEqualTo("artistId", uid)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || getContext() == null) return;

                    List<Order> orders = new ArrayList<>();
                    for (var doc : snapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            order.id = doc.getId();
                            orders.add(order);
                        }
                    }
                    adapter.submitList(orders);
                    emptyState.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                    rvList.setVisibility(orders.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void updateStatus(Order order, String newStatus) {
        FirebaseUtil.ordersRef().document(order.id).update("status", newStatus)
                .addOnSuccessListener(v -> {
                    if (Constants.STATUS_PROCESSING.equals(newStatus)) {
                        markItemsCompleted(order);
                    } else if (Constants.STATUS_REJECTED.equals(newStatus)) {
                        restoreAvailability(order);
                    }
                    Toast.makeText(requireContext(),
                            "Order " + newStatus, Toast.LENGTH_SHORT).show();
                });
    }

    /** When an artist accepts an order, the pieces move to a completed/settled state. */
    private void markItemsCompleted(Order order) {
        FirebaseUtil.ordersRef().document(order.id).update("status", Constants.STATUS_PROCESSING);
    }

    /** When an artist rejects an order, restore the artwork's availability for other buyers. */
    private void restoreAvailability(Order order) {
        if (order.items == null) return;
        for (var item : order.items) {
            FirebaseUtil.artworksRef().document(item.artworkId).update("available", true);
        }
        String uid = FirebaseUtil.currentUid();
        if (uid != null) {
            FirebaseUtil.artistsRef().document(uid)
                    .update("totalSales", FieldValue.increment(-order.totalAmount));
        }
    }
}
