package com.ripti.cluerush.task;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

public abstract class BaseTaskView extends FrameLayout {

    public interface Listener {
        void onFinished(boolean solved, int mistakes);
    }

    protected static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    protected static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    protected static final int ERROR_RED = 0xFFFF6B6B;

    private final float density;
    private Listener listener;
    private boolean done;

    protected BaseTaskView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public abstract int getMistakes();


    public void lock() {
        done = true;
    }

    protected boolean isDone() {
        return done;
    }

    protected void finishTask(boolean solved) {
        if (done) {
            return;
        }
        done = true;
        if (listener != null) {
            listener.onFinished(solved, getMistakes());
        }
    }

    protected int dp(float value) {
        return Math.round(value * density);
    }

    protected int color(@ColorRes int res) {
        return ContextCompat.getColor(getContext(), res);
    }

    protected GradientDrawable box(int fill, int stroke, float radiusDp, float strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0f) {
            drawable.setStroke(Math.max(1, dp(strokeDp)), stroke);
        }
        return drawable;
    }

    protected TextView label(String text, float sp, int textColor, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(textColor);
        view.setGravity(Gravity.CENTER);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    protected void shake(View view) {
        float shift = 8f * density;
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                        0f, -shift, shift, -shift * 0.6f, shift * 0.6f, 0f)
                .setDuration(260L)
                .start();
    }
}