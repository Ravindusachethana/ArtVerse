package com.artverse.app.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.common.ReceiptActivity;
import com.artverse.app.models.Order;
import com.artverse.app.models.OrderItem;
import com.artverse.app.utils.Constants;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArtistOrderAdapter extends RecyclerView.Adapter<ArtistOrderAdapter.ViewHolder> {

    public interface OrderActionListener {
        void onAccept(Order order);
        void onReject(Order order);
    }

    private final List<Order> orders = new ArrayList<>();
    private final OrderActionListener listener;

    public ArtistOrderAdapter(OrderActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Order> newList) {
        orders.clear();
        orders.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(orders.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatus, tvCustomerName, tvDate, tvItemsSummary, tvTotal, tvViewReceipt;
        View actionRow, btnAccept, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvItemsSummary = itemView.findViewById(R.id.tvItemsSummary);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvViewReceipt = itemView.findViewById(R.id.tvViewReceipt);
            actionRow = itemView.findViewById(R.id.actionRow);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        void bind(Order order, OrderActionListener listener) {
            String shortId = order.id != null && order.id.length() >= 6
                    ? order.id.substring(0, 6).toUpperCase(Locale.ROOT) : order.id;
            tvOrderId.setText("Order #" + shortId);
            tvCustomerName.setText("Customer: " + order.customerName);

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(order.orderDate)));

            StringBuilder summary = new StringBuilder();
            if (order.items != null) {
                for (int i = 0; i < order.items.size(); i++) {
                    OrderItem item = order.items.get(i);
                    summary.append(item.title).append(" ×").append(item.quantity);
                    if (i < order.items.size() - 1) summary.append(", ");
                }
            }
            tvItemsSummary.setText(summary.toString());

            NumberFormat format = NumberFormat.getInstance(Locale.US);
            tvTotal.setText("LKR " + format.format(order.totalAmount));

            // New orders arrive as "processing" (awaiting the artist's decision);
            // "pending" is kept for orders created before this flow existed.
            boolean awaitingDecision = Constants.STATUS_PROCESSING.equals(order.status)
                    || Constants.STATUS_PENDING.equals(order.status);
            actionRow.setVisibility(awaitingDecision ? View.VISIBLE : View.GONE);

            btnAccept.setOnClickListener(v -> { if (listener != null) listener.onAccept(order); });
            btnReject.setOnClickListener(v -> { if (listener != null) listener.onReject(order); });

            bindStatus(order.status);
            bindReceiptLink(order);
        }

        /** Settled orders (completed/rejected) open their receipt when tapped. */
        private void bindReceiptLink(Order order) {
            boolean settled = Constants.STATUS_COMPLETED.equals(order.status)
                    || Constants.STATUS_REJECTED.equals(order.status);
            tvViewReceipt.setVisibility(settled ? View.VISIBLE : View.GONE);
            itemView.setClickable(settled);
            itemView.setOnClickListener(!settled ? null : v -> {
                Intent intent = new Intent(v.getContext(), ReceiptActivity.class);
                intent.putExtra(Constants.EXTRA_ORDER_ID, order.id);
                v.getContext().startActivity(intent);
            });
        }

        private void bindStatus(String status) {
            String label = status != null ? status.substring(0, 1).toUpperCase(Locale.ROOT) + status.substring(1) : "Pending";
            tvStatus.setText(label);

            int bg, color;
            switch (status != null ? status : "") {
                case Constants.STATUS_PROCESSING:
                    // The customer sees "Processing"; for the artist this order is new.
                    tvStatus.setText("New");
                    bg = R.drawable.bg_pill_processing;
                    color = R.color.status_processing;
                    break;
                case Constants.STATUS_COMPLETED:
                    bg = R.drawable.bg_pill_completed;
                    color = R.color.status_success;
                    break;
                case Constants.STATUS_REJECTED:
                    bg = R.drawable.bg_pill_rejected;
                    color = R.color.status_rejected;
                    break;
                default:
                    bg = R.drawable.bg_pill_pending;
                    color = R.color.status_pending;
            }
            tvStatus.setBackgroundResource(bg);
            tvStatus.setTextColor(tvStatus.getResources().getColor(color, null));
        }
    }
}
