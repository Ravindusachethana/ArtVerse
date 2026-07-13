package com.artverse.app.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.models.CartItem;
import com.artverse.app.models.Order;
import com.artverse.app.models.OrderItem;
import com.artverse.app.models.Transaction;
import com.artverse.app.models.User;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implements FR07 - Order Management Module and FR08 - Payment Module.
 * Reads the customer's cart, groups items by artist (an Order is scoped to
 * one artist per the ER design), and writes Order + Transaction records plus
 * the artwork stock decrement inside a single Firestore transaction so stock
 * is locked atomically - see placeOrder(). Clears the cart once that commits.
 * The payment step here is a confirmation-based simulation, as noted in the
 * dissertation's scope (Section 1.4) - swap sendOrder() for a real gateway
 * callback once one is integrated.
 */
public class CheckoutActivity extends AppCompatActivity {

    private TextInputEditText etAddress;
    private RadioGroup radioGroupPayment;
    private LinearLayout orderSummaryContainer;
    private TextView tvTotal;
    private View progressBar, btnPlaceOrder;

    private final List<CartItem> cartItems = new ArrayList<>();
    private double total = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        etAddress = findViewById(R.id.etAddress);
        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        orderSummaryContainer = findViewById(R.id.orderSummaryContainer);
        tvTotal = findViewById(R.id.tvTotal);
        progressBar = findViewById(R.id.progressBar);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        prefillAddress();
        loadCart();
    }

    private void prefillAddress() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;
        FirebaseUtil.usersRef().document(uid).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null && user.address != null) etAddress.setText(user.address);
        });
    }

    private void loadCart() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.cartRef(uid).get().addOnSuccessListener(snapshot -> {
            cartItems.clear();
            orderSummaryContainer.removeAllViews();
            total = 0;

            for (var doc : snapshot.getDocuments()) {
                CartItem item = doc.toObject(CartItem.class);
                if (item == null) continue;
                cartItems.add(item);
                total += item.lineTotal();
                addSummaryRow(item);
            }

            NumberFormat format = NumberFormat.getInstance(Locale.US);
            tvTotal.setText("LKR " + format.format(total));

            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void addSummaryRow(CartItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvName = new TextView(this);
        tvName.setText(item.title + "  ×" + item.quantity);
        tvName.setTextColor(getColor(R.color.text_primary));
        tvName.setTextSize(14);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(tvName, nameParams);

        TextView tvLineTotal = new TextView(this);
        NumberFormat format = NumberFormat.getInstance(Locale.US);
        tvLineTotal.setText("LKR " + format.format(item.lineTotal()));
        tvLineTotal.setTextColor(getColor(R.color.text_secondary));
        tvLineTotal.setTextSize(14);
        row.addView(tvLineTotal);

        orderSummaryContainer.addView(row);
    }

    private void placeOrder() {
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        if (address.isEmpty()) {
            etAddress.setError("Delivery address is required");
            return;
        }
        if (cartItems.isEmpty()) return;

        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        String paymentMethod = radioGroupPayment.getCheckedRadioButtonId() == R.id.radioCash
                ? "Cash on Delivery" : "Credit / Debit Card";

        setLoading(true);
        String customerName = new SessionManager(this).getName();

        // Group cart items by artist - the ER model scopes one Order to one Artist.
        Map<String, List<CartItem>> byArtist = new HashMap<>();
        for (CartItem item : cartItems) {
            byArtist.computeIfAbsent(item.artistId, k -> new ArrayList<>()).add(item);
        }

        long now = System.currentTimeMillis();

        // Runs as a single transaction so the stock check-and-decrement for every
        // artwork is atomic: if two customers race for the last piece, only the
        // first transaction to commit wins and the second is aborted below with
        // an up-to-date "left in stock" message rather than overselling.
        FirebaseUtil.db().runTransaction(transaction -> {
            Map<String, Long> remainingStock = new HashMap<>();
            for (CartItem item : cartItems) {
                DocumentReference artworkRef = FirebaseUtil.artworksRef().document(item.artworkId);
                DocumentSnapshot snapshot = transaction.get(artworkRef);
                Long remaining = snapshot.getLong("quantity");
                if (remaining == null || remaining < item.quantity) {
                    throw new FirebaseFirestoreException(
                            "Only " + (remaining == null ? 0 : remaining) + " left of \"" + item.title
                                    + "\" - please update the quantity in your cart.",
                            FirebaseFirestoreException.Code.ABORTED);
                }
                remainingStock.put(item.artworkId, remaining);
            }

            for (Map.Entry<String, List<CartItem>> entry : byArtist.entrySet()) {
                String artistId = entry.getKey();
                List<CartItem> artistItems = entry.getValue();

                List<OrderItem> orderItems = new ArrayList<>();
                double artistTotal = 0;
                for (CartItem ci : artistItems) {
                    orderItems.add(new OrderItem(ci.artworkId, ci.title, ci.imageUrl, ci.price, ci.quantity));
                    artistTotal += ci.lineTotal();
                }

                DocumentReference orderRef = FirebaseUtil.ordersRef().document();
                Order order = new Order(orderRef.getId(), uid, customerName, artistId, address,
                        orderItems, artistTotal, Constants.STATUS_PENDING, paymentMethod, now);
                transaction.set(orderRef, order);

                for (CartItem ci : artistItems) {
                    DocumentReference txRef = FirebaseUtil.transactionsRef().document();
                    String invoice = "INV-" + now + "-" + ci.artworkId.substring(0, Math.min(5, ci.artworkId.length()));
                    Transaction tx = new Transaction(txRef.getId(), orderRef.getId(), ci.artworkId, artistId,
                            invoice, ci.quantity, ci.price, ci.lineTotal(), now);
                    transaction.set(txRef, tx);

                    // Lock in the sale: decrement remaining stock and flip
                    // availability off once the last piece is sold.
                    long newQuantity = remainingStock.get(ci.artworkId) - ci.quantity;
                    DocumentReference artworkRef = FirebaseUtil.artworksRef().document(ci.artworkId);
                    Map<String, Object> update = new HashMap<>();
                    update.put("quantity", newQuantity);
                    update.put("available", newQuantity > 0);
                    transaction.update(artworkRef, update);
                }

                DocumentReference artistRef = FirebaseUtil.artistsRef().document(artistId);
                transaction.update(artistRef, "totalSales", FieldValue.increment(artistTotal));
            }
            return null;
        }).addOnSuccessListener(v -> clearCartAndFinish(uid))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage() != null ? e.getMessage()
                            : "Could not place order", Toast.LENGTH_LONG).show();
                });
    }

    private void clearCartAndFinish(String uid) {
        FirebaseUtil.cartRef(uid).get().addOnSuccessListener(snapshot -> {
            WriteBatch clearBatch = FirebaseUtil.db().batch();
            for (var doc : snapshot.getDocuments()) clearBatch.delete(doc.getReference());
            clearBatch.commit().addOnCompleteListener(t -> {
                setLoading(false);
                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPlaceOrder.setEnabled(!loading);
    }
}
