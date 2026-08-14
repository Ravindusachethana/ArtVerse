package com.artverse.app.common;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.artverse.app.R;
import com.artverse.app.models.Order;
import com.artverse.app.models.OrderItem;
import com.artverse.app.models.User;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.OrderActions;
import com.artverse.app.utils.ReceiptPdfWriter;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * In-app receipt for a settled (completed or rejected) order, opened from
 * the Orders pages of both roles. Shows the invoice-style summary and lets
 * the user download it as a PDF into the device's Downloads folder
 * (ReceiptPdfWriter).
 */
public class ReceiptActivity extends AppCompatActivity {

    private static final int REQ_WRITE_STORAGE = 81;

    private View receiptCard, btnDownload;
    private TextView tvStatus, tvReceiptNo, tvOrderDate, tvCustomer, tvArtist,
            tvAddress, tvPayment, tvTotal, tvFooterNote, tvGeneratedOn;
    private LinearLayout itemsContainer;

    private Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        receiptCard = findViewById(R.id.receiptCard);
        btnDownload = findViewById(R.id.btnDownload);
        tvStatus = findViewById(R.id.tvReceiptStatus);
        tvReceiptNo = findViewById(R.id.tvReceiptNo);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvCustomer = findViewById(R.id.tvCustomer);
        tvArtist = findViewById(R.id.tvArtist);
        tvAddress = findViewById(R.id.tvAddress);
        tvPayment = findViewById(R.id.tvPayment);
        tvTotal = findViewById(R.id.tvReceiptTotal);
        tvFooterNote = findViewById(R.id.tvFooterNote);
        tvGeneratedOn = findViewById(R.id.tvGeneratedOn);
        itemsContainer = findViewById(R.id.itemsContainer);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnDownload.setOnClickListener(v -> downloadPdf());
        btnDownload.setEnabled(false);

        String orderId = getIntent().getStringExtra(Constants.EXTRA_ORDER_ID);
        if (orderId == null) {
            finish();
            return;
        }
        loadOrder(orderId);
    }

    private void loadOrder(String orderId) {
        FirebaseUtil.ordersRef().document(orderId).get()
                .addOnSuccessListener(doc -> {
                    Order loaded = doc.toObject(Order.class);
                    if (loaded == null) {
                        Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    loaded.id = doc.getId();
                    order = loaded;
                    bind();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load receipt: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void bind() {
        boolean rejected = Constants.STATUS_REJECTED.equals(order.status);

        tvStatus.setText(rejected ? "Rejected" : "Completed");
        tvStatus.setBackgroundResource(rejected
                ? R.drawable.bg_pill_rejected : R.drawable.bg_pill_completed);
        tvStatus.setTextColor(getColor(rejected
                ? R.color.status_rejected : R.color.status_success));

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        tvReceiptNo.setText("RCP-" + OrderActions.shortId(order.id));
        tvOrderDate.setText(sdf.format(new Date(order.orderDate)));
        tvCustomer.setText(order.customerName);
        tvAddress.setText(order.deliveryAddress);
        tvPayment.setText(order.paymentMethod);

        tvArtist.setText("...");
        FirebaseUtil.usersRef().document(order.artistId).get().addOnSuccessListener(doc -> {
            User artist = doc.toObject(User.class);
            tvArtist.setText(artist != null && artist.name != null ? artist.name : "-");
        });

        NumberFormat format = NumberFormat.getInstance(Locale.US);
        itemsContainer.removeAllViews();
        if (order.items != null) {
            for (OrderItem item : order.items) {
                addItemRow(item, format);
            }
        }
        tvTotal.setText("LKR " + format.format(order.totalAmount));

        tvFooterNote.setText("Payment settled with the artist. Thank you for supporting "
                        + "Sri Lankan handmade art!");
        tvGeneratedOn.setText("Generated by ArtVerse on "
                + sdf.format(new Date(System.currentTimeMillis())));

        btnDownload.setEnabled(true);
    }

    private void addItemRow(OrderItem item, NumberFormat format) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvName = new TextView(this);
        tvName.setText(item.title + "  ×" + item.quantity);
        tvName.setTextColor(getColor(R.color.text_primary));
        tvName.setTextSize(13);
        row.addView(tvName, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvAmount = new TextView(this);
        tvAmount.setText("LKR " + format.format(item.unitPrice * item.quantity));
        tvAmount.setTextColor(getColor(R.color.text_secondary));
        tvAmount.setTextSize(13);
        row.addView(tvAmount);

        itemsContainer.addView(row);
    }

    private void downloadPdf() {
        if (order == null) return;

        // Android 9 and below write to the legacy Downloads directory, which
        // needs the storage permission; 10+ goes through MediaStore without it.
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_STORAGE);
            return;
        }

        try {
            String fileName = "ArtVerse-Receipt-" + OrderActions.shortId(order.id) + ".pdf";
            ReceiptPdfWriter.saveToDownloads(this, receiptCard, fileName);
            Toast.makeText(this, "Saved to Downloads as " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WRITE_STORAGE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadPdf();
        }
    }
}
