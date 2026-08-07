package com.artverse.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.models.CollectionEntry;
import com.artverse.app.utils.ArtCategories;
import com.artverse.app.utils.CollectionAccess;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Renders My Collection as a category-sectioned grid: a full-width header per
 * category followed by that category's pieces. Digital pieces carry a download
 * shortcut on the tile itself.
 *
 * Pair with {@link #spanSizeLookup(int)} so the headers span the whole row.
 */
public class CollectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ART = 1;

    public interface CollectionActionListener {
        /** Opens the piece full screen at original quality. */
        void onOpen(CollectionEntry entry);
        /** Saves the original file to the device. */
        void onDownload(CollectionEntry entry);
    }

    /** A section header, or one owned piece - the grid is a flat list of these. */
    private static class Row {
        final String category;
        final int categorySize;
        final boolean digitalCategory;
        final CollectionEntry entry;

        Row(String category, int categorySize, boolean digitalCategory) {
            this.category = category;
            this.categorySize = categorySize;
            this.digitalCategory = digitalCategory;
            this.entry = null;
        }

        Row(CollectionEntry entry) {
            this.category = null;
            this.categorySize = 0;
            this.digitalCategory = false;
            this.entry = entry;
        }

        boolean isHeader() {
            return entry == null;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final CollectionActionListener listener;

    public CollectionAdapter(CollectionActionListener listener) {
        this.listener = listener;
    }

    /** Groups the owned pieces by category and rebuilds the sectioned grid. */
    public void submitEntries(List<CollectionEntry> entries) {
        rows.clear();

        for (Map.Entry<String, List<CollectionEntry>> section : groupByCategory(entries).entrySet()) {
            List<CollectionEntry> pieces = section.getValue();
            rows.add(new Row(section.getKey(), pieces.size(), ArtCategories.isDigital(section.getKey())));
            for (CollectionEntry entry : pieces) rows.add(new Row(entry));
        }
        notifyDataSetChanged();
    }

    /**
     * Section a piece is filed under - its category, or "Other" for an order
     * line old enough not to have recorded one.
     */
    public static String sectionOf(String categoryName) {
        return categoryName == null || categoryName.trim().isEmpty() ? "Other" : categoryName.trim();
    }

    /**
     * Categories in the app's own listing order (ArtCategories.DEFAULT), with
     * anything unrecognised - a category the admin added, or an old order with
     * none recorded - alphabetically after them. Newest purchase first inside
     * each category.
     */
    private Map<String, List<CollectionEntry>> groupByCategory(List<CollectionEntry> entries) {
        Map<String, List<CollectionEntry>> known = new LinkedHashMap<>();
        for (String category : ArtCategories.DEFAULT) known.put(category, new ArrayList<>());
        Map<String, List<CollectionEntry>> extra = new TreeMap<>();

        for (CollectionEntry entry : entries) {
            String category = sectionOf(entry.categoryName);
            List<CollectionEntry> bucket = known.get(category);
            if (bucket == null) bucket = extra.computeIfAbsent(category, k -> new ArrayList<>());
            bucket.add(entry);
        }

        Map<String, List<CollectionEntry>> sections = new LinkedHashMap<>();
        for (Map<String, List<CollectionEntry>> source : List.of(known, extra)) {
            for (Map.Entry<String, List<CollectionEntry>> section : source.entrySet()) {
                if (section.getValue().isEmpty()) continue;
                section.getValue().sort((a, b) -> Long.compare(b.purchasedAt, a.purchasedAt));
                sections.put(section.getKey(), section.getValue());
            }
        }
        return sections;
    }

    /** Lets category headers take the full row width in a grid of {@code spanCount}. */
    public GridLayoutManager.SpanSizeLookup spanSizeLookup(int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return rows.get(position).isHeader() ? spanCount : 1;
            }
        };
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader() ? TYPE_HEADER : TYPE_ART;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_collection_header, parent, false));
        }
        return new ArtViewHolder(inflater.inflate(R.layout.item_collection_art, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderViewHolder headerHolder) {
            headerHolder.bind(row);
        } else {
            ((ArtViewHolder) holder).bind(row.entry, listener);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategory, tvCategoryCount, tvDownloadableHint;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvCategoryCount = itemView.findViewById(R.id.tvCategoryCount);
            tvDownloadableHint = itemView.findViewById(R.id.tvDownloadableHint);
        }

        void bind(Row row) {
            tvCategory.setText(row.category);
            tvCategoryCount.setText(itemView.getContext().getString(
                    row.categorySize == 1 ? R.string.collection_pieces : R.string.collection_pieces_plural,
                    row.categorySize));
            tvDownloadableHint.setVisibility(row.digitalCategory ? View.VISIBLE : View.GONE);
        }
    }

    static class ArtViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivArtwork, btnDownload;
        final TextView tvTitle, tvMeta, tvDigitalBadge;

        ArtViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvDigitalBadge = itemView.findViewById(R.id.tvDigitalBadge);
        }

        void bind(CollectionEntry entry, CollectionActionListener listener) {
            tvTitle.setText(entry.title);

            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
            String meta = CollectionAccess.deliveryLabel(entry.categoryName)
                    + " · " + dateFormat.format(new Date(entry.purchasedAt));
            if (entry.quantity > 1) meta = "×" + entry.quantity + " · " + meta;
            tvMeta.setText(meta);

            tvDigitalBadge.setVisibility(entry.downloadable ? View.VISIBLE : View.GONE);
            btnDownload.setVisibility(entry.downloadable ? View.VISIBLE : View.GONE);

            Glide.with(itemView.getContext())
                    .load(entry.imageUrl)
                    .placeholder(R.drawable.ph_artwork)
                    .error(R.drawable.ph_artwork)
                    .centerCrop()
                    .into(ivArtwork);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOpen(entry);
            });
            btnDownload.setOnClickListener(v -> {
                if (listener != null) listener.onDownload(entry);
            });
        }
    }
}
