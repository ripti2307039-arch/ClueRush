package com.ripti.cluerush.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.ripti.cluerush.MainActivity;
import com.ripti.cluerush.R;
import com.ripti.cluerush.databinding.ActivityLoginBinding;
import com.ripti.cluerush.ui.BaseActivity;
import com.ripti.cluerush.util.AuthErrorMapper;
import com.ripti.cluerush.util.EntranceAnimator;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            openHome();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> attemptLogin());
        binding.guestButton.setOnClickListener(v -> signInAsGuest());
        binding.forgotButton.setOnClickListener(v -> sendPasswordReset());
        binding.registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        if (savedInstanceState == null) {
            EntranceAnimator.play(binding.contentColumn);
        }
    }

    private void attemptLogin() {
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        String email = readText(binding.emailInput.getText());
        String password = readText(binding.passwordInput.getText());
        boolean valid = true;

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
        }

        if (!valid) {
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                openHome();
            } else {
                setLoading(false);
                showError(AuthErrorMapper.toMessage(this, task.getException()));
            }
        });
    }

    private void signInAsGuest() {
        setLoading(true);
        auth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                openHome();
            } else {
                setLoading(false);
                showError(AuthErrorMapper.toMessage(this, task.getException()));
            }
        });
    }

    private void sendPasswordReset() {
        binding.emailLayout.setError(null);
        String email = readText(binding.emailInput.getText());

        if (email.isEmpty()) {
            binding.emailLayout.setError(getString(R.string.reset_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.error_email_invalid));
            return;
        }

        setLoading(true);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(this, task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                showError(getString(R.string.reset_sent));
            } else {
                showError(AuthErrorMapper.toMessage(this, task.getException()));
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.loginButton.setEnabled(!loading);
        binding.guestButton.setEnabled(!loading);
        binding.forgotButton.setEnabled(!loading);
        binding.registerLink.setEnabled(!loading);
        binding.loginButton.setText(loading ? R.string.loading : R.string.action_login);
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