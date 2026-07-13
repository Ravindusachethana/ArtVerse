package com.artverse.app.artist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.auth.LoginActivity;
import com.artverse.app.models.Artist;
import com.artverse.app.models.User;
import com.artverse.app.utils.ArtCategories;
import com.artverse.app.utils.ChipStyler;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.SessionManager;
import com.artverse.app.utils.ValidationUtil;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.SetOptions;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets an artist update their public-facing profile: name/studio name, photo,
 * phone, location, bio, and the categories they work in. Also hosts the
 * artist's log-out action since the artist bottom nav has no dedicated
 * profile tab.
 */
public class ArtistProfileEditActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etLocation, etBio;
    private ChipGroup chipGroupCategories;
    private CircleImageView ivAvatarPreview;
    private View progressBar, btnSave;

    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedImageUri = uri;
                ivAvatarPreview.setPadding(0, 0, 0, 0);
                Glide.with(this).load(uri).into(ivAvatarPreview);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_profile_edit);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etLocation = findViewById(R.id.etLocation);
        etBio = findViewById(R.id.etBio);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        ivAvatarPreview = findViewById(R.id.ivAvatarPreview);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);

        populateCategoryChips();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChangePhoto).setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSave.setOnClickListener(v -> save());
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        loadExistingProfile();
    }

    private void populateCategoryChips() {
        for (String category : ArtCategories.DEFAULT) {
            Chip chip = new Chip(this);
            chip.setText(category);
            ChipStyler.styleCategoryChip(chip);
            chipGroupCategories.addView(chip);
        }
    }

    private void loadExistingProfile() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.usersRef().document(uid).get().addOnSuccessListener(userDoc -> {
            User user = userDoc.toObject(User.class);
            if (user == null) return;

            etName.setText(user.name);
            etPhone.setText(user.phone);
            if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
                ivAvatarPreview.setPadding(0, 0, 0, 0);
                Glide.with(this).load(user.profileImageUrl).into(ivAvatarPreview);
            }
        });

        FirebaseUtil.artistsRef().document(uid).get().addOnSuccessListener(artistDoc -> {
            Artist artist = artistDoc.toObject(Artist.class);
            if (artist == null) return;

            etLocation.setText(artist.location);
            etBio.setText(artist.bio);

            if (artist.categories != null) {
                for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
                    Chip chip = (Chip) chipGroupCategories.getChildAt(i);
                    if (artist.categories.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }
                }
            }
        });
    }

    private List<String> selectedCategories() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
            if (chip.isChecked()) selected.add(chip.getText().toString());
        }
        return selected;
    }

    private void save() {
        String name = text(etName);
        String phone = text(etPhone);
        String location = text(etLocation);
        String bio = text(etBio);
        List<String> categories = selectedCategories();

        if (!ValidationUtil.isNotEmpty(name)) {
            toast("Please enter your name");
            return;
        }

        setLoading(true);
        String uid = FirebaseUtil.currentUid();
        if (uid == null) {
            setLoading(false);
            return;
        }

        if (selectedImageUri != null) {
            FirebaseUtil.profileImageRef(uid).putFile(selectedImageUri)
                    .continueWithTask(task -> FirebaseUtil.profileImageRef(uid).getDownloadUrl())
                    .addOnSuccessListener(downloadUri ->
                            saveProfile(uid, name, phone, location, bio, categories, downloadUri.toString()))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        toast("Photo upload failed: " + e.getMessage());
                    });
        } else {
            saveProfile(uid, name, phone, location, bio, categories, null);
        }
    }

    private void saveProfile(String uid, String name, String phone, String location, String bio,
                              List<String> categories, String newImageUrl) {

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("name", name);
        userUpdates.put("phone", phone);
        if (newImageUrl != null) userUpdates.put("profileImageUrl", newImageUrl);

        Map<String, Object> artistUpdates = new HashMap<>();
        artistUpdates.put("businessName", name);
        artistUpdates.put("location", location);
        artistUpdates.put("bio", bio);
        artistUpdates.put("categories", categories);

        FirebaseUtil.usersRef().document(uid).set(userUpdates, SetOptions.merge())
                .continueWithTask(t -> FirebaseUtil.artistsRef().document(uid)
                        .set(artistUpdates, SetOptions.merge()))
                .addOnSuccessListener(v -> {
                    new SessionManager(this).saveSession(uid, Constants.ROLE_ARTIST, name);
                    setLoading(false);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Could not save profile: " + e.getMessage());
                });
    }

    private void logout() {
        FirebaseUtil.auth().signOut();
        new SessionManager(this).clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}
