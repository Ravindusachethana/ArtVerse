package com.artverse.app.adapters;

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
 * Horizontal pager of an artwork's photos on the detail screen, so an Art Lover
 * can swipe through the extra angles a physical piece was shot from (see
 * ArtCategories.supportsMultipleImages). With a single image it behaves exactly
 * like the old static hero image.
 *
 * Pair with a PagerSnapHelper so each swipe settles on one photo, and an
 * optional tap callback for opening the piece larger.
 */
public class ArtworkImageCarouselAdapter
        extends RecyclerView.Adapter<ArtworkImageCarouselAdapter.PageViewHolder> {

    private final List<String> imageUrls = new ArrayList<>();
    private final Runnable onImageTapped;

    public ArtworkImageCarouselAdapter(Runnable onImageTapped) {
        this.onImageTapped = onImageTapped;
    }

    public void submitImages(List<String> urls) {
        imageUrls.clear();
        if (urls != null) imageUrls.addAll(urls);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artwork_image, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(imageUrls.get(position), onImageTapped);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPage;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPage = (ImageView) itemView;
        }

        void bind(String imageUrl, Runnable onImageTapped) {
            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ph_artwork)
                    .error(R.drawable.ph_artwork)
                    .centerCrop()
                    .into(ivPage);

            if (onImageTapped != null) {
                itemView.setOnClickListener(v -> onImageTapped.run());
            }
        }
    }
}
