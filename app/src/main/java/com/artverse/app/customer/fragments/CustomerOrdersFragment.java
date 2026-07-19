package com.artverse.app.customer.fragments;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.artverse.app.R;
import com.artverse.app.adapters.OrderAdapter;
import com.artverse.app.models.Order;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer-facing order history (part of FR07), split into three tabs -
 * Processing / Rejected / Completed - backed by one live query filtered
 * client-side. Tapping a settled (completed/rejected) order opens its
 * receipt (see OrderAdapter / ReceiptActivity).
 */
public class CustomerOrdersFragment extends Fragment {

    private static final String[] TAB_LABELS = {"Processing", "Rejected", "Completed"};

    private RecyclerView rvList;
    private View emptyState;
    private TextView tvEmptyMessage;
    private OrderAdapter adapter;

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

        ((TextView) view.findViewById(R.id.tvScreenTitle)).setText(R.string.nav_orders);
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

        adapter = new OrderAdapter();
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        // Equality filter only, sorted client-side: keeps the query free of
        // composite-index requirements so it can never fail on a fresh project.
        FirebaseUtil.ordersRef()
                .whereEqualTo("customerId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (getContext() == null) return;
                    if (error != null) {
                        Log.e("CustomerOrders", "Order query failed", error);
                        Toast.makeText(getContext(), "Could not load orders: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
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
            case 1: return Constants.STATUS_REJECTED.equals(order.status);
            case 2: return Constants.STATUS_COMPLETED.equals(order.status);
            default: // Processing tab also covers legacy "pending" orders.
                return Constants.STATUS_PROCESSING.equals(order.status)
                        || Constants.STATUS_PENDING.equals(order.status);
        }
    }
}
