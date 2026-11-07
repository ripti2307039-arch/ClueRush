package com.ripti.cluerush.game;

import android.os.SystemClock;

import com.google.firebase.database.DatabaseError;
import com.ripti.cluerush.data.CaptureRepository;

import java.util.Map;

public final class CaptureController {

    public interface Listener {
        void onCapturesUpdated(Map<String, CaptureRepository.CaptureRecord> captures);

        void onCaptureSuccess(CaptureRepository.CaptureRecord record);

        void onLocationAlreadyCaptured(CaptureRepository.CaptureRecord record);

        void onEnemyCaptured(String locationId);

        void onCooldown(long remainingMs);

        void onError(DatabaseError error);
    }

    private final String code;
    private final String uid;
    private final String team;
    private final CaptureSession session;
    private final CaptureRules rules;
    private final CaptureRepository repository;
    private final Listener listener;

    private CaptureRepository.ListenerHandle handle;
    private boolean stopped;

    public CaptureController(String code, String uid, String team,
                             CaptureSession session,
                             CaptureRepository repository,
                             Listener listener) {

        if (code == null || uid == null || team == null
                || session == null || repository == null || listener == null) {
            throw new IllegalArgumentException("Invalid capture controller configuration");
        }

        this.code = code;
        this.uid = uid;
        this.team = team;
        this.session = session;
        this.repository = repository;
        this.listener = listener;
        this.rules = new CaptureRules();
    }

    public void start() {
        if (stopped || handle != null) {
            return;
        }

        handle = repository.observeCaptures(code,
                new CaptureRepository.CaptureListener() {

                    @Override
                    public void onCapturesChanged(
                            Map<String, CaptureRepository.CaptureRecord> captures) {

                        if (stopped) {
                            return;
                        }

                        String selected = session.getSelectedLocationId();

                        for (CaptureRepository.CaptureRecord record : captures.values()) {
                            session.applyCapture(
                                    record.getLocationId(),
                                    record.getTeam(),
                                    record.getCapturedAt(),
                                    record.getTaskPoints()
                            );
                        }

                        if (selected != null) {
                            CaptureRepository.CaptureRecord record =
                                    captures.get(selected);

                            if (record != null) {
                                session.releaseSelection();

                                if (!team.equals(record.getTeam())) {
                                    listener.onEnemyCaptured(selected);
                                } else {
                                    listener.onLocationAlreadyCaptured(record);
                                }
                            }
                        }

                        listener.onCapturesUpdated(captures);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (!stopped) {
                            listener.onError(error);
                        }
                    }
                });
    }

    public boolean selectLocation(String locationId) {
        if (stopped) {
            return false;
        }

        return session.selectLocation(locationId);
    }

    public void releaseSelection() {
        session.releaseSelection();
    }

    public long getRemainingCooldown(String locationId) {
        return rules.getRemainingCooldown(uid, locationId,
                SystemClock.elapsedRealtime());
    }

    public boolean canAttempt(String locationId) {
        return !stopped && rules.canAttempt(
                uid,
                locationId,
                session,
                SystemClock.elapsedRealtime()
        );
    }

    public void submitWrongAnswer(String locationId) {
        if (stopped || session.isCaptured(locationId)) {
            return;
        }

        long now = SystemClock.elapsedRealtime();

        rules.recordWrongAnswer(uid, locationId, now);

        listener.onCooldown(CaptureRules.WRONG_ANSWER_COOLDOWN_MS);
    }

    public void submitCorrectAnswer(String locationId, int taskPoints) {
        if (stopped) {
            return;
        }

        long now = SystemClock.elapsedRealtime();

        if (!rules.canAttempt(uid, locationId, session, now)) {
            long remaining = rules.getRemainingCooldown(uid, locationId, now);
            listener.onCooldown(remaining);
            return;
        }

        repository.capture(
                code,
                locationId,
                team,
                taskPoints,
                new CaptureRepository.CaptureCallback() {

                    @Override
                    public void onCaptured(
                            CaptureRepository.CaptureRecord record) {

                        if (stopped) {
                            return;
                        }

                        if (record == null) {
                            listener.onError(DatabaseError.fromException(
                                    new IllegalStateException("Empty capture record")));
                            return;
                        }

                        session.applyCapture(
                                record.getLocationId(),
                                record.getTeam(),
                                record.getCapturedAt(),
                                record.getTaskPoints()
                        );

                        rules.clearCooldown(uid, locationId);
                        listener.onCaptureSuccess(record);
                    }

                    @Override
                    public void onAlreadyCaptured(
                            CaptureRepository.CaptureRecord record) {

                        if (stopped) {
                            return;
                        }

                        if (record == null) {
                            listener.onError(DatabaseError.fromException(
                                    new IllegalStateException("Empty capture record")));
                            return;
                        }

                        session.applyCapture(
                                record.getLocationId(),
                                record.getTeam(),
                                record.getCapturedAt(),
                                record.getTaskPoints()
                        );

                        session.releaseSelection();

                        if (!team.equals(record.getTeam())) {
                            listener.onEnemyCaptured(locationId);
                        } else {
                            listener.onLocationAlreadyCaptured(record);
                        }
                    }

                    @Override
                    public void onError(DatabaseError error) {
                        if (!stopped) {
                            listener.onError(error);
                        }
                    }
                }
        );
    }

    public CaptureSession getSession() {
        return session;
    }

    public void stop() {
        stopped = true;

        if (handle != null) {
            repository.removeCaptureListener(handle);
            handle = null;
        }

        rules.clearAll();
        session.releaseSelection();
    }
}