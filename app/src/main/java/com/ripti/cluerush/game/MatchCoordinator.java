package com.ripti.cluerush.game;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.ripti.cluerush.data.MatchLiveRepository;
import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.PlayerState;
import com.ripti.cluerush.util.GeoMath;
import com.ripti.cluerush.util.ServerClock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MatchCoordinator {

    public enum Reason {
        TIME_UP,
        COUNTDOWN,
        CLINCH
    }

    public static final class Opponent {
        public final String name;
        public final int rounds;
        public final String phase;
        public final boolean hasPosition;
        public final double latitude;
        public final double longitude;
        public final int countdownSeconds;
        public final int offlineSeconds;

        Opponent(String name, int rounds, String phase, boolean hasPosition, double latitude,
                 double longitude, int countdownSeconds, int offlineSeconds) {
            this.name = name;
            this.rounds = rounds;
            this.phase = phase;
            this.hasPosition = hasPosition;
            this.latitude = latitude;
            this.longitude = longitude;
            this.countdownSeconds = countdownSeconds;
            this.offlineSeconds = offlineSeconds;
        }
    }

    public interface Listener {
        void onOpponent(Opponent opponent);

        void onForceFinish(Reason reason);

        void onResult(MatchResult result);
    }

    private static final long TICK_MS = 500L;
    private static final long OFFLINE_LIMIT_MS = 30_000L;
    private static final long RESULT_GRACE_MS = 15_000L;
    private static final long POSITION_INTERVAL_MS = 1500L;
    private static final double POSITION_MIN_METERS = 3.0;
    private static final boolean EARLY_CLINCH = false;

    private final MatchLiveRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String code;
    private final String uid;
    private final boolean host;
    private final Difficulty difficulty;
    private final int totalRounds;
    private final long matchEndLocal;
    private final Listener listener;
    private final Runnable tick = this::onTick;

    private MatchLiveRepository.Handle playersHandle;
    private MatchLiveRepository.Handle presenceHandle;
    private PlayerState opponent;
    private String lastName = "";
    private boolean running;
    private boolean loaded;
    private boolean seenOpponent;
    private boolean resolved;
    private boolean forced;
    private Reason forcedReason;
    private long offlineSince;
    private long missingSince;
    private long lastPositionAt;
    private double lastLatitude = Double.NaN;
    private double lastLongitude = Double.NaN;

    private int myRounds;
    private int myScore;
    private int mySolved;
    private int myWrong;
    private int myFinal;
    private String myRoundPoints = "";
    private long myFinishedLocal;
    private long myFinishedServer;
    private boolean mineTerminal;

    public MatchCoordinator(MatchLiveRepository repository, String code, String uid,
                            boolean host, Difficulty difficulty, int totalRounds,
                            long matchEndLocal, Listener listener) {
        this.repository = repository;
        this.code = code;
        this.uid = uid;
        this.host = host;
        this.difficulty = difficulty;
        this.totalRounds = totalRounds;
        this.matchEndLocal = matchEndLocal;
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        presenceHandle = repository.armPresence(code, uid, host);
        playersHandle = repository.observe(code, new MatchLiveRepository.Listener() {
            @Override
            public void onPlayers(List<PlayerState> players) {
                PlayerState found = null;
                for (PlayerState player : players) {
                    if (!player.getUid().equals(uid)) {
                        found = player;
                    }
                }
                opponent = found;
                loaded = true;
                evaluate();
            }

            @Override
            public void onRemoved() {
                handleRemoved();
            }
        });
        pushProgress(false);
        handler.postDelayed(tick, TICK_MS);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
        if (playersHandle != null) {
            playersHandle.remove();
            playersHandle = null;
        }
        if (presenceHandle != null) {
            presenceHandle.remove();
            presenceHandle = null;
        }
    }

    public boolean isMineTerminal() {
        return mineTerminal;
    }

    public void publishProgress(int rounds, boolean solving, int score, int solved, int wrong) {
        if (!running || mineTerminal || resolved) {
            return;
        }
        myRounds = rounds;
        myScore = score;
        mySolved = solved;
        myWrong = wrong;
        pushProgress(solving);
    }

    public void publishPosition(double latitude, double longitude) {
        if (!running || mineTerminal || resolved) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (lastPositionAt != 0L && now - lastPositionAt < POSITION_INTERVAL_MS) {
            return;
        }
        if (!Double.isNaN(lastLatitude) && GeoMath.distanceMeters(lastLatitude, lastLongitude,
                latitude, longitude) < POSITION_MIN_METERS) {
            return;
        }
        lastPositionAt = now;
        lastLatitude = latitude;
        lastLongitude = longitude;
        Map<String, Object> values = new HashMap<>();
        values.put("lat", latitude);
        values.put("lng", longitude);
        repository.update(code, uid, values);
    }

    public void finishMine(int finalScore, int solved, int wrong, int rounds,
                           String roundPoints) {
        if (!running || mineTerminal || resolved) {
            return;
        }
        mineTerminal = true;
        myFinal = finalScore;
        myScore = finalScore;
        mySolved = solved;
        myWrong = wrong;
        myRounds = rounds;
        myRoundPoints = roundPoints;
        myFinishedLocal = forcedReason == Reason.TIME_UP
                ? matchEndLocal : System.currentTimeMillis();
        myFinishedServer = ServerClock.fromLocal(myFinishedLocal);

        Map<String, Object> values = new HashMap<>();
        values.put("phase", PlayerState.PHASE_FINISHED);
        values.put("finalScore", finalScore);
        values.put("score", finalScore);
        values.put("solved", solved);
        values.put("wrong", wrong);
        values.put("rounds", rounds);
        values.put("roundPoints", roundPoints);
        values.put("finishedAt", myFinishedServer);
        repository.update(code, uid, values);
        handler.post(this::evaluate);
    }

    public void forfeitMine() {
        if (!running || mineTerminal || resolved) {
            return;
        }
        mineTerminal = true;
        Map<String, Object> values = new HashMap<>();
        values.put("phase", PlayerState.PHASE_FORFEIT);
        repository.update(code, uid, values);
        stop();
    }

    public void deleteMatch() {
        repository.deleteMatch(code);
    }

    private void pushProgress(boolean solving) {
        Map<String, Object> values = new HashMap<>();
        values.put("phase", solving ? PlayerState.PHASE_SOLVING : PlayerState.PHASE_EXPLORING);
        values.put("rounds", myRounds);
        values.put("score", myScore);
        values.put("solved", mySolved);
        values.put("wrong", myWrong);
        repository.update(code, uid, values);
    }

    private void onTick() {
        if (!running) {
            return;
        }
        evaluate();
        if (running && !resolved) {
            handler.postDelayed(tick, TICK_MS);
        }
    }

    private void evaluate() {
        if (!running || resolved || !loaded) {
            return;
        }
        long now = System.currentTimeMillis();
        PlayerState opp = opponent;

        if (opp == null) {
            if (seenOpponent) {
                conclude(MatchResult.Outcome.WIN, MatchResult.Decider.FORFEIT, null);
                return;
            }
            if (missingSince == 0L) {
                missingSince = now;
            } else if (now - missingSince >= OFFLINE_LIMIT_MS) {
                conclude(MatchResult.Outcome.WIN, MatchResult.Decider.DISCONNECT_OPPONENT, null);
            }
            return;
        }
        seenOpponent = true;
        missingSince = 0L;
        lastName = opp.getName();

        if (opp.isForfeit()) {
            conclude(MatchResult.Outcome.WIN, MatchResult.Decider.FORFEIT, opp);
            return;
        }
        if (uid.equals(opp.getForfeitedUid())) {
            conclude(MatchResult.Outcome.LOSS, MatchResult.Decider.DISCONNECT_ME, opp);
            return;
        }

        boolean oppFinished = opp.isFinished();
        if (!oppFinished && !opp.isConnected()) {
            if (offlineSince == 0L) {
                offlineSince = now;
            } else if (now - offlineSince >= OFFLINE_LIMIT_MS) {
                Map<String, Object> values = new HashMap<>();
                values.put("forfeitedUid", opp.getUid());
                repository.update(code, uid, values);
                conclude(MatchResult.Outcome.WIN, MatchResult.Decider.DISCONNECT_OPPONENT, opp);
                return;
            }
        } else {
            offlineSince = 0L;
        }

        if (mineTerminal) {
            if (oppFinished) {
                conclude(MatchRules.decide(currentMe(), opp));
                return;
            }
            if (EARLY_CLINCH) {
                int oppPotential = MatchRules.potential(difficulty, totalRounds,
                        opp.getRounds(), opp.getScore());
                if (oppPotential < myFinal) {
                    conclude(MatchResult.Outcome.WIN, MatchResult.Decider.CLINCH, opp);
                    return;
                }
            }
            if (now >= matchEndLocal + RESULT_GRACE_MS) {
                PlayerState synthetic = new PlayerState(opp.getUid(), opp.getName(),
                        opp.getRounds(), PlayerState.PHASE_FINISHED, opp.getScore(),
                        opp.getSolved(), opp.getWrong(), opp.getScore(),
                        ServerClock.fromLocal(matchEndLocal) + RESULT_GRACE_MS, 0.0, 0.0,
                        false, true, "", "");
                conclude(MatchRules.decide(currentMe(), synthetic));
                return;
            }

        } else if (!forced) {
            if (oppFinished && opp.getFinishedAt() > 0L) {
                int myPotential = MatchRules.potential(difficulty, totalRounds, myRounds, myScore);
                if (EARLY_CLINCH && myPotential < opp.getFinalScore()) {
                    force(Reason.CLINCH);
                } else if (now >= countdownDeadline(ServerClock.toLocal(opp.getFinishedAt()))) {
                    force(Reason.COUNTDOWN);
                }
            }
            if (!forced && now >= matchEndLocal) {
                force(Reason.TIME_UP);
            }
        }

        publishOpponent(opp, now);
    }

    private void force(Reason reason) {
        forced = true;
        forcedReason = reason;
        listener.onForceFinish(reason);
    }

    private long countdownDeadline(long firstFinishedLocal) {
        long limit = MatchRules.finishCountdownSeconds(difficulty) * 1000L;
        return Math.min(firstFinishedLocal + limit, matchEndLocal);
    }

    private void publishOpponent(PlayerState opp, long now) {
        int countdown = -1;
        if (opp.isFinished() && !mineTerminal && opp.getFinishedAt() > 0L) {
            countdown = secondsUntil(countdownDeadline(ServerClock.toLocal(opp.getFinishedAt())),
                    now);
        } else if (mineTerminal && !opp.isFinished()) {
            countdown = secondsUntil(countdownDeadline(myFinishedLocal), now);
        }
        int offline = offlineSince > 0L ? (int) ((now - offlineSince) / 1000L) : -1;
        listener.onOpponent(new Opponent(opp.getName(), opp.getRounds(), opp.getPhase(),
                opp.hasPosition(), opp.getLatitude(), opp.getLongitude(), countdown, offline));
    }

    private static int secondsUntil(long deadline, long now) {
        return (int) Math.max(0L, (deadline - now + 999L) / 1000L);
    }

    private PlayerState currentMe() {
        return new PlayerState(uid, "", myRounds, PlayerState.PHASE_FINISHED, myFinal, mySolved,
                myWrong, myFinal, myFinishedServer, 0.0, 0.0, false, true, "", myRoundPoints);
    }

    private int currentScore() {
        return mineTerminal ? myFinal : myScore;
    }

    private void handleRemoved() {
        if (!running || resolved) {
            return;
        }
        PlayerState opp = opponent;
        if (opp != null && opp.isFinished()) {
            if (!mineTerminal) {
                mineTerminal = true;
                myFinal = myScore;
                myFinishedLocal = System.currentTimeMillis();
                myFinishedServer = ServerClock.fromLocal(myFinishedLocal);
            }
            conclude(MatchRules.decide(currentMe(), opp));
            return;
        }
        conclude(MatchResult.Outcome.WIN, MatchResult.Decider.FORFEIT, opp);
    }

    private void conclude(MatchResult.Outcome outcome, MatchResult.Decider decider,
                          PlayerState opp) {
        boolean known = opp != null && opp.isFinished();
        conclude(new MatchResult(outcome, decider, currentScore(),
                known ? opp.getFinalScore() : MatchResult.UNKNOWN_SCORE,
                opp != null ? opp.getName() : lastName,
                known ? opp.roundPointsArray() : null));
    }

    private void conclude(MatchResult result) {
        if (resolved) {
            return;
        }
        resolved = true;
        stop();
        listener.onResult(result);
    }
}