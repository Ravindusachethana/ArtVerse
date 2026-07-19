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
            tvDescription, tvQtyValue, tvOrderTotal, tvSoldOut;
    private ImageView ivArtwork;
    private View quantityRow, orderTotalRow, actionButtonsRow, btnQtyMinus, btnQtyPlus;

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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> addToCart(false));
        findViewById(R.id.btnBuyNow).setOnClickListener(v -> addToCart(true));
        btnQtyMinus.setOnClickListener(v -> changeQuantity(-1));
        btnQtyPlus.setOnClickListener(v -> changeQuantity(1));

        String artworkId = getIntent().getStringExtra(Constants.EXTRA_ARTWORK_ID);
        if (artworkId != null) loadArtwork(artworkId);
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
                        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
