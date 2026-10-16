package com.ripti.cluerush.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.ripti.cluerush.MainActivity;
import com.ripti.cluerush.R;
import com.ripti.cluerush.data.UserRepository;
import com.ripti.cluerush.databinding.ActivityRegisterBinding;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.util.AuthErrorMapper;

public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.registerButton.setOnClickListener(v -> attemptRegister());
        binding.backToLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        binding.nameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmLayout.setError(null);

        String name = readText(binding.nameInput.getText());
        String email = readText(binding.emailInput.getText());
        String password = readText(binding.passwordInput.getText());
        String confirm = readText(binding.confirmInput.getText());
        boolean valid = true;

        if (name.isEmpty()) {
            binding.nameLayout.setError(getString(R.string.error_name_required));
            valid = false;
        }
        if (email.isEmpty()) {
            binding.emailLayout.setError(getString(R.string.error_email_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.error_email_invalid));
            valid = false;
        }
        if (password.isEmpty()) {
            binding.passwordLayout.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (password.length() < 6) {
            binding.passwordLayout.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (!confirm.equals(password)) {
            binding.confirmLayout.setError(getString(R.string.error_password_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                setLoading(false);
                showError(AuthErrorMapper.toMessage(this, task.getException()));
                return;
            }
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                setLoading(false);
                showError(getString(R.string.error_generic));
                return;
            }
            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            user.updateProfile(request);
            userRepository.createProfile(user.getUid(), name, email, this::openHome);
        });
    }

    private void setLoading(boolean loading) {
        binding.registerButton.setEnabled(!loading);
        binding.backToLogin.setEnabled(!loading);
        binding.registerButton.setText(loading ? R.string.loading : R.string.action_register);
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private String readText(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}