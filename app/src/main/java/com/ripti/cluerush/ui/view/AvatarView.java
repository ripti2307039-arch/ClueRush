package com.ripti.cluerush.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ripti.cluerush.R;

public class AvatarView extends View {

    private static final long CYCLE_MS = 1200L;
    private static final double TWO_PI = Math.PI * 2.0;

    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF shadowRect = new RectF();
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

    private float phase;
    private boolean moving;
    private boolean facingRight = true;

    public AvatarView(Context context) {
        this(context, null);
    }

    public AvatarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        int blue = ContextCompat.getColor(context, R.color.rush_blue);

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(ContextCompat.getColor(context, R.color.avatar_shadow));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2.5f * density);
        ringPaint.setColor(blue);

        bodyPaint.setStyle(Paint.Style.FILL);
        bodyPaint.setColor(blue);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f * density);
        borderPaint.setColor(ContextCompat.getColor(context, R.color.white));

        eyePaint.setStyle(Paint.Style.FILL);
        eyePaint.setColor(ContextCompat.getColor(context, R.color.white));

        pupilPaint.setStyle(Paint.Style.FILL);
        pupilPaint.setColor(ContextCompat.getColor(context, R.color.navy_950));

        animator.setDuration(CYCLE_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) * 0.22f;

        float bob = moving
                ? (float) Math.abs(Math.sin(phase * Math.PI * 3.0)) * r * 0.28f
                : (float) Math.sin(phase * TWO_PI) * r * 0.03f;
        float bodyY = cy - bob;

        ringPaint.setAlpha((int) (150 * (1f - phase)));
        canvas.drawCircle(cx, cy, r * (1.3f + 0.7f * phase), ringPaint);

        float shadowScale = 1f - bob / (r * 1.2f);
        float shadowHalfWidth = r * 0.95f * shadowScale;
        float shadowHalfHeight = r * 0.28f * shadowScale;
        float shadowY = cy + r * 1.05f;
        shadowRect.set(cx - shadowHalfWidth, shadowY - shadowHalfHeight,
                cx + shadowHalfWidth, shadowY + shadowHalfHeight);
        canvas.drawOval(shadowRect, shadowPaint);

        canvas.drawCircle(cx, bodyY, r, bodyPaint);
        canvas.drawCircle(cx, bodyY, r, borderPaint);

        float direction = facingRight ? 1f : -1f;
        float shift = direction * r * 0.16f;
        float pupilShift = direction * r * 0.06f;
        float eyeY = bodyY - r * 0.08f;
        float eyeGap = r * 0.34f;
        for (int side = -1; side <= 1; side += 2) {
            float eyeX = cx + shift + side * eyeGap;
            canvas.drawCircle(eyeX, eyeY, r * 0.2f, eyePaint);
            canvas.drawCircle(eyeX + pupilShift, eyeY, r * 0.095f, pupilPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            animator.resume();
        } else {
            animator.pause();
        }
    }
}