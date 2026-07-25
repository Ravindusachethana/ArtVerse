package com.artverse.app.artist;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artverse.app.R;
import com.artverse.app.adapters.ArtistOrderAdapter;
import com.artverse.app.models.Artwork;
import com.artverse.app.models.Order;
import com.artverse.app.models.OrderItem;
import com.artverse.app.utils.Constants;
import com.artverse.app.utils.FirebaseUtil;
import com.artverse.app.utils.ReportCharts;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Live sales &amp; delivery reports for the signed-in artist (FR09 extension).
 *
 * Layout is two independent, live-updating panels:
 *   1. A chart carousel that shows one chart at a time - best-sellers, sales,
 *      or artworks-published - swapped with the prev/next arrows. Only the
 *      visible chart is (re)drawn.
 *   2. A Completed / Rejected order switch backed by a single list that swaps
 *      its contents when the artist picks a side.
 *
 * Everything is driven by two index-free snapshot listeners ("orders" and
 * "artworks" filtered by artistId only), so figures refresh in place as orders
 * move through delivery and new work is published - without resetting which
 * chart or order tab the artist is looking at. The aggregation is client-side,
 * matching the app's "equality filter, sort in memory" approach that keeps a
 * fresh Firebase project free of composite indexes.
 */
public class ArtistReportsActivity extends AppCompatActivity {

    private static final int BEST_SELLER_LIMIT = 6;
    private static final int WEEK_BUCKETS = 8;
    private static final int MONTH_BUCKETS = 6;

    // Carousel positions.
    private static final int CHART_BEST_SELLERS = 0;
    private static final int CHART_SALES = 1;
    private static final int CHART_PUBLISHED = 2;
    private static final int CHART_COUNT = 3;

    // Order switch positions.
    private static final int TAB_COMPLETED = 0;
    private static final int TAB_REJECTED = 1;

    private HorizontalBarChart chartBestSellers;
    private BarChart chartSales, chartPublished;
    private TextView tvChartTitle, tvChartHint, tvChartEmpty, tvNoOrders;
    private MaterialButtonToggleGroup toggleChartPeriod;
    private LinearLayout chartDots;
    private RecyclerView rvOrders;
    private ArtistOrderAdapter ordersAdapter;

    /** Cached so the carousel / toggles can re-render without a re-query. */
    private final List<Order> completedOrders = new ArrayList<>();
    private final List<Order> rejectedOrders = new ArrayList<>();
    private final List<Artwork> myArtworks = new ArrayList<>();

    private int currentChart = CHART_BEST_SELLERS;
    private int selectedOrderTab = TAB_COMPLETED;
    private boolean salesMonthly = false;
    private boolean publishedMonthly = false;
    private boolean suppressPeriodToggle = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_reports);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        chartBestSellers = findViewById(R.id.chartBestSellers);
        chartSales = findViewById(R.id.chartSales);
        chartPublished = findViewById(R.id.chartPublished);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        tvChartHint = findViewById(R.id.tvChartHint);
        tvChartEmpty = findViewById(R.id.tvChartEmpty);
        tvNoOrders = findViewById(R.id.tvNoOrders);
        toggleChartPeriod = findViewById(R.id.toggleChartPeriod);
        chartDots = findViewById(R.id.chartDots);
        rvOrders = findViewById(R.id.rvOrders);

        ReportCharts.styleBarChart(chartBestSellers, axisColor());
        ReportCharts.styleBarChart(chartSales, axisColor());
        ReportCharts.styleBarChart(chartPublished, axisColor());

        // Read-only order list here, so the adapter callbacks never fire.
        ArtistOrderAdapter.OrderActionListener noop = new ArtistOrderAdapter.OrderActionListener() {
            @Override public void onConfirm(Order order) { }
            @Override public void onHandOver(Order order) { }
            @Override public void onReject(Order order) { }
        };
        ordersAdapter = new ArtistOrderAdapter(noop);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(ordersAdapter);

        buildDots();
        wireCarousel();
        wireOrderTabs();

        showChart(currentChart);
        showOrders();

        loadOrders();
        loadArtworks();
    }

    // ----- carousel wiring -----

    private void wireCarousel() {
        findViewById(R.id.btnPrevChart).setOnClickListener(v ->
                showChart((currentChart - 1 + CHART_COUNT) % CHART_COUNT));
        findViewById(R.id.btnNextChart).setOnClickListener(v ->
                showChart((currentChart + 1) % CHART_COUNT));

        toggleChartPeriod.check(R.id.btnPeriodWeekly);
        toggleChartPeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || suppressPeriodToggle) return;
            boolean monthly = checkedId == R.id.btnPeriodMonthly;
            if (currentChart == CHART_SALES) salesMonthly = monthly;
            else if (currentChart == CHART_PUBLISHED) publishedMonthly = monthly;
            renderCurrentChart(true);
        });
    }

    /** Shows one chart, updates the header/toggle/dots, then draws it. */
    private void showChart(int index) {
        currentChart = index;

        chartBestSellers.setVisibility(index == CHART_BEST_SELLERS ? View.VISIBLE : View.GONE);
        chartSales.setVisibility(index == CHART_SALES ? View.VISIBLE : View.GONE);
        chartPublished.setVisibility(index == CHART_PUBLISHED ? View.VISIBLE : View.GONE);

        switch (index) {
            case CHART_SALES:
                tvChartTitle.setText(R.string.report_sales);
                tvChartHint.setText(R.string.report_sales_hint);
                showPeriodToggle(salesMonthly);
                break;
            case CHART_PUBLISHED:
                tvChartTitle.setText(R.string.report_published);
                tvChartHint.setText(R.string.report_published_hint);
                showPeriodToggle(publishedMonthly);
                break;
            default:
                tvChartTitle.setText(R.string.report_best_sellers);
                tvChartHint.setText(R.string.report_best_sellers_hint);
                toggleChartPeriod.setVisibility(View.GONE);
                break;
        }

        updateDots();
        renderCurrentChart(true);
    }

    /** Reflects the current chart's period without triggering a redraw loop. */
    private void showPeriodToggle(boolean monthly) {
        suppressPeriodToggle = true;
        toggleChartPeriod.check(monthly ? R.id.btnPeriodMonthly : R.id.btnPeriodWeekly);
        suppressPeriodToggle = false;
        toggleChartPeriod.setVisibility(View.VISIBLE);
    }

    private void renderCurrentChart(boolean animate) {
        switch (currentChart) {
            case CHART_SALES: renderSalesChart(animate); break;
            case CHART_PUBLISHED: renderPublishedChart(animate); break;
            default: renderBestSellers(animate); break;
        }
    }

    // ----- order switch wiring -----

    private void wireOrderTabs() {
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleOrders);
        toggle.check(R.id.btnOrdersCompleted);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            selectedOrderTab = checkedId == R.id.btnOrdersRejected ? TAB_REJECTED : TAB_COMPLETED;
            showOrders();
        });
    }

    private void showOrders() {
        List<Order> list = selectedOrderTab == TAB_REJECTED ? rejectedOrders : completedOrders;
        ordersAdapter.submitList(list);
        tvNoOrders.setText(selectedOrderTab == TAB_REJECTED
                ? R.string.report_no_rejected : R.string.report_no_completed);
        boolean empty = list.isEmpty();
        tvNoOrders.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ----- data -----

    private void loadOrders() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.ordersRef()
                .whereEqualTo("artistId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing()) return;
                    if (error != null) {
                        Log.e("ArtistReports", "Order query failed", error);
                        Toast.makeText(this, "Could not load reports: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snapshot == null) return;

                    completedOrders.clear();
                    rejectedOrders.clear();
                    int inDelivery = 0;
                    long totalDeliveryMs = 0;
                    int deliveredWithTiming = 0;

                    for (var doc : snapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order == null) continue;
                        order.id = doc.getId();

                        if (Constants.STATUS_COMPLETED.equals(order.status)) {
                            completedOrders.add(order);
                            if (order.deliveredAt > 0 && order.orderDate > 0
                                    && order.deliveredAt >= order.orderDate) {
                                totalDeliveryMs += order.deliveredAt - order.orderDate;
                                deliveredWithTiming++;
                            }
                        } else if (Constants.STATUS_REJECTED.equals(order.status)) {
                            rejectedOrders.add(order);
                        } else if (Constants.STATUS_OUT_FOR_DELIVERY.equals(order.status)) {
                            inDelivery++;
                        }
                    }

                    completedOrders.sort((a, b) -> Long.compare(b.orderDate, a.orderDate));
                    rejectedOrders.sort((a, b) -> Long.compare(b.orderDate, a.orderDate));

                    double avgDays = deliveredWithTiming == 0 ? 0
                            : (double) totalDeliveryMs / deliveredWithTiming / 86_400_000d;
                    bindDeliveryStats(completedOrders.size(), inDelivery, rejectedOrders.size(), avgDays);

                    // Live refresh: only the panels affected by orders, and
                    // without re-animating (keeps the current view steady).
                    showOrders();
                    if (currentChart != CHART_PUBLISHED) renderCurrentChart(false);
                });
    }

    private void loadArtworks() {
        String uid = FirebaseUtil.currentUid();
        if (uid == null) return;

        FirebaseUtil.artworksRef()
                .whereEqualTo("artistId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || snapshot == null) {
                        if (error != null) Log.e("ArtistReports", "Artwork query failed", error);
                        return;
                    }
                    myArtworks.clear();
                    for (var doc : snapshot.getDocuments()) {
                        Artwork artwork = doc.toObject(Artwork.class);
                        if (artwork != null) {
                            artwork.id = doc.getId();
                            myArtworks.add(artwork);
                        }
                    }
                    if (currentChart == CHART_PUBLISHED) renderCurrentChart(false);
                });
    }

    // ----- delivery stats -----

    private void bindDeliveryStats(int delivered, int inDelivery, int rejected, double avgDays) {
        bindStat(R.id.statDelivered, String.valueOf(delivered), getString(R.string.stat_delivered));
        bindStat(R.id.statInDelivery, String.valueOf(inDelivery), getString(R.string.stat_in_delivery));
        bindStat(R.id.statRejected, String.valueOf(rejected), getString(R.string.stat_rejected));
        String avg = avgDays <= 0 ? "—" : String.format(Locale.US, "%.1f", avgDays);
        bindStat(R.id.statAvgDays, avg, getString(R.string.stat_avg_days));
    }

    private void bindStat(int includeId, String value, String label) {
        View card = findViewById(includeId);
        ((TextView) card.findViewById(R.id.tvStatValue)).setText(value);
        ((TextView) card.findViewById(R.id.tvStatLabel)).setText(label);
    }

    // ----- best-sellers -----

    private void renderBestSellers(boolean animate) {
        Map<String, Integer> soldByTitle = new LinkedHashMap<>();
        for (Order order : completedOrders) {
            if (order.items == null) continue;
            for (OrderItem item : order.items) {
                String title = item.title != null ? item.title : "Untitled";
                soldByTitle.merge(title, item.quantity, Integer::sum);
            }
        }

        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(soldByTitle.entrySet());
        ranked.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (ranked.size() > BEST_SELLER_LIMIT) ranked = ranked.subList(0, BEST_SELLER_LIMIT);

        if (applyEmptyState(chartBestSellers, ranked.isEmpty(), R.string.report_no_sales)) return;

        // Horizontal bars read top-to-bottom, so feed the ranking in reverse
        // (the highest entry gets the largest y index and lands on top).
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = ranked.size() - 1; i >= 0; i--) {
            entries.add(new BarEntry(entries.size(), ranked.get(i).getValue()));
            labels.add(ReportCharts.ellipsize(ranked.get(i).getKey(), 14));
        }

        BarDataSet set = ReportCharts.dataSet(entries, "Sold",
                ContextCompat.getColor(this, R.color.brand_accent), axisColor());
        BarData data = new BarData(set);
        data.setBarWidth(0.6f);

        XAxis x = chartBestSellers.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setLabelCount(labels.size());
        x.setDrawGridLines(false);
        chartBestSellers.getAxisLeft().setGranularity(1f);
        chartBestSellers.setData(data);
        chartBestSellers.setFitBars(true);
        if (animate) chartBestSellers.animateY(600);
        chartBestSellers.invalidate();
    }

    // ----- sales over time -----

    private void renderSalesChart(boolean animate) {
        long[] times = new long[completedOrders.size()];
        double[] values = new double[completedOrders.size()];
        for (int i = 0; i < completedOrders.size(); i++) {
            Order order = completedOrders.get(i);
            times[i] = order.deliveredAt > 0 ? order.deliveredAt : order.orderDate;
            values[i] = order.totalAmount;
        }

        if (applyEmptyState(chartSales, completedOrders.isEmpty(), R.string.report_no_sales)) return;

        Series series = salesMonthly
                ? monthlySeries(times, values, MONTH_BUCKETS)
                : weeklySeries(times, values, WEEK_BUCKETS);

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < series.values.length; i++) {
            entries.add(new BarEntry(i, (float) series.values[i]));
        }
        BarDataSet set = ReportCharts.dataSet(entries, "Revenue",
                ContextCompat.getColor(this, R.color.brand_primary), axisColor());
        set.setDrawValues(false);   // amounts get large; rely on the axis
        BarData data = new BarData(set);
        data.setBarWidth(0.55f);

        XAxis x = chartSales.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(series.labels));
        x.setLabelCount(series.labels.length);
        x.setLabelRotationAngle(series.labels.length > 6 ? -40f : 0f);
        chartSales.setData(data);
        chartSales.setFitBars(true);
        if (animate) chartSales.animateY(600);
        chartSales.invalidate();
    }

    // ----- artworks published over time -----

    private void renderPublishedChart(boolean animate) {
        long[] times = new long[myArtworks.size()];
        double[] values = new double[myArtworks.size()];
        for (int i = 0; i < myArtworks.size(); i++) {
            times[i] = myArtworks.get(i).createdAt;
            values[i] = 1;   // one artwork == one count
        }

        if (applyEmptyState(chartPublished, myArtworks.isEmpty(), R.string.report_no_published)) return;

        Series series = publishedMonthly
                ? monthlySeries(times, values, MONTH_BUCKETS)
                : weeklySeries(times, values, WEEK_BUCKETS);

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < series.values.length; i++) {
            entries.add(new BarEntry(i, (float) series.values[i]));
        }
        BarDataSet set = ReportCharts.dataSet(entries, "Published",
                ContextCompat.getColor(this, R.color.brand_primary_light), axisColor());
        BarData data = new BarData(set);
        data.setBarWidth(0.55f);

        XAxis x = chartPublished.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(series.labels));
        x.setLabelCount(series.labels.length);
        x.setLabelRotationAngle(series.labels.length > 6 ? -40f : 0f);
        chartPublished.getAxisLeft().setGranularity(1f);
        chartPublished.setData(data);
        chartPublished.setFitBars(true);
        if (animate) chartPublished.animateY(600);
        chartPublished.invalidate();
    }

    /**
     * Toggles the shared empty message over the (visible) chart. Returns true
     * when the caller should stop because there is nothing to plot.
     */
    private boolean applyEmptyState(View chart, boolean empty, int emptyTextRes) {
        if (empty) {
            chart.setVisibility(View.GONE);
            tvChartEmpty.setText(emptyTextRes);
            tvChartEmpty.setVisibility(View.VISIBLE);
            return true;
        }
        tvChartEmpty.setVisibility(View.GONE);
        chart.setVisibility(View.VISIBLE);
        return false;
    }

    // ----- page indicator -----

    private void buildDots() {
        int size = dp(7);
        int margin = dp(4);
        for (int i = 0; i < CHART_COUNT; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            dot.setBackground(shape);
            chartDots.addView(dot);
        }
    }

    private void updateDots() {
        int active = ContextCompat.getColor(this, R.color.brand_primary);
        int inactive = ContextCompat.getColor(this, R.color.divider);
        for (int i = 0; i < chartDots.getChildCount(); i++) {
            View dot = chartDots.getChildAt(i);
            ((GradientDrawable) dot.getBackground()).setColor(i == currentChart ? active : inactive);
        }
    }

    // ----- time bucketing -----

    /** Parallel label/value arrays ready to feed a chart. */
    private static final class Series {
        final String[] labels;
        final double[] values;
        Series(String[] labels, double[] values) {
            this.labels = labels;
            this.values = values;
        }
    }

    private Series weeklySeries(long[] times, double[] values, int weeks) {
        final long weekMs = 7L * 86_400_000L;
        long now = System.currentTimeMillis();
        double[] totals = new double[weeks];
        String[] labels = new String[weeks];

        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < weeks; i++) {
            long bucketStart = now - (long) (weeks - 1 - i) * weekMs;
            cal.setTimeInMillis(bucketStart);
            labels[i] = String.format(Locale.US, "%d/%d",
                    cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
        for (int i = 0; i < times.length; i++) {
            if (times[i] <= 0) continue;
            int idx = weeks - 1 - (int) ((now - times[i]) / weekMs);
            if (idx >= 0 && idx < weeks) totals[idx] += values[i];
        }
        return new Series(labels, totals);
    }

    private Series monthlySeries(long[] times, double[] values, int months) {
        double[] totals = new double[months];
        String[] labels = new String[months];
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        Calendar now = Calendar.getInstance();
        int nowIndex = now.get(Calendar.YEAR) * 12 + now.get(Calendar.MONTH);
        for (int i = 0; i < months; i++) {
            int monthIndex = nowIndex - (months - 1 - i);
            labels[i] = monthNames[((monthIndex % 12) + 12) % 12];
        }
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < times.length; i++) {
            if (times[i] <= 0) continue;
            cal.setTimeInMillis(times[i]);
            int monthIndex = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH);
            int idx = months - 1 - (nowIndex - monthIndex);
            if (idx >= 0 && idx < months) totals[idx] += values[i];
        }
        return new Series(labels, totals);
    }

    // ----- helpers -----

    private int axisColor() {
        return ContextCompat.getColor(this, R.color.text_secondary);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
