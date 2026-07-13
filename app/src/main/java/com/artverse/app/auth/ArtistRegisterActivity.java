package com.artverse.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.artist.ArtistMainActivity;
import com.artverse.app.models.Artist;
import com.artverse.app.models.User;
import com.artverse.app.utils.ArtCategories;
import com.artverse.app.utils.ChipStyler;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;
import com.artverse.app.utils.ValidationUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/** Artist Registration Module. */
public class ArtistRegisterActivity extends AppCompatActivity {

    private TextInputEditText etBusinessName, etEmail, etLocation, etBio, etPassword;
    private ChipGroup chipGroupCategories;
    private View progressBar, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_register);

        etBusinessName = findViewById(R.id.etBusinessName);
        etEmail = findViewById(R.id.etEmail);
        etLocation = findViewById(R.id.etLocation);
        etBio = findViewById(R.id.etBio);
        etPassword = findViewById(R.id.etPassword);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        progressBar = findViewById(R.id.progressBar);
        btnRegister = findViewById(R.id.btnRegister);

        populateCategoryChips();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> register());
    }

    private void populateCategoryChips() {
        for (String category : ArtCategories.DEFAULT) {
            Chip chip = new Chip(this);
            chip.setText(category);
            ChipStyler.styleCategoryChip(chip);
            chipGroupCategories.addView(chip);
        }
    }

    private List<String> selectedCategories() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
            if (chip.isChecked()) selected.add(chip.getText().toString());
        }
        return selected;
    }

    private void register() {
        String businessName = text(etBusinessName);
        String email = text(etEmail);
        String location = text(etLocation);
        String bio = text(etBio);
        String password = text(etPassword);
        List<String> categories = selectedCategories();

        if (!ValidationUtil.isNotEmpty(businessName)) {
            toast("Please enter your artist/business name");
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            toast("Please enter a valid email");
            return;
        }
        if (!ValidationUtil.isValidPassword(password)) {
            toast("Password must be at least 6 characters");
            return;
        }
        if (categories.isEmpty()) {
            toast("Select at least one category you work in");
            return;
        }

        setLoading(true);
        FirebaseUtil.auth().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = FirebaseUtil.currentUid();

                    User user = new User(uid, businessName, email, null, location, null,
                            Constants.ROLE_ARTIST, System.currentTimeMillis());
                    Artist artist = new Artist(uid, businessName, bio, location, categories);

                    FirebaseUtil.usersRef().document(uid).set(user)
                            .continueWithTask(t -> FirebaseUtil.artistsRef().document(uid).set(artist))
                            .addOnSuccessListener(v -> {
                                setLoading(false);
                                new SessionManager(this).saveSession(uid, Constants.ROLE_ARTIST, businessName);
                                Intent intent = new Intent(this, ArtistMainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                toast("Could not save profile: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Registration failed: " + e.getMessage());
                });
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
