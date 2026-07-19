package com.artverse.app.artist.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.TransactionAdapter;
import com.artverse.app.models.Transaction;
import com.artverse.app.utils.FirebaseUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implements FR09 - Sales Tracking Module: artists view a history of their
 * sales and orders, backed by the "transactions" collection (the audit
 * trail described in the dissertation's security section).
 */
public class SalesReportFragment extends Fragment {

    private RecyclerView rvTransactions;
    private TextView tvNoTransactions, tvTotalRevenue, tvPiecesSold;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sales_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTransactions = view.findViewById(R.id.rvTransactions);
        tvNoTransactions = view.findViewById(R.id.tvNoTransactions);
        tvTotalRevenue = view.findViewById(R.id.tvTotalRevenue);
        tvPiecesSold = view.findViewById(R.id.tvPiecesSold);

        adapter = new TransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);

        loadTransactions();
    }

    private void loadTransactions() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        // Equality filter only, sorted client-side: keeps the query free of
        // composite-index requirements so it can never fail on a fresh project.
        FirebaseUtil.transactionsRef()
                .whereEqualTo("artistId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (getContext() == null) return;
                    if (error != null) {
                        Log.e("SalesReport", "Transaction query failed", error);
                        Toast.makeText(getContext(), "Could not load sales: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snapshot == null) return;

                    List<Transaction> transactions = new ArrayList<>();
                    double totalRevenue = 0;
                    int totalPieces = 0;
                    for (var doc : snapshot.getDocuments()) {
                        Transaction tx = doc.toObject(Transaction.class);
                        if (tx != null) {
                            transactions.add(tx);
                            totalRevenue += tx.total;
                            totalPieces += tx.quantity;
                        }
                    }
                    transactions.sort((a, b) -> Long.compare(b.date, a.date));
                    adapter.submitList(transactions);

                    NumberFormat format = NumberFormat.getInstance(Locale.US);
                    tvTotalRevenue.setText("LKR " + format.format(totalRevenue));
                    tvPiecesSold.setText(String.valueOf(totalPieces));

                    tvNoTransactions.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
                    rvTransactions.setVisibility(transactions.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }
}
