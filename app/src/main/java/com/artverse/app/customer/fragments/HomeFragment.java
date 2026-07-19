package com.artverse.app.customer.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.artverse.app.R;
import com.artverse.app.adapters.ArtworkAdapter;
import com.artverse.app.customer.ArtworkDetailActivity;
import com.artverse.app.models.Artwork;
import com.artverse.app.utils.ArtCategories;
import com.artverse.app.utils.ChipStyler;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.QuerySnapshot;

import android.content.Intent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Implements FR05 - Browse and Search Module: customers browse and search
 * artwork by category or keyword, backed by a real-time Firestore listener
 * on the "artworks" collection (satisfies NFR04).
 */
public class HomeFragment extends Fragment {

    private RecyclerView rvArtworks;
    private ChipGroup chipGroupCategories;
    private View emptyState;
    private TextView tvGreeting;
    private SwipeRefreshLayout swipeRefresh;

    private ArtworkAdapter adapter;
    private final List<Artwork> allArtworks = new ArrayList<>();
    private String selectedCategory = "All";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvArtworks = view.findViewById(R.id.rvArtworks);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        emptyState = view.findViewById(R.id.emptyState);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        setGreeting();
        setupChips();
        setupGrid();
        setupSearch(view);

        swipeRefresh.setOnRefreshListener(this::loadArtworks);
        loadArtworks();
    }

    private void setGreeting() {
        SessionManager session = new SessionManager(requireContext());
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String time = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        String name = session.getName();
        tvGreeting.setText(name != null && !name.isEmpty() ? time + ", " + name.split(" ")[0] : time);
    }

    private void setupChips() {
        Chip all = new Chip(requireContext());
        all.setText("All");
        ChipStyler.styleCategoryChip(all);
        all.setChecked(true);
        chipGroupCategories.addView(all);

        for (String category : ArtCategories.DEFAULT) {
            Chip chip = new Chip(requireContext());
            chip.setText(category);
            ChipStyler.styleCategoryChip(chip);
            chipGroupCategories.addView(chip);
        }

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null) {
                selectedCategory = chip.getText().toString();
                applyFilters();
            }
        });
    }

    private void setupGrid() {
        adapter = new ArtworkAdapter(artwork -> {
            Intent intent = new Intent(requireContext(), ArtworkDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ARTWORK_ID, artwork.id);
            startActivity(intent);
        });
        rvArtworks.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvArtworks.setAdapter(adapter);
    }

    private void setupSearch(View root) {
        android.widget.EditText etSearch = root.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase(Locale.ROOT).trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void loadArtworks() {
        FirebaseUtil.artworksRef()
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot snapshot, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
                    swipeRefresh.setRefreshing(false);
                    if (error != null || snapshot == null || getContext() == null) return;

                    allArtworks.clear();
                    for (var doc : snapshot.getDocuments()) {
                        Artwork artwork = doc.toObject(Artwork.class);
                        // Only admin-approved artwork is visible to buyers.
                        if (artwork != null && Artwork.isPublished(artwork)) {
                            artwork.id = doc.getId();
                            allArtworks.add(artwork);
                        }
                    }
                    applyFilters();
                });
    }

    private void applyFilters() {
        List<Artwork> filtered = new ArrayList<>();
        for (Artwork artwork : allArtworks) {
            boolean matchesCategory = "All".equals(selectedCategory)
                    || selectedCategory.equalsIgnoreCase(artwork.categoryName);
            boolean matchesSearch = searchQuery.isEmpty()
                    || (artwork.title != null && artwork.title.toLowerCase(Locale.ROOT).contains(searchQuery))
                    || (artwork.artistName != null && artwork.artistName.toLowerCase(Locale.ROOT).contains(searchQuery));
            if (matchesCategory && matchesSearch) filtered.add(artwork);
        }
        adapter.submitList(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvArtworks.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
