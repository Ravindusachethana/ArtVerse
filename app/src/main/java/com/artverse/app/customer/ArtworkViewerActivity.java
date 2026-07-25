package com.artverse.app.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.ArtworkPagerAdapter;
import com.artverse.app.models.Artwork;
import com.artverse.app.utils.ArtworkDownloader;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen look at a piece the Art Lover owns, opened from My Collection.
 *
 * It starts from the image recorded on the order line so something is on
 * screen straight away, then swaps in the live listing's files if the artist
 * uploaded more than one. For a digital piece the Download button saves the
 * artist's original file to the device's gallery; for a physical one the
 * button stays hidden and the screen is simply a large view of the piece.
 */
public class ArtworkViewerActivity extends AppCompatActivity {

    private static final int REQ_WRITE_STORAGE = 83;

    private RecyclerView rvPages;
    private View progressBar, btnDownload;
    private TextView tvTitle, tvSubtitle, tvPageCounter;

    private ArtworkPagerAdapter adapter;
    private final PagerSnapHelper snapHelper = new PagerSnapHelper();

    private String artworkTitle;
    private boolean allowDownload;
    private boolean chromeVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artwork_viewer);

        rvPages = findViewById(R.id.rvPages);
        progressBar = findViewById(R.id.progressBar);
        btnDownload = findViewById(R.id.btnDownload);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvPageCounter = findViewById(R.id.tvPageCounter);

        artworkTitle = getIntent().getStringExtra(Constants.EXTRA_ARTWORK_TITLE);
        allowDownload = getIntent().getBooleanExtra(Constants.EXTRA_ALLOW_DOWNLOAD, false);
        String artworkId = getIntent().getStringExtra(Constants.EXTRA_ARTWORK_ID);
        String fallbackImage = getIntent().getStringExtra(Constants.EXTRA_ARTWORK_IMAGE_URL);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        btnDownload.setOnClickListener(v -> download());
        btnDownload.setVisibility(allowDownload ? View.VISIBLE : View.GONE);

        tvTitle.setText(artworkTitle != null ? artworkTitle : "");
        tvSubtitle.setText(allowDownload
                ? getString(R.string.viewer_subtitle_digital)
                : getString(R.string.viewer_subtitle_physical));

        adapter = new ArtworkPagerAdapter(this::toggleChrome);
        rvPages.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvPages.setAdapter(adapter);
        snapHelper.attachToRecyclerView(rvPages);
        rvPages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) updatePageCounter();
            }
        });

        List<String> images = new ArrayList<>();
        if (fallbackImage != null && !fallbackImage.isEmpty()) images.add(fallbackImage);
        showImages(images);

        if (artworkId != null) {
            loadFullResolutionImages(artworkId, fallbackImage);
        } else {
            // Nothing left to wait for - never leave the spinner running.
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * The order line only keeps the cover image. The listing may hold several
     * files, so it is read here - quietly, since what is already on screen is
     * a valid view of the piece if the listing has since been removed.
     */
    private void loadFullResolutionImages(String artworkId, String fallbackImage) {
        FirebaseUtil.artworksRef().document(artworkId).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing()) return;
                    progressBar.setVisibility(View.GONE);

                    Artwork artwork = doc.toObject(Artwork.class);
                    if (artwork == null || artwork.imageUrls == null || artwork.imageUrls.isEmpty()) return;

                    List<String> images = new ArrayList<>(artwork.imageUrls);
                    // Keep the page already on screen first so the view does
                    // not jump to a different image as the listing arrives.
                    if (fallbackImage != null && images.remove(fallbackImage)) {
                        images.add(0, fallbackImage);
                    }
                    showImages(images);
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing()) progressBar.setVisibility(View.GONE);
                });
    }

    private void showImages(List<String> images) {
        adapter.submitImages(images);
        progressBar.setVisibility(images.isEmpty() ? View.VISIBLE : View.GONE);
        tvPageCounter.setVisibility(images.size() > 1 ? View.VISIBLE : View.GONE);
        updatePageCounter();
    }

    private void updatePageCounter() {
        int total = adapter.getItemCount();
        if (total <= 1) {
            tvPageCounter.setVisibility(View.GONE);
            return;
        }
        tvPageCounter.setVisibility(chromeVisible ? View.VISIBLE : View.GONE);
        tvPageCounter.setText((currentPage() + 1) + " / " + total);
    }

    /** Index of the page snapped into view, 0 when nothing is settled yet. */
    private int currentPage() {
        RecyclerView.LayoutManager layoutManager = rvPages.getLayoutManager();
        if (layoutManager == null) return 0;
        View snapped = snapHelper.findSnapView(layoutManager);
        if (snapped == null) return 0;
        int position = layoutManager.getPosition(snapped);
        return position == RecyclerView.NO_POSITION ? 0 : position;
    }

    /** Tapping the image hides the overlay so the artwork can be seen whole. */
    private void toggleChrome() {
        chromeVisible = !chromeVisible;
        int visibility = chromeVisible ? View.VISIBLE : View.GONE;
        findViewById(R.id.btnClose).setVisibility(visibility);
        findViewById(R.id.bottomBar).setVisibility(visibility);
        updatePageCounter();
    }

    private void download() {
        if (!allowDownload) return;

        String imageUrl = adapter.imageAt(currentPage());
        if (imageUrl == null) {
            Toast.makeText(this, R.string.download_nothing_to_save, Toast.LENGTH_SHORT).show();
            return;
        }

        // Android 9 and below save into the public Pictures folder, which needs
        // the storage permission; 10+ goes through MediaStore without one.
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_STORAGE);
            return;
        }

        btnDownload.setEnabled(false);
        Toast.makeText(this, getString(R.string.download_started, artworkTitle), Toast.LENGTH_SHORT).show();

        ArtworkDownloader.saveToGallery(this, imageUrl, artworkTitle, new ArtworkDownloader.Callback() {
            @Override
            public void onSaved(String fileName) {
                if (isFinishing()) return;
                btnDownload.setEnabled(true);
                Toast.makeText(ArtworkViewerActivity.this,
                        getString(R.string.download_saved, fileName), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailed(String message) {
                if (isFinishing()) return;
                btnDownload.setEnabled(true);
                Toast.makeText(ArtworkViewerActivity.this,
                        getString(R.string.download_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_WRITE_STORAGE) return;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            download();
        } else {
            Toast.makeText(this, R.string.download_permission_needed, Toast.LENGTH_LONG).show();
        }
    }
}
