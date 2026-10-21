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
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ripti.cluerush.data.StatsRepository;
import com.ripti.cluerush.game.StatsRules;
import com.ripti.cluerush.util.UserDisplay;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.ArenaCatalog;
import com.ripti.cluerush.data.MatchLiveRepository;
import com.ripti.cluerush.databinding.ActivityGameBinding;
import com.ripti.cluerush.game.BestScoreStore;
import com.ripti.cluerush.game.GameLoop;
import com.ripti.cluerush.game.GameSession;
import com.ripti.cluerush.game.HeatLevel;
import com.ripti.cluerush.game.LocationScanner;
import com.ripti.cluerush.game.MatchCoordinator;
import com.ripti.cluerush.game.MatchResult;
import com.ripti.cluerush.game.PlayerController;
import com.ripti.cluerush.game.Rank;
import com.ripti.cluerush.game.RoundGenerator;
import com.ripti.cluerush.game.RoundScore;
import com.ripti.cluerush.game.ScoreRules;
import com.ripti.cluerush.map.GameMap;
import com.ripti.cluerush.map.MapMarker;
import com.ripti.cluerush.map.OsmGameMap;
import com.ripti.cluerush.map.OverviewMap;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.CampusLocation;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.Match;
import com.ripti.cluerush.model.PlayerState;
import com.ripti.cluerush.model.Round;
import com.ripti.cluerush.task.BaseTaskView;
import com.ripti.cluerush.task.TaskFactory;
import com.ripti.cluerush.task.TaskOverlayView;
import com.ripti.cluerush.task.TaskType;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.util.GeoMath;
import com.ripti.cluerush.util.MapConfig;
import com.ripti.cluerush.util.ServerClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class GameActivity extends BaseActivity {

    public static final String EXTRA_ARENA_ID = "extra_arena_id";
    public static final String EXTRA_DIFFICULTY = "extra_difficulty";
    public static final String EXTRA_SEED = "extra_seed";
    public static final String EXTRA_MATCH_CODE = "extra_match_code";
    public static final String EXTRA_IS_HOST = "extra_is_host";
    public static final String EXTRA_START_AT = "extra_start_at";
    public static final String EXTRA_DURATION = "extra_duration";

    private static final String PICK_TAG = "MapPick";
    private static final String TASK_TAG = "GameTask";
    private static final boolean OPPONENT_ALWAYS_ON_MINIMAP = false;
    private static final int MAX_ROUNDS = 5;
    private static final int MATCH_LOW_SECONDS = 60;
    private static final int URGENT_SECONDS = 10;
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
    private static final long SCORE_COUNT_MS = 600L;
    private static final long COUNTDOWN_TICK_MS = 100L;
    private static final int LOSS_COLOR = 0xFFFF6B6B;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideHint = this::dismissHint;
    private final Runnable countdownTick = this::onCountdownTick;

    private Random random;
    private ActivityGameBinding binding;
    private GameMap gameMap;
    private OverviewMap overviewMap;
    private TaskOverlayView taskOverlay;
    private TextView countdownText;
    private PlayerController player;
    private GameLoop gameLoop;
    private GameSession session;
    private Arena arena;
    private Difficulty difficulty;
    private MatchCoordinator coordinator;
    private MatchCoordinator.Opponent opponent;
    private MatchCoordinator.Reason forceReason;
    private String matchCode;
    private boolean matchHost;
    private long matchStartAt;
    private long matchEndAt;
    private int lastShownSeconds;
    private boolean runEnded;
    private boolean resultShown;
    private boolean forcedEnd;
    private double baseZoom;
    private float zoomOffset;
    private float appliedZoomOffset;
    private double lastTrailLatitude = Double.NaN;
    private double lastTrailLongitude = Double.NaN;
    private double roundSeconds;
    private int displayedScore;
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
    private ValueAnimator scoreAnimator;

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
        matchCode = getIntent().getStringExtra(EXTRA_MATCH_CODE);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (isMatch() && user == null) {
            finish();
            return;
        }
        if (isMatch()) {
            ServerClock.start();
            matchHost = getIntent().getBooleanExtra(EXTRA_IS_HOST, false);
            matchStartAt = getIntent().getLongExtra(EXTRA_START_AT, System.currentTimeMillis());
            long duration = getIntent().getLongExtra(EXTRA_DURATION,
                    Match.DEFAULT_DURATION_SECONDS);
            matchEndAt = matchStartAt + duration * 1000L;
            lastShownSeconds = (int) duration;
        }

        long seed = getIntent().getLongExtra(EXTRA_SEED, System.nanoTime());
        random = new Random(seed);
        int rounds = Math.min(MAX_ROUNDS, TaskType.values().length);
        session = new GameSession(
                RoundGenerator.generate(this, arena, rounds, difficulty, random, !isMatch()),
                difficulty);
        configureWindow();
        MapConfig.init(this);

        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.hudLayer);
        applyInsets(binding.mapUiLayer);

        taskOverlay = new TaskOverlayView(this);
        ViewGroup root = (ViewGroup) binding.getRoot();
        root.addView(taskOverlay, root.indexOfChild(binding.resultOverlay),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        applyInsets(taskOverlay);

        renderScoreText();

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

        if (isMatch()) {
            coordinator = new MatchCoordinator(new MatchLiveRepository(), matchCode,
                    user.getUid(), matchHost, difficulty, session.getTotal(), matchEndAt,
                    new MatchCoordinator.Listener() {
                        @Override
                        public void onOpponent(MatchCoordinator.Opponent info) {
                            onOpponentUpdate(info);
                        }

                        @Override
                        public void onForceFinish(MatchCoordinator.Reason reason) {
                            handleForceFinish(reason);
                        }

                        @Override
                        public void onResult(MatchResult result) {
                            handleMatchResult(result);
                        }
                    });
        }

        gameLoop = new GameLoop(this::onFrame);
        showRound();

        if (isMatch()) {
            coordinator.start();
            coordinator.publishPosition(player.getLatitude(), player.getLongitude());
            beginMatchCountdown();
        }
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
        if (taskOverlay.isActive()) {
            taskOverlay.resume();
        }
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
            taskOverlay.pause();
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
        if (coordinator != null) {
            if (!resultShown && !runEnded) {
                saveForfeitStats();
            }
            coordinator.forfeitMine();
            coordinator.stop();
        }
        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }
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

    private boolean isMatch() {
        return matchCode != null;
    }

    private void beginMatchCountdown() {
        if (System.currentTimeMillis() >= matchStartAt) {
            return;
        }
        countdownText = new TextView(this);
        countdownText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72f);
        countdownText.setTextColor(ContextCompat.getColor(this, R.color.treasure_gold));
        countdownText.setTypeface(Typeface.DEFAULT_BOLD);
        countdownText.setGravity(Gravity.CENTER);
        countdownText.setBackgroundColor(ContextCompat.getColor(this, R.color.overlay_scrim));
        countdownText.setClickable(true);
        ((ViewGroup) binding.getRoot()).addView(countdownText,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        lockInput();
        onCountdownTick();
    }

    private void onCountdownTick() {
        long left = matchStartAt - System.currentTimeMillis();
        if (left <= 0L) {
            countdownText.setText(R.string.match_go);
            if (!runEnded) {
                inputLocked = false;
            }
            countdownText.animate()
                    .alpha(0f)
                    .setStartDelay(500L)
                    .setDuration(300L)
                    .withEndAction(() -> countdownText.setVisibility(View.GONE))
                    .start();
            return;
        }
        countdownText.setText(String.valueOf((left + 999L) / 1000L));
        handler.postDelayed(countdownTick, COUNTDOWN_TICK_MS);
    }

    private void updateMatchClock() {
        if (resultShown) {
            return;
        }
        long now = Math.max(System.currentTimeMillis(), matchStartAt);
        long left = matchEndAt - now;
        int seconds = (int) Math.max(0L, (left + 999L) / 1000L);
        if (seconds != lastShownSeconds) {
            lastShownSeconds = seconds;
            renderScoreText();
        }
    }

    private void renderScoreText() {
        if (!isMatch()) {
            binding.scoreText.setText(getString(R.string.score_hud, displayedScore));
            binding.scoreText.setTextColor(displayedScore < 0
                    ? LOSS_COLOR : ContextCompat.getColor(this, R.color.treasure_gold));
            return;
        }
        int seconds = Math.max(0, lastShownSeconds);
        binding.scoreText.setText(getString(R.string.score_hud_match, displayedScore,
                formatClock(seconds)));
        binding.scoreText.setTextColor(seconds <= MATCH_LOW_SECONDS
                ? LOSS_COLOR : ContextCompat.getColor(this, R.color.treasure_gold));
    }

    private static String formatClock(int seconds) {
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private void publishProgress(boolean solving) {
        if (coordinator == null) {
            return;
        }
        coordinator.publishProgress(session.getRoundsPlayed(), solving,
                session.getTotalScore(), session.getTasksSolved(), session.getWrongAttempts());
    }

    private void onOpponentUpdate(MatchCoordinator.Opponent info) {
        if (isFinishing() || isDestroyed() || resultShown) {
            return;
        }
        opponent = info;
        renderOpponent();
        updateOpponentMarkers();
        if (runEnded) {
            binding.resultBody.setText(buildWaitingBody());
        }
    }

    private void renderOpponent() {
        if (opponent == null || runEnded || resultShown) {
            binding.opponentText.setVisibility(View.GONE);
            return;
        }
        MatchCoordinator.Opponent info = opponent;
        int total = session.getTotal();
        String text;
        boolean urgent = false;
        if (info.offlineSeconds >= 0) {
            text = getString(R.string.opp_status_offline, info.name, info.offlineSeconds);
        } else if (PlayerState.PHASE_FINISHED.equals(info.phase)) {
            if (info.countdownSeconds >= 0) {
                text = getString(R.string.opp_status_finished_clock, info.name,
                        formatClock(info.countdownSeconds));
                urgent = info.countdownSeconds <= URGENT_SECONDS;
            } else {
                text = getString(R.string.opp_status_finished, info.name);
            }
        } else if (info.rounds >= total) {
            text = getString(R.string.opp_status_finishing, info.name);
        } else if (PlayerState.PHASE_SOLVING.equals(info.phase)) {
            text = getString(R.string.opp_status_solving, info.name, info.rounds + 1, total);
        } else {
            text = getString(R.string.opp_status_exploring, info.name, info.rounds + 1, total);
        }
        binding.opponentText.setText(text);
        binding.opponentText.setTextColor(urgent
                ? LOSS_COLOR : ContextCompat.getColor(this, R.color.white));
        binding.opponentText.setVisibility(View.VISIBLE);
    }

    private void updateOpponentMarkers() {
        if (opponent == null || !opponent.hasPosition || resultShown) {
            gameMap.setOpponent(0.0, 0.0, false, "");
            binding.minimapView.setOpponent(0.0, 0.0, false);
            return;
        }
        double lat = opponent.latitude;
        double lng = opponent.longitude;
        boolean near = GeoMath.distanceMeters(player.getLatitude(), player.getLongitude(),
                lat, lng) <= VISION_RADIUS_METERS;
        String label = opponent.name.isEmpty()
                ? "?" : opponent.name.substring(0, 1).toUpperCase(Locale.ROOT);
        gameMap.setOpponent(lat, lng, near, label);
        binding.minimapView.setOpponent(lat, lng, near || OPPONENT_ALWAYS_ON_MINIMAP);
    }

    private void handleForceFinish(MatchCoordinator.Reason reason) {
        if (isFinishing() || isDestroyed() || runEnded || forcedEnd) {
            return;
        }
        forcedEnd = true;
        forceReason = reason;
        if (taskOverlay.isActive()) {
            taskOverlay.abort();
            return;
        }
        if (session.getRoundsPlayed() > session.getIndex()) {
            session.advance();
        }
        endRun();
    }

    private void endRun() {
        if (runEnded) {
            return;
        }
        runEnded = true;
        lockInput();
        stopPulse();
        gameMap.hideZone();
        publishMarkers();
        binding.opponentText.setVisibility(View.GONE);

        boolean completed = session.isFinished() && !forcedEnd;
        if (completed) {
            long left = Math.max(0L, matchEndAt - System.currentTimeMillis());
            int totalSeconds = (int) ((matchEndAt - matchStartAt) / 1000L);
            session.setFinishBonus(
                    ScoreRules.finishBonus(difficulty, (int) (left / 1000L), totalSeconds));
        }
        animateScoreTo(session.getTotalScore());
        coordinator.finishMine(session.getTotalScore(), session.getTasksSolved(),
                session.getWrongAttempts(), session.getRoundsPlayed(),
                session.getRoundPointsCsv());
        showWaiting();
    }

    private void showWaiting() {
        boolean timeOut = forcedEnd && forceReason != MatchCoordinator.Reason.CLINCH;
        showResult(getString(timeOut ? R.string.match_time_up : R.string.match_waiting_title),
                rankLine(session.getRank()), buildWaitingBody(), R.string.action_leave);
    }

    private String buildWaitingBody() {
        StringBuilder text = new StringBuilder();
        String name = opponent == null
                ? getString(R.string.match_opponent_fallback) : opponent.name;
        text.append(getString(R.string.match_waiting_for, name));
        if (opponent != null) {
            if (opponent.offlineSeconds >= 0) {
                appendLine(text, getString(R.string.opp_status_offline, name,
                        opponent.offlineSeconds));
            } else if (opponent.countdownSeconds >= 0) {
                appendLine(text, getString(R.string.match_waiting_clock, name,
                        formatClock(opponent.countdownSeconds)));
            } else {
                appendLine(text, getString(R.string.match_waiting_progress, name,
                        Math.min(opponent.rounds + 1, session.getTotal()), session.getTotal()));
            }
        }
        text.append("\n");
        text.append(buildFinalSummary(null, null));
        return text.toString();
    }

    private void handleMatchResult(MatchResult result) {
        if (isFinishing() || isDestroyed() || resultShown) {
            return;
        }
        resultShown = true;
        runEnded = true;
        finishedState = true;
        lockInput();
        stopPulse();
        taskOverlay.cancel();
        if (mapOpen) {
            closeMap();
        }
        gameMap.hideZone();
        gameMap.setOpponent(0.0, 0.0, false, "");
        binding.minimapView.setOpponent(0.0, 0.0, false);
        binding.opponentText.setVisibility(View.GONE);
        animateScoreTo(result.getMyScore());

        int titleRes;
        switch (result.getOutcome()) {
            case WIN:
                titleRes = R.string.match_result_win;
                break;
            case LOSS:
                titleRes = R.string.match_result_loss;
                break;
            default:
                titleRes = R.string.match_result_draw;
                break;
        }
        String opponentScore = result.isOpponentScoreKnown()
                ? String.valueOf(result.getOpponentScore()) : "\u2014";
        String subtitle = getString(R.string.match_vs_line, result.getMyScore(),
                result.getOpponentName(), opponentScore);
        String body = deciderLine(result) + "\n" + rankLine(session.getRank()) + "\n"
                + buildFinalSummary(null, result);
        String gain = saveMatchStats(result);
        if (!gain.isEmpty()) {
            body = body + "\n" + gain;
        }
        showResult(getString(titleRes), subtitle, body, R.string.action_back_home);
    }

    private String deciderLine(MatchResult result) {
        String name = result.getOpponentName();
        switch (result.getDecider()) {
            case SCORE:
                return getString(R.string.decided_score);
            case SOLVED:
                return getString(R.string.decided_solved);
            case WRONG_PLACES:
                return getString(R.string.decided_wrong);
            case TIME:
                return getString(R.string.decided_time);
            case FORFEIT:
                return getString(R.string.decided_forfeit, name);
            case DISCONNECT_OPPONENT:
                return getString(R.string.decided_disconnect_opp, name);
            case DISCONNECT_ME:
                return getString(R.string.decided_disconnect_me);
            case CLINCH:
                return getString(R.string.decided_clinch, name);
            default:
                return getString(R.string.decided_draw);
        }
    }

    private void onFrame(float deltaSeconds) {
        if (!inputLocked) {
            roundSeconds += deltaSeconds;
        }
        if (isMatch()) {
            updateMatchClock();
        }
        boolean moved = player.update(deltaSeconds);
        if (moved) {
            gameMap.moveCamera(player.getLatitude(), player.getLongitude());
            gameMap.setPlayerPosition(player.getLatitude(), player.getLongitude());
            binding.minimapView.setPlayer(player.getLatitude(), player.getLongitude());
            addTrailIfNeeded();
            if (coordinator != null) {
                coordinator.publishPosition(player.getLatitude(), player.getLongitude());
                updateOpponentMarkers();
            }
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
        roundSeconds = 0.0;
        binding.clueIndexText.setText(getString(R.string.clue_index,
                session.getIndex() + 1, session.getTotal()));
        binding.clueText.setText(round.getClue());
        publishMarkers();
        publishProgress(false);
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
            HeatLevel heat = HeatLevel.forDistance(distance,
                    round.getLocation().getRadiusMeters());
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
            startTask();
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (now - lastWrongAt < WRONG_COOLDOWN_MS) {
            return;
        }
        lastWrongAt = now;
        session.registerWrongAttempt();
        shakeActionButton();
        animateScoreTo(session.getTotalScore());
        showScoreDelta(-ScoreRules.WRONG_PLACE_PENALTY);
        publishProgress(false);
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

    private void animateScoreTo(int target) {
        if (scoreAnimator != null) {
            scoreAnimator.cancel();
        }
        if (target == displayedScore) {
            return;
        }
        scoreAnimator = ValueAnimator.ofInt(displayedScore, target);
        scoreAnimator.setDuration(SCORE_COUNT_MS);
        scoreAnimator.addUpdateListener(animator -> {
            displayedScore = (int) animator.getAnimatedValue();
            renderScoreText();
        });
        scoreAnimator.start();
    }

    private void showScoreDelta(int delta) {
        if (delta == 0) {
            return;
        }
        float rise = 16f * getResources().getDisplayMetrics().density;
        binding.scoreDeltaText.animate().cancel();
        binding.scoreDeltaText.setText(getString(
                delta > 0 ? R.string.score_gain : R.string.score_loss, Math.abs(delta)));
        binding.scoreDeltaText.setTextColor(delta > 0
                ? ContextCompat.getColor(this, R.color.mint) : LOSS_COLOR);
        binding.scoreDeltaText.setAlpha(1f);
        binding.scoreDeltaText.setTranslationY(0f);
        binding.scoreDeltaText.animate()
                .translationY(-rise)
                .alpha(0f)
                .setStartDelay(500L)
                .setDuration(900L)
                .start();
    }

    private void startTask() {
        Round round = session.getCurrent();
        if (round == null) {
            return;
        }
        lockInput();
        stopPulse();
        publishProgress(true);

        BaseTaskView view = TaskFactory.create(this, round.getTask(), !isMatch());
        if (view == null) {
            Log.e(TASK_TAG, "Task content missing for " + round.getTask().getType()
                    + ", skipping the task");
            onTaskFinished(true, 0, 0);
            return;
        }
        taskOverlay.begin(round.getTask().getType(), difficulty, view,
                (solved, mistakes, secondsLeft) -> onTaskFinished(solved, mistakes, secondsLeft));
    }

    private void onTaskFinished(boolean solved, int mistakes, int secondsLeft) {
        Round round = session.getCurrent();
        if (round == null) {
            return;
        }
        int limit = round.getTask().getType().timeLimitSeconds(difficulty);
        RoundScore score = session.recordTask(solved, mistakes, secondsLeft, limit, roundSeconds);
        animateScoreTo(session.getTotalScore());
        showScoreDelta(score.getTotal());
        publishProgress(false);

        if (isMatch()) {
            if (forcedEnd) {
                if (session.getRoundsPlayed() > session.getIndex()) {
                    session.advance();
                }
                endRun();
                return;
            }
            if (session.getRoundsPlayed() >= session.getTotal()) {
                session.advance();
                endRun();
                return;
            }
        }

        if (solved) {
            binding.getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        String title = getString(solved ? R.string.task_solved_title : R.string.task_failed_title);
        showResult(title, round.getLocation().getName(),
                buildTaskResult(solved, mistakes, secondsLeft, score), R.string.action_continue);
    }

    private String buildTaskResult(boolean solved, int mistakes, int secondsLeft,
                                   RoundScore score) {
        StringBuilder text = new StringBuilder();
        text.append(solved
                ? getString(R.string.task_solved_body, secondsLeft, mistakes)
                : getString(R.string.task_failed_body, mistakes));
        if (solved) {
            appendLine(text, getString(R.string.score_base, score.getBase()));
            if (score.getTimeBonus() > 0) {
                appendLine(text, getString(R.string.score_time, score.getTimeBonus()));
            }
            if (score.getMistakePenalty() > 0) {
                appendLine(text, getString(R.string.score_mistakes, score.getMistakePenalty()));
            }
            if (score.getStreakBonus() > 0) {
                appendLine(text, getString(R.string.score_streak,
                        String.format(Locale.US, "%.1f", score.getStreakMultiplier()),
                        score.getStreakBonus()));
            }
        }
        if (score.getFindBonus() > 0) {
            appendLine(text, getString(R.string.score_find, score.getFindBonus()));
        }
        appendLine(text, getString(R.string.score_round_total, score.getTotal()));
        return text.toString();
    }

    private String buildFinalSummary(@Nullable String bestLine, @Nullable MatchResult result) {
        StringBuilder text = new StringBuilder();
        if (isMatch()) {
            text.append(getString(R.string.match_progress_body,
                    session.getRoundsPlayed(), session.getTotal(), session.getWrongAttempts()));
        } else {
            text.append(getString(R.string.game_finished_body,
                    session.getTotal(), session.getWrongAttempts()));
        }
        appendLine(text, getString(R.string.task_summary,
                session.getTasksSolved(), session.getTotal()));

        int[] opponentPoints = result == null ? null : result.getOpponentRoundPoints();
        List<RoundScore> scores = session.getRoundScores();
        for (int i = 0; i < scores.size() && i < session.getTotal(); i++) {
            Round round = session.getRound(i);
            text.append("\n");
            if (i == 0) {
                text.append("\n");
            }
            String title = getString(round.getTask().getType().getTitleRes());
            if (opponentPoints != null) {
                String theirs = i < opponentPoints.length
                        ? String.valueOf(opponentPoints[i]) : "\u2014";
                text.append(getString(R.string.score_round_line_vs, i + 1, title,
                        scores.get(i).getTotal(), theirs));
            } else {
                text.append(getString(R.string.score_round_line, i + 1, title,
                        scores.get(i).getTotal()));
            }
        }

        text.append("\n");
        appendLine(text, getString(R.string.score_earned, session.getEarnedPoints()));
        if (session.getPenaltyPoints() > 0) {
            appendLine(text, getString(R.string.score_wrong_places, session.getPenaltyPoints()));
        }
        if (session.getPerfectBonus() > 0) {
            appendLine(text, getString(R.string.score_perfect, session.getPerfectBonus()));
        }
        if (session.getFinishBonus() > 0) {
            appendLine(text, getString(R.string.score_finish_bonus, session.getFinishBonus()));
        }
        appendLine(text, getString(R.string.score_final, session.getTotalScore()));
        if (bestLine != null) {
            appendLine(text, bestLine);
        }
        return text.toString();
    }

    private String rankLine(Rank rank) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rank.getStars(); i++) {
            stars.append('\u2605');
        }
        return getString(R.string.rank_line, rank.getLabel(), stars.toString()).trim();
    }

    private static void appendLine(StringBuilder builder, String line) {
        builder.append("\n").append(line);
    }

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

    private void lockInput() {
        inputLocked = true;
        binding.joystick.reset();
        player.setInput(0f, 0f);
    }

    private void onResultContinue() {
        if (isMatch() && runEnded) {
            if (resultShown) {
                leaveAfterResult();
            } else {
                confirmLeaveWaiting();
            }
            return;
        }
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

    private void leaveAfterResult() {
        if (coordinator != null) {
            coordinator.deleteMatch();
        }
        finish();
    }

    private void showFinished() {
        finishedState = true;
        gameMap.hideZone();
        publishMarkers();
        animateScoreTo(session.getTotalScore());

        int score = session.getTotalScore();

        int previousBest = BestScoreStore.get(
                this, arena.getId(), difficulty
        );

        boolean newBest = BestScoreStore.submit(
                this, arena.getId(), difficulty, score
        );

        String bestLine = newBest
                ? getString(R.string.score_new_best)
                : getString(R.string.score_best, previousBest);

        String summary = buildFinalSummary(bestLine, null);
        showResult(
                getString(R.string.game_finished_title),
                rankLine(session.getRank()),
                summary,
                R.string.action_back_home
        );
    }

    private boolean statsSaved;

    private String saveMatchStats(MatchResult result) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (statsSaved || user == null || user.isAnonymous()) {
            return "";
        }
        statsSaved = true;
        new StatsRepository().recordMatch(user.getUid(), UserDisplay.displayName(this, user),
                arena.getId(), difficulty.getKey(), result.getMyScore(), result.getOutcome());
        return getString(R.string.stats_gain_match,
                StatsRules.matchXp(result.getMyScore(), result.getOutcome()),
                StatsRules.ratingDelta(result.getOutcome()));
    }

    private void saveForfeitStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (statsSaved || user == null || user.isAnonymous()) {
            return;
        }
        statsSaved = true;
        new StatsRepository().recordMatch(user.getUid(), UserDisplay.displayName(this, user),
                arena.getId(), difficulty.getKey(), 0, MatchResult.Outcome.LOSS);
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

    private void confirmLeaveWaiting() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.leave_waiting_title)
                .setMessage(R.string.leave_waiting_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_leave, (dialog, which) -> finish())
                .show();
    }

    private void confirmExit() {
        if (isMatch() && resultShown) {
            leaveAfterResult();
            return;
        }
        if (isMatch() && runEnded) {
            confirmLeaveWaiting();
            return;
        }
        boolean match = isMatch();
        new MaterialAlertDialogBuilder(this)
                .setTitle(match ? R.string.forfeit_title : R.string.exit_match_title)
                .setMessage(match ? R.string.forfeit_message : R.string.exit_match_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_leave_match, (dialog, which) -> {
                    if (coordinator != null) {
                        coordinator.forfeitMine();
                    }
                    finish();
                })
                .show();
    }
}