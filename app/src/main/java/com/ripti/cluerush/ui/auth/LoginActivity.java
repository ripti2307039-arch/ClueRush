
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

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyInsets(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.loginButton.setOnClickListener(v -> attemptLogin());

        binding.registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });

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
            binding.emailLayout.setError(
                    getString(R.string.error_email_required)
            );
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(
                    getString(R.string.error_email_invalid)
            );
            valid = false;
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setError(
                    getString(R.string.error_password_required)
            );
            valid = false;
        }

        if (!valid) {
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        openHome();
                    } else {
                        showError(
                                AuthErrorMapper.toMessage(
                                        this,
                                        task.getException()
                                )
                        );
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.loginButton.setEnabled(!loading);
        binding.registerLink.setEnabled(!loading);

        binding.loginButton.setText(
                loading ? R.string.loading : R.string.action_login
        );
    }

    private void showError(String message) {
        Snackbar.make(
                binding.getRoot(),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }

    private String readText(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}
