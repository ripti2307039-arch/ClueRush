package com.ripti.cluerush.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

public class RushAlertView extends LinearLayout {

    private static final long SLIDE_MS = 280L;
    private static final long VISIBLE_MS = 4500L;

    private final TextView titleText;
    private final TextView bodyText;
    private final float density;
    private final Runnable hide = this::slideOut;

    public RushAlertView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setPadding(dp(18), dp(10), dp(18), dp(12));
        setVisibility(GONE);
        setClickable(false);
        setElevation(dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(context, R.color.alert_red));
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), Color.WHITE);
        setBackground(bg);

        titleText = new TextView(context);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        titleText.setTextColor(Color.WHITE);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setGravity(Gravity.CENTER);
        addView(titleText, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        bodyText = new TextView(context);
        bodyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        bodyText.setTextColor(Color.WHITE);
        bodyText.setGravity(Gravity.CENTER);
        LayoutParams bodyParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(2);
        addView(bodyText, bodyParams);
    }

    public void show(String title, String body) {
        removeCallbacks(hide);
        animate().cancel();
        titleText.setText(title);
        bodyText.setText(body);
        setVisibility(VISIBLE);
        setAlpha(0f);
        setTranslationY(-dp(60));
        animate().alpha(1f).translationY(0f).setDuration(SLIDE_MS).start();
        postDelayed(hide, VISIBLE_MS);
    }

    public void dismissNow() {
        removeCallbacks(hide);
        animate().cancel();
        setVisibility(GONE);
    }

    private void slideOut() {
        animate().alpha(0f).translationY(-dp(60)).setDuration(SLIDE_MS)
                .withEndAction(() -> setVisibility(View.GONE)).start();
    }

    private int dp(float value) {
        return Math.round(value * density);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(hide);
        animate().cancel();
        super.onDetachedFromWindow();
    }
}