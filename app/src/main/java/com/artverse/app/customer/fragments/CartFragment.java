package com.artverse.app.customer.fragments;

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
import com.artverse.app.adapters.CartAdapter;
import com.artverse.app.customer.CheckoutActivity;
import com.artverse.app.models.CartItem;
import com.artverse.app.utils.FirebaseUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cart Management Module: add/update/remove items before checkout. */
public class CartFragment extends Fragment {

    private RecyclerView rvCart;
    private View emptyState, checkoutBar;
    private TextView tvCartTotal;
    private CartAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCart = view.findViewById(R.id.rvCart);
        emptyState = view.findViewById(R.id.emptyState);
        checkoutBar = view.findViewById(R.id.checkoutBar);
        tvCartTotal = view.findViewById(R.id.tvCartTotal);

        adapter = new CartAdapter(new CartAdapter.CartActionListener() {
            @Override
            public void onQuantityChanged(CartItem item, int newQuantity) {
                updateQuantity(item, newQuantity);
            }

            @Override
            public void onRemove(CartItem item) {
                removeItem(item);
            }
        });
        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCart.setAdapter(adapter);

        view.findViewById(R.id.btnCheckout).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CheckoutActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.cartRef(uid).addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || getContext() == null) return;

            List<CartItem> items = new ArrayList<>();
            double total = 0;
            for (var doc : snapshot.getDocuments()) {
                CartItem item = doc.toObject(CartItem.class);
                if (item != null) {
                    items.add(item);
                    total += item.lineTotal();
                }
            }
            adapter.submitList(items);

            boolean empty = items.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvCart.setVisibility(empty ? View.GONE : View.VISIBLE);
            checkoutBar.setVisibility(empty ? View.GONE : View.VISIBLE);

            NumberFormat format = NumberFormat.getInstance(Locale.US);
            tvCartTotal.setText("LKR " + format.format(total));
        });
    }

    private void updateQuantity(CartItem item, int newQuantity) {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;
        item.quantity = newQuantity;
        FirebaseUtil.cartRef(uid).document(item.artworkId).set(item);
    }

    private void removeItem(CartItem item) {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;
        FirebaseUtil.cartRef(uid).document(item.artworkId).delete();
    }
}
