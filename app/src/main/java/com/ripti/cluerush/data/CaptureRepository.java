package com.ripti.cluerush.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CaptureRepository {

    public interface CaptureCallback {
        void onCaptured(CaptureRecord record);

        void onAlreadyCaptured(CaptureRecord record);

        void onError(DatabaseError error);
    }

    public interface CaptureListener {
        void onCapturesChanged(Map<String, CaptureRecord> captures);

        void onCancelled(DatabaseError error);
    }

    public static class CaptureRecord {

        private final String locationId;
        private final String team;
        private final long capturedAt;
        private final int taskPoints;

        public CaptureRecord(String locationId, String team,
                             long capturedAt, int taskPoints) {
            this.locationId = locationId;
            this.team = team;
            this.capturedAt = capturedAt;
            this.taskPoints = taskPoints;
        }

        public String getLocationId() {
            return locationId;
        }

        public String getTeam() {
            return team;
        }

        public long getCapturedAt() {
            return capturedAt;
        }

        public int getTaskPoints() {
            return taskPoints;
        }
    }

    public static class ListenerHandle {

        private final DatabaseReference reference;
        private final ValueEventListener listener;

        ListenerHandle(DatabaseReference reference,
                       ValueEventListener listener) {
            this.reference = reference;
            this.listener = listener;
        }

        public void remove() {
            reference.removeEventListener(listener);
        }
    }

    public void capture(String code, String locationId, String team,
                        int taskPoints, CaptureCallback callback) {

        if (code == null || code.trim().isEmpty()
                || locationId == null || locationId.trim().isEmpty()
                || team == null || team.trim().isEmpty()
                || callback == null) {
            return;
        }

        DatabaseReference reference = capturedReference(code)
                .child(locationId);

        try {
            reference.runTransaction(new Transaction.Handler() {

                @NonNull
                @Override
                public Transaction.Result doTransaction(
                        @NonNull MutableData currentData) {

                    if (currentData.getValue() != null) {
                        return Transaction.abort();
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("team", team);
                    data.put("at", ServerValue.TIMESTAMP);
                    data.put("taskPoints", Math.max(0, taskPoints));

                    currentData.setValue(data);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(DatabaseError error,
                                       boolean committed,
                                       DataSnapshot currentData) {

                    if (error != null) {
                        callback.onError(error);
                        return;
                    }

                    if (committed) {
                        CaptureRecord record = parseRecord(
                                locationId, currentData
                        );

                        callback.onCaptured(record);
                    } else {
                        CaptureRecord record = parseRecord(
                                locationId, currentData
                        );

                        callback.onAlreadyCaptured(record);
                    }
                }
            }, false);

        } catch (Exception e) {
            callback.onError(DatabaseError.fromException(e));
        }
    }

    public ListenerHandle observeCaptures(String code,
                                          CaptureListener listener) {

        DatabaseReference reference = capturedReference(code);

        ValueEventListener valueListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Map<String, CaptureRecord> captures = new HashMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String locationId = child.getKey();

                    if (locationId == null) {
                        continue;
                    }

                    CaptureRecord record = parseRecord(locationId, child);

                    if (record != null) {
                        captures.put(locationId, record);
                    }
                }

                listener.onCapturesChanged(captures);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onCancelled(error);
            }
        };

        reference.addValueEventListener(valueListener);

        return new ListenerHandle(reference, valueListener);
    }

    public void removeCaptureListener(ListenerHandle handle) {
        if (handle != null) {
            handle.remove();
        }
    }

    private DatabaseReference capturedReference(String code) {
        return FirebaseDatabase.getInstance()
                .getReference("matches")
                .child(code)
                .child("captured");
    }

    private CaptureRecord parseRecord(String locationId,
                                      DataSnapshot snapshot) {

        if (!snapshot.exists()) {
            return null;
        }

        Object teamValue = snapshot.child("team").getValue();

        if (!(teamValue instanceof String)) {
            return null;
        }

        String team = (String) teamValue;

        Object timeValue = snapshot.child("at").getValue();
        Object pointsValue = snapshot.child("taskPoints").getValue();

        long capturedAt = timeValue instanceof Number
                ? ((Number) timeValue).longValue() : 0L;

        int taskPoints = pointsValue instanceof Number
                ? ((Number) pointsValue).intValue() : 0;

        return new CaptureRecord(
                locationId,
                team,
                capturedAt,
                taskPoints
        );
    }
}