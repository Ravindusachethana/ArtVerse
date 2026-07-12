package com.artverse.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.models.Artwork;
import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArtworkAdapter extends RecyclerView.Adapter<ArtworkAdapter.ArtworkViewHolder> {

    public interface OnArtworkClickListener {
        void onArtworkClick(Artwork artwork);
    }

    private final List<Artwork> artworks = new ArrayList<>();
    private final OnArtworkClickListener listener;

    public ArtworkAdapter(OnArtworkClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Artwork> newList) {
        artworks.clear();
        artworks.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArtworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artwork, parent, false);
        return new ArtworkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtworkViewHolder holder, int position) {
        holder.bind(artworks.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return artworks.size();
    }

    static class ArtworkViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArtwork;
        TextView tvTitle, tvArtist, tvPrice, tvSoldBadge;

        ArtworkViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSoldBadge = itemView.findViewById(R.id.tvSoldBadge);
        }

        void bind(Artwork artwork, OnArtworkClickListener listener) {
            tvTitle.setText(artwork.title);
            tvArtist.setText("by " + artwork.artistName);

            NumberFormat currencyFormat = NumberFormat.getInstance(Locale.US);
            tvPrice.setText("LKR " + currencyFormat.format(artwork.price));

            tvSoldBadge.setVisibility(artwork.available ? View.GONE : View.VISIBLE);

            String imageUrl = (artwork.imageUrls != null && !artwork.imageUrls.isEmpty())
                    ? artwork.imageUrls.get(0) : null;

            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ph_artwork)
                    .error(R.drawable.ph_artwork)
                    .centerCrop()
                    .into(ivArtwork);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onArtworkClick(artwork);
            });
        }
    }
}
