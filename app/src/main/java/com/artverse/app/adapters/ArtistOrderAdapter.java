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

public class ArtistOrderAdapter extends RecyclerView.Adapter<ArtistOrderAdapter.ViewHolder> {

    public interface OrderActionListener {
        /** Artist accepts a new order - it becomes "confirmed". */
        void onConfirm(Order order);
        /** Artist hands a confirmed order to the delivery section. */
        void onHandOver(Order order);
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
        TextView tvOrderId, tvStatus, tvCustomerName, tvDate, tvItemsSummary, tvTotal,
                tvViewReceipt, tvAwaitingAdmin;
        View actionRow, btnAccept, btnHandOver;
        OrderTrackerView orderTracker;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvItemsSummary = itemView.findViewById(R.id.tvItemsSummary);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvViewReceipt = itemView.findViewById(R.id.tvViewReceipt);
            tvAwaitingAdmin = itemView.findViewById(R.id.tvAwaitingAdmin);
            actionRow = itemView.findViewById(R.id.actionRow);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnHandOver = itemView.findViewById(R.id.btnHandOver);
            orderTracker = itemView.findViewById(R.id.orderTracker);
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

            // Stage-appropriate actions: decide on a new order, then hand the
            // confirmed one to delivery. Once dispatched the artist only
            // watches - the admin closes the order out.
            boolean awaitingDecision = OrderStatus.isAwaitingArtist(order.status);
            actionRow.setVisibility(awaitingDecision ? View.VISIBLE : View.GONE);
            btnHandOver.setVisibility(OrderStatus.isConfirmed(order.status) ? View.VISIBLE : View.GONE);
            tvAwaitingAdmin.setVisibility(OrderStatus.isOutForDelivery(order.status)
                    ? View.VISIBLE : View.GONE);

            btnAccept.setOnClickListener(v -> { if (listener != null) listener.onConfirm(order); });
            btnHandOver.setOnClickListener(v -> { if (listener != null) listener.onHandOver(order); });

            bindStatus(order.status);
            bindReceiptLink(order);
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
            // The buyer sees "Pending"; to the artist an undecided order is "New".
            tvStatus.setText(OrderStatus.artistLabel(status));
            tvStatus.setBackgroundResource(OrderStatus.pillBackground(status));
            tvStatus.setTextColor(tvStatus.getResources()
                    .getColor(OrderStatus.pillColor(status), null));
        }
    }
}
