package com.artverse.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.models.CartItem;
import com.artverse.app.utils.ArtCategories;
import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface CartActionListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemove(CartItem item);
    }

    private final List<CartItem> items = new ArrayList<>();
    private final CartActionListener listener;

    public CartAdapter(CartActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CartItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public List<CartItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArtwork, btnMinus, btnPlus, btnRemove;
        TextView tvTitle, tvArtist, tvPrice, tvQuantity, tvSingleCopyBadge;
        View quantityStepper;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvSingleCopyBadge = itemView.findViewById(R.id.tvSingleCopyBadge);
            quantityStepper = itemView.findViewById(R.id.quantityStepper);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        void bind(CartItem item, CartActionListener listener) {
            tvTitle.setText(item.title);
            tvArtist.setText("by " + item.artistName);
            tvQuantity.setText(String.valueOf(item.quantity));

            NumberFormat format = NumberFormat.getInstance(Locale.US);
            tvPrice.setText("LKR " + format.format(item.lineTotal()));

            Glide.with(itemView.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ph_artwork)
                    .error(R.drawable.ph_artwork)
                    .centerCrop()
                    .into(ivArtwork);

            // Only reproducible pieces keep the stepper; a single-copy line -
            // an original or a digital file - shows a badge saying which,
            // since there is no second piece to order.
            boolean reproducible = ArtCategories.isReproducible(item.categoryName);
            quantityStepper.setVisibility(reproducible ? View.VISIBLE : View.GONE);
            tvSingleCopyBadge.setVisibility(reproducible ? View.GONE : View.VISIBLE);
            tvSingleCopyBadge.setText(ArtCategories.isDigital(item.categoryName)
                    ? R.string.digital_badge : R.string.label_original);

            btnMinus.setOnClickListener(v -> {
                if (item.quantity > 1 && listener != null) {
                    listener.onQuantityChanged(item, item.quantity - 1);
                }
            });
            btnPlus.setOnClickListener(v -> {
                if (listener != null) listener.onQuantityChanged(item, item.quantity + 1);
            });
            btnRemove.setOnClickListener(v -> {
                if (listener != null) listener.onRemove(item);
            });
        }
    }
}
