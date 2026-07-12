package com.artverse.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.models.Order;
import com.artverse.app.models.OrderItem;
import com.artverse.app.utils.Constants;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final List<Order> orders = new ArrayList<>();

    public void submitList(List<Order> newList) {
        orders.clear();
        orders.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatus, tvDate, tvItemsSummary, tvTotal;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvItemsSummary = itemView.findViewById(R.id.tvItemsSummary);
            tvTotal = itemView.findViewById(R.id.tvTotal);
        }

        void bind(Order order) {
            String shortId = order.id != null && order.id.length() >= 6
                    ? order.id.substring(0, 6).toUpperCase(Locale.ROOT) : order.id;
            tvOrderId.setText("Order #" + shortId);

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

            bindStatus(order.status);
        }

        private void bindStatus(String status) {
            String label = status != null ? status.substring(0, 1).toUpperCase(Locale.ROOT) + status.substring(1) : "Pending";
            tvStatus.setText(label);

            int bg, color;
            switch (status != null ? status : "") {
                case Constants.STATUS_PROCESSING:
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
