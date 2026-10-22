package com.ripti.cluerush;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ripti.cluerush.data.ArenaCatalog;
import com.ripti.cluerush.data.MatchRepository;
import com.ripti.cluerush.databinding.ActivityMainBinding;
import com.ripti.cluerush.databinding.DialogJoinCodeBinding;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.ui.auth.LoginActivity;
import com.ripti.cluerush.ui.game.GameActivity;
import com.ripti.cluerush.ui.home.ArenaAdapter;
import com.ripti.cluerush.ui.leaderboard.LeaderboardActivity;
import com.ripti.cluerush.ui.lobby.LobbyActivity;
import com.ripti.cluerush.ui.profile.ProfileActivity;
import com.ripti.cluerush.util.EntranceAnimator;
import com.ripti.cluerush.util.NetworkUtils;
import com.ripti.cluerush.util.UserDisplay;

import java.util.List;

public class MainActivity extends BaseActivity {

    private static final String CODE_PATTERN = "[A-Z0-9]{6}";

    private interface DifficultyListener {
        void onChosen(String difficulty);
    }

    private final MatchRepository matchRepository = new MatchRepository();

    // Android 13+ e notification dekhate runtime permission lage
    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> { });

    private ActivityMainBinding binding;
    private Arena selectedArena;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        String name = UserDisplay.displayName(this, user);
        binding.greetingText.setText(getString(R.string.greeting_user, name));
        binding.avatarButton.setText(UserDisplay.initial(name));
        binding.avatarButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        List<Arena> arenas = ArenaCatalog.getAll(this);
        if (arenas.isEmpty()) {
            binding.arenaList.setVisibility(View.GONE);
            binding.createMatchButton.setEnabled(false);
            binding.practiceButton.setEnabled(false);
            showMessage(getString(R.string.error_no_arenas));
        } else {
            selectedArena = arenas.get(0);
            ArenaAdapter adapter = new ArenaAdapter(arenas, arena -> selectedArena = arena);
            binding.arenaList.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            binding.arenaList.setItemAnimator(null);
            binding.arenaList.setAdapter(adapter);
        }

        binding.createMatchButton.setOnClickListener(v -> {
            if (selectedArena != null) {
                showDifficultyDialog(
                        getString(R.string.create_dialog_title, selectedArena.getName()),
                        R.string.action_create, this::createMatch);
            }
        });
        binding.joinButton.setOnClickListener(v -> showJoinDialog());
        binding.leaderboardButton.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));
        binding.practiceButton.setOnClickListener(v -> {
            if (selectedArena != null) {
                showDifficultyDialog(
                        getString(R.string.practice_dialog_title, selectedArena.getName()),
                        R.string.action_start, this::openGame);
            }
        });

        if (savedInstanceState == null) {
            EntranceAnimator.play(binding.contentColumn);
        }

        askNotificationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null && binding != null) {
            openLogin();
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void showDifficultyDialog(String title, @StringRes int positiveLabel,
                                      DifficultyListener listener) {
        String[] options = {
                getString(R.string.difficulty_easy),
                getString(R.string.difficulty_medium),
                getString(R.string.difficulty_hard)
        };
        Difficulty[] values = Difficulty.values();
        int[] choice = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setSingleChoiceItems(options, 0, (dialog, which) -> choice[0] = which)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(positiveLabel, (dialog, which) ->
                        listener.onChosen(values[choice[0]].getKey()))
                .show();
    }

    private void createMatch(String difficulty) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }
        if (!NetworkUtils.isOnline(this)) {
            showMessage(getString(R.string.error_no_internet));
            return;
        }

        setBusy(true, true);
        matchRepository.createMatch(user.getUid(), UserDisplay.displayName(this, user),
                selectedArena.getId(), difficulty, new MatchRepository.CreateCallback() {
                    @Override
                    public void onCreated(String code) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        setBusy(false, true);
                        openLobby(code);
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        setBusy(false, true);
                        showMessage(getString(messageRes));
                    }
                });
    }

    private void showJoinDialog() {
        DialogJoinCodeBinding dialogBinding = DialogJoinCodeBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.join_with_code)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_join, null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    CharSequence raw = dialogBinding.codeInput.getText();
                    String code = raw == null ? "" : raw.toString().trim().toUpperCase();
                    if (!code.matches(CODE_PATTERN)) {
                        dialogBinding.codeLayout.setError(getString(R.string.join_error));
                        return;
                    }
                    dialog.dismiss();
                    joinMatch(code);
                }));
        dialog.show();
    }

    private void joinMatch(String code) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }
        if (!NetworkUtils.isOnline(this)) {
            showMessage(getString(R.string.error_no_internet));
            return;
        }

        setBusy(true, false);
        matchRepository.joinMatch(user.getUid(), UserDisplay.displayName(this, user), code,
                new MatchRepository.JoinCallback() {
                    @Override
                    public void onJoined() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        setBusy(false, false);
                        openLobby(code);
                    }

                    @Override
                    public void onError(@StringRes int messageRes) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        setBusy(false, false);
                        showMessage(getString(messageRes));
                    }
                });
    }

    private void setBusy(boolean busy, boolean fromCreate) {
        binding.createMatchButton.setEnabled(!busy);
        binding.joinButton.setEnabled(!busy);
        binding.practiceButton.setEnabled(!busy);
        if (fromCreate) {
            binding.createMatchButton.setText(busy ? R.string.loading : R.string.create_match);
        } else {
            binding.joinButton.setText(busy ? R.string.loading : R.string.join_with_code);
        }
    }

    private void openLobby(String code) {
        Intent intent = new Intent(this, LobbyActivity.class);
        intent.putExtra(LobbyActivity.EXTRA_CODE, code);
        startActivity(intent);
    }

    private void openGame(String difficulty) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_ARENA_ID, selectedArena.getId());
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty);
        startActivity(intent);
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}