package com.ripti.cluerush.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.ripti.cluerush.game.MatchResult;
import com.ripti.cluerush.game.StatsRules;

import java.util.HashMap;
import java.util.Map;

public class StatsRepository {

    private static final String USERS = "users";
    private static final String LEADERBOARD = "leaderboard";
    private static final String SCORES = "scores";

    private static final class Change {
        int xp;
        int rating;
        int matches;
        int wins;
        int draws;
        int solo;
    }

    public static String boardKey(String arenaId, String difficultyKey) {
        return arenaId + "_" + difficultyKey;
    }

    public void recordSolo(String uid, String name, String arenaId, String difficultyKey,
                           int score) {
        Change change = new Change();
        change.xp = StatsRules.soloXp(score);
        change.solo = 1;
        apply(uid, name, change);
        submitBest(uid, name, arenaId, difficultyKey, score);
    }

    public void recordMatch(String uid, String name, String arenaId, String difficultyKey,
                            int score, MatchResult.Outcome outcome) {
        Change change = new Change();
        change.xp = StatsRules.matchXp(score, outcome);
        change.rating = StatsRules.ratingDelta(outcome);
        change.matches = 1;
        change.wins = outcome == MatchResult.Outcome.WIN ? 1 : 0;
        change.draws = outcome == MatchResult.Outcome.DRAW ? 1 : 0;
        apply(uid, name, change);
        submitBest(uid, name, arenaId, difficultyKey, score);
    }

    private void apply(String uid, String name, Change change) {
        try {
            DatabaseReference reference = FirebaseDatabase.getInstance()
                    .getReference(USERS).child(uid);
            reference.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData data) {
                    long xp = read(data, "xp") + change.xp;
                    data.child("xp").setValue(xp);
                    data.child("level").setValue(StatsRules.levelForXp(xp));
                    data.child("rating").setValue(
                            StatsRules.applyRating(read(data, "rating"), change.rating));
                    data.child("totalWins").setValue(read(data, "totalWins") + change.wins);
                    data.child("matchesPlayed").setValue(
                            read(data, "matchesPlayed") + change.matches);
                    data.child("draws").setValue(read(data, "draws") + change.draws);
                    data.child("soloGames").setValue(read(data, "soloGames") + change.solo);
                    return Transaction.success(data);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed,
                                       @Nullable DataSnapshot snapshot) {
                    if (committed && snapshot != null) {
                        publish(uid, name, snapshot);
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void publish(String uid, String name, DataSnapshot snapshot) {
        try {
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", name);
            entry.put("xp", readLong(snapshot, "xp"));
            entry.put("rating", readLong(snapshot, "rating"));
            entry.put("totalWins", readLong(snapshot, "totalWins"));
            entry.put("matchesPlayed", readLong(snapshot, "matchesPlayed"));
            FirebaseDatabase.getInstance().getReference(LEADERBOARD).child(uid).setValue(entry);
        } catch (Exception ignored) {
        }
    }

    private void submitBest(String uid, String name, String arenaId, String difficultyKey,
                            int score) {
        if (score <= 0) {
            return;
        }
        try {
            DatabaseReference reference = FirebaseDatabase.getInstance()
                    .getReference(SCORES).child(boardKey(arenaId, difficultyKey)).child(uid);
            reference.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData data) {
                    if (score <= read(data, "score")) {
                        return Transaction.abort();
                    }
                    data.child("score").setValue(score);
                    data.child("name").setValue(name);
                    return Transaction.success(data);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed,
                                       @Nullable DataSnapshot snapshot) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static long read(MutableData data, String key) {
        Object value = data.child(key).getValue();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static long readLong(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}