package com.ripti.cluerush.util;

import android.content.Context;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.ripti.cluerush.R;

public final class AuthErrorMapper {

    private AuthErrorMapper() {
    }

    public static String toMessage(Context context, Exception error) {
        if (error instanceof FirebaseNetworkException) {
            return context.getString(R.string.error_no_internet);
        }
        if (error instanceof FirebaseAuthWeakPasswordException) {
            return context.getString(R.string.error_weak_password);
        }
        if (error instanceof FirebaseAuthUserCollisionException) {
            return context.getString(R.string.error_email_in_use);
        }
        if (error instanceof FirebaseAuthInvalidUserException
                || error instanceof FirebaseAuthInvalidCredentialsException) {
            return context.getString(R.string.error_wrong_credentials);
        }
        if (error instanceof FirebaseTooManyRequestsException) {
            return context.getString(R.string.error_too_many);
        }
        return context.getString(R.string.error_generic);
    }
}