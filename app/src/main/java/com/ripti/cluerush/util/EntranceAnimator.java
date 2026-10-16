package com.ripti.cluerush.util;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;


public final class EntranceAnimator {

    private static final long STAGGER_MS = 80L;
    private static final long DURATION_MS = 450L;

    private EntranceAnimator() {
    }

    public static void play(ViewGroup column) {
        float offset = 28f * column.getResources().getDisplayMetrics().density;
        DecelerateInterpolator interpolator = new DecelerateInterpolator(1.6f);
        for (int i = 0; i < column.getChildCount(); i++) {
            View child = column.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(offset);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * STAGGER_MS)
                    .setDuration(DURATION_MS)
                    .setInterpolator(interpolator)
                    .start();
        }
    }
}