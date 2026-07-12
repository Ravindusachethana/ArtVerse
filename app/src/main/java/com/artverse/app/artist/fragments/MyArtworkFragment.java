package com.artverse.app.artist.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MyArtworkFragment extends Fragment {

    private RecyclerView rvList;
    private View emptyState;
    private MyArtworkAdapter adapter;

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
                confirmDelete(artwork);
            }
        });
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        view.findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddEditArtworkActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadArtworks();
    }

    private void loadArtworks() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.artworksRef()
                .whereEqualTo("artistId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || getContext() == null) return;

                    List<Artwork> artworks = new ArrayList<>();
                    for (var doc : snapshot.getDocuments()) {
                        Artwork artwork = doc.toObject(Artwork.class);
                        if (artwork != null) {
                            artwork.id = doc.getId();
                            artworks.add(artwork);
                        }
                    }
                    adapter.submitList(artworks);
                    emptyState.setVisibility(artworks.isEmpty() ? View.VISIBLE : View.GONE);
                    rvList.setVisibility(artworks.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void confirmDelete(Artwork artwork) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove artwork")
                .setMessage("Delete \"" + artwork.title + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteArtwork(artwork))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteArtwork(Artwork artwork) {
        FirebaseUtil.artworksRef().document(artwork.id).delete();
        String uid = FirebaseUtil.currentUid();
        if (uid != null) {
            FirebaseUtil.artistsRef().document(uid)
                    .update("totalArtworks", com.google.firebase.firestore.FieldValue.increment(-1));
        }
    }
}
