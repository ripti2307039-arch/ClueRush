package com.ripti.cluerush.ui.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.UserRepository;
import com.ripti.cluerush.databinding.ActivityProfileBinding;
import com.ripti.cluerush.model.UserProfile;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.ui.auth.LoginActivity;
import com.ripti.cluerush.util.EntranceAnimator;
import com.ripti.cluerush.util.UserDisplay;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private boolean guest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        guest = user.isAnonymous();
        String name = UserDisplay.displayName(this, user);
        binding.profileName.setText(name);
        binding.profileAvatar.setText(UserDisplay.initial(name));
        if (guest) {
            binding.profileEmail.setText(R.string.guest_hint);
        } else {
            binding.profileEmail.setText(user.getEmail());
        }

        binding.backButton.setOnClickListener(v -> finish());
        binding.logoutButton.setOnClickListener(v -> confirmLogout());

        bindStats(UserProfile.empty());
        if (!guest) {
            new UserRepository().loadProfile(user.getUid(), profile -> {
                if (!isFinishing() && !isDestroyed()) {
                    bindStats(profile);
                }
            });
        }

        if (savedInstanceState == null) {
            EntranceAnimator.play(binding.contentColumn);
        }
    }

    private void bindStats(UserProfile profile) {
        binding.levelText.setText(getString(R.string.level_label, profile.getLevel()));
        binding.xpText.setText(getString(R.string.xp_progress,
                profile.getXpInLevel(), UserProfile.XP_PER_LEVEL));
        binding.levelProgress.setProgressCompat(profile.getXpInLevel(), true);
        binding.winsValue.setText(String.valueOf(profile.getTotalWins()));
        binding.matchesValue.setText(String.valueOf(profile.getMatchesPlayed()));
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(guest ? R.string.logout_guest_message : R.string.logout_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_logout, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    openLogin();
                })
                .show();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}