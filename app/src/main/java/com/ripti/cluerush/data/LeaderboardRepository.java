package com.ripti.cluerush.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderboardRepository {

    public static final int LIMIT = 50;
    public static final String FIELD_RATING = "rating";
    public static final String FIELD_XP = "xp";
    public static final String FIELD_SCORE = "score";

    private static final String LEADERBOARD = "leaderboard";
    private static final String SCORES = "scores";

    public static final class Entry {
        public final String uid;
        public final String name;
        public final long value;
        public final long xp;

        Entry(String uid, String name, long value, long xp) {
            this.uid = uid;
            this.name = name;
            this.value = value;
            this.xp = xp;
        }
    }

    public interface Callback {
        void onLoaded(List<Entry> entries);

        void onError();
    }

    public interface RankCallback {
        void onRank(long rank, long value);

        void onError();
    }

    public void loadTop(String field, Callback callback) {
        try {
            Query query = database().getReference(LEADERBOARD)
                    .orderByChild(field).startAt(1).limitToLast(LIMIT);
            run(query, field, callback);
        } catch (Exception e) {
            callback.onError();
        }
    }

    public void loadBoard(String arenaId, String difficultyKey, Callback callback) {
        try {
            Query query = database().getReference(SCORES)
                    .child(StatsRepository.boardKey(arenaId, difficultyKey))
                    .orderByChild(FIELD_SCORE).startAt(1).limitToLast(LIMIT);
            run(query, FIELD_SCORE, callback);
        } catch (Exception e) {
            callback.onError();
        }
    }

    public void loadMyRank(String field, String uid, RankCallback callback) {
        try {
            rankIn(database().getReference(LEADERBOARD), field, uid, callback);
        } catch (Exception e) {
            callback.onError();
        }
    }

    public void loadMyBoardRank(String arenaId, String difficultyKey, String uid,
                                RankCallback callback) {
        try {
            rankIn(database().getReference(SCORES)
                            .child(StatsRepository.boardKey(arenaId, difficultyKey)),
                    FIELD_SCORE, uid, callback);
        } catch (Exception e) {
            callback.onError();
        }
    }

    private void rankIn(DatabaseReference collection, String field, String uid,
                        RankCallback callback) {
        collection.child(uid).child(field).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object raw = snapshot.getValue();
                        long mine = raw instanceof Number ? ((Number) raw).longValue() : 0L;
                        if (mine <= 0L) {
                            callback.onRank(0L, 0L);
                            return;
                        }
                        collection.orderByChild(field).startAt(mine + 1)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot above) {
                                        callback.onRank(above.getChildrenCount() + 1L, mine);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        callback.onError();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError();
                    }
                });
    }

    private void run(Query query, String field, Callback callback) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> entries = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    if (uid == null) {
                        continue;
                    }
                    Object name = child.child("name").getValue();
                    entries.add(new Entry(uid,
                            name instanceof String ? (String) name : "",
                            readLong(child, field), readLong(child, FIELD_XP)));
                }
                Collections.sort(entries, (a, b) -> Long.compare(b.value, a.value));
                withCurrentNames(entries, callback);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError();
            }
        });
    }

    private static long readLong(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static FirebaseDatabase database() {
        return FirebaseDatabase.getInstance();
    }

    /**
     * The name copied into leaderboard / scores can be out of date after a rename, so the
     * list shows the live name from users/{uid}/name and only falls back to the copy.
     */
    private void withCurrentNames(List<Entry> entries, Callback callback) {
        if (entries.isEmpty()) {
            callback.onLoaded(entries);
            return;
        }
        final List<Entry> result = new ArrayList<>(entries);
        final int[] pending = {entries.size()};
        for (int i = 0; i < entries.size(); i++) {
            final int index = i;
            final Entry old = entries.get(i);
            try {
                database().getReference("users").child(old.uid).child("name")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Object live = snapshot.getValue();
                                if (live instanceof String && !((String) live).trim().isEmpty()) {
                                    result.set(index, new Entry(old.uid, ((String) live).trim(),
                                            old.value, old.xp));
                                }
                                done();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                done();
                            }

                            private void done() {
                                pending[0]--;
                                if (pending[0] == 0) {
                                    callback.onLoaded(result);
                                }
                            }
                        });
            } catch (Exception e) {
                pending[0]--;
                if (pending[0] == 0) {
                    callback.onLoaded(result);
                }
            }
        }
    }
}