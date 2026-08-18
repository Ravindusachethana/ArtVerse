package com.artverse.app.artist;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.AdditionalImagesAdapter;
import com.artverse.app.models.Artwork;
import com.artverse.app.utils.ArtCategories;
import com.artverse.app.utils.ChipStyler;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.ValidationUtil;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class AddEditArtworkActivity extends AppCompatActivity {

    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private TextInputEditText etPrice;

    private TextInputEditText dPrice;
    private TextInputEditText etQuantity;
    private TextInputEditText etMedium;
    private TextInputEditText etDimensions;
    private TextInputLayout tilQuantity;
    private ChipGroup chipGroupCategory;
    private ImageView ivPreview;
    private View imagePlaceholderContent, additionalImagesSection, progressBar, btnSave;
    private RecyclerView rvAdditionalImages;
    private AdditionalImagesAdapter additionalAdapter;

    private Uri selectedImageUri;
    private String existingImageUrl;
    private boolean editMode = false;
    private String artworkId;
    /** Loaded in edit mode - decides whether a save publishes directly
     *  (unpublished resubmission) or stages changes for admin review. */
    private Artwork existingArtwork;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                selectedImageUri = uri;
                imagePlaceholderContent.setVisibility(View.GONE);
                Glide.with(this).load(uri).into(ivPreview);
            });

    /** Picks one or more extra photos for the "more views" strip at once. */
    private final ActivityResultLauncher<String> additionalImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) additionalAdapter.addImages(uris);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_artwork);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        dPrice = findViewById(R.id.dPrice);
        etQuantity = findViewById(R.id.etQuantity);
        tilQuantity = findViewById(R.id.tilQuantity);
        etMedium = findViewById(R.id.etMedium);
        etDimensions = findViewById(R.id.etDimensions);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        ivPreview = findViewById(R.id.ivPreview);
        imagePlaceholderContent = findViewById(R.id.imagePlaceholderContent);
        additionalImagesSection = findViewById(R.id.additionalImagesSection);
        rvAdditionalImages = findViewById(R.id.rvAdditionalImages);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);


        additionalAdapter = new AdditionalImagesAdapter(ArtCategories.MAX_IMAGES - 1,
                () -> additionalImagePicker.launch("image/*"));
        rvAdditionalImages.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvAdditionalImages.setAdapter(additionalAdapter);

        populateCategoryChips();

        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> onCategoryChanged());
        onCategoryChanged();


        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.imagePickerFrame).setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSave.setOnClickListener(v -> save());

        editMode = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_MODE, false);
        artworkId = getIntent().getStringExtra(Constants.EXTRA_ARTWORK_ID);

        if (editMode && artworkId != null) {
            ((TextView) findViewById(R.id.tvScreenTitle)).setText(R.string.edit_artwork);
            loadExistingArtwork(artworkId);
        }
    }

    private void populateCategoryChips() {
        for (String category : ArtCategories.DEFAULT) {
            Chip chip = new Chip(this);
            chip.setText(category);
            ChipStyler.styleCategoryChip(chip);
            chipGroupCategory.addView(chip);
        }
    }

    private void loadExistingArtwork(String id) {
        FirebaseUtil.artworksRef().document(id).get().addOnSuccessListener(doc -> {
            Artwork artwork = doc.toObject(Artwork.class);
            if (artwork == null) return;
            existingArtwork = artwork;

            String title = artwork.title;
            String description = artwork.description;
            double price = artwork.price;
            int quantity = artwork.quantity;
            String medium = artwork.medium;
            String dimensions = artwork.dimensions;
            String categoryName = artwork.categoryName;
            List<String> imageUrls = artwork.imageUrls;

            // A previous edit may still be in review - show that submission
            // so the artist keeps editing their latest version.
            if (Artwork.hasPendingEdit(artwork)) {
                Map<String, Object> staged = artwork.pendingChanges;
                if (staged.get("title") instanceof String s) title = s;
                if (staged.get("description") instanceof String s) description = s;
                if (staged.get("price") instanceof Number n) price = n.doubleValue();
                if (staged.get("quantity") instanceof Number n) quantity = n.intValue();
                if (staged.get("medium") instanceof String s) medium = s;
                if (staged.get("dimensions") instanceof String s) dimensions = s;
                if (staged.get("categoryName") instanceof String s) categoryName = s;
                if (staged.get("imageUrls") instanceof List<?> list) {
                    List<String> urls = new ArrayList<>();
                    for (Object o : list) if (o instanceof String s) urls.add(s);
                    imageUrls = urls;
                }
                toast("Your previous edit is still in review - saving replaces that submission.");
            }

            etTitle.setText(title);
            etDescription.setText(description);
            etPrice.setText(String.valueOf(price));
            etQuantity.setText(String.valueOf(quantity));
            etMedium.setText(medium);
            etDimensions.setText(dimensions);

            for (int i = 0; i < chipGroupCategory.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupCategory.getChildAt(i);
                if (chip.getText().toString().equalsIgnoreCase(categoryName)) {
                    chip.setChecked(true);
                }
            }

            if (imageUrls != null && !imageUrls.isEmpty()) {
                existingImageUrl = imageUrls.get(0);
                imagePlaceholderContent.setVisibility(View.GONE);
                Glide.with(this).load(existingImageUrl).into(ivPreview);

                // Any images past the cover are the extra angles - load them
                // into the strip so the artist can add to or trim them.
                List<AdditionalImagesAdapter.ImageSlot> extra = new ArrayList<>();
                for (int i = 1; i < imageUrls.size(); i++) {
                    extra.add(AdditionalImagesAdapter.ImageSlot.existing(imageUrls.get(i)));
                }
                additionalAdapter.setSlots(extra);
            }
        });
    }

    private String selectedCategory() {
        int checkedId = chipGroupCategory.getCheckedChipId();
        if (checkedId == View.NO_ID) return null;
        Chip chip = findViewById(checkedId);
        return chip != null ? chip.getText().toString() : null;
    }

    /** Re-applies every category-dependent rule after the choice changes. */
    private void onCategoryChanged() {
        applyQuantityRule();
        applyImageRule();
    }

    /** The extra-views strip is offered only for multi-angle physical pieces. */
    private void applyImageRule() {
        additionalImagesSection.setVisibility(
                ArtCategories.supportsMultipleImages(selectedCategory()) ? View.VISIBLE : View.GONE);
    }


    private void applyQuantityRule() {
        String category = selectedCategory();
        boolean sellByQuantity = category == null || ArtCategories.isReproducible(category);

        etQuantity.setEnabled(sellByQuantity);
        if (!sellByQuantity) etQuantity.setText("1");
        tilQuantity.setHelperText(sellByQuantity ? null : singlePieceHelper(category));
    }

    /** Why the quantity is fixed at one, worded for the kind of piece. */
    private String singlePieceHelper(String category) {
        if (ArtCategories.isDigital(category)) return getString(R.string.single_piece_helper_digital);
        if (ArtCategories.isOneOfAKind(category)) return getString(R.string.single_piece_helper_unique);
        return getString(R.string.single_piece_helper);
    }

    private void save() {
        String title = text(etTitle);
        String description = text(etDescription);
        String priceText = text(etPrice);
        String dpriceText = text(dPrice);
        String quantityText = text(etQuantity);
        String medium = text(etMedium);
        String dimensions = text(etDimensions);
        String category = selectedCategory();

        if (!ValidationUtil.isNotEmpty(title)) {
            toast("Please enter a title");
            return;
        }
        if (category == null) {
            toast("Please select a category");
            return;
        }
        if (!ValidationUtil.isValidPrice(priceText)) {
            toast("Please enter a valid price");
            return;
        }

        if (!ValidationUtil.isValidPrice(dpriceText)) {
            toast("Please enter a valid price");
            return;
        }
        int parsedQuantity;
        try {
            parsedQuantity = Integer.parseInt(quantityText);
            if (parsedQuantity < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            toast("Please enter a valid quantity");
            return;
        }
        // A one-time original or digital piece is a single copy, whatever the
        // field says - guards against stale text left when the category changed.
        final int quantity = ArtCategories.isReproducible(category) ? parsedQuantity : 1;

        setLoading(true);
        String uid = FirebaseUtil.currentUid();
        if (uid == null) {
            setLoading(false);
            return;
        }

        DocumentReference ref = editMode && artworkId != null
                ? FirebaseUtil.artworksRef().document(artworkId)
                : FirebaseUtil.artworksRef().document();

        // The cover leads the list; the extra angles follow, but only for the
        // categories that show the strip.
        List<AdditionalImagesAdapter.ImageSlot> imageSlots = new ArrayList<>();
        if (selectedImageUri != null) {
            imageSlots.add(AdditionalImagesAdapter.ImageSlot.local(selectedImageUri));
        } else if (existingImageUrl != null) {
            imageSlots.add(AdditionalImagesAdapter.ImageSlot.existing(existingImageUrl));
        }
        if (ArtCategories.supportsMultipleImages(category)) {
            imageSlots.addAll(additionalAdapter.getSlots());
        }

        final double price = Double.parseDouble(priceText);
        final double dprice = Double.parseDouble(dpriceText);
        uploadImages(ref, imageSlots, imageUrls -> saveArtworkDoc(ref, uid, title, description,
                category, price, dprice, quantity, medium, dimensions, imageUrls));
    }


    private void uploadImages(DocumentReference ref, List<AdditionalImagesAdapter.ImageSlot> slots,
                              Consumer<List<String>> onReady) {
        if (slots.isEmpty()) {
            onReady.accept(new ArrayList<>());
            return;
        }

        List<Task<String>> urlTasks = new ArrayList<>();
        long stamp = System.currentTimeMillis();
        for (int i = 0; i < slots.size(); i++) {
            AdditionalImagesAdapter.ImageSlot slot = slots.get(i);
            if (slot.existingUrl != null) {
                urlTasks.add(Tasks.forResult(slot.existingUrl));
                continue;
            }
            StorageReference imageRef =
                    FirebaseUtil.artworkImageRef(ref.getId(), "img_" + stamp + "_" + i + ".jpg");
            urlTasks.add(imageRef.putFile(slot.localUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                        return imageRef.getDownloadUrl();
                    })
                    .continueWith(task -> task.getResult().toString()));
        }

        Tasks.whenAllSuccess(urlTasks).addOnSuccessListener(results -> {
            List<String> imageUrls = new ArrayList<>();
            for (Task<String> task : urlTasks) imageUrls.add(task.getResult());
            onReady.accept(imageUrls);
        }).addOnFailureListener(e -> {
            setLoading(false);
            toast("Image upload failed: " + e.getMessage());
        });
    }

    private void saveArtworkDoc(DocumentReference ref, String uid, String title, String description,
                                 String category, double price, double dprice, int quantity, String medium,
                                 String dimensions, List<String> images) {


        if (editMode && (existingArtwork == null || Artwork.isPublished(existingArtwork))) {
            stagePendingChanges(ref, title, description, category, price, dprice, quantity,
                    medium, dimensions, images);
            return;
        }

        FirebaseUtil.usersRef().document(uid).get().addOnSuccessListener(userDoc -> {
            String artistName = userDoc.getString("name");

            Artwork artwork = new Artwork(ref.getId(), title, description, category, category,
                    uid, artistName, price, dPrice, quantity, images, medium, dimensions, true,
                    editMode && existingArtwork != null
                            ? existingArtwork.createdAt : System.currentTimeMillis());

            artwork.moderationStatus = Constants.REVIEW_STATUS_PENDING;

            ref.set(artwork)
                    .addOnSuccessListener(v -> {
                        if (!editMode) {
                            FirebaseUtil.artistsRef().document(uid)
                                    .update("totalArtworks", FieldValue.increment(1));
                        }
                        setLoading(false);
                        Toast.makeText(this, editMode
                                        ? "Artwork resubmitted for review"
                                        : "Artwork submitted - it will be published once an admin approves it",
                                Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        toast("Could not save artwork: " + e.getMessage());
                    });
        });
    }

    /** Writes the edited fields into pendingChanges without touching the live listing. */
    private void stagePendingChanges(DocumentReference ref, String title, String description,
                                      String category, double price, double dprice, int quantity, String medium,
                                      String dimensions, List<String> images) {
        Map<String, Object> changes = new HashMap<>();
        changes.put("title", title);
        changes.put("description", description);
        changes.put("categoryId", category);
        changes.put("categoryName", category);
        changes.put("price", price);
        changes.put("dprice", dprice);
        changes.put("quantity", quantity);
        changes.put("medium", medium);
        changes.put("dimensions", dimensions);
        changes.put("imageUrls", images);

        ref.update("pendingChanges", changes)
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Changes submitted for review - your current listing stays live until an admin approves the update",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Could not submit changes: " + e.getMessage());
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
        btnSave.setEnabled(!loading);
    }
}
