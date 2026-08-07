package com.artverse.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.artist.ArtistMainActivity;
import com.artverse.app.artist.ArtistPendingActivity;
import com.artverse.app.customer.CustomerMainActivity;
import com.artverse.app.models.Artist;
import com.artverse.app.models.User;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.PushTokens;
import com.artverse.app.utils.SessionManager;
import com.artverse.app.utils.ValidationUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private View progressBar, btnLogin;
    private SessionManager sessionManager;

    /** Deep link parked by SplashActivity when a push was tapped while signed out. */
    private boolean pendingOpenOrders;
    private String pendingOrderId;
    private String pendingTargetUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        pendingOpenOrders = Constants.ROUTE_ORDERS.equals(getIntent().getStringExtra(Constants.EXTRA_ROUTE));
        pendingOrderId = getIntent().getStringExtra(Constants.EXTRA_ORDER_ID);
        pendingTargetUid = getIntent().getStringExtra(Constants.EXTRA_TARGET_UID);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tvSignUp).setOnClickListener(v ->
                startActivity(new Intent(this, RoleSelectionActivity.class)));
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> sendPasswordReset());
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (!ValidationUtil.isValidEmail(email)) {
            tilEmail.setError("Enter a valid email address");
            return;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        FirebaseUtil.auth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> loadUserProfileAndRoute())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadUserProfileAndRoute() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) {
            setLoading(false);
            return;
        }
        FirebaseUtil.usersRef().document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    User user = doc.toObject(User.class);
                    if (user == null) {
                        Toast.makeText(this, "Profile not found. Please contact support.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    sessionManager.saveSession(uid, user.role, user.name);
                    // This device can now receive order pushes for the account.
                    PushTokens.register();
                    if (Constants.ROLE_ARTIST.equals(user.role)) {
                        routeArtistByApprovalStatus(uid);
                    } else {
                        routeTo(CustomerMainActivity.class);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Artists only enter their dashboard once an admin has approved the
     * registration; until then (or after a rejection) they land on the
     * read-only gallery. A missing status means the account predates the
     * approval feature and keeps its access.
     */
    private void routeArtistByApprovalStatus(String uid) {
        FirebaseUtil.artistsRef().document(uid).get()
                .addOnSuccessListener(doc -> {
                    Artist artist = doc.toObject(Artist.class);
                    String status = artist != null ? artist.status : null;
                    sessionManager.saveArtistStatus(status);

                    boolean approved = status == null
                            || Constants.ARTIST_STATUS_APPROVED.equals(status);
                    routeTo(approved ? ArtistMainActivity.class : ArtistPendingActivity.class);
                })
                .addOnFailureListener(e -> {
                    // Fail closed: without a readable status the artist waits
                    // on the pending screen, which keeps listening for it.
                    routeTo(ArtistPendingActivity.class);
                });
    }

    private void routeTo(Class<?> target) {
        setLoading(false);
        Intent intent = new Intent(this, target);

        // Replay a parked notification tap - but only into a real home screen,
        // and only when the account that just signed in is the one the alert
        // was addressed to.
        boolean sameRecipient = pendingTargetUid == null
                || pendingTargetUid.equals(FirebaseUtil.currentUid());
        if (pendingOpenOrders && sameRecipient && target != ArtistPendingActivity.class) {
            intent.putExtra(Constants.EXTRA_OPEN_ORDERS, true);
            if (pendingOrderId != null) intent.putExtra(Constants.EXTRA_ORDER_ID, pendingOrderId);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void sendPasswordReset() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (!ValidationUtil.isValidEmail(email)) {
            tilEmail.setError("Enter your email above first");
            return;
        }
        FirebaseUtil.auth().sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> Toast.makeText(this, "Password reset email sent", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
