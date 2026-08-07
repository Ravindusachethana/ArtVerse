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
import com.artverse.app.utils.OrderStatus;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer-facing order history and live delivery tracking,
 * split into three tabs backed by one live query filtered client-side.
 * "Pending" holds every order still in flight - awaiting the artist,
 * confirmed, or out for delivery - and because the query is a snapshot
 * listener the card's status pill and tracking bar advance in place as the
 * artist and admin act, then the order jumps to Completed or Rejected on
 * its own. Tapping a settled order opens its receipt (see OrderAdapter /
 * ReceiptActivity).
 */
public class CustomerOrdersFragment extends Fragment {

    private static final String[] TAB_LABELS = {"Pending", "Completed", "Rejected"};

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
            case 1: return Constants.STATUS_COMPLETED.equals(order.status);
            case 2: return Constants.STATUS_REJECTED.equals(order.status);
            default:
                // "Pending" is every order still on its way: awaiting the
                // artist (incl. legacy "pending"), confirmed, or out for
                // delivery. The card's pill and tracking bar show which,
                // and update live without the order leaving this tab.
                return !OrderStatus.isSettled(order.status);
        }
    }
}
