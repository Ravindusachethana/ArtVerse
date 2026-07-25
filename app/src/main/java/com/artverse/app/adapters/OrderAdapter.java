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
import com.artverse.app.utils.OrderStatus;
import com.artverse.app.views.OrderTrackerView;

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
        TextView tvOrderId, tvStatus, tvDate, tvItemsSummary, tvTotal, tvViewReceipt;
        OrderTrackerView orderTracker;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvItemsSummary = itemView.findViewById(R.id.tvItemsSummary);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvViewReceipt = itemView.findViewById(R.id.tvViewReceipt);
            orderTracker = itemView.findViewById(R.id.orderTracker);
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
            bindReceiptLink(order);
            // Advances on its own - the orders screen listens to Firestore,
            // so the artist's and admin's actions land here live.
            orderTracker.setStep(OrderStatus.trackerStep(order.status));
        }

        /** Settled orders (completed/rejected) open their receipt when tapped. */
        private void bindReceiptLink(Order order) {
            boolean settled = OrderStatus.isSettled(order.status);
            tvViewReceipt.setVisibility(settled ? View.VISIBLE : View.GONE);
            itemView.setClickable(settled);
            itemView.setOnClickListener(!settled ? null : v -> {
                Intent intent = new Intent(v.getContext(), ReceiptActivity.class);
                intent.putExtra(Constants.EXTRA_ORDER_ID, order.id);
                v.getContext().startActivity(intent);
            });
        }

        private void bindStatus(String status) {
            tvStatus.setText(OrderStatus.customerLabel(status));
            tvStatus.setBackgroundResource(OrderStatus.pillBackground(status));
            tvStatus.setTextColor(tvStatus.getResources()
                    .getColor(OrderStatus.pillColor(status), null));
        }
    }
}
