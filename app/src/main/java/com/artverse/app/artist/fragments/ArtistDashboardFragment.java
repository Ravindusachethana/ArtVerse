package com.artverse.app.artist.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.ArtistOrderAdapter;
import com.artverse.app.artist.AddEditArtworkActivity;
import com.artverse.app.artist.ArtistProfileEditActivity;
import com.artverse.app.models.Artist;
import com.artverse.app.models.Order;
import com.artverse.app.models.User;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.Query;
import de.hdodenhof.circleimageview.CircleImageView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Artist landing screen: quick stats sourced from "artists/{uid}" plus a live orders count. */
public class ArtistDashboardFragment extends Fragment {

    private TextView tvTotalArtworks, tvPendingOrders, tvTotalSales, tvNoOrders;
    private RecyclerView rvRecentOrders;
    private CircleImageView ivArtistAvatar;
    private ArtistOrderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artist_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalArtworks = view.findViewById(R.id.tvTotalArtworks);
        tvPendingOrders = view.findViewById(R.id.tvPendingOrders);
        tvTotalSales = view.findViewById(R.id.tvTotalSales);
        tvNoOrders = view.findViewById(R.id.tvNoOrders);
        rvRecentOrders = view.findViewById(R.id.rvRecentOrders);
        ivArtistAvatar = view.findViewById(R.id.ivArtistAvatar);

        SessionManager session = new SessionManager(requireContext());
        ((TextView) view.findViewById(R.id.tvGreeting)).setText("Welcome back");
        ((TextView) view.findViewById(R.id.tvStudioName)).setText(session.getName());

        ivArtistAvatar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ArtistProfileEditActivity.class)));

        adapter = new ArtistOrderAdapter(new ArtistOrderAdapter.OrderActionListener() {
            @Override
            public void onAccept(Order order) { updateStatus(order, Constants.STATUS_PROCESSING); }

            @Override
            public void onReject(Order order) { updateStatus(order, Constants.STATUS_REJECTED); }
        });
        rvRecentOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentOrders.setAdapter(adapter);

        view.findViewById(R.id.btnAddArtwork).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddEditArtworkActivity.class)));

        loadArtistStats();
        loadRecentOrders();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh in case the user just returned from editing their name/photo.
        View view = getView();
        if (view != null) {
            ((TextView) view.findViewById(R.id.tvStudioName)).setText(new SessionManager(requireContext()).getName());
        }
        loadAvatar();
    }

    private void loadAvatar() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null || getContext() == null) return;

        FirebaseUtil.usersRef().document(uid).get().addOnSuccessListener(doc -> {
            if (getContext() == null) return;
            User user = doc.toObject(User.class);
            if (user != null && user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                ivArtistAvatar.setPadding(0, 0, 0, 0);
                ivArtistAvatar.setBackgroundColor(getResources().getColor(R.color.bg_surface_alt, null));
                Glide.with(this).load(user.profileImageUrl).into(ivArtistAvatar);
            }
        });
    }

    private void loadArtistStats() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.artistsRef().document(uid).addSnapshotListener((doc, error) -> {
            if (error != null || doc == null || !doc.exists() || getContext() == null) return;
            Artist artist = doc.toObject(Artist.class);
            if (artist == null) return;

            tvTotalArtworks.setText(String.valueOf(artist.totalArtworks));

            NumberFormat format = NumberFormat.getInstance(Locale.US);
            tvTotalSales.setText("LKR " + format.format(artist.totalSales));
        });
    }

    private void loadRecentOrders() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.ordersRef()
                .whereEqualTo("artistId", uid)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || getContext() == null) return;

                    List<Order> orders = new ArrayList<>();
                    int pendingCount = 0;
                    for (var d : snapshot.getDocuments()) {
                        Order order = d.toObject(Order.class);
                        if (order != null) {
                            order.id = d.getId();
                            orders.add(order);
                            if (Constants.STATUS_PENDING.equals(order.status)) pendingCount++;
                        }
                    }
                    adapter.submitList(orders);
                    tvPendingOrders.setText(String.valueOf(pendingCount));
                    tvNoOrders.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                    rvRecentOrders.setVisibility(orders.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void updateStatus(Order order, String newStatus) {
        FirebaseUtil.ordersRef().document(order.id).update("status", newStatus);
    }
}
