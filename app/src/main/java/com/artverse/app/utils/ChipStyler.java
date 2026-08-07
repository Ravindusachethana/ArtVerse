package com.artverse.app.utils;

import androidx.core.content.ContextCompat;

import com.artverse.app.R;
import com.google.android.material.chip.Chip;

/**
 * Shared look for category-filter chips app-wide: a flat fill with no
 * outline, highlighted in the theme's accent color when selected (rather
 * than Material's default checkmark + stroke treatment).
 */
public final class ChipStyler {

    private ChipStyler() { }

    public static void styleCategoryChip(Chip chip) {
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipStrokeWidth(0f);
        chip.setChipBackgroundColor(ContextCompat.getColorStateList(chip.getContext(), R.color.chip_category_background));
        chip.setTextColor(ContextCompat.getColorStateList(chip.getContext(), R.color.chip_category_text));
    }
}
