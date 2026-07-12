package com.artverse.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.artist.ArtistMainActivity;
import com.artverse.app.customer.CustomerMainActivity;
import com.artverse.app.models.User;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;
import com.artverse.app.utils.ValidationUtil;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private View progressBar, btnLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

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
                    routeToHome(user.role);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void routeToHome(String role) {
        Intent intent = "artist".equals(role)
                ? new Intent(this, ArtistMainActivity.class)
                : new Intent(this, CustomerMainActivity.class);
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
