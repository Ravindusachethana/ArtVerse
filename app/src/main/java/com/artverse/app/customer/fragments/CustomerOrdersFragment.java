package com.artverse.app.customer.fragments;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.artverse.app.R;
import com.artverse.app.adapters.OrderAdapter;
import com.artverse.app.models.Order;
import com.artverse.app.utils.FirebaseUtil;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer-facing order history - reads all orders placed by this customer,
 * across every artist, with live status updates (part of FR07).
 */
public class CustomerOrdersFragment extends Fragment {

    private RecyclerView rvList;
    private View emptyState;
    private OrderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_generic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.tvScreenTitle)).setText(R.string.nav_orders);
        ((TextView) view.findViewById(R.id.tvEmptyMessage)).setText("No orders yet");

        rvList = view.findViewById(R.id.rvList);
        emptyState = view.findViewById(R.id.emptyState);
        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setEnabled(false);

        adapter = new OrderAdapter();
        rvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvList.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.ordersRef()
                .whereEqualTo("customerId", uid)
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
}
