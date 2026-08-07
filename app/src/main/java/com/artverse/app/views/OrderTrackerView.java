package com.artverse.app.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.artverse.app.R;
import com.artverse.app.utils.OrderStatus;

/**
 * Live delivery tracker shown on order cards: four nodes
 * (Placed - Confirmed - Out for delivery - Delivered) where everything up to
 * the order's current stage is highlighted. Because the order screens listen
 * to Firestore, the bar advances by itself as the artist and admin act.
 */
public class OrderTrackerView extends LinearLayout {

    private final View[] dots = new View[4];
    private final View[] lines = new View[3];
    private final TextView[] labels = new TextView[4];

    public OrderTrackerView(Context context) {
        this(context, null);
    }

    public OrderTrackerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_order_tracker, this, true);

        dots[0] = findViewById(R.id.dot0);
        dots[1] = findViewById(R.id.dot1);
        dots[2] = findViewById(R.id.dot2);
        dots[3] = findViewById(R.id.dot3);
        lines[0] = findViewById(R.id.line0);
        lines[1] = findViewById(R.id.line1);
        lines[2] = findViewById(R.id.line2);
        labels[0] = findViewById(R.id.lbl0);
        labels[1] = findViewById(R.id.lbl1);
        labels[2] = findViewById(R.id.lbl2);
        labels[3] = findViewById(R.id.lbl3);

        for (int i = 0; i < labels.length; i++) {
            labels[i].setText(OrderStatus.STEP_LABELS[i]);
        }
    }

    /**
     * Highlights the track up to {@code step}. Pass a step from
     * {@link OrderStatus#trackerStep(String)}; a rejected order
     * ({@link OrderStatus#STEP_REJECTED}) hides the bar, since it never
     * finished the journey.
     */
    public void setStep(int step) {
        if (step == OrderStatus.STEP_REJECTED) {
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);

        // A fully delivered order turns green; in-flight stages use brand blue.
        int reachedColor = ContextCompat.getColor(getContext(),
                step >= OrderStatus.STEP_DELIVERED ? R.color.status_success : R.color.brand_primary);
        int pendingColor = ContextCompat.getColor(getContext(), R.color.divider);
        int reachedText = ContextCompat.getColor(getContext(), R.color.text_secondary);
        int pendingText = ContextCompat.getColor(getContext(), R.color.text_hint);

        for (int i = 0; i < dots.length; i++) {
            boolean reached = i <= step;
            dots[i].setBackgroundTintList(ColorStateList.valueOf(reached ? reachedColor : pendingColor));
            labels[i].setTextColor(reached ? reachedText : pendingText);
            labels[i].setTypeface(null, reached ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
        }
        for (int i = 0; i < lines.length; i++) {
            lines[i].setBackgroundColor(i < step ? reachedColor : pendingColor);
        }
    }
}
