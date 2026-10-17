package com.ripti.cluerush;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ripti.cluerush.data.ArenaCatalog;
import com.ripti.cluerush.databinding.ActivityMainBinding;
import com.ripti.cluerush.databinding.DialogJoinCodeBinding;
import com.ripti.cluerush.model.Arena;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.ui.auth.LoginActivity;
import com.ripti.cluerush.ui.home.ArenaAdapter;
import com.ripti.cluerush.ui.profile.ProfileActivity;
import com.ripti.cluerush.util.EntranceAnimator;
import com.ripti.cluerush.util.UserDisplay;

import java.util.List;

public class MainActivity extends BaseActivity {

    private static final String CODE_PATTERN = "[A-Z0-9]{6}";

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
        selectedArena = arenas.get(0);
        ArenaAdapter adapter = new ArenaAdapter(arenas, arena -> selectedArena = arena);
        binding.arenaList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.arenaList.setItemAnimator(null);
        binding.arenaList.setAdapter(adapter);

        binding.createMatchButton.setOnClickListener(v ->
                showMessage(getString(R.string.coming_create, selectedArena.getName())));
        binding.joinButton.setOnClickListener(v -> showJoinDialog());
        binding.practiceButton.setOnClickListener(v ->
                showMessage(getString(R.string.coming_practice)));

        if (savedInstanceState == null) {
            EntranceAnimator.play(binding.contentColumn);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null && binding != null) {
            openLogin();
        }
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
                    showMessage(getString(R.string.coming_join, code));
                }));
        dialog.show();
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