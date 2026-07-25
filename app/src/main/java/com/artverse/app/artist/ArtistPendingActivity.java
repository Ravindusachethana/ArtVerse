package com.artverse.app.artist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.auth.LoginActivity;
import com.artverse.app.adapters.ArtworkAdapter;
import com.artverse.app.models.Artist;
import com.artverse.app.models.Artwork;
import com.artverse.app.utils.AuthActions;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Waiting room for artists whose registration has not been approved yet.
 * Shows the art-lover home listing in read-only mode: no bottom navigation,
 * and tapping an artwork does nothing but explain the restriction.
 *
 * A real-time listener on artists/{uid} watches the admin's decision - the
 * moment the web admin panel sets status to "approved", this screen routes
 * to the full artist experience automatically. A rejection flips the banner
 * into its rejected state.
 */
public class ArtistPendingActivity extends AppCompatActivity {

    private MaterialCardView cardStatus;
    private TextView tvStatusTitle, tvStatusMessage;
    private ProgressBar statusSpinner;
    private ImageView statusIcon;

    private SessionManager sessionManager;
    private ListenerRegistration statusListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_pending);

        sessionManager = new SessionManager(this);

        cardStatus = findViewById(R.id.cardStatus);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        statusSpinner = findViewById(R.id.statusSpinner);
        statusIcon = findViewById(R.id.statusIcon);

        findViewById(R.id.btnSignOut).setOnClickListener(v -> signOut());

        setupReadOnlyGallery();
        listenForApproval();

        if (Constants.ARTIST_STATUS_REJECTED.equals(sessionManager.getArtistStatus())) {
            showRejectedBanner();
        }
    }

    /** Same artwork grid the art-lover home shows, but with view-only taps. */
    private void setupReadOnlyGallery() {
        RecyclerView rvArtworks = findViewById(R.id.rvArtworks);
        View emptyState = findViewById(R.id.emptyState);

        ArtworkAdapter adapter = new ArtworkAdapter(artwork ->
                Toast.makeText(this, R.string.pending_read_only, Toast.LENGTH_SHORT).show());
        rvArtworks.setLayoutManager(new GridLayoutManager(this, 2));
        rvArtworks.setAdapter(adapter);

        FirebaseUtil.artworksRef()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || isFinishing()) return;

                    List<Artwork> artworks = new ArrayList<>();
                    for (var doc : snapshot.getDocuments()) {
                        Artwork artwork = doc.toObject(Artwork.class);
                        // Same visibility rule as the buyer gallery.
                        if (artwork != null && Artwork.isPublished(artwork)) {
                            artwork.id = doc.getId();
                            artworks.add(artwork);
                        }
                    }
                    adapter.submitList(artworks);
                    emptyState.setVisibility(artworks.isEmpty() ? View.VISIBLE : View.GONE);
                    rvArtworks.setVisibility(artworks.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void listenForApproval() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) {
            signOut();
            return;
        }

        statusListener = FirebaseUtil.artistsRef().document(uid)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || isFinishing()) return;

                    Artist artist = doc.toObject(Artist.class);
                    if (artist == null) return;

                    sessionManager.saveArtistStatus(artist.status);

                    if (Constants.ARTIST_STATUS_APPROVED.equals(artist.status)) {
                        Toast.makeText(this, R.string.approved_toast, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, ArtistMainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else if (Constants.ARTIST_STATUS_REJECTED.equals(artist.status)) {
                        showRejectedBanner();
                    }
                });
    }

    private void showRejectedBanner() {
        cardStatus.setStrokeColor(ContextCompat.getColor(this, R.color.status_rejected));
        tvStatusTitle.setText(R.string.rejected_banner_title);
        tvStatusMessage.setText(R.string.rejected_banner_message);
        statusSpinner.setVisibility(View.GONE);
        statusIcon.setVisibility(View.VISIBLE);
    }

    private void signOut() {
        AuthActions.signOut(this, () -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (statusListener != null) statusListener.remove();
        super.onDestroy();
    }
}
