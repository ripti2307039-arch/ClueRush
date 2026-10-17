package com.ripti.cluerush.util;

import java.security.SecureRandom;

public final class MatchCodeGenerator {

    public static final int CODE_LENGTH = 6;

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private MatchCodeGenerator() {
    }

    public static String generate() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}