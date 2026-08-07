package com.artverse.app.customer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.CollectionAdapter;
import com.artverse.app.models.Artwork;
import com.artverse.app.models.CollectionEntry;
import com.artverse.app.models.Order;
import com.artverse.app.models.OrderItem;
import com.artverse.app.utils.ArtworkDownloader;
import com.artverse.app.utils.CollectionAccess;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Art Lover's own gallery of everything they have bought, reached from
 * their profile. Pieces are grouped into category sections; digital ones
 * (Digital Art, Photography) can be opened at original quality and saved to
 * the device - see ArtworkViewerActivity and ArtworkDownloader.
 *
 * What counts as "owned" lives in CollectionAccess: a physical piece joins the
 * collection once its delivery is confirmed, a digital one as soon as the
 * artist accepts the order, since there is nothing to ship.
 */
public class MyCollectionActivity extends AppCompatActivity
        implements CollectionAdapter.CollectionActionListener {

    private static final int GRID_SPANS = 2;
    private static final int REQ_WRITE_STORAGE = 82;

    private RecyclerView rvCollection;
    private View emptyState, progressBar;
    private TextView tvCollectionCount;
    private CollectionAdapter adapter;

    /** Piece waiting on the storage permission prompt (Android 9 and below). */
    private CollectionEntry pendingDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_collection);

        rvCollection = findViewById(R.id.rvCollection);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);
        tvCollectionCount = findViewById(R.id.tvCollectionCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new CollectionAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, GRID_SPANS);
        layoutManager.setSpanSizeLookup(adapter.spanSizeLookup(GRID_SPANS));
        rvCollection.setLayoutManager(layoutManager);
        rvCollection.setAdapter(adapter);

        loadCollection();
    }

    private void loadCollection() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) {
            finish();
            return;
        }

        // Equality filter only, sorted in memory - same shape as the orders
        // screen, so no composite index is ever needed.
        FirebaseUtil.ordersRef().whereEqualTo("customerId", uid).get()
                .addOnSuccessListener(snapshot -> {
                    List<CollectionEntry> candidates = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order == null || order.items == null) continue;
                        if (Constants.STATUS_REJECTED.equals(order.status)) continue;
                        order.id = doc.getId();

                        for (OrderItem item : order.items) {
                            candidates.add(toEntry(order, item));
                        }
                    }
                    fillMissingCategories(candidates);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load your collection: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private CollectionEntry toEntry(Order order, OrderItem item) {
        CollectionEntry entry = new CollectionEntry();
        entry.artworkId = item.artworkId;
        entry.title = item.title;
        entry.categoryName = item.categoryName;
        entry.imageUrl = item.imageUrl;
        entry.orderId = order.id;
        entry.orderStatus = order.status;
        // When the piece reached the buyer: delivery for a physical one, the
        // artist's confirmation for a digital one, falling back to the order.
        if (order.deliveredAt > 0) {
            entry.purchasedAt = order.deliveredAt;
        } else if (order.confirmedAt > 0) {
            entry.purchasedAt = order.confirmedAt;
        } else {
            entry.purchasedAt = order.orderDate;
        }
        entry.quantity = item.quantity;
        entry.unitPrice = item.unitPrice;
        return entry;
    }

    /**
     * Orders placed before the category was recorded on the line have none, so
     * the listing is read for those pieces only - otherwise a digital piece
     * bought back then would be filed as uncategorised and lose its download.
     * Ownership can only be judged once every category is known.
     */
    private void fillMissingCategories(List<CollectionEntry> candidates) {
        Set<String> unknown = new LinkedHashSet<>();
        for (CollectionEntry entry : candidates) {
            if (entry.categoryName == null || entry.categoryName.trim().isEmpty()) {
                if (entry.artworkId != null) unknown.add(entry.artworkId);
            }
        }

        if (unknown.isEmpty()) {
            render(candidates);
            return;
        }

        List<Task<DocumentSnapshot>> lookups = new ArrayList<>();
        for (String artworkId : unknown) {
            lookups.add(FirebaseUtil.artworksRef().document(artworkId).get());
        }

        // Each snapshot carries its own id, so the results are matched by that
        // rather than by their position in the list.
        Tasks.whenAllComplete(lookups).addOnSuccessListener(results -> {
            for (Task<?> task : results) {
                if (!task.isSuccessful() || !(task.getResult() instanceof DocumentSnapshot doc)) continue;

                Artwork artwork = doc.toObject(Artwork.class);
                if (artwork == null || artwork.categoryName == null) continue;

                for (CollectionEntry entry : candidates) {
                    if (doc.getId().equals(entry.artworkId)
                            && (entry.categoryName == null || entry.categoryName.trim().isEmpty())) {
                        entry.categoryName = artwork.categoryName;
                    }
                }
            }
            render(candidates);
        }).addOnFailureListener(e -> render(candidates));
    }

    private void render(List<CollectionEntry> candidates) {
        if (isFinishing()) return;

        List<CollectionEntry> owned = new ArrayList<>();
        Set<String> categories = new HashSet<>();
        for (CollectionEntry entry : candidates) {
            if (!CollectionAccess.isOwned(entry.orderStatus, entry.categoryName)) continue;
            entry.downloadable = CollectionAccess.isDownloadable(entry.orderStatus, entry.categoryName);
            owned.add(entry);
            // Counted the way the grid groups them, so the two always agree.
            categories.add(CollectionAdapter.sectionOf(entry.categoryName));
        }

        progressBar.setVisibility(View.GONE);
        adapter.submitEntries(owned);

        boolean empty = owned.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvCollection.setVisibility(empty ? View.GONE : View.VISIBLE);

        tvCollectionCount.setText(empty ? "" : summaryOf(owned.size(), categories.size()));
    }

    private String summaryOf(int pieceCount, int categoryCount) {
        String pieces = getString(pieceCount == 1
                ? R.string.collection_pieces : R.string.collection_pieces_plural, pieceCount);
        return pieces + " · " + categoryCount + (categoryCount == 1 ? " category" : " categories");
    }

    @Override
    public void onOpen(CollectionEntry entry) {
        Intent intent = new Intent(this, ArtworkViewerActivity.class);
        intent.putExtra(Constants.EXTRA_ARTWORK_ID, entry.artworkId);
        intent.putExtra(Constants.EXTRA_ARTWORK_TITLE, entry.title);
        intent.putExtra(Constants.EXTRA_ARTWORK_IMAGE_URL, entry.imageUrl);
        intent.putExtra(Constants.EXTRA_ALLOW_DOWNLOAD, entry.downloadable);
        startActivity(intent);
    }

    @Override
    public void onDownload(CollectionEntry entry) {
        if (!entry.downloadable) return;

        // Android 9 and below save into the public Pictures folder, which needs
        // the storage permission; 10+ goes through MediaStore without one.
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            pendingDownload = entry;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_STORAGE);
            return;
        }

        Toast.makeText(this, getString(R.string.download_started, entry.title),
                Toast.LENGTH_SHORT).show();

        ArtworkDownloader.saveToGallery(this, entry.imageUrl, entry.title,
                new ArtworkDownloader.Callback() {
                    @Override
                    public void onSaved(String fileName) {
                        if (isFinishing()) return;
                        Toast.makeText(MyCollectionActivity.this,
                                getString(R.string.download_saved, fileName), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailed(String message) {
                        if (isFinishing()) return;
                        Toast.makeText(MyCollectionActivity.this,
                                getString(R.string.download_failed, message), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_WRITE_STORAGE) return;

        CollectionEntry entry = pendingDownload;
        pendingDownload = null;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (entry != null) onDownload(entry);
        } else {
            Toast.makeText(this, R.string.download_permission_needed, Toast.LENGTH_LONG).show();
        }
    }
}
