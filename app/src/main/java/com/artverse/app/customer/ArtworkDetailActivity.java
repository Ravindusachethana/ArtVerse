package com.artverse.app.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.models.Artwork;
import com.artverse.app.models.CartItem;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.QuantitySelector;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Shows a single artwork (FR05 detail view) and lets a signed-in customer
 * add it to their cart (FR06) or jump straight to checkout (FR07/FR08).
 * The quantity stepper is capped to the artwork's live remaining stock
 * ("quantity" in Firestore) - the actual stock lock happens atomically at
 * checkout (see CheckoutActivity), this is just a client-side hint.
 */
public class ArtworkDetailActivity extends AppCompatActivity {

    private Artwork currentArtwork;
    private int selectedQuantity = 1;

    private TextView tvTitle, tvArtist, tvPrice, tvStock, tvCategory, tvMedium, tvDimensions,
            tvDescription, tvQtyValue, tvOrderTotal, tvSoldOut, tvCartBadge;
    private ImageView ivArtwork;
    private View quantityRow, orderTotalRow, actionButtonsRow, btnQtyMinus, btnQtyPlus, cartShortcut;

    private ListenerRegistration cartListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artwork_detail);

        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvPrice = findViewById(R.id.tvPrice);
        tvStock = findViewById(R.id.tvStock);
        tvCategory = findViewById(R.id.tvCategory);
        tvMedium = findViewById(R.id.tvMedium);
        tvDimensions = findViewById(R.id.tvDimensions);
        tvDescription = findViewById(R.id.tvDescription);
        tvQtyValue = findViewById(R.id.tvQtyValue);
        tvOrderTotal = findViewById(R.id.tvOrderTotal);
        tvSoldOut = findViewById(R.id.tvSoldOut);
        ivArtwork = findViewById(R.id.ivArtwork);
        quantityRow = findViewById(R.id.quantityRow);
        orderTotalRow = findViewById(R.id.orderTotalRow);
        actionButtonsRow = findViewById(R.id.actionButtonsRow);
        btnQtyMinus = findViewById(R.id.btnQtyMinus);
        btnQtyPlus = findViewById(R.id.btnQtyPlus);
        cartShortcut = findViewById(R.id.cartShortcut);
        tvCartBadge = findViewById(R.id.tvCartBadge);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> addToCart(false));
        findViewById(R.id.btnBuyNow).setOnClickListener(v -> addToCart(true));
        cartShortcut.setOnClickListener(v -> openCart());
        btnQtyMinus.setOnClickListener(v -> changeQuantity(-1));
        btnQtyPlus.setOnClickListener(v -> changeQuantity(1));

        observeCart();

        String artworkId = getIntent().getStringExtra(Constants.EXTRA_ARTWORK_ID);
        if (artworkId != null) loadArtwork(artworkId);
    }

    /**
     * Keeps the cart shortcut in sync with the customer's cart in real time:
     * it stays hidden while the cart is empty and appears (with the item
     * count) the moment something is added - including the add that happens
     * on this very screen.
     */
    private void observeCart() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        cartListener = FirebaseUtil.cartRef(uid).addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || isFinishing()) return;

            int itemCount = snapshot.size();
            boolean wasHidden = cartShortcut.getVisibility() != View.VISIBLE;

            cartShortcut.setVisibility(itemCount > 0 ? View.VISIBLE : View.GONE);
            tvCartBadge.setText(itemCount > 99 ? "99+" : String.valueOf(itemCount));

            // Small pop so the button is noticed when it first shows up.
            if (itemCount > 0 && wasHidden) {
                cartShortcut.setScaleX(0.6f);
                cartShortcut.setScaleY(0.6f);
                cartShortcut.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
            }
        });
    }

    /** Opens the Cart tab, reusing the existing customer screen underneath. */
    private void openCart() {
        Intent intent = new Intent(this, CustomerMainActivity.class);
        intent.putExtra(Constants.EXTRA_OPEN_CART, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (cartListener != null) cartListener.remove();
        super.onDestroy();
    }

    private void loadArtwork(String artworkId) {
        FirebaseUtil.artworksRef().document(artworkId).get()
                .addOnSuccessListener(doc -> {
                    Artwork artwork = doc.toObject(Artwork.class);
                    if (artwork == null) return;
                    // Unpublished art (awaiting/failed review) is only listed
                    // for its owner; block direct opens for everyone else.
                    if (!Artwork.isPublished(artwork)
                            && !java.util.Objects.equals(artwork.artistId, FirebaseUtil.currentUid())) {
                        Toast.makeText(this, "This artwork is not available", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    artwork.id = doc.getId();
                    currentArtwork = artwork;
                    bind(artwork);
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void bind(Artwork artwork) {
        tvTitle.setText(artwork.title);
        tvArtist.setText("by " + artwork.artistName);
        tvCategory.setText(artwork.categoryName);
        tvMedium.setText(artwork.medium != null ? artwork.medium : "—");
        tvDimensions.setText(artwork.dimensions != null ? artwork.dimensions : "—");
        tvDescription.setText(artwork.description);

        NumberFormat format = NumberFormat.getInstance(Locale.US);
        tvPrice.setText("LKR " + format.format(artwork.price));

        String imageUrl = (artwork.imageUrls != null && !artwork.imageUrls.isEmpty())
                ? artwork.imageUrls.get(0) : null;
        Glide.with(this).load(imageUrl)
                .placeholder(R.drawable.ph_artwork)
                .error(R.drawable.ph_artwork)
                .into(ivArtwork);

        boolean inStock = artwork.available && artwork.quantity > 0;
        tvStock.setText(artwork.quantity + (artwork.quantity == 1 ? " piece available" : " pieces available"));
        tvStock.setVisibility(inStock ? View.VISIBLE : View.GONE);

        quantityRow.setVisibility(inStock ? View.VISIBLE : View.GONE);
        orderTotalRow.setVisibility(inStock ? View.VISIBLE : View.GONE);
        actionButtonsRow.setVisibility(inStock ? View.VISIBLE : View.GONE);
        tvSoldOut.setVisibility(inStock ? View.GONE : View.VISIBLE);

        selectedQuantity = 1;
        updateQuantityUi();
    }

    private void changeQuantity(int delta) {
        if (currentArtwork == null) return;
        selectedQuantity = QuantitySelector.nextQuantity(selectedQuantity, delta, currentArtwork.quantity);
        updateQuantityUi();
    }

    private void updateQuantityUi() {
        if (currentArtwork == null) return;
        tvQtyValue.setText(String.valueOf(selectedQuantity));
        boolean canDecrease = QuantitySelector.canDecrease(selectedQuantity);
        boolean canIncrease = QuantitySelector.canIncrease(selectedQuantity, currentArtwork.quantity);
        btnQtyMinus.setEnabled(canDecrease);
        btnQtyMinus.setAlpha(canDecrease ? 1f : 0.35f);
        btnQtyPlus.setEnabled(canIncrease);
        btnQtyPlus.setAlpha(canIncrease ? 1f : 0.35f);

        NumberFormat format = NumberFormat.getInstance(Locale.US);
        tvOrderTotal.setText("LKR " + format.format(QuantitySelector.lineTotal(currentArtwork.price, selectedQuantity)));
    }

    private void addToCart(boolean goToCheckout) {
        if (currentArtwork == null) return;
        String uid = FirebaseUtil.currentUid();
        if (uid == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentArtwork.available || currentArtwork.quantity <= 0) {
            Toast.makeText(this, "This artwork is no longer available", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedQuantity > currentArtwork.quantity) {
            Toast.makeText(this, "Only " + currentArtwork.quantity + " left in stock", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageUrl = (currentArtwork.imageUrls != null && !currentArtwork.imageUrls.isEmpty())
                ? currentArtwork.imageUrls.get(0) : null;

        CartItem item = new CartItem(currentArtwork.id, currentArtwork.title, imageUrl,
                currentArtwork.artistId, currentArtwork.artistName, currentArtwork.price, selectedQuantity);

        FirebaseUtil.cartRef(uid).document(currentArtwork.id).set(item)
                .addOnSuccessListener(v -> {
                    if (goToCheckout) {
                        Intent intent = new Intent(this, CheckoutActivity.class);
                        startActivity(intent);
                    } else {
                        // The cart shortcut in the header appears via the
                        // snapshot listener, so no navigation is needed here.
                        Toast.makeText(this, R.string.added_to_cart, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
