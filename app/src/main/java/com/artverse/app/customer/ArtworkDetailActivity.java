package com.artverse.app.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.models.Artwork;
import com.artverse.app.models.CartItem;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Shows a single artwork (FR05 detail view) and lets a signed-in customer
 * add it to their cart (FR06) or jump straight to checkout (FR07/FR08).
 */
public class ArtworkDetailActivity extends AppCompatActivity {

    private Artwork currentArtwork;
    private TextView tvTitle, tvArtist, tvPrice, tvCategory, tvMedium, tvDimensions, tvDescription;
    private ImageView ivArtwork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artwork_detail);

        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvPrice = findViewById(R.id.tvPrice);
        tvCategory = findViewById(R.id.tvCategory);
        tvMedium = findViewById(R.id.tvMedium);
        tvDimensions = findViewById(R.id.tvDimensions);
        tvDescription = findViewById(R.id.tvDescription);
        ivArtwork = findViewById(R.id.ivArtwork);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> addToCart(false));
        findViewById(R.id.btnBuyNow).setOnClickListener(v -> addToCart(true));

        String artworkId = getIntent().getStringExtra(Constants.EXTRA_ARTWORK_ID);
        if (artworkId != null) loadArtwork(artworkId);
    }

    private void loadArtwork(String artworkId) {
        FirebaseUtil.artworksRef().document(artworkId).get()
                .addOnSuccessListener(doc -> {
                    Artwork artwork = doc.toObject(Artwork.class);
                    if (artwork == null) return;
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
    }

    private void addToCart(boolean goToCheckout) {
        if (currentArtwork == null) return;
        String uid = FirebaseUtil.currentUid();
        if (uid == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentArtwork.available) {
            Toast.makeText(this, "This artwork is no longer available", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageUrl = (currentArtwork.imageUrls != null && !currentArtwork.imageUrls.isEmpty())
                ? currentArtwork.imageUrls.get(0) : null;

        CartItem item = new CartItem(currentArtwork.id, currentArtwork.title, imageUrl,
                currentArtwork.artistId, currentArtwork.artistName, currentArtwork.price, 1);

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
