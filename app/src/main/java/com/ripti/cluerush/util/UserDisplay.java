package com.ripti.cluerush.util;

import android.content.Context;

import com.google.firebase.auth.FirebaseUser;
import com.ripti.cluerush.R;

import java.util.Locale;

public final class UserDisplay {

    private UserDisplay() {
    }

    public static String displayName(Context context, FirebaseUser user) {
        if (user.isAnonymous()) {
            return context.getString(R.string.guest_name);
        }
        String name = user.getDisplayName();
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return context.getString(R.string.guest_name);
    }

    public static String initial(String name) {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        int firstCodePoint = name.codePointAt(0);
        return new String(Character.toChars(firstCodePoint)).toUpperCase(Locale.ROOT);
    }
}