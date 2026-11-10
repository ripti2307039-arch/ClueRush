package com.ripti.cluerush.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.ProfilePhotoRepository;
import com.ripti.cluerush.data.UserRepository;
import com.ripti.cluerush.databinding.ActivityProfileBinding;
import com.ripti.cluerush.databinding.DialogEditProfileBinding;
import com.ripti.cluerush.model.UserProfile;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.ui.auth.LoginActivity;
import com.ripti.cluerush.ui.history.HistoryActivity;
import com.ripti.cluerush.util.EntranceAnimator;
import com.ripti.cluerush.util.UserDisplay;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private boolean guest;

    private final UserRepository userRepository = new UserRepository();

    // Edit dialog state
    private DialogEditProfileBinding editBinding;
    private Bitmap photoBitmap;
    private Bitmap pendingPhoto;

    // System photo picker
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    this::onPhotoPicked
            );

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

        binding.historyButton.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        if (guest) {
            binding.editProfileButton.setVisibility(View.GONE);
        } else {
            binding.editProfileButton.setOnClickListener(v -> showEditDialog());

            ProfilePhotoRepository.load(this, user.getUid(), bitmap -> {
                if (!isFinishing() && !isDestroyed() && bitmap != null) {
                    photoBitmap = bitmap;
                    showAvatarPhoto(binding.profileAvatar, bitmap);
                }
            });
        }

        bindStats(UserProfile.empty());

        if (!guest) {
            userRepository.loadProfile(user.getUid(), profile -> {
                if (!isFinishing() && !isDestroyed()) {
                    bindStats(profile);
                }
            });
        }

        if (savedInstanceState == null) {
            EntranceAnimator.play(binding.contentColumn);
        }
    }

    // -----------------------------------------
    // SHOW AVATAR PHOTO
    // -----------------------------------------

    private void showAvatarPhoto(
            android.widget.TextView avatar,
            Bitmap bitmap
    ) {
        avatar.setBackground(
                ProfilePhotoRepository.circle(this, bitmap)
        );

        avatar.setText("");
    }

    // -----------------------------------------
    // PHOTO PICKER CALLBACK
    // -----------------------------------------

    private void onPhotoPicked(@Nullable Uri uri) {

        if (uri == null || editBinding == null) {
            return;
        }

        ProfilePhotoRepository.decode(this, uri, bitmap -> {

            if (isFinishing() || isDestroyed() || editBinding == null) {
                return;
            }

            if (bitmap == null) {
                Toast.makeText(
                        this,
                        R.string.photo_load_failed,
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            pendingPhoto = bitmap;

            showAvatarPhoto(editBinding.editAvatar, bitmap);
        });
    }

    // -----------------------------------------
    // EDIT PROFILE DIALOG
    // -----------------------------------------

    private void showEditDialog() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        editBinding = DialogEditProfileBinding.inflate(
                getLayoutInflater()
        );

        pendingPhoto = null;

        String current = UserDisplay.displayName(this, user);

        editBinding.nameInput.setText(current);

        editBinding.nameInput.setSelection(current.length());

        if (photoBitmap != null) {
            showAvatarPhoto(editBinding.editAvatar, photoBitmap);
        } else {
            editBinding.editAvatar.setText(
                    UserDisplay.initial(current)
            );
        }

        // Corrected Photo Picker request:
        // Default media type is ImageOnly.
        View.OnClickListener pick = v -> photoPicker.launch(
                new PickVisualMediaRequest.Builder().build()
        );

        editBinding.changePhotoButton.setOnClickListener(pick);

        editBinding.editAvatar.setOnClickListener(pick);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_profile_title)
                .setView(editBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();

        dialog.setOnDismissListener(d -> {
            editBinding = null;
            pendingPhoto = null;
        });

        dialog.show();

        // Keep dialog open when validation fails.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> saveProfile(dialog));
    }

    // -----------------------------------------
    // SAVE PROFILE
    // -----------------------------------------

    /** Tells whether the server confirmed the write or rejected it. */
    private void showSyncError(String message) {
        Toast.makeText(getApplicationContext(),
                message, Toast.LENGTH_LONG).show();
    }

    private void saveProfile(AlertDialog dialog) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || editBinding == null) {
            return;
        }

        editBinding.nameLayout.setError(null);

        CharSequence typed = editBinding.nameInput.getText();

        String name = typed == null
                ? ""
                : typed.toString().trim();

        if (name.isEmpty()) {
            editBinding.nameLayout.setError(
                    getString(R.string.error_name_required)
            );

            return;
        }

        boolean nameChanged = !name.equals(
                UserDisplay.displayName(this, user)
        );

        Bitmap newPhoto = pendingPhoto;

        if (!nameChanged && newPhoto == null) {
            dialog.dismiss();
            return;
        }

        // Save selected photo
        if (newPhoto != null) {

            ProfilePhotoRepository.save(
                    this,
                    user.getUid(),
                    newPhoto,
                    this::showSyncError
            );

            photoBitmap = newPhoto;

            showAvatarPhoto(
                    binding.profileAvatar,
                    newPhoto
            );
        }

        if (!nameChanged) {

            dialog.dismiss();

            Toast.makeText(
                    this,
                    R.string.profile_saved,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(false);

        UserProfileChangeRequest request =
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();

        user.updateProfile(request).addOnCompleteListener(task -> {

            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (task.isSuccessful()) {

                userRepository.updateName(
                        this,
                        user.getUid(),
                        name,
                        this::showSyncError
                );

                binding.profileName.setText(name);

                if (photoBitmap == null) {
                    binding.profileAvatar.setText(
                            UserDisplay.initial(name)
                    );
                }

                dialog.dismiss();

                Toast.makeText(
                        this,
                        R.string.profile_saved,
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setEnabled(true);

                if (editBinding != null) {
                    editBinding.nameLayout.setError(
                            getString(R.string.profile_save_failed)
                    );
                }
            }
        });
    }

    // -----------------------------------------
    // PROFILE STATS
    // -----------------------------------------

    private void bindStats(UserProfile profile) {

        int[] titles = {
                R.string.title_rookie,
                R.string.title_explorer,
                R.string.title_hunter,
                R.string.title_master
        };

        binding.levelText.setText(
                getString(
                        R.string.level_with_title,
                        profile.getLevel(),
                        getString(titles[profile.getTitleIndex()])
                )
        );

        binding.xpText.setText(
                getString(
                        R.string.xp_progress,
                        profile.getXpInLevel(),
                        profile.getXpSpan()
                )
        );

        binding.levelProgress.setMax(profile.getXpSpan());

        binding.levelProgress.setProgressCompat(
                profile.getXpInLevel(),
                true
        );

        binding.winsValue.setText(
                String.valueOf(profile.getTotalWins())
        );

        binding.matchesValue.setText(
                String.valueOf(profile.getMatchesPlayed())
        );

        binding.ratingValue.setText(
                String.valueOf(profile.getRating())
        );

        binding.winRateValue.setText(
                getString(
                        R.string.win_rate_value,
                        profile.getWinRatePercent()
                )
        );
    }

    // -----------------------------------------
    // LOGOUT
    // -----------------------------------------

    private void confirmLogout() {

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(
                        guest
                                ? R.string.logout_guest_message
                                : R.string.logout_message
                )
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(
                        R.string.action_logout,
                        (dialog, which) -> {

                            FirebaseAuth.getInstance().signOut();

                            openLogin();
                        }
                )
                .show();
    }

    private void openLogin() {

        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}