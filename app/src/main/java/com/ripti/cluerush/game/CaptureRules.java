
package com.ripti.cluerush.game;

import java.util.HashMap;
import java.util.Map;

public final class CaptureRules {

    public static final long WRONG_ANSWER_COOLDOWN_MS = 7000L;

    private final Map<String, Long> cooldowns = new HashMap<>();

    public boolean canAttempt(String uid, String locationId,
                              CaptureSession session, long currentTime) {

        if (uid == null || locationId == null || session == null) {
            return false;
        }

        if (session.getRoundByLocationId(locationId) == null
                || session.isCaptured(locationId)) {
            return false;
        }

        String key = createKey(uid, locationId);
        Long blockedUntil = cooldowns.get(key);

        return blockedUntil == null || currentTime >= blockedUntil;
    }

    public void recordWrongAnswer(String uid, String locationId,
                                  long currentTime) {

        if (uid == null || locationId == null) {
            return;
        }

        cooldowns.put(
                createKey(uid, locationId),
                currentTime + WRONG_ANSWER_COOLDOWN_MS
        );
    }

    public long getRemainingCooldown(String uid, String locationId,
                                     long currentTime) {

        if (uid == null || locationId == null) {
            return 0L;
        }

        Long blockedUntil = cooldowns.get(createKey(uid, locationId));

        if (blockedUntil == null) {
            return 0L;
        }

        long remaining = blockedUntil - currentTime;

        if (remaining <= 0L) {
            cooldowns.remove(createKey(uid, locationId));
            return 0L;
        }

        return remaining;
    }

    public void clearCooldown(String uid, String locationId) {
        if (uid != null && locationId != null) {
            cooldowns.remove(createKey(uid, locationId));
        }
    }

    public void clearAll() {
        cooldowns.clear();
    }

    private String createKey(String uid, String locationId) {
        return uid + "_" + locationId;
    }
}
