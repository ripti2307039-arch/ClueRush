package com.ripti.cluerush.ui.lobby;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.ArenaCatalog;
import com.ripti.cluerush.data.MatchRepository;
import com.ripti.cluerush.databinding.ActivityLobbyBinding;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.Match;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.ui.game.GameActivity;
import com.ripti.cluerush.util.EntranceAnimator;
import com.ripti.cluerush.util.LobbyMusic;
import com.ripti.cluerush.util.ServerClock;

public class LobbyActivity extends BaseActivity {

    public static final String EXTRA_CODE = "extra_code";

    private static final long START_DELAY_MS = 5000L;
    private static final float START_DIM_ALPHA = 0.4f;

    private final MatchRepository repository = new MatchRepository();

    private ActivityLobbyBinding binding;
    private LobbyMusic music;
    private PlayerAdapter adapter;
    private MatchRepository.ListenerHandle handle;
    private Match currentMatch;
    private String code;
    private String uid;
    private boolean leaving;
    private boolean launched;
    private boolean startClicked;
    private boolean startWasActive;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        code = getIntent().getStringExtra(EXTRA_CODE);
        if (user == null || code == null) {
            finish();
            return;
        }
        uid = user.getUid();
        ServerClock.start();

        binding = ActivityLobbyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        music = new LobbyMusic(this);

        binding.codeText.setText(code);

        adapter = new PlayerAdapter();
        binding.playerList.setLayoutManager(new LinearLayoutManager(this));
        binding.playerList.setItemAnimator(null);
        binding.playerList.setAdapter(adapter);

        binding.copyButton.setOnClickListener(v -> copyCode());
        binding.shareButton.setOnClickListener(v -> shareCode());
        binding.readyButton.setOnClickListener(v -> toggleReady());
        binding.startButton.setOnClickListener(v -> startMatch());
        binding.leaveButton.setOnClickListener(v -> confirmLeave());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
            }
        });

        if (savedInstanceState == null) {
            EntranceAnimator.play(binding.contentColumn);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null) {
            return;
        }
        music.start();
        handle = repository.observeMatch(code, new MatchRepository.MatchListener() {
            @Override
            public void onMatchChanged(Match match) {
                handleMatch(match);
            }

            @Override
            public void onMatchRemoved() {
                handleRemoved();
            }
        });
    }

    @Override
    protected void onStop() {
        if (music != null) {
            music.pause();
        }
        if (handle != null) {
            handle.remove();
            handle = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (music != null) {
            music.release();
        }
        super.onDestroy();
    }

    private boolean isStarted(Match match) {
        return (Match.STATUS_STARTING.equals(match.getStatus())
                || Match.STATUS_PLAYING.equals(match.getStatus()))
                && match.getStartAt() > 0L;
    }

    private void handleMatch(Match match) {
        if (launched) {
            return;
        }
        currentMatch = match;
        if (isStarted(match)) {
            launchGame(match);
            return;
        }
        if (!match.allPlayersReady()) {
            startClicked = false;
        }
        render(match);
    }

    private void startMatch() {
        if (currentMatch == null || startClicked || !currentMatch.isHost(uid)) {
            return;
        }
        if (!currentMatch.allPlayersReady()) {
            shakeStartButton();
            boolean missingOpponent =
                    currentMatch.getPlayers().size() < currentMatch.getMaxPlayers();
            Snackbar.make(binding.getRoot(),
                    missingOpponent ? R.string.start_need_opponent : R.string.start_need_ready,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }
        startClicked = true;
        binding.startButton.setEnabled(false);
        repository.startMatch(code, ServerClock.now() + START_DELAY_MS);
    }

    private void shakeStartButton() {
        float shift = 10f * getResources().getDisplayMetrics().density;
        ObjectAnimator.ofFloat(binding.startButton, View.TRANSLATION_X,
                        0f, -shift, shift, -shift * 0.6f, shift * 0.6f, 0f)
                .setDuration(280L)
                .start();
    }

    private void popStartButton() {
        binding.startButton.setScaleX(0.92f);
        binding.startButton.setScaleY(0.92f);
        binding.startButton.animate().scaleX(1f).scaleY(1f).setDuration(220L).start();
    }

    private void launchGame(Match match) {
        launched = true;
        leaving = true;
        if (match.isHost(uid)) {
            repository.setStatus(code, Match.STATUS_PLAYING);
        }
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_ARENA_ID, match.getArenaId());
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, match.getDifficulty());
        intent.putExtra(GameActivity.EXTRA_SEED, match.getSeed());
        intent.putExtra(GameActivity.EXTRA_MATCH_CODE, code);
        intent.putExtra(GameActivity.EXTRA_IS_HOST, match.isHost(uid));
        intent.putExtra(GameActivity.EXTRA_START_AT, match.getStartAt() - ServerClock.offset());
        intent.putExtra(GameActivity.EXTRA_DURATION, (long) match.getDurationSeconds());
        startActivity(intent);
        finish();
    }

    private void handleRemoved() {
        if (leaving || launched || isFinishing() || isDestroyed()) {
            return;
        }
        leaving = true;
        Toast.makeText(this, R.string.host_left, Toast.LENGTH_LONG).show();
        finish();
    }

    @StringRes
    private int difficultyLabel(String difficulty) {
        if (Match.DIFFICULTY_HARD.equals(difficulty)) {
            return R.string.difficulty_hard_short;
        }
        if (Match.DIFFICULTY_MEDIUM.equals(difficulty)) {
            return R.string.difficulty_medium_short;
        }
        return R.string.difficulty_easy_short;
    }

    private void render(Match match) {
        Arena arena = ArenaCatalog.findById(this, match.getArenaId());
        binding.arenaText.setText(arena != null ? arena.getName() : match.getArenaId());
        binding.difficultyText.setText(difficultyLabel(match.getDifficulty()));

        adapter.submit(match.getPlayers(), match.getMaxPlayers(), uid);

        boolean host = match.isHost(uid);
        boolean allReady = match.allPlayersReady();

        binding.readyButton.setText(match.isReady(uid)
                ? R.string.action_unready
                : R.string.action_ready);
        binding.readyButton.setEnabled(!startClicked);

        boolean startActive = host && allReady && !startClicked;
        binding.startButton.setVisibility(host ? View.VISIBLE : View.GONE);
        binding.startButton.setEnabled(!startClicked);
        binding.startButton.animate()
                .alpha(startActive || startClicked ? 1f : START_DIM_ALPHA)
                .setDuration(200L)
                .start();
        if (startActive && !startWasActive) {
            popStartButton();
        }
        startWasActive = startActive;

        if (startClicked) {
            binding.statusText.setText(R.string.lobby_status_starting);
        } else if (match.getPlayers().size() < match.getMaxPlayers()) {
            binding.statusText.setText(R.string.lobby_status_waiting_opponent);
        } else if (allReady) {
            binding.statusText.setText(host
                    ? R.string.lobby_status_all_ready
                    : R.string.lobby_status_waiting_host);
        } else {
            binding.statusText.setText(R.string.lobby_status_waiting_ready);
        }
    }

    private void toggleReady() {
        if (currentMatch == null) {
            return;
        }
        repository.setReady(code, uid, !currentMatch.isReady(uid));
    }

    private void copyCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.lobby_match_code), code));
        }
        Snackbar.make(binding.getRoot(), R.string.code_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void shareCode() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_code_text, code));
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)));
    }

    private void confirmLeave() {
        boolean host = currentMatch != null && currentMatch.isHost(uid);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.leave_title)
                .setMessage(host ? R.string.leave_message_host : R.string.leave_message_player)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_leave, (dialog, which) -> {
                    leaving = true;
                    repository.leaveMatch(code, uid, host);
                    finish();
                })
                .show();
    }
}