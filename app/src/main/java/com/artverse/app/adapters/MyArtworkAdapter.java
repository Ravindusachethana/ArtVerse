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

/** Implements FR04 - Artwork Management Module list view (edit/delete per listing). */
public class MyArtworkAdapter extends RecyclerView.Adapter<MyArtworkAdapter.ViewHolder> {

    public interface ActionListener {
        void onEdit(Artwork artwork);
        void onDelete(Artwork artwork);
    }

    private final List<Artwork> artworks = new ArrayList<>();
    private final ActionListener listener;

    public MyArtworkAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Artwork> newList) {
        artworks.clear();
        artworks.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_artwork, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(artworks.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return artworks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArtwork, btnEdit, btnDelete;
        TextView tvTitle, tvCategory, tvPrice, tvAvailability;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Artwork artwork, ActionListener listener) {
            tvTitle.setText(artwork.title);
            tvCategory.setText(artwork.categoryName);

            NumberFormat format = NumberFormat.getInstance(Locale.US);
            tvPrice.setText("LKR " + format.format(artwork.price));

            tvAvailability.setText(artwork.available ? "Available" : "Sold");
            tvAvailability.setBackgroundResource(artwork.available
                    ? R.drawable.bg_pill_completed : R.drawable.bg_pill_rejected);
            tvAvailability.setTextColor(tvAvailability.getResources().getColor(
                    artwork.available ? R.color.status_success : R.color.status_rejected, null));

            String imageUrl = (artwork.imageUrls != null && !artwork.imageUrls.isEmpty())
                    ? artwork.imageUrls.get(0) : null;
            Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ph_artwork)
                    .error(R.drawable.ph_artwork)
                    .centerCrop()
                    .into(ivArtwork);

            btnEdit.setOnClickListener(v -> { if (listener != null) listener.onEdit(artwork); });
            btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(artwork); });
        }
    }
}
