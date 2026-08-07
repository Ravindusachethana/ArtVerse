package com.artverse.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Pages of the full-screen artwork viewer - one image per page, shown whole
 * (fitCenter) at the screen's full resolution rather than cropped to a tile.
 *
 * Caching the original source data rather than the decoded bitmap means the
 * Download button can hand the artist's untouched file to ArtworkDownloader
 * without fetching it a second time.
 */
public class ArtworkPagerAdapter extends RecyclerView.Adapter<ArtworkPagerAdapter.PageViewHolder> {

    private final List<String> imageUrls = new ArrayList<>();
    private final Runnable onPageTapped;

    public ArtworkPagerAdapter(Runnable onPageTapped) {
        this.onPageTapped = onPageTapped;
    }

    public void submitImages(List<String> urls) {
        imageUrls.clear();
        imageUrls.addAll(urls);
        notifyDataSetChanged();
    }

    public String imageAt(int position) {
        return position >= 0 && position < imageUrls.size() ? imageUrls.get(position) : null;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_viewer_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(imageUrls.get(position), onPageTapped);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPage;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPage = itemView.findViewById(R.id.ivPage);
        }

        void bind(String imageUrl, Runnable onPageTapped) {
            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .placeholder(R.drawable.ph_artwork)
                    .error(R.drawable.ph_artwork)
                    .fitCenter()
                    .into(ivPage);

            itemView.setOnClickListener(v -> {
                if (onPageTapped != null) onPageTapped.run();
            });
        }
    }
}
