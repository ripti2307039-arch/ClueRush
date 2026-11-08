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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import com.google.firebase.database.DatabaseError;
import com.ripti.cluerush.data.StatsRepository;
import com.ripti.cluerush.game.StatsRules;
import com.ripti.cluerush.util.UserDisplay;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.ArenaCatalog;
import com.ripti.cluerush.data.CaptureRepository;
import com.ripti.cluerush.data.MatchLiveRepository;
import com.ripti.cluerush.data.MatchRepository;
import com.ripti.cluerush.data.TeamRepository;
import com.ripti.cluerush.databinding.ActivityGameBinding;
import com.ripti.cluerush.game.BestScoreStore;
import com.ripti.cluerush.game.GameLoop;
import com.ripti.cluerush.game.CaptureController;
import com.ripti.cluerush.game.CaptureSession;
import com.ripti.cluerush.game.GameSession;
import com.ripti.cluerush.game.HeatLevel;
import com.ripti.cluerush.game.LocationScanner;
import com.ripti.cluerush.game.MatchCoordinator;
import com.ripti.cluerush.game.MatchResult;
import com.ripti.cluerush.game.TeamMatchRules;
import com.ripti.cluerush.game.MatchRules;
import com.ripti.cluerush.game.PlayerController;
import com.ripti.cluerush.game.Rank;
import com.ripti.cluerush.game.RoundGenerator;
import com.ripti.cluerush.game.RoundScore;
import com.ripti.cluerush.game.ScoreRules;
import com.ripti.cluerush.map.GameMap;
import com.ripti.cluerush.map.ImageGameMap;
import com.ripti.cluerush.map.ImageOverviewMap;
import com.ripti.cluerush.map.IslandSpace;
import com.ripti.cluerush.map.MapMarker;
import com.ripti.cluerush.map.OsmGameMap;
import com.ripti.cluerush.map.OverviewMap;
import com.ripti.cluerush.map.OverviewMapView;
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
import com.ripti.cluerush.ui.view.RushAlertView;
import com.ripti.cluerush.util.GameMusic;
import com.ripti.cluerush.util.GeoMath;
import com.ripti.cluerush.util.MapConfig;
import com.ripti.cluerush.util.ServerClock;
import com.ripti.cluerush.data.MatchHistoryRepository;
import com.ripti.cluerush.model.MatchHistoryItem;
import com.ripti.cluerush.service.MatchService;
import com.ripti.cluerush.receiver.MatchStatusReceiver;
import com.ripti.cluerush.game.DailyScoreStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public static final String EXTRA_DAILY_DATE = "extra_daily_date";
    public static final String EXTRA_GAME_MODE = "extra_game_mode";
    public static final String EXTRA_TEAM = "extra_team";

    private static final String PICK_TAG = "MapPick";
    private static final String TASK_TAG = "GameTask";
    private static final boolean OPPONENT_ALWAYS_ON_MINIMAP = false;
    private static final int WAIT_URGENT_SECONDS = 10;
    private static final int MAX_ROUNDS = 5;
    private static final int MATCH_LOW_SECONDS = 60;
    private static final int URGENT_SECONDS = 10;
    private static final long RUSH_TICK_MS = 250L;
    private static final int RUSH_URGENT_SECONDS = 10;
    private static final int RUSH_WARN_SECONDS = 30;
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
    private static final int TEAM_BLUE = 0xFF36A3FF;
    private static final int TEAM_RED = 0xFFFF4D6D;
    private static final long DIRECTION_REFRESH_MS = 2000L;
    private static final int LONG_CLUE_CHARS = 42;
    private static final long PING_COOLDOWN_MS = 1500L;
    private static final long PING_MAX_AGE_MS = 8000L;
    private static final long PING_VISIBLE_MS = 4000L;
    private static final long PING_REMOVE_MS = 12000L;
    private static final int PING_FEED_MAX = 3;
    private static final long OFFLINE_LIMIT_MS = 30_000L;
    private static final long PRESENCE_TICK_MS = 3000L;
    private static final long TEAM_POSITION_INTERVAL_MS = 1500L;
    private static final double TEAM_POSITION_MIN_METERS = 3.0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideHint = this::dismissHint;
    private final Runnable countdownTick = this::onCountdownTick;
    private final Runnable rushTick = this::onRushTick;

    private Random random;
    private ActivityGameBinding binding;
    private GameMap gameMap;
    private OverviewMapView overviewMap;
    private TaskOverlayView taskOverlay;
    private TextView countdownText;
    private RushAlertView rushAlert;
    private long rushDeadline;
    private boolean rushAnnounced;
    private int rushLastSecond = -1;
    private String rushName = "";
    private PlayerController player;
    private GameLoop gameLoop;
    private GameSession session;
    private Arena arena;
    private Difficulty difficulty;
    private MatchCoordinator coordinator;
    private MatchStatusReceiver statusReceiver;
    private MatchCoordinator.Opponent opponent;
    private MatchCoordinator.Reason forceReason;
    private String matchCode;
    private String dailyDate;
    private boolean matchHost;
    private long matchStartAt;
    private long matchEndAt;
    private int lastShownSeconds;
    private boolean runEnded;
    private boolean resultShown;
    private int waitCountdownMax;
    private ObjectAnimator liveBlink;
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
    private GameMusic music;
    private boolean musicStopped;
    private boolean captureMode;
    private String myTeam;
    private CaptureSession captureSession;
    private CaptureController captureController;
    private String captureTaskLocationId;
    private int myCaptureCount;
    private int myCapturePoints;
    private int capturePenalty;
    private String myUid;
    private TeamRepository teamRepository;
    private TeamRepository.ListenerHandle claimsHandle;
    private MatchRepository matchRepository;
    private MatchRepository.ListenerHandle matchHandle;
    private final Map<String, String> claims = new HashMap<>();
    private final Map<String, String> playerNames = new HashMap<>();
    private final HashSet<String> expandedClues = new HashSet<>();
    private final Runnable directionTick = this::onDirectionTick;
    private TeamRepository.PingHandle pingHandle;
    private MatchLiveRepository liveRepository;
    private MatchLiveRepository.Handle liveHandle;
    private MatchLiveRepository.Handle presenceHandle;
    private final Map<String, PlayerState> livePlayers = new HashMap<>();
    private final Map<String, String> playerTeams = new HashMap<>();
    private final Map<String, Long> offlineSince = new HashMap<>();
    private boolean shortHandedShown;
    private final Runnable presenceTick = this::onPresenceTick;
    private long lastPingAt;

    private TeamRepository.ListenerHandle positionsHandle;
    private final Map<String, double[]> teamPositions = new HashMap<>();
    private long lastTeamPositionAt;
    private double lastTeamLat = Double.NaN;
    private double lastTeamLng = Double.NaN;

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
        dailyDate = getIntent().getStringExtra(EXTRA_DAILY_DATE);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (isMatch() && user == null) {
            finish();
            return;
        }
        captureMode = isMatch()
                && Match.MODE_TEAM_CAPTURE.equals(getIntent().getStringExtra(EXTRA_GAME_MODE));
        myTeam = getIntent().getStringExtra(EXTRA_TEAM);
        if (captureMode && myTeam == null) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show();
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
        if (captureMode) {
            try {
                captureSession = new CaptureSession(this, arena, difficulty, seed);
            } catch (RuntimeException e) {
                Log.e(TASK_TAG, "Could not build capture session", e);
                Toast.makeText(this, R.string.error_arena_load, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            session = new GameSession(captureSession.getRounds(), difficulty);
        } else {
            int rounds = Math.min(MAX_ROUNDS, TaskType.values().length);
            session = new GameSession(
                    RoundGenerator.generate(this, arena, rounds, difficulty, random,
                            avoidRepeats()),
                    difficulty);
        }
        configureWindow();
        MapConfig.init(this);

        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.hudLayer);
        applyInsets(binding.mapUiLayer);

        taskOverlay = new TaskOverlayView(this);
        music = new GameMusic(this);
        ViewGroup root = (ViewGroup) binding.getRoot();
        root.addView(taskOverlay, root.indexOfChild(binding.resultOverlay),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        applyInsets(taskOverlay);

        rushAlert = new RushAlertView(this);
        FrameLayout.LayoutParams rushParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        rushParams.topMargin = Math.round(28 * getResources().getDisplayMetrics().density);
        root.addView(rushAlert, root.indexOfChild(binding.resultOverlay), rushParams);

        renderScoreText();

        baseZoom = arena.getZoom();
        player = new PlayerController(arena.getCenterLatitude(), arena.getCenterLongitude(),
                arena.getBounds());

        boolean island = IslandSpace.isIsland(arena);
        if (island) {
            // player.setWalkMask(IslandSpace.walkMask(this));
        }

        if (island) {
            binding.mapView.setVisibility(View.GONE);
            binding.imageMapView.setVisibility(View.VISIBLE);
            gameMap = new ImageGameMap(binding.imageMapView, IslandSpace.mapBitmap(this));
            binding.minimapView.setBackdrop(IslandSpace.mapBitmap(this));
            binding.attributionText.setVisibility(View.GONE);
            binding.mapAttribution.setVisibility(View.GONE);
        } else {
            gameMap = new OsmGameMap(binding.mapView);
        }
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

        binding.spectateMap.setBounds(arena.getBounds());
        binding.spectateMap.setSpectate(true);
        if (island) {
            binding.spectateMap.setBackdrop(IslandSpace.mapBitmap(this));
        }

        if (island) {
            binding.overviewMapView.setVisibility(View.GONE);
            binding.overviewImageView.setVisibility(View.VISIBLE);
            overviewMap = new ImageOverviewMap(binding.overviewImageView,
                    IslandSpace.mapBitmap(this));
        } else {
            overviewMap = new OverviewMap(binding.overviewMapView);
        }
        overviewMap.setBounds(arena.getBounds());
        overviewMap.setPickListener(this::onMapPicked);
        binding.mapTitle.setText(arena.getName());
        binding.mapCloseButton.setOnClickListener(v -> closeMap());
        binding.debugToggle.setVisibility(isDebuggable() ? View.VISIBLE : View.GONE);
        binding.debugToggle.setOnClickListener(v -> setDebugTools(!debugTools));
        binding.debugCopyButton.setOnClickListener(v -> copyPick());
        setDebugTools(false);

        binding.heatChip.setVisibility(
                difficulty.showsHeat() && !captureMode ? View.VISIBLE : View.GONE);
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

        if (isMatch() && !captureMode) {
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
            if (captureMode) {
                startCapture(user.getUid());
            } else {
                coordinator.start();
                coordinator.publishPosition(player.getLatitude(), player.getLongitude());
            }
            beginMatchCountdown();
            MatchService.start(this, arena.getName(), matchEndAt);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (isMatch() && !resultShown) {
            if (statusReceiver == null) {
                statusReceiver = new MatchStatusReceiver(this, new MatchStatusReceiver.Listener() {
                    @Override
                    public void onNetworkChanged(boolean online) {
                        Toast.makeText(GameActivity.this,
                                online ? R.string.net_back : R.string.net_lost,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onBatteryLow() {
                        Toast.makeText(GameActivity.this, R.string.battery_low_warning,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            statusReceiver.register(this);
        }
    }

    @Override
    protected void onStop() {
        if (statusReceiver != null) {
            statusReceiver.unregister(this);
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameLoop == null) {
            return;
        }
        hideSystemBars();
        if (music != null && !musicStopped) {
            music.start();
        }
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
        if (music != null) {
            music.pause();
        }
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
        if (music != null) {
            music.release();
        }
        MatchService.stop(this);
        handler.removeCallbacksAndMessages(null);
        if (captureController != null) {
            captureController.stop();
        }
        if (captureMode && !runEnded) {
            forfeitCapture();
        }
        stopTeamObservers();
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
        if (liveBlink != null) {
            liveBlink.cancel();
            liveBlink = null;
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
    private boolean isDaily() {
        return dailyDate != null;
    }
    private boolean avoidRepeats() {
        return !isMatch() && !isDaily();
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
        if (captureMode && seconds <= 0 && !runEnded) {
            endCapture(true);
        }
    }

    private void renderScoreText() {
        if (captureMode && captureSession != null) {
            int secondsLeft = Math.max(0, lastShownSeconds);
            binding.scoreText.setText(getString(R.string.capture_hud,
                    countCaptured(true), countCaptured(false), formatClock(secondsLeft)));
            binding.scoreText.setTextColor(secondsLeft <= MATCH_LOW_SECONDS
                    ? LOSS_COLOR : ContextCompat.getColor(this, R.color.treasure_gold));
            return;
        }
        if (!isMatch()) {
            binding.scoreText.setText(getString(R.string.score_hud, displayedScore));
            binding.scoreText.setTextColor(displayedScore < 0
                    ? LOSS_COLOR : ContextCompat.getColor(this, R.color.treasure_gold));
            return;
        }
        int seconds = Math.max(0, lastShownSeconds);
        boolean rush = rushDeadline > 0L && !runEnded;
        if (rush) {
            seconds = Math.min(seconds, secondsToRush());
        }
        binding.scoreText.setText(getString(
                rush ? R.string.score_hud_rush : R.string.score_hud_match,
                displayedScore, formatClock(seconds)));
        binding.scoreText.setTextColor(rush || seconds <= MATCH_LOW_SECONDS
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
        updateRush(info);
        renderOpponent();
        updateOpponentMarkers();
        if (runEnded) {
            renderWaitingPanel();
        }
    }

    private void updateRush(MatchCoordinator.Opponent info) {
        if (!isMatch() || runEnded || resultShown) {
            clearRush();
            return;
        }
        if (info.deadlineLocal <= 0L || !PlayerState.PHASE_FINISHED.equals(info.phase)) {
            return;
        }
        rushDeadline = info.deadlineLocal;
        rushName = info.name.isEmpty() ? getString(R.string.match_opponent_fallback) : info.name;
        if (!rushAnnounced) {
            rushAnnounced = true;
            rushAlert.show(getString(R.string.rush_alert_title, rushName),
                    getString(R.string.rush_alert_body, formatClock(secondsToRush())));
            buzz(400L);
            handler.removeCallbacks(rushTick);
            handler.post(rushTick);
        }
    }

    private void onRushTick() {
        if (rushDeadline <= 0L || runEnded || resultShown) {
            clearRush();
            return;
        }
        int seconds = secondsToRush();
        taskOverlay.showRush(getString(R.string.rush_strip, formatClock(seconds), rushName),
                seconds <= RUSH_URGENT_SECONDS);
        renderScoreText();
        if (seconds != rushLastSecond) {
            rushLastSecond = seconds;
            if (seconds == RUSH_WARN_SECONDS) {
                buzz(200L);
                showHint(getString(R.string.rush_hint_30));
            } else if (seconds == RUSH_URGENT_SECONDS) {
                buzz(250L);
                showHint(getString(R.string.rush_hint_10));
            } else if (seconds > 0 && seconds < 5) {
                buzz(60L);
            }
        }
        handler.postDelayed(rushTick, RUSH_TICK_MS);
    }

    private int secondsToRush() {
        return (int) Math.max(0L, (rushDeadline - System.currentTimeMillis() + 999L) / 1000L);
    }

    private void clearRush() {
        handler.removeCallbacks(rushTick);
        rushDeadline = 0L;
        if (taskOverlay != null) {
            taskOverlay.hideRush();
        }
        if (rushAlert != null) {
            rushAlert.dismissNow();
        }
    }

    @SuppressWarnings("deprecation")
    private void buzz(long millis) {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(millis);
            }
        } catch (RuntimeException ignored) {
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
        clearRush();
        stopMusic();
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
        binding.waitingPanel.setVisibility(View.VISIBLE);
        binding.spectateMap.setPlayer(player.getLatitude(), player.getLongitude());
        startLiveBlink();
        renderWaitingPanel();
        showResult(getString(timeOut ? R.string.match_time_up : R.string.match_waiting_title),
                rankLine(session.getRank()), buildWaitingBody(), R.string.action_leave);
    }

    private String buildWaitingBody() {
        return buildFinalSummary(null, null);
    }

    private void renderWaitingPanel() {
        if (binding.waitingPanel.getVisibility() != View.VISIBLE) {
            return;
        }
        MatchCoordinator.Opponent info = opponent;
        int total = Math.max(1, session.getTotal());
        int mine = session.getTotalScore();
        String name = info == null || info.name.isEmpty()
                ? getString(R.string.match_opponent_fallback) : info.name;

        binding.raceTrack.setTotal(total);
        binding.raceTrack.setMe(getString(R.string.wait_you), session.getRoundsPlayed(), mine);

        if (info == null) {
            binding.countdownRing.setState(1f, "--", "", false, false);
            binding.waitingPhase.setText("");
            binding.waitingCaption.setText(getString(R.string.wait_caption_waiting, name));
            return;
        }

        boolean solving = PlayerState.PHASE_SOLVING.equals(info.phase);
        boolean exploring = PlayerState.PHASE_EXPLORING.equals(info.phase);
        boolean offline = info.offlineSeconds >= 0;
        int rounds = Math.max(0, Math.min(total, info.rounds));

        binding.raceTrack.setOpponent(name, rounds, info.score, solving);

        if (info.hasPosition) {
            binding.spectateMap.setOpponent(info.latitude, info.longitude, true);
        }

        if (info.countdownSeconds > waitCountdownMax) {
            waitCountdownMax = info.countdownSeconds;
        }
        if (offline) {
            binding.countdownRing.setState(0f, getString(R.string.wait_ring_offline), "",
                    false, true);
        } else if (info.countdownSeconds >= 0) {
            float fraction = waitCountdownMax <= 0
                    ? 0f : info.countdownSeconds / (float) waitCountdownMax;
            boolean urgent = info.countdownSeconds > 0
                    && info.countdownSeconds <= WAIT_URGENT_SECONDS;
            binding.countdownRing.setState(fraction, formatClock(info.countdownSeconds),
                    getString(R.string.wait_ring_label), urgent, false);
        } else {
            binding.countdownRing.setState(1f, "--", "", false, false);
        }

        int phaseRes = solving ? R.string.wait_phase_solving
                : exploring ? R.string.wait_phase_exploring : R.string.wait_phase_unknown;
        binding.waitingPhase.setText(getString(phaseRes, name));

        String caption;
        if (offline) {
            caption = getString(R.string.wait_caption_offline, name, info.offlineSeconds);
        } else if (info.countdownSeconds == 0) {
            caption = getString(R.string.wait_caption_settling);
        } else {
            int potential = MatchRules.potential(difficulty, total, rounds, info.score);
            if (potential < mine) {
                caption = getString(R.string.wait_caption_safe, name);
            } else if (info.score > mine) {
                caption = getString(R.string.wait_caption_ahead, name, info.score - mine);
            } else if (info.score == mine) {
                caption = getString(R.string.wait_caption_tied, name);
            } else {
                caption = getString(R.string.wait_caption_needs, name,
                        mine - info.score + 1);
            }
            if (total - rounds == 1) {
                caption = getString(R.string.wait_caption_last_round, caption);
            }
        }
        binding.waitingCaption.setText(caption);
    }

    private void startLiveBlink() {
        if (liveBlink != null) {
            return;
        }
        liveBlink = ObjectAnimator.ofFloat(binding.spectateBadge, View.ALPHA, 1f, 0.25f);
        liveBlink.setDuration(700L);
        liveBlink.setRepeatCount(ValueAnimator.INFINITE);
        liveBlink.setRepeatMode(ValueAnimator.REVERSE);
        liveBlink.start();
    }

    private void stopLiveBlink() {
        if (liveBlink != null) {
            liveBlink.cancel();
            liveBlink = null;
        }
        binding.spectateBadge.setAlpha(1f);
    }

    private void handleMatchResult(MatchResult result) {
        if (isFinishing() || isDestroyed() || resultShown) {
            return;
        }
        resultShown = true;
        runEnded = true;
        clearRush();
        binding.waitingPanel.setVisibility(View.GONE);
        stopLiveBlink();
        stopMusic();
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
        saveHistory(result.getOutcome(), result.getMyScore(),
                result.isOpponentScoreKnown() ? result.getOpponentScore() : null,
                result.getOpponentName(), result.getDecider().name());
        String gain = saveMatchStats(result);
        if (!gain.isEmpty()) {
            body = body + "\n" + gain;
        }
        MatchService.finish(this, getString(titleRes), subtitle);
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
            } else if (captureMode) {
                publishTeamPosition();
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
        if (captureMode) {
            renderCapturePanel();
            publishMarkers();
            updateProximity();
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
        if (captureMode) {
            publishCaptureMarkers();
            return;
        }
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
        binding.spectateMap.setMarkers(markers);
        overviewMap.setMarkers(markers);
    }

    private void updateProximity() {
        Round round = session.getCurrent();
        if (round == null || inputLocked) {
            return;
        }
        double lat = player.getLatitude();
        double lng = player.getLongitude();

        if (difficulty.showsHeat() && !captureMode) {
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
        if (captureMode) {
            onCaptureAction();
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

    private void startCapture(String uid) {
        captureController = new CaptureController(matchCode, uid, myTeam, captureSession,
                new CaptureRepository(), new CaptureController.Listener() {
            @Override
            public void onCapturesUpdated(
                    java.util.Map<String, CaptureRepository.CaptureRecord> captures) {
                afterCaptureChange();
            }

            @Override
            public void onCaptureSuccess(CaptureRepository.CaptureRecord record) {
                onMyCapture(record);
            }

            @Override
            public void onLocationAlreadyCaptured(
                    CaptureRepository.CaptureRecord record) {
                onTeamCapturedTarget(record);
            }

            @Override
            public void onEnemyCaptured(String locationId) {
                onEnemyCapturedTarget(locationId);
            }

            @Override
            public void onCooldown(long remainingMs) {
                showHint(getString(R.string.capture_wrong_cooldown,
                        (int) Math.max(1L, (remainingMs + 999L) / 1000L)));
            }

            @Override
            public void onError(DatabaseError error) {
                onCaptureError(error);
            }
        });
        captureController.start();
        startTeamObservers(uid);
    }

    private void startTeamObservers(String uid) {
        myUid = uid;
        teamRepository = new TeamRepository();
        claimsHandle = teamRepository.observeClaims(matchCode, myTeam,
                new TeamRepository.ClaimsListener() {
                    @Override
                    public void onClaimsChanged(Map<String, String> updated) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        claims.clear();
                        claims.putAll(updated);
                        renderCapturePanel();
                        scheduleDirectionRefresh();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TASK_TAG, "Claims unavailable: "
                                + (error == null ? "?" : error.getMessage()));
                    }
                });
        pingHandle = teamRepository.observePings(matchCode, myTeam,
                new TeamRepository.PingListener() {
                    @Override
                    public void onPing(String fromUid, String type, long at) {
                        onPingReceived(fromUid, type, at);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TASK_TAG, "Quick messages unavailable: "
                                + (error == null ? "?" : error.getMessage()));
                    }
                });

        gameMap.setOpponentColor(MapMarker.COLOR_MINE);
        binding.minimapView.setOpponentColor(MapMarker.COLOR_MINE);
        teamRepository.armPosition(matchCode, myTeam, uid);
        positionsHandle = teamRepository.observePositions(matchCode, myTeam,
                new TeamRepository.PositionListener() {
                    @Override
                    public void onPositions(Map<String, double[]> positions) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        teamPositions.clear();
                        teamPositions.putAll(positions);
                        updateTeammateMarker();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TASK_TAG, "Teammate position unavailable: "
                                + (error == null ? "?" : error.getMessage()));
                    }
                });
        publishTeamPosition();

        setupQuickMessages();
        matchRepository = new MatchRepository();
        matchHandle = matchRepository.observeMatch(matchCode,
                new MatchRepository.MatchListener() {
                    @Override
                    public void onMatchChanged(Match match) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        playerNames.clear();
                        for (com.ripti.cluerush.model.LobbyPlayer p : match.getPlayers()) {
                            playerNames.put(p.getUid(), p.getName());
                            playerTeams.put(p.getUid(), p.getTeam());
                        }
                        renderCapturePanel();
                    }

                    @Override
                    public void onMatchRemoved() {
                        // Result is computed locally from captured data; nothing to do.
                    }
                });
        liveRepository = new MatchLiveRepository();
        presenceHandle = liveRepository.armPresence(matchCode, uid, matchHost);
        liveHandle = liveRepository.observe(matchCode, new MatchLiveRepository.Listener() {
            @Override
            public void onPlayers(List<PlayerState> players) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                livePlayers.clear();
                for (PlayerState state : players) {
                    livePlayers.put(state.getUid(), state);
                }
                evaluatePresence();
            }

            @Override
            public void onRemoved() {
                // The match node may be removed after the result; nothing to do.
            }
        });
        handler.postDelayed(presenceTick, PRESENCE_TICK_MS);
    }

    private void onPresenceTick() {
        if (runEnded || isFinishing() || isDestroyed()) {
            return;
        }
        evaluatePresence();
        handler.postDelayed(presenceTick, PRESENCE_TICK_MS);
    }

    private boolean isPlayerGone(String uid, long now) {
        PlayerState state = livePlayers.get(uid);
        if (state == null) {
            return false;
        }
        if (PlayerState.PHASE_FORFEIT.equals(state.getPhase())) {
            return true;
        }
        Long since = offlineSince.get(uid);
        return !state.isConnected() && since != null && now - since >= OFFLINE_LIMIT_MS;
    }

    private void evaluatePresence() {
        if (!captureMode || runEnded || myTeam == null || myUid == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        for (PlayerState state : livePlayers.values()) {
            if (state.isConnected()) {
                offlineSince.remove(state.getUid());
            } else if (!offlineSince.containsKey(state.getUid())) {
                offlineSince.put(state.getUid(), now);
            }
        }
        String enemy = "A".equals(myTeam) ? "B" : "A";
        boolean enemySeen = false;
        boolean enemyAllGone = true;
        boolean mateGone = false;
        for (Map.Entry<String, String> entry : playerTeams.entrySet()) {
            String uid = entry.getKey();
            if (uid.equals(myUid)) {
                continue;
            }
            boolean gone = isPlayerGone(uid, now);
            if (enemy.equals(entry.getValue())) {
                enemySeen = true;
                if (!gone) {
                    enemyAllGone = false;
                }
            } else if (myTeam.equals(entry.getValue()) && gone) {
                mateGone = true;
            }
        }
        if (mateGone && !shortHandedShown) {
            shortHandedShown = true;
            showHint(getString(R.string.capture_teammate_left));
        }
        if (enemySeen && enemyAllGone) {
            endCaptureTeamLeft(myTeam);
        }
    }

    private void markPhase(String phase) {
        if (liveRepository == null || myUid == null || matchCode == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("phase", phase);
        liveRepository.update(matchCode, myUid, values);
    }

    private void forfeitCapture() {
        if (runEnded) {
            return;
        }
        runEnded = true;
        markPhase(PlayerState.PHASE_FORFEIT);
        String enemy = "A".equals(myTeam) ? "B" : "A";
        saveHistory(MatchResult.Outcome.LOSS, countCaptured(true), countCaptured(false),
                getString(R.string.capture_opponent_team, enemy), "FORFEIT_ME");
        saveTeamStats(MatchResult.Outcome.LOSS, 0);
    }

    private void leaveCaptureMatch() {
        if (liveRepository == null || matchCode == null) {
            return;
        }
        boolean othersActive = false;
        for (PlayerState state : livePlayers.values()) {
            if (!state.getUid().equals(myUid) && state.isConnected()
                    && !PlayerState.PHASE_FINISHED.equals(state.getPhase())
                    && !PlayerState.PHASE_FORFEIT.equals(state.getPhase())) {
                othersActive = true;
                break;
            }
        }
        if (othersActive) {
            liveRepository.removePlayer(matchCode, myUid);
        } else {
            liveRepository.deleteMatch(matchCode);
        }
    }

    private String saveTeamStats(MatchResult.Outcome outcome, int points) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (statsSaved || user == null || user.isAnonymous()) {
            return "";
        }
        statsSaved = true;
        new StatsRepository().recordTeamMatch(user.getUid(),
                UserDisplay.displayName(this, user), points, outcome);
        return getString(R.string.stats_gain_match,
                StatsRules.matchXp(points, outcome), StatsRules.ratingDelta(outcome));
    }

    private String teamDeciderLine(TeamMatchRules.Result result) {
        switch (result.getDecider()) {
            case CAPTURES:
                return getString(R.string.capture_decided_captures);
            case TASK_POINTS:
                return getString(R.string.capture_decided_points);
            case LAST_CAPTURE:
                return getString(R.string.capture_decided_last);
            case TEAM_LEFT:
                return getString(R.string.capture_decided_left);
            default:
                return getString(R.string.capture_decided_draw);
        }
    }

    private void stopTeamObservers() {
        if (teamRepository != null && myUid != null && claims.containsKey(myUid)) {
            teamRepository.release(matchCode, myTeam, myUid);
        }
        if (claimsHandle != null) {
            claimsHandle.remove();
            claimsHandle = null;
        }
        if (positionsHandle != null) {
            positionsHandle.remove();
            positionsHandle = null;
        }
        if (teamRepository != null && myUid != null && myTeam != null) {
            teamRepository.clearPosition(matchCode, myTeam, myUid);
        }
        if (matchHandle != null) {
            matchHandle.remove();
            matchHandle = null;
        }
        if (pingHandle != null) {
            pingHandle.remove();
            pingHandle = null;
        }
        if (liveHandle != null) {
            liveHandle.remove();
            liveHandle = null;
        }
        if (presenceHandle != null) {
            presenceHandle.remove();
            presenceHandle = null;
        }
    }

    private void publishTeamPosition() {
        if (teamRepository == null || runEnded || myUid == null || myTeam == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (lastTeamPositionAt != 0L && now - lastTeamPositionAt < TEAM_POSITION_INTERVAL_MS) {
            return;
        }
        double lat = player.getLatitude();
        double lng = player.getLongitude();
        if (!Double.isNaN(lastTeamLat) && GeoMath.distanceMeters(lastTeamLat, lastTeamLng,
                lat, lng) < TEAM_POSITION_MIN_METERS) {
            return;
        }
        lastTeamPositionAt = now;
        lastTeamLat = lat;
        lastTeamLng = lng;
        teamRepository.publishPosition(matchCode, myTeam, myUid, lat, lng);
    }

    private void updateTeammateMarker() {
        String mateUid = null;
        double[] position = null;
        for (Map.Entry<String, double[]> entry : teamPositions.entrySet()) {
            if (!entry.getKey().equals(myUid)) {
                mateUid = entry.getKey();
                position = entry.getValue();
                break;
            }
        }
        if (position == null || runEnded) {
            gameMap.setOpponent(0.0, 0.0, false, "");
            binding.minimapView.setOpponent(0.0, 0.0, false);
            return;
        }
        PlayerState state = livePlayers.get(mateUid);
        String name = state == null ? null : state.getName();
        String label = name == null || name.isEmpty()
                ? "T" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        gameMap.setOpponent(position[0], position[1], true, label);
        binding.minimapView.setOpponent(position[0], position[1], true);
    }

    private void setupQuickMessages() {
        binding.quickMessageToggle.setVisibility(View.VISIBLE);
        binding.quickMessageToggle.setOnClickListener(v -> {
            boolean open = binding.quickMessageBar.getVisibility() == View.VISIBLE;
            binding.quickMessageBar.setVisibility(open ? View.GONE : View.VISIBLE);
        });

        LinearLayout row = binding.quickMessageRow;
        row.removeAllViews();
        String[] types = {
                TeamRepository.PING_OK,
                TeamRepository.PING_HELP,
                TeamRepository.PING_ENEMY,
                TeamRepository.PING_CAPTURED,
                TeamRepository.PING_COME
        };
        for (String type : types) {
            TextView chip = new TextView(this);
            chip.setText(pingLabel(type));
            chip.setBackgroundResource(R.drawable.bg_hud_pill);
            chip.setTextColor(0xFFFFFFFF);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(dpPx(14), dpPx(8), dpPx(14), dpPx(8));
            chip.setOnClickListener(v -> sendQuickMessage(type));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dpPx(8));
            row.addView(chip, params);
        }
    }

    private String pingLabel(String type) {
        if (TeamRepository.PING_HELP.equals(type)) {
            return getString(R.string.capture_msg_help);
        }
        if (TeamRepository.PING_ENEMY.equals(type)) {
            return getString(R.string.capture_msg_enemy);
        }
        if (TeamRepository.PING_CAPTURED.equals(type)) {
            return getString(R.string.capture_msg_captured);
        }
        if (TeamRepository.PING_COME.equals(type)) {
            return getString(R.string.capture_msg_come);
        }
        return getString(R.string.capture_msg_ok);
    }

    private void sendQuickMessage(String type) {
        binding.quickMessageBar.setVisibility(View.GONE);
        if (runEnded || teamRepository == null || myUid == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastPingAt < PING_COOLDOWN_MS) {
            return;
        }
        lastPingAt = now;
        final String key = teamRepository.sendPing(matchCode, myTeam, myUid, type);
        if (key == null) {
            return;
        }
        handler.postDelayed(() -> teamRepository.removePing(matchCode, myTeam, key),
                PING_REMOVE_MS);
        showPing(getString(R.string.capture_ping_you), pingLabel(type));
    }

    private void onPingReceived(String fromUid, String type, long at) {
        if (isFinishing() || isDestroyed() || runEnded
                || fromUid.equals(myUid) || !TeamRepository.isValidPing(type)) {
            return;
        }
        if (at > 0L && ServerClock.now() - at > PING_MAX_AGE_MS) {
            return;
        }
        showPing(nameOf(fromUid), pingLabel(type));
        buzz(120L);
    }

    private void showPing(String who, String label) {
        TextView view = new TextView(this);
        view.setText(getString(R.string.capture_ping_line, who, label));
        view.setBackgroundResource(R.drawable.bg_hud_pill);
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(dpPx(12), dpPx(6), dpPx(12), dpPx(6));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dpPx(4);
        binding.pingFeed.addView(view, 0, params);
        while (binding.pingFeed.getChildCount() > PING_FEED_MAX) {
            binding.pingFeed.removeViewAt(binding.pingFeed.getChildCount() - 1);
        }
        view.animate()
                .alpha(0f)
                .setStartDelay(PING_VISIBLE_MS)
                .setDuration(800L)
                .withEndAction(() -> binding.pingFeed.removeView(view))
                .start();
    }

    private int countCaptured(boolean mine) {
        int count = 0;
        for (CaptureSession.CaptureInfo info : captureSession.getCapturedLocations().values()) {
            if (myTeam.equals(info.getTeam()) == mine) {
                count++;
            }
        }
        return count;
    }

    private int countTeam(String team) {
        int count = 0;
        for (CaptureSession.CaptureInfo info : captureSession.getCapturedLocations().values()) {
            if (team.equals(info.getTeam())) {
                count++;
            }
        }
        return count;
    }

    private void renderCapturePanel() {
        if (captureSession == null) {
            return;
        }
        int captured = captureSession.getCapturedCount();
        binding.clueIndexText.setText(getString(R.string.capture_panel_title,
                captured, CaptureSession.LOCATION_COUNT));
        binding.captureCounter.setText(getString(R.string.capture_counter,
                captured, CaptureSession.LOCATION_COUNT));
        binding.captureCounter.setVisibility(View.VISIBLE);
        binding.clueText.setVisibility(View.GONE);
        binding.captureClueScroll.setVisibility(View.VISIBLE);

        LinearLayout list = binding.captureClueList;
        list.removeAllViews();
        List<Round> rounds = captureSession.getRounds();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < rounds.size(); i++) {
                Round round = rounds.get(i);
                CaptureSession.CaptureInfo info =
                        captureSession.getCaptureInfo(round.getLocation().getId());
                if ((info == null) == (pass == 0)) {
                    list.addView(buildClueRow(i, round, info));
                }
            }
        }
        capClueListHeight(list);
    }

    private void capClueListHeight(LinearLayout list) {
        int width = getResources().getDimensionPixelSize(R.dimen.clue_panel_width)
                - 2 * getResources().getDimensionPixelSize(R.dimen.space_md);
        list.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int max = Math.round(getResources().getDisplayMetrics().heightPixels * 0.5f);
        ViewGroup.LayoutParams params = binding.captureClueScroll.getLayoutParams();
        int target = Math.min(list.getMeasuredHeight(), max);
        if (params.height != target) {
            params.height = target;
            binding.captureClueScroll.setLayoutParams(params);
        }
    }

    private int dpPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private View buildClueRow(int index, Round round, CaptureSession.CaptureInfo info) {
        final String id = round.getLocation().getId();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dpPx(6), 0, dpPx(6));

        if (info != null) {
            boolean mine = myTeam.equals(info.getTeam());
            TextView done = new TextView(this);
            done.setText(getString(mine
                    ? R.string.capture_clue_mine : R.string.capture_clue_enemy, index + 1));
            done.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            done.setTextColor(mine ? TEAM_BLUE : TEAM_RED);
            row.setAlpha(0.55f);
            row.addView(done);
            return row;
        }

        boolean expanded = expandedClues.contains(id);
        boolean longClue = round.getClue().length() > LONG_CLUE_CHARS;

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText(getString(R.string.capture_clue_line, index + 1, round.getClue()));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        title.setTextColor(0xFFFFFFFF);
        if (!expanded) {
            title.setMaxLines(2);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }
        top.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (longClue) {
            TextView more = new TextView(this);
            more.setText(expanded ? R.string.capture_collapse : R.string.capture_expand);
            more.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            more.setTextColor(ContextCompat.getColor(this, R.color.hud_text_secondary));
            more.setPadding(dpPx(8), dpPx(2), 0, dpPx(6));
            more.setOnClickListener(v -> {
                if (!expandedClues.remove(id)) {
                    expandedClues.add(id);
                }
                renderCapturePanel();
            });
            top.addView(more);
        }
        row.addView(top);

        String myClaim = myUid == null ? null : claims.get(myUid);
        boolean iAmGoing = id.equals(myClaim);

        if (iAmGoing) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            TextView status = new TextView(this);
            status.setText(directionText(round));
            status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            status.setTextColor(ContextCompat.getColor(this, R.color.treasure_gold));
            line.addView(status, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView release = new TextView(this);
            release.setText(R.string.capture_release);
            release.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            release.setTextColor(0xFFFFFFFF);
            release.setTypeface(Typeface.DEFAULT_BOLD);
            release.setPadding(dpPx(10), dpPx(4), dpPx(4), dpPx(4));
            release.setOnClickListener(v -> releaseMyClaim());
            line.addView(release);
            row.addView(line);
        }

        for (Map.Entry<String, String> entry : claims.entrySet()) {
            if (id.equals(entry.getValue()) && !entry.getKey().equals(myUid)) {
                TextView mate = new TextView(this);
                mate.setText(getString(R.string.capture_going_mate,
                        nameOf(entry.getKey())));
                mate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                mate.setTextColor(TEAM_BLUE);
                row.addView(mate);
            }
        }

        row.setOnClickListener(v -> onClueTapped(id));
        return row;
    }

    private String nameOf(String uid) {
        String name = playerNames.get(uid);
        return name == null || name.trim().isEmpty()
                ? getString(R.string.capture_teammate_fallback) : name;
    }

    private String directionText(Round round) {
        CampusLocation target = round.getLocation();
        double dy = target.getLatitude() - player.getLatitude();
        double dx = (target.getLongitude() - player.getLongitude())
                * Math.cos(Math.toRadians(player.getLatitude()));
        double angle = Math.toDegrees(Math.atan2(dx, dy));
        int slot = (int) Math.round(((angle % 360.0) + 360.0) % 360.0 / 45.0) % 8;
        String[] compass = getResources().getStringArray(R.array.capture_compass);
        return getString(R.string.capture_going_toward, compass[slot]);
    }

    private void onClueTapped(String id) {
        if (runEnded || myUid == null || teamRepository == null
                || captureSession.isCaptured(id)) {
            return;
        }
        if (id.equals(claims.get(myUid))) {
            return;
        }
        for (Map.Entry<String, String> entry : claims.entrySet()) {
            if (id.equals(entry.getValue()) && !entry.getKey().equals(myUid)) {
                showHint(getString(R.string.capture_claim_conflict,
                        nameOf(entry.getKey())));
                break;
            }
        }
        teamRepository.claim(matchCode, myTeam, myUid, id);
    }

    private void releaseMyClaim() {
        if (teamRepository != null && myUid != null) {
            teamRepository.release(matchCode, myTeam, myUid);
        }
    }

    private void scheduleDirectionRefresh() {
        handler.removeCallbacks(directionTick);
        if (myUid != null && claims.containsKey(myUid) && !runEnded) {
            handler.postDelayed(directionTick, DIRECTION_REFRESH_MS);
        }
    }

    private void onDirectionTick() {
        if (runEnded || captureSession == null) {
            return;
        }
        renderCapturePanel();
        scheduleDirectionRefresh();
    }

    private void publishCaptureMarkers() {
        Set<String> seen = session.getDiscoveredIds();
        List<MapMarker> markers = new ArrayList<>();
        for (CampusLocation location : arena.getLocations()) {
            CaptureSession.CaptureInfo info = captureSession.getCaptureInfo(location.getId());
            String team = info == null ? null
                    : (myTeam.equals(info.getTeam())
                    ? MapMarker.CAPTURE_MINE : MapMarker.CAPTURE_ENEMY);
            markers.add(new MapMarker(location.getId(), location.getName(),
                    location.getLatitude(), location.getLongitude(),
                    location.getRadiusMeters(),
                    info != null,
                    seen.contains(location.getId()),
                    team));
        }
        gameMap.setMarkers(markers);
        binding.minimapView.setMarkers(markers);
        binding.spectateMap.setMarkers(markers);
        overviewMap.setMarkers(markers);
    }

    private void afterCaptureChange() {
        if (captureSession == null || isFinishing() || isDestroyed()) {
            return;
        }
        String myClaim = myUid == null ? null : claims.get(myUid);
        if (myClaim != null && captureSession.isCaptured(myClaim)) {
            releaseMyClaim();
        }
        renderCapturePanel();
        publishMarkers();
        renderScoreText();
        if (captureSession.isComplete()) {
            endCapture(false);
        }
    }

    private void unlockAfterCapture() {
        if (runEnded) {
            return;
        }
        inputLocked = false;
        if (insideLocation != null) {
            startPulse();
        }
    }

    private void onCaptureAction() {
        if (insideLocation == null) {
            showHint(getString(R.string.hint_no_place));
            return;
        }
        String id = insideLocation.getId();
        Round round = captureSession.getRoundByLocationId(id);
        if (round == null) {
            long now = SystemClock.uptimeMillis();
            if (now - lastWrongAt < WRONG_COOLDOWN_MS) {
                return;
            }
            lastWrongAt = now;
            capturePenalty += ScoreRules.WRONG_PLACE_PENALTY;
            shakeActionButton();
            showHint(getString(R.string.capture_wrong_place, insideLocation.getName(),
                    ScoreRules.WRONG_PLACE_PENALTY));
            return;
        }
        if (captureSession.isCaptured(id)) {
            showHint(getString(R.string.capture_already_taken));
            return;
        }
        long remaining = captureController.getRemainingCooldown(id);
        if (remaining > 0L) {
            shakeActionButton();
            showHint(getString(R.string.capture_cooldown,
                    (int) Math.max(1L, (remaining + 999L) / 1000L)));
            return;
        }
        startCaptureTask(round);
    }

    private void startCaptureTask(Round round) {
        String id = round.getLocation().getId();
        BaseTaskView view = TaskFactory.create(this, round.getTask(), false);
        if (view == null) {
            Log.e(TASK_TAG, "Task content missing for " + round.getTask().getType());
            showHint(getString(R.string.error_generic));
            return;
        }
        captureController.selectLocation(id);
        captureTaskLocationId = id;
        lockInput();
        stopPulse();
        taskOverlay.begin(round.getTask().getType(), difficulty, view,
                (solved, mistakes, secondsLeft) ->
                        onCaptureTaskFinished(round, solved, mistakes, secondsLeft));
    }

    private void onCaptureTaskFinished(Round round, boolean solved, int mistakes,
                                       int secondsLeft) {
        if (runEnded || captureController == null) {
            return;
        }
        String id = round.getLocation().getId();
        captureTaskLocationId = null;

        if (!solved) {
            captureController.releaseSelection();
            captureController.submitWrongAnswer(id);
            unlockAfterCapture();
            return;
        }

        int limit = round.getTask().getType().timeLimitSeconds(difficulty);
        RoundScore score = ScoreRules.score(difficulty, true, mistakes, secondsLeft, limit,
                ScoreRules.findParSeconds(difficulty), 0);
        int points = Math.max(0, score.getTotal() - capturePenalty);
        capturePenalty = 0;
        captureController.submitCorrectAnswer(id, points);
    }

    private void onMyCapture(CaptureRepository.CaptureRecord record) {
        myCaptureCount++;
        myCapturePoints += record.getTaskPoints();
        binding.getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        CampusLocation location = findLocation(record.getLocationId());
        showHint(getString(R.string.capture_success,
                location == null ? record.getLocationId() : location.getName()));
        afterCaptureChange();
        unlockAfterCapture();
    }

    private void onEnemyCapturedTarget(String locationId) {
        closeCaptureTask();
        showHint(getString(R.string.capture_enemy_took));
        afterCaptureChange();
        unlockAfterCapture();
    }

    private void onTeamCapturedTarget(CaptureRepository.CaptureRecord record) {
        if (record.getLocationId().equals(captureTaskLocationId) && taskOverlay.isActive()) {
            closeCaptureTask();
            showHint(getString(R.string.capture_team_took));
            afterCaptureChange();
            unlockAfterCapture();
        }
    }

    private void closeCaptureTask() {
        captureTaskLocationId = null;
        if (taskOverlay.isActive()) {
            taskOverlay.cancel();
        }
        binding.getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    private void onCaptureError(DatabaseError error) {
        Log.e(TASK_TAG, "Capture error: " + (error == null ? "?" : error.getMessage()));
        captureController.releaseSelection();
        closeCaptureTask();
        Toast.makeText(this, R.string.capture_sync_error, Toast.LENGTH_LONG).show();
        unlockAfterCapture();
    }

    @Nullable
    private CampusLocation findLocation(String id) {
        for (CampusLocation location : arena.getLocations()) {
            if (location.getId().equals(id)) {
                return location;
            }
        }
        return null;
    }

    private void endCapture(boolean timeUp) {
        finishCapture(TeamMatchRules.decide(captureSession.getCapturedLocations()));
    }

    private void endCaptureTeamLeft(String winnerTeam) {
        finishCapture(TeamMatchRules.teamLeft(winnerTeam,
                captureSession.getCapturedLocations()));
    }

    private void finishCapture(TeamMatchRules.Result result) {
        if (runEnded) {
            return;
        }
        runEnded = true;
        resultShown = true;
        if (taskOverlay.isActive()) {
            taskOverlay.cancel();
        }
        captureTaskLocationId = null;
        releaseMyClaim();
        handler.removeCallbacks(directionTick);
        handler.removeCallbacks(presenceTick);
        binding.quickMessageToggle.setVisibility(View.GONE);
        binding.quickMessageBar.setVisibility(View.GONE);
        lockInput();
        stopPulse();
        stopMusic();
        clearRush();
        gameMap.hideZone();
        markPhase(PlayerState.PHASE_FINISHED);

        String enemy = "A".equals(myTeam) ? "B" : "A";
        MatchResult.Outcome outcome = result.outcomeFor(myTeam);
        int titleRes = outcome == MatchResult.Outcome.WIN ? R.string.capture_result_win
                : (outcome == MatchResult.Outcome.LOSS
                ? R.string.capture_result_lose : R.string.capture_result_draw);
        String title = getString(titleRes);
        String subtitle = getString(R.string.capture_result_score,
                result.getCapturesA(), result.getCapturesB());
        String body = teamDeciderLine(result) + "\n"
                + getString(R.string.capture_result_mine, myCaptureCount, myCapturePoints);

        saveHistory(outcome, result.getCaptures(myTeam), result.getCaptures(enemy),
                getString(R.string.capture_opponent_team, enemy),
                result.getDecider().name());
        String gain = saveTeamStats(outcome, myCapturePoints);
        if (!gain.isEmpty()) {
            body = body + "\n" + gain;
        }
        MatchService.finish(this, title, subtitle);
        showResult(title, subtitle, body, R.string.action_back_home);
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

        BaseTaskView view = TaskFactory.create(this, round.getTask(),avoidRepeats());
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
        if (captureMode) {
            leaveCaptureMatch();
        }
        if (coordinator != null) {
            coordinator.deleteMatch();
        }
        finish();
    }

    private void stopMusic() {
        musicStopped = true;
        if (music != null) {
            music.pause();
        }
    }

    private void showFinished() {
        stopMusic();
        finishedState = true;
        gameMap.hideZone();
        publishMarkers();
        animateScoreTo(session.getTotalScore());

        int score = session.getTotalScore();
        String bestLine;
        if (isDaily()) {
            Integer previousDaily = DailyScoreStore.get(this, dailyDate);
            boolean newDailyBest = DailyScoreStore.submit(this, dailyDate, score);
            bestLine = newDailyBest
                    ? getString(R.string.daily_new_best)
                    : getString(R.string.daily_best, previousDaily == null ? 0 : previousDaily);
        } else {
            int previousBest = BestScoreStore.get(this, arena.getId(), difficulty);
            boolean newBest = BestScoreStore.submit(this, arena.getId(), difficulty, score);
            bestLine = newBest
                    ? getString(R.string.score_new_best)
                    : getString(R.string.score_best, previousBest);
        }

        String summary = buildFinalSummary(bestLine, null);
        showResult(
                getString(R.string.game_finished_title),
                rankLine(session.getRank()),
                summary,
                R.string.action_back_home
        );
    }

    private boolean statsSaved;
    private boolean historySaved;

    private void saveHistory(MatchResult.Outcome outcome, int myScore,
                             @Nullable Integer opponentScore, String opponentName,
                             String decider) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (historySaved || user == null) {
            return;
        }
        historySaved = true;
        new MatchHistoryRepository(this).insert(MatchHistoryItem.create(
                user.getUid(), arena.getId(), arena.getName(), difficulty.getKey(),
                myScore, opponentScore, opponentName, outcome.name(), decider));
    }

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
        if (isMatch()) {
            saveHistory(MatchResult.Outcome.LOSS, session.getTotalScore(), null,
                    opponent == null ? getString(R.string.match_opponent_fallback)
                            : opponent.name,
                    "FORFEIT_ME");
        }
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
                    if (captureMode) {
                        forfeitCapture();
                    } else if (coordinator != null) {
                        coordinator.forfeitMine();
                    }
                    finish();
                })
                .show();
    }
}