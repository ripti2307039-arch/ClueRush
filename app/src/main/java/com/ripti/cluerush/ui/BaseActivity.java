package com.ripti.cluerush.ui;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.ripti.cluerush.R;
import com.ripti.cluerush.ui.view.NeonMapBackgroundView;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String LIFECYCLE_TAG = "Lifecycle";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logLifecycle("onCreate");
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        installBackground();
        installSystemBarAppearance();
    }

    // Lifecycle callbacks: Logcat e tag:Lifecycle diye filter korle dekha jabe
    @Override
    protected void onStart() {
        super.onStart();
        logLifecycle("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        logLifecycle("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        logLifecycle("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        logLifecycle("onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        logLifecycle("onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logLifecycle("onDestroy");
    }

    private void logLifecycle(String event) {
        Log.d(LIFECYCLE_TAG, getClass().getSimpleName() + " -> " + event);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        setContentView(getLayoutInflater().inflate(layoutResId, null, false));
    }

    @Override
    public void setContentView(View content) {
        FrameLayout frame = new FrameLayout(this);
        frame.addView(new NeonMapBackgroundView(this), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        frame.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        super.setContentView(frame);
    }

    private void installBackground() {
        Drawable gradient = ContextCompat.getDrawable(this, R.drawable.bg_main);
        if (gradient == null) {
            return;
        }
        Drawable grid = new GridPatternDrawable(this);
        getWindow().setBackgroundDrawable(new LayerDrawable(new Drawable[]{gradient, grid}));
    }

    private void installSystemBarAppearance() {
        int nightMask = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = nightMask == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!isNight);
        controller.setAppearanceLightNavigationBars(!isNight);
    }

    protected void applyInsets(View root) {
        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(baseLeft + bars.left, baseTop + bars.top,
                    baseRight + bars.right, baseBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}