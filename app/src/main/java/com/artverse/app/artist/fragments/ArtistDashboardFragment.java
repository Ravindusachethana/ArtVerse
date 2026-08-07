package com.artverse.app.artist.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.ArtistOrderAdapter;
import com.artverse.app.artist.AddEditArtworkActivity;
import com.artverse.app.artist.ArtistProfileEditActivity;
import com.artverse.app.artist.ArtistReportsActivity;
import com.artverse.app.models.Artist;
import com.artverse.app.models.Order;
import com.artverse.app.models.User;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.OrderActions;
import com.artverse.app.utils.OrderStatus;
import com.artverse.app.utils.SessionManager;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Artist landing screen: quick stats sourced from "artists/{uid}" plus a live orders count. */
public class ArtistDashboardFragment extends Fragment {

    private TextView tvTotalArtworks, tvPendingOrders, tvTotalSales, tvNoOrders,
            tvReportCompleted, tvReportRejected;
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
        tvReportCompleted = view.findViewById(R.id.tvReportCompleted);
        tvReportRejected = view.findViewById(R.id.tvReportRejected);
        rvRecentOrders = view.findViewById(R.id.rvRecentOrders);
        ivArtistAvatar = view.findViewById(R.id.ivArtistAvatar);

        view.findViewById(R.id.cardReports).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ArtistReportsActivity.class)));

        SessionManager session = new SessionManager(requireContext());
        ((TextView) view.findViewById(R.id.tvGreeting)).setText("Welcome back");
        ((TextView) view.findViewById(R.id.tvStudioName)).setText(session.getName());

        ivArtistAvatar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ArtistProfileEditActivity.class)));

        adapter = new ArtistOrderAdapter(new ArtistOrderAdapter.OrderActionListener() {
            @Override
            public void onConfirm(Order order) {
                OrderActions.confirm(order)
                        .addOnFailureListener(e -> toast("Could not confirm order: " + e.getMessage()));
            }

            @Override
            public void onHandOver(Order order) {
                OrderActions.handOverToDelivery(order)
                        .addOnFailureListener(e -> toast("Could not update order: " + e.getMessage()));
            }
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

        // Equality filter only, sorted/trimmed client-side: keeps the query
        // free of composite-index requirements (and lets the pending count
        // cover every order, not just the visible five).
        FirebaseUtil.ordersRef()
                .whereEqualTo("artistId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (getContext() == null) return;
                    if (error != null) {
                        Log.e("ArtistDashboard", "Order query failed", error);
                        return;
                    }
                    if (snapshot == null) return;

                    List<Order> orders = new ArrayList<>();
                    int pendingCount = 0;
                    int completedCount = 0;
                    int rejectedCount = 0;
                    for (var d : snapshot.getDocuments()) {
                        Order order = d.toObject(Order.class);
                        if (order != null) {
                            order.id = d.getId();
                            orders.add(order);
                            // Orders still needing the artist: awaiting a
                            // decision, or confirmed but not yet dispatched.
                            if (OrderStatus.isAwaitingArtist(order.status)
                                    || OrderStatus.isConfirmed(order.status)) pendingCount++;
                            else if (Constants.STATUS_COMPLETED.equals(order.status)) completedCount++;
                            else if (Constants.STATUS_REJECTED.equals(order.status)) rejectedCount++;
                        }
                    }
                    orders.sort((a, b) -> Long.compare(b.orderDate, a.orderDate));
                    List<Order> recent = orders.size() > 5 ? orders.subList(0, 5) : orders;
                    adapter.submitList(recent);
                    tvPendingOrders.setText(String.valueOf(pendingCount));
                    tvReportCompleted.setText(String.valueOf(completedCount));
                    tvReportRejected.setText(String.valueOf(rejectedCount));
                    tvNoOrders.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
                    rvRecentOrders.setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void toast(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
}
