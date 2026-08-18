package com.artverse.app.artist.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.artverse.app.utils.OrderActions;
import com.artverse.app.utils.OrderStatus;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * incoming orders split into three tabs.
 * "Pending" holds every order still in flight; the artist confirms a new one
 * then hands the confirmed one to the delivery section, and the card updates
 * in place until the admin closes it out. Cancelling an order is the admin's
 * call, so the artist has no reject action. Settled orders open their receipt
 * (see ReceiptActivity).
 */
public class ArtistOrdersFragment extends Fragment {

    private static final String[] TAB_LABELS = {"Pending", "Completed", "Rejected"};

    private RecyclerView rvList;
    private View emptyState;
    private TextView tvEmptyMessage;
    private ArtistOrderAdapter adapter;

    private final List<Order> allOrders = new ArrayList<>();
    private int selectedTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.tvScreenTitle)).setText("Incoming Orders");
        ((ImageView) view.findViewById(R.id.ivEmptyIcon)).setImageResource(R.drawable.ic_orders);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        rvList = view.findViewById(R.id.rvList);
        emptyState = view.findViewById(R.id.emptyState);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setEnabled(false);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        for (String label : TAB_LABELS) tabLayout.addTab(tabLayout.newTab().setText(label));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                showSelectedTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        adapter = new ArtistOrderAdapter(new ArtistOrderAdapter.OrderActionListener() {
            @Override
            public void onConfirm(Order order) {
                OrderActions.confirm(order)
                        .addOnSuccessListener(v -> toast("Order confirmed - hand it over when it's ready"))
                        .addOnFailureListener(e -> toast("Could not confirm order: " + e.getMessage()));
            }

            @Override
            public void onHandOver(Order order) {
                OrderActions.handOverToDelivery(order)
                        .addOnSuccessListener(v -> toast("Marked out for delivery"))
                        .addOnFailureListener(e -> toast("Could not update order: " + e.getMessage()));
            }
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
                .addSnapshotListener((snapshot, error) -> {
                    if (getContext() == null) return;
                    if (error != null) {
                        Log.e("ArtistOrders", "Order query failed", error);
                        toast("Could not load orders: " + error.getMessage());
                        return;
                    }
                    if (snapshot == null) return;

                    allOrders.clear();
                    for (var doc : snapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            order.id = doc.getId();
                            allOrders.add(order);
                        }
                    }
                    allOrders.sort((a, b) -> Long.compare(b.orderDate, a.orderDate));
                    showSelectedTab();
                });
    }

    private void showSelectedTab() {
        List<Order> filtered = new ArrayList<>();
        for (Order order : allOrders) {
            if (belongsToSelectedTab(order)) filtered.add(order);
        }
        adapter.submitList(filtered);
        tvEmptyMessage.setText("No " + TAB_LABELS[selectedTab].toLowerCase() + " orders");
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvList.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private boolean belongsToSelectedTab(Order order) {
        switch (selectedTab) {
            case 1: return Constants.STATUS_COMPLETED.equals(order.status);
            case 2: return Constants.STATUS_REJECTED.equals(order.status);
            default:
                // "Pending" is every order still on its way: awaiting a
                // decision (incl. legacy "pending"), confirmed, or out for
                // delivery. The stage-appropriate buttons and the tracking
                // bar change in place as the order progresses.
                return !OrderStatus.isSettled(order.status);
        }
    }

    private void toast(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
