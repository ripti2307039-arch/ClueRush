package com.ripti.cluerush.ui.game;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
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
import com.ripti.cluerush.game.LocationScanner;
import com.ripti.cluerush.game.PlayerController;
import com.ripti.cluerush.game.RoundGenerator;
import com.ripti.cluerush.map.GameMap;
import com.ripti.cluerush.map.MapMarker;
import com.ripti.cluerush.map.OsmGameMap;
import com.ripti.cluerush.map.OverviewMap;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.Round;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.util.GeoMath;
import com.ripti.cluerush.util.MapConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class GameActivity extends BaseActivity {

    public static final String EXTRA_ARENA_ID = "extra_arena_id";
    public static final String EXTRA_DIFFICULTY = "extra_difficulty";

    private static final String PICK_TAG = "MapPick";
    private static final int ROUND_COUNT = 5;
    private static final double VISION_RADIUS_METERS = 80.0;
    private static final float MAX_ZOOM_OUT = 0.35f;
    private static final float ZOOM_RESPONSE = 3f;
    private static final float ZOOM_APPLY_THRESHOLD = 0.004f;
    private static final float INACTIVE_BUTTON_ALPHA = 0.6f;
    private static final double TRAIL_SPACING_METERS = 7.0;
    private static final long HINT_VISIBLE_MS = 2600L;
    private static final long PANEL_ANIMATION_MS = 220L;
    private static final long WRONG_COOLDOWN_MS = 900L;
    private static final long MAP_FADE_MS = 180L;

    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideHint = this::dismissHint;

    private ActivityGameBinding binding;
    private GameMap gameMap;
    private OverviewMap overviewMap;
    private PlayerController player;
    private GameLoop gameLoop;
    private GameSession session;
    private Arena arena;
    private Difficulty difficulty;
    private double baseZoom;
    private float zoomOffset;
    private float appliedZoomOffset;
    private double lastTrailLatitude = Double.NaN;
    private double lastTrailLongitude = Double.NaN;
    private boolean inputLocked;
    private boolean finishedState;
    private boolean panelExpanded = true;
    private boolean mapOpen;
    private boolean debugTools;
    private String lastPickText;
    private long lastWrongAt;
    private HeatLevel currentHeat;
    private CampusLocation insideLocation;
    private CampusLocation zoneLocation;
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

        difficulty = Difficulty.fromKey(getIntent().getStringExtra(EXTRA_DIFFICULTY));
        session = new GameSession(
                RoundGenerator.generate(arena, ROUND_COUNT, difficulty, random));

        configureWindow();
        MapConfig.init(this);

        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.hudLayer);
        applyInsets(binding.mapUiLayer);

        baseZoom = arena.getZoom();
        player = new PlayerController(arena.getCenterLatitude(), arena.getCenterLongitude(),
                arena.getBounds());

        gameMap = new OsmGameMap(binding.mapView);
        gameMap.setBounds(arena.getBounds());
        gameMap.setNightStyle(isNightMode());
        gameMap.setZoom(baseZoom);
        gameMap.moveCamera(player.getLatitude(), player.getLongitude());
        gameMap.setPlayerPosition(player.getLatitude(), player.getLongitude());
        gameMap.setVisionRadius(VISION_RADIUS_METERS);

        binding.minimapView.setBounds(arena.getBounds());
        binding.minimapView.setVisionMeters(VISION_RADIUS_METERS);
        binding.minimapView.setPlayer(player.getLatitude(), player.getLongitude());
        binding.minimapView.setOnClickListener(v -> openMap());

        overviewMap = new OverviewMap(binding.overviewMapView);
        overviewMap.setBounds(arena.getBounds());
        overviewMap.setPickListener(this::onMapPicked);
        binding.mapTitle.setText(arena.getName());
        binding.mapCloseButton.setOnClickListener(v -> closeMap());
        binding.debugToggle.setVisibility(isDebuggable() ? View.VISIBLE : View.GONE);
        binding.debugToggle.setOnClickListener(v -> setDebugTools(!debugTools));
        binding.debugCopyButton.setOnClickListener(v -> copyPick());
        setDebugTools(false);

        binding.heatChip.setVisibility(difficulty.showsHeat() ? View.VISIBLE : View.GONE);
        binding.actionButton.setAlpha(INACTIVE_BUTTON_ALPHA);

        binding.joystick.setListener((x, y) -> {
            if (!inputLocked && !mapOpen) {
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
                if (mapOpen) {
                    closeMap();
                } else {
                    confirmExit();
                }
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
        if (mapOpen) {
            overviewMap.onResume();
        }
        gameLoop.start();
        if (insideLocation != null && !inputLocked) {
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
            if (mapOpen) {
                overviewMap.onPause();
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (gameMap != null) {
            gameMap.onDestroy();
        }
        if (overviewMap != null) {
            overviewMap.onDestroy();
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
            gameMap.setPlayerPosition(player.getLatitude(), player.getLongitude());
            binding.minimapView.setPlayer(player.getLatitude(), player.getLongitude());
            addTrailIfNeeded();
        }
        binding.avatarView.setMoving(player.isMoving());
        binding.avatarView.setFacingRight(player.isFacingRight());
        updateZoom(deltaSeconds);
        updateProximity();
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
        currentHeat = null;
        binding.clueIndexText.setText(getString(R.string.clue_index,
                session.getIndex() + 1, session.getTotal()));
        binding.clueText.setText(round.getClue());
        publishMarkers();
        updateProximity();
    }

    private void publishMarkers() {
        Set<String> done = session.getCompletedLocationIds();
        Set<String> seen = session.getDiscoveredIds();
        List<MapMarker> markers = new ArrayList<>();
        for (CampusLocation location : arena.getLocations()) {
            markers.add(new MapMarker(location.getId(), location.getName(),
                    location.getLatitude(), location.getLongitude(),
                    location.getRadiusMeters(),
                    done.contains(location.getId()),
                    seen.contains(location.getId())));
        }
        gameMap.setMarkers(markers);
        binding.minimapView.setMarkers(markers);
        overviewMap.setMarkers(markers);
    }

    private void updateProximity() {
        Round round = session.getCurrent();
        if (round == null || inputLocked) {
            return;
        }
        double lat = player.getLatitude();
        double lng = player.getLongitude();

        if (difficulty.showsHeat()) {
            double distance = LocationScanner.distanceTo(round.getLocation(), lat, lng);
            HeatLevel heat = HeatLevel.forDistance(distance);
            if (heat != currentHeat) {
                currentHeat = heat;
                applyHeat(heat);
            }
        }

        LocationScanner.Hit hit = LocationScanner.nearest(arena, lat, lng);
        if (hit == null) {
            return;
        }

        CampusLocation zoneNow = hit.isNearZone() ? hit.getLocation() : null;
        if (!sameLocation(zoneNow, zoneLocation)) {
            zoneLocation = zoneNow;
            if (zoneNow != null) {
                gameMap.showZone(zoneNow.getLatitude(), zoneNow.getLongitude(),
                        zoneNow.getRadiusMeters());
            } else {
                gameMap.hideZone();
            }
        }

        CampusLocation insideNow = hit.isInside() ? hit.getLocation() : null;
        if (!sameLocation(insideNow, insideLocation)) {
            insideLocation = insideNow;
            if (insideNow != null) {
                onAreaEntered(insideNow);
            } else {
                onAreaLeft();
            }
        }
    }

    private static boolean sameLocation(CampusLocation a, CampusLocation b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getId().equals(b.getId());
    }

    private void onAreaEntered(CampusLocation location) {
        gameMap.playZoneRipple();
        binding.getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        binding.actionButton.setAlpha(1f);
        startPulse();
        if (session.discover(location.getId())) {
            publishMarkers();
            showHint(getString(R.string.hint_discovered, location.getName()));
        }
    }

    private void onAreaLeft() {
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
        Round round = session.getCurrent();
        if (inputLocked || mapOpen || round == null) {
            return;
        }
        if (insideLocation == null) {
            showHint(getString(difficulty.showsHeat()
                    ? R.string.hint_no_place_easy : R.string.hint_no_place));
            return;
        }
        if (insideLocation.getId().equals(round.getLocation().getId())) {
            openLocationResult();
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (now - lastWrongAt < WRONG_COOLDOWN_MS) {
            return;
        }
        lastWrongAt = now;
        session.registerWrongAttempt();
        shakeActionButton();
        showHint(getString(R.string.hint_wrong_place, insideLocation.getName(),
                GameSession.WRONG_PLACE_PENALTY));
    }

    private void shakeActionButton() {
        float shift = 10f * getResources().getDisplayMetrics().density;
        ObjectAnimator.ofFloat(binding.actionButton, View.TRANSLATION_X,
                        0f, -shift, shift, -shift * 0.6f, shift * 0.6f, 0f)
                .setDuration(280L)
                .start();
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

    // ---- Full-screen map + debug tools ----

    private void openMap() {
        if (mapOpen || inputLocked) {
            return;
        }
        mapOpen = true;
        binding.joystick.reset();
        player.setInput(0f, 0f);
        overviewMap.setPlayer(player.getLatitude(), player.getLongitude());
        overviewMap.onResume();
        binding.mapOverlay.animate().cancel();
        binding.mapOverlay.setAlpha(0f);
        binding.mapOverlay.setVisibility(View.VISIBLE);
        binding.mapOverlay.animate().alpha(1f).setDuration(MAP_FADE_MS).start();
        overviewMap.fit();
    }

    private void closeMap() {
        if (!mapOpen) {
            return;
        }
        mapOpen = false;
        binding.mapOverlay.animate().cancel();
        binding.mapOverlay.animate()
                .alpha(0f)
                .setDuration(MAP_FADE_MS)
                .withEndAction(() -> {
                    if (!mapOpen) {
                        binding.mapOverlay.setVisibility(View.GONE);
                        overviewMap.onPause();
                    }
                })
                .start();
    }

    private void setDebugTools(boolean on) {
        debugTools = on;
        overviewMap.setRevealAll(on);
        overviewMap.setPickEnabled(on);
        binding.debugPanel.setVisibility(on ? View.VISIBLE : View.GONE);
        binding.debugToggle.setText(on ? R.string.debug_on : R.string.debug_off);
        lastPickText = null;
        overviewMap.clearPicked();
        if (on) {
            binding.debugCoordText.setText(R.string.debug_pick_hint);
        }
    }

    private void onMapPicked(double lat, double lng) {
        if (!debugTools) {
            return;
        }
        overviewMap.setPicked(lat, lng);

        CampusLocation nearest = null;
        double best = Double.MAX_VALUE;
        for (CampusLocation location : arena.getLocations()) {
            double distance = LocationScanner.distanceTo(location, lat, lng);
            if (distance < best) {
                best = distance;
                nearest = location;
            }
        }

        String coords = String.format(Locale.US, "%.6f, %.6f", lat, lng);
        lastPickText = String.format(Locale.US, "lat=\"%.6f\" lng=\"%.6f\"", lat, lng);
        String nearestId = nearest == null ? "-" : nearest.getId();
        Log.i(PICK_TAG, lastPickText + "  nearest=" + nearestId
                + " (" + Math.round(best) + " m)");
        binding.debugCoordText.setText(getString(R.string.debug_pick_result,
                coords, nearestId, (int) Math.round(best)));
    }

    private void copyPick() {
        if (lastPickText == null) {
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("coordinates", lastPickText));
            Toast.makeText(this, R.string.debug_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isDebuggable() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    // ---- Result overlay ----

    private void openLocationResult() {
        Round round = session.getCurrent();
        if (round == null) {
            return;
        }
        lockInput();
        stopPulse();
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
        if (insideLocation != null) {
            startPulse();
        }
    }

    private void showFinished() {
        finishedState = true;
        gameMap.hideZone();
        publishMarkers();
        showResult(getString(R.string.game_finished_title), "",
                getString(R.string.game_finished_body, session.getTotal(),
                        session.getWrongAttempts()),
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