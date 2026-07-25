package com.artverse.app.customer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.ArtworkImageCarouselAdapter;
import com.artverse.app.models.Artwork;
import com.artverse.app.models.CartItem;
import com.artverse.app.utils.ArtCategories;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.QuantitySelector;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Shows a single artwork (FR05 detail view) and lets a signed-in customer
 * add it to their cart (FR06) or jump straight to checkout (FR07/FR08).
 * The quantity stepper is capped to the artwork's live remaining stock
 * ("quantity" in Firestore) - the actual stock lock happens atomically at
 * checkout (see CheckoutActivity), this is just a client-side hint.
 *
 * The quantity stepper only appears for reproducible pieces (Sculpture,
 * Ceramics, Printmaking), which the artist can remake to order. A one-time
 * original (Painting, Drawing, Mixed Media) and a digital file (Digital Art,
 * Photography) are each a single copy, so they show an explanatory note in
 * place of the stepper instead - see ArtCategories.isReproducible.
 */
public class ArtworkDetailActivity extends AppCompatActivity {

    private Artwork currentArtwork;
    private int selectedQuantity = 1;

    private TextView tvTitle, tvArtist, tvPrice, tvStock, tvCategory, tvMedium, tvDimensions,
            tvDescription, tvQtyValue, tvOrderTotal, tvSoldOut, tvCartBadge,
            tvSingleCopyTitle, tvSingleCopyMessage;
    private ImageView ivSingleCopyIcon;
    private View quantityRow, orderTotalRow, actionButtonsRow, btnQtyMinus, btnQtyPlus,
            cartShortcut, singleCopyNote;

    private RecyclerView rvImages;
    private LinearLayout imageDots;
    private ArtworkImageCarouselAdapter imageAdapter;
    private final PagerSnapHelper imageSnapHelper = new PagerSnapHelper();

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
        setupImageCarousel();
        quantityRow = findViewById(R.id.quantityRow);
        singleCopyNote = findViewById(R.id.singleCopyNote);
        ivSingleCopyIcon = findViewById(R.id.ivSingleCopyIcon);
        tvSingleCopyTitle = findViewById(R.id.tvSingleCopyTitle);
        tvSingleCopyMessage = findViewById(R.id.tvSingleCopyMessage);
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

        bindImages(artwork.imageUrls);

        boolean inStock = artwork.available && artwork.quantity > 0;
        // Only reproducible pieces are sold in multiples; the remaining-stock
        // line and the stepper belong to them. A single-copy piece - a one-time
        // original or a digital file - shows an explanatory note instead.
        boolean reproducible = ArtCategories.isReproducible(artwork.categoryName);

        tvStock.setText(artwork.quantity + (artwork.quantity == 1 ? " piece available" : " pieces available"));
        tvStock.setVisibility(inStock && reproducible ? View.VISIBLE : View.GONE);

        quantityRow.setVisibility(inStock && reproducible ? View.VISIBLE : View.GONE);

        boolean singleCopy = inStock && !reproducible;
        singleCopyNote.setVisibility(singleCopy ? View.VISIBLE : View.GONE);
        if (singleCopy) bindSingleCopyNote(ArtCategories.isDigital(artwork.categoryName));

        orderTotalRow.setVisibility(inStock ? View.VISIBLE : View.GONE);
        actionButtonsRow.setVisibility(inStock ? View.VISIBLE : View.GONE);
        tvSoldOut.setVisibility(inStock ? View.GONE : View.VISIBLE);

        selectedQuantity = 1;
        updateQuantityUi();
    }

    /**
     * The note that stands in for the stepper on single-copy pieces: a digital
     * one is delivered and downloadable in the app, a physical original is
     * one-of-a-kind.
     */
    private void bindSingleCopyNote(boolean digital) {
        ivSingleCopyIcon.setImageResource(digital ? R.drawable.ic_download : R.drawable.ic_palette);
        tvSingleCopyTitle.setText(digital ? R.string.digital_delivery_title : R.string.unique_piece_title);
        tvSingleCopyMessage.setText(digital ? R.string.digital_delivery_message : R.string.unique_piece_message);
    }

    private void setupImageCarousel() {
        rvImages = findViewById(R.id.rvImages);
        imageDots = findViewById(R.id.imageDots);
        imageAdapter = new ArtworkImageCarouselAdapter(null);
        rvImages.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvImages.setAdapter(imageAdapter);
        imageSnapHelper.attachToRecyclerView(rvImages);
        rvImages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) updateActiveDot();
            }
        });
    }

    /**
     * Feeds the photo carousel. A listing with no image still gets one page so
     * the placeholder shows, exactly as the old single image view did.
     */
    private void bindImages(java.util.List<String> imageUrls) {
        java.util.List<String> pages = imageUrls != null && !imageUrls.isEmpty()
                ? imageUrls : java.util.Collections.singletonList(null);
        imageAdapter.submitImages(pages);
        rvImages.scrollToPosition(0);
        buildDots(pages.size());
    }

    private void buildDots(int count) {
        imageDots.removeAllViews();
        // A single photo needs no pager dots - keep the frame clean.
        if (count <= 1) {
            imageDots.setVisibility(View.GONE);
            return;
        }
        imageDots.setVisibility(View.VISIBLE);
        int size = dp(7);
        int margin = dp(3);
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            dot.setBackground(shape);
            imageDots.addView(dot);
        }
        updateActiveDot();
    }

    private void updateActiveDot() {
        int count = imageDots.getChildCount();
        if (count == 0) return;
        int active = currentImagePage();
        for (int i = 0; i < count; i++) {
            GradientDrawable shape = (GradientDrawable) imageDots.getChildAt(i).getBackground();
            // White reads on any photo; the resting dots are a translucent white.
            shape.setColor(i == active ? Color.WHITE : Color.argb(120, 255, 255, 255));
        }
    }

    /** Index of the photo snapped into view, 0 before anything settles. */
    private int currentImagePage() {
        RecyclerView.LayoutManager layoutManager = rvImages.getLayoutManager();
        if (layoutManager == null) return 0;
        View snapped = imageSnapHelper.findSnapView(layoutManager);
        if (snapped == null) return 0;
        int position = layoutManager.getPosition(snapped);
        return position == RecyclerView.NO_POSITION ? 0 : position;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void changeQuantity(int delta) {
        if (currentArtwork == null || !ArtCategories.isReproducible(currentArtwork.categoryName)) return;
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
                currentArtwork.categoryName, currentArtwork.artistId, currentArtwork.artistName,
                currentArtwork.price, selectedQuantity);

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
