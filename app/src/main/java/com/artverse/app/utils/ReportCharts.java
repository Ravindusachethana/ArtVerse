package com.artverse.app.utils;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.List;

/**
 * Shared MPAndroidChart styling so every report chart across the app reads as
 * one design system - flat bars, no chart chrome, brand-coloured axes. Keeps
 * the per-screen chart code focused on the data rather than cosmetics.
 */
public final class ReportCharts {

    private ReportCharts() { }

    /** Strips the chart chrome and applies the shared axis styling. */
    public static void styleBarChart(BarChart chart, int axisColor) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setScaleEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setPinchZoom(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setExtraBottomOffset(6f);
        chart.setNoDataText("");

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setGranularity(1f);
        x.setTextColor(axisColor);
        x.setTextSize(10f);

        YAxis left = chart.getAxisLeft();
        left.setDrawAxisLine(false);
        left.setGridColor(0x22000000);
        left.setTextColor(axisColor);
        left.setTextSize(10f);
        left.setAxisMinimum(0f);

        chart.getAxisRight().setEnabled(false);
    }

    /** A flat, brand-coloured bar series with value labels on top. */
    public static BarDataSet dataSet(List<BarEntry> entries, String label, int barColor, int valueColor) {
        BarDataSet set = new BarDataSet(entries, label);
        set.setColor(barColor);
        set.setDrawValues(true);
        set.setValueTextColor(valueColor);
        set.setValueTextSize(10f);
        set.setHighlightEnabled(false);
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Whole counts render without a trailing ".0".
                return value == Math.rint(value)
                        ? String.valueOf((long) value)
                        : String.valueOf(value);
            }
        });
        return set;
    }

    /** Trims a long artwork title so it fits a chart axis label. */
    public static String ellipsize(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }
}
