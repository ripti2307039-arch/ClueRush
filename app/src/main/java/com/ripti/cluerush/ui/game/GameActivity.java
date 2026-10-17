package com.ripti.cluerush.ui.game;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.ArenaCatalog;
import com.ripti.cluerush.databinding.ActivityGameBinding;
import com.ripti.cluerush.game.GameLoop;
import com.ripti.cluerush.game.GameSession;
import com.ripti.cluerush.game.HeatLevel;
import com.ripti.cluerush.game.PlayerController;
import com.ripti.cluerush.game.RoundGenerator;
import com.ripti.cluerush.game.TargetInfo;
import com.ripti.cluerush.game.TargetTracker;
import com.ripti.cluerush.map.GameMap;
import com.ripti.cluerush.map.MapMarker;
import com.ripti.cluerush.map.OsmGameMap;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.model.Match;
import com.ripti.cluerush.model.Round;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.util.GeoMath;
import com.ripti.cluerush.util.MapConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GameActivity extends BaseActivity {

    public static final String EXTRA_ARENA_ID = "extra_arena_id";
    public static final String EXTRA_DIFFICULTY = "extra_difficulty";

    private static final int ROUND_COUNT = 5;
    private static final float MAX_ZOOM_OUT = 0.35f;
    private static final float ZOOM_RESPONSE = 3f;
    private static final float ZOOM_APPLY_THRESHOLD = 0.004f;
    private static final float INACTIVE_BUTTON_ALPHA = 0.6f;
    private static final double TRAIL_SPACING_METERS = 7.0;
    private static final long HINT_VISIBLE_MS = 2600L;
    private static final long PANEL_ANIMATION_MS = 220L;

    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideHint = this::dismissHint;

    private ActivityGameBinding binding;
    private GameMap gameMap;
    private PlayerController player;
    private GameLoop gameLoop;
    private GameSession session;
    private Arena arena;
    private boolean hard;
    private double baseZoom;
    private float zoomOffset;
    private float appliedZoomOffset;
    private double lastTrailLatitude = Double.NaN;
    private double lastTrailLongitude = Double.NaN;
    private boolean inputLocked;
    private boolean finishedState;
    private boolean inside;
    private boolean zoneShown;
    private boolean panelExpanded = true;
    private HeatLevel currentHeat;
    private TargetInfo lastInfo;
    private ObjectAnimator pulseAnimator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        arena = ArenaCatalog.findById(this, getIntent().getStringExtra(EXTRA_ARENA_ID));
        if (arena == null || arena.getLocations().isEmpty()) {
            Toast.makeText(this, R.string.error_arena_load, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        hard = Match.DIFFICULTY_HARD.equals(getIntent().getStringExtra(EXTRA_DIFFICULTY));
        session = new GameSession(RoundGenerator.generate(arena, ROUND_COUNT, random));

        configureWindow();
        MapConfig.init(this);

        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.hudLayer);

        baseZoom = arena.getZoom();
        player = new PlayerController(arena.getCenterLatitude(), arena.getCenterLongitude(),
                arena.getBounds());

        gameMap = new OsmGameMap(binding.mapView);
        gameMap.setBounds(arena.getBounds());
        gameMap.setNightStyle(isNightMode());
        gameMap.setZoom(baseZoom);
        gameMap.moveCamera(player.getLatitude(), player.getLongitude());
        gameMap.setLabelsVisible(!hard);

        binding.joystick.setListener((x, y) -> {
            if (!inputLocked) {
                player.setInput(x, y);
            }
        });
        binding.actionButton.setOnClickListener(v -> onActionPressed());
        binding.exitButton.setOnClickListener(v -> confirmExit());
        binding.panelToggle.setOnClickListener(v -> setPanelExpanded(!panelExpanded));
        binding.resultButton.setOnClickListener(v -> onResultContinue());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });

        gameLoop = new GameLoop(this::onFrame);
        showRound();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameLoop == null) {
            return;
        }
        hideSystemBars();
        gameMap.onResume();
        gameLoop.start();
        if (inside && !inputLocked) {
            startPulse();
        }
    }

    @Override
    protected void onPause() {
        if (gameLoop != null) {
            gameLoop.stop();
            binding.joystick.reset();
            player.setInput(0f, 0f);
            stopPulse();
            handler.removeCallbacks(hideHint);
            gameMap.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (gameMap != null) {
            gameMap.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && binding != null) {
            hideSystemBars();
        }
    }

    private void onFrame(float deltaSeconds) {
        boolean moved = player.update(deltaSeconds);
        if (moved) {
            gameMap.moveCamera(player.getLatitude(), player.getLongitude());
            addTrailIfNeeded();
        }
        binding.avatarView.setMoving(player.isMoving());
        binding.avatarView.setFacingRight(player.isFacingRight());
        updateZoom(deltaSeconds);
        updateTarget();
    }

    private void updateZoom(float deltaSeconds) {
        float target = -MAX_ZOOM_OUT * player.getSpeedFraction();
        zoomOffset += (target - zoomOffset) * Math.min(1f, deltaSeconds * ZOOM_RESPONSE);
        if (Math.abs(zoomOffset - appliedZoomOffset) > ZOOM_APPLY_THRESHOLD) {
            appliedZoomOffset = zoomOffset;
            gameMap.setZoom(baseZoom + zoomOffset);
        }
    }

    private void addTrailIfNeeded() {
        double latitude = player.getLatitude();
        double longitude = player.getLongitude();
        boolean first = Double.isNaN(lastTrailLatitude);
        if (first || GeoMath.distanceMeters(lastTrailLatitude, lastTrailLongitude,
                latitude, longitude) >= TRAIL_SPACING_METERS) {
            gameMap.addTrailPoint(latitude, longitude);
            lastTrailLatitude = latitude;
            lastTrailLongitude = longitude;
        }
    }

    private void showRound() {
        Round round = session.getCurrent();
        if (round == null) {
            return;
        }
        inside = false;
        zoneShown = false;
        currentHeat = null;
        lastInfo = null;
        gameMap.hideZone();
        stopPulse();
        binding.actionButton.setAlpha(INACTIVE_BUTTON_ALPHA);
        binding.clueIndexText.setText(getString(R.string.clue_index,
                session.getIndex() + 1, session.getTotal()));
        binding.clueText.setText(round.getClue());
        publishMarkers();
        updateTarget();
    }

    private void publishMarkers() {
        Set<String> done = session.getCompletedLocationIds();
        List<MapMarker> markers = new ArrayList<>();
        for (CampusLocation location : arena.getLocations()) {
            markers.add(new MapMarker(location.getId(), location.getName(),
                    location.getLatitude(), location.getLongitude(),
                    done.contains(location.getId())));
        }
        gameMap.setMarkers(markers);
    }

    private void updateTarget() {
        Round round = session.getCurrent();
        if (round == null || inputLocked) {
            return;
        }
        CampusLocation target = round.getLocation();
        TargetInfo info = TargetTracker.compute(player.getLatitude(), player.getLongitude(),
                target, hard);
        lastInfo = info;

        binding.compassView.setBearing((float) info.getBearingDegrees());

        if (info.getHeat() != currentHeat) {
            currentHeat = info.getHeat();
            applyHeat(currentHeat);
        }

        if (info.isNearZone() != zoneShown) {
            zoneShown = info.isNearZone();
            if (zoneShown) {
                gameMap.showZone(target.getLatitude(), target.getLongitude(),
                        target.getRadiusMeters());
            } else {
                gameMap.hideZone();
            }
        }

        if (info.isInside() != inside) {
            inside = info.isInside();
            if (inside) {
                onZoneEntered();
            } else {
                onZoneLeft();
            }
        }
    }

    private void onZoneEntered() {
        gameMap.playZoneRipple();
        binding.getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        binding.actionButton.setAlpha(1f);
        startPulse();
    }

    private void onZoneLeft() {
        stopPulse();
        binding.actionButton.setAlpha(INACTIVE_BUTTON_ALPHA);
    }

    private void applyHeat(HeatLevel level) {
        int label;
        int background;
        int foreground;
        switch (level) {
            case HOT:
                label = R.string.heat_hot;
                background = R.color.heat_hot;
                foreground = R.color.white;
                break;
            case WARM:
                label = R.string.heat_warm;
                background = R.color.treasure_gold;
                foreground = R.color.navy_950;
                break;
            case COOL:
                label = R.string.heat_cool;
                background = R.color.mint;
                foreground = R.color.navy_950;
                break;
            default:
                label = R.string.heat_cold;
                background = R.color.heat_cold;
                foreground = R.color.white;
                break;
        }
        binding.heatChip.setText(label);
        binding.heatChip.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, background)));
        binding.heatChip.setTextColor(ContextCompat.getColor(this, foreground));
    }

    private void onActionPressed() {
        if (inputLocked || lastInfo == null) {
            return;
        }
        if (lastInfo.isInside()) {
            openLocationResult();
        } else {
            showFarHint(lastInfo);
        }
    }

    private void showFarHint(TargetInfo info) {
        int step = hard ? 50 : 10;
        int rounded = Math.max(step, (int) (Math.round(info.getDistanceMeters() / step) * step));
        String direction = getResources().getStringArray(R.array.directions)[
                TargetTracker.directionIndex(info.getBearingDegrees())];
        showHint(getString(R.string.hint_far, rounded, direction));
    }

    private void showHint(String message) {
        handler.removeCallbacks(hideHint);
        binding.hintBanner.animate().cancel();
        binding.hintBanner.setText(message);
        binding.hintBanner.setVisibility(View.VISIBLE);
        binding.hintBanner.animate().alpha(1f).setDuration(160L).start();
        handler.postDelayed(hideHint, HINT_VISIBLE_MS);
    }

    private void dismissHint() {
        binding.hintBanner.animate().cancel();
        binding.hintBanner.animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction(() -> binding.hintBanner.setVisibility(View.GONE))
                .start();
    }

    private void openLocationResult() {
        Round round = session.getCurrent();
        if (round == null) {
            return;
        }
        lockInput();
        stopPulse();
        gameMap.hideZone();
        showResult(getString(R.string.location_reached_title),
                round.getLocation().getName(),
                getString(R.string.location_reached_body),
                R.string.action_continue);
    }

    private void lockInput() {
        inputLocked = true;
        binding.joystick.reset();
        player.setInput(0f, 0f);
    }

    private void onResultContinue() {
        if (finishedState) {
            finish();
            return;
        }
        session.advance();
        if (session.isFinished()) {
            showFinished();
            return;
        }
        binding.resultOverlay.setVisibility(View.GONE);
        inputLocked = false;
        showRound();
    }

    private void showFinished() {
        finishedState = true;
        gameMap.hideZone();
        publishMarkers();
        showResult(getString(R.string.game_finished_title), "",
                getString(R.string.game_finished_body, session.getTotal()),
                R.string.action_back_home);
    }

    private void showResult(String title, String subtitle, String body,
                            @StringRes int buttonLabel) {
        binding.resultTitle.setText(title);
        binding.resultLocation.setText(subtitle);
        binding.resultLocation.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        binding.resultBody.setText(body);
        binding.resultButton.setText(buttonLabel);

        if (binding.resultOverlay.getVisibility() != View.VISIBLE) {
            binding.resultOverlay.setAlpha(0f);
            binding.resultOverlay.setVisibility(View.VISIBLE);
            binding.resultOverlay.animate().alpha(1f).setDuration(200L).start();
            binding.resultCard.setScaleX(0.92f);
            binding.resultCard.setScaleY(0.92f);
            binding.resultCard.animate().scaleX(1f).scaleY(1f).setDuration(220L).start();
        }
    }

    private void setPanelExpanded(boolean expanded) {
        panelExpanded = expanded;
        float shift = expanded ? 0f : binding.clueCard.getWidth();
        binding.cluePanel.animate().translationX(shift).setDuration(PANEL_ANIMATION_MS).start();
        binding.panelToggle.animate().rotation(expanded ? 0f : 180f)
                .setDuration(PANEL_ANIMATION_MS).start();
    }

    private void startPulse() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            return;
        }
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.actionButton,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f));
        pulseAnimator.setDuration(520L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.start();
    }

    private void stopPulse() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        binding.actionButton.setScaleX(1f);
        binding.actionButton.setScaleY(1f);
    }

    private void configureWindow() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    private boolean isNightMode() {
        int mask = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mask == Configuration.UI_MODE_NIGHT_YES;
    }

    private void confirmExit() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exit_match_title)
                .setMessage(R.string.exit_match_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_leave_match, (dialog, which) -> finish())
                .show();
    }
}