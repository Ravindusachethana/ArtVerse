package com.artverse.app.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * The artist's horizontal strip of extra photos for a physical piece
 * (ArtCategories.supportsMultipleImages). Holds already-uploaded images (in
 * edit mode) and freshly picked ones side by side, each removable, followed by
 * an "add" tile until the listing hits its image cap.
 *
 * These are the images that follow the cover; the cover stays in its own picker
 * above. The owning screen assembles cover + these into the final imageUrls.
 */
public class AdditionalImagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_IMAGE = 0;
    private static final int TYPE_ADD = 1;

    /** One extra photo: either already uploaded (existingUrl) or newly picked (localUri). */
    public static class ImageSlot {
        public final String existingUrl;
        public final Uri localUri;

        private ImageSlot(String existingUrl, Uri localUri) {
            this.existingUrl = existingUrl;
            this.localUri = localUri;
        }

        public static ImageSlot existing(String url) {
            return new ImageSlot(url, null);
        }

        public static ImageSlot local(Uri uri) {
            return new ImageSlot(null, uri);
        }
    }

    public interface Listener {
        void onAddClicked();
    }

    private final List<ImageSlot> slots = new ArrayList<>();
    private final int maxSlots;
    private final Listener listener;

    public AdditionalImagesAdapter(int maxSlots, Listener listener) {
        this.maxSlots = maxSlots;
        this.listener = listener;
    }

    public List<ImageSlot> getSlots() {
        return new ArrayList<>(slots);
    }

    public void setSlots(List<ImageSlot> newSlots) {
        slots.clear();
        if (newSlots != null) slots.addAll(newSlots);
        notifyDataSetChanged();
    }

    /** Adds picked images up to the remaining room; ignores the overflow. */
    public void addImages(List<Uri> uris) {
        if (uris == null) return;
        for (Uri uri : uris) {
            if (slots.size() >= maxSlots) break;
            slots.add(ImageSlot.local(uri));
        }
        notifyDataSetChanged();
    }

    /** Room left before the listing hits its image cap. */
    public int remainingCapacity() {
        return Math.max(0, maxSlots - slots.size());
    }

    private boolean canAddMore() {
        return slots.size() < maxSlots;
    }

    @Override
    public int getItemViewType(int position) {
        return position < slots.size() ? TYPE_IMAGE : TYPE_ADD;
    }

    @Override
    public int getItemCount() {
        return slots.size() + (canAddMore() ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            return new AddViewHolder(inflater.inflate(R.layout.item_image_add, parent, false));
        }
        return new ImageViewHolder(inflater.inflate(R.layout.item_image_slot, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ImageViewHolder imageHolder) {
            imageHolder.bind(slots.get(position));
        } else {
            ((AddViewHolder) holder).bind(listener);
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivThumb, btnRemove;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivThumb);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        void bind(ImageSlot slot) {
            Object model = slot.existingUrl != null ? slot.existingUrl : slot.localUri;
            Glide.with(itemView.getContext())
                    .load(model)
                    .placeholder(R.drawable.ph_artwork)
                    .error(R.drawable.ph_artwork)
                    .centerCrop()
                    .into(ivThumb);

            btnRemove.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= slots.size()) return;
                slots.remove(position);
                notifyDataSetChanged();
            });
        }
    }

    static class AddViewHolder extends RecyclerView.ViewHolder {
        AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void bind(Listener listener) {
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAddClicked();
            });
        }
    }
}
