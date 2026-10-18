package com.ripti.cluerush.game;

import com.ripti.cluerush.model.Difficulty;
import com.ripti.cluerush.model.Round;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameSession {

    public static final int WRONG_PLACE_PENALTY = ScoreRules.WRONG_PLACE_PENALTY;

    private final List<Round> rounds;
    private final Difficulty difficulty;
    private final Set<String> discovered = new HashSet<>();
    private final List<RoundScore> roundScores = new ArrayList<>();
    private int index;
    private int wrongAttempts;
    private int streak;

    public GameSession(List<Round> rounds, Difficulty difficulty) {
        this.rounds = new ArrayList<>(rounds);
        this.difficulty = difficulty;
    }

    public Round getCurrent() {
        return index < rounds.size() ? rounds.get(index) : null;
    }

    public Round getRound(int position) {
        return rounds.get(position);
    }

    public int getIndex() {
        return index;
    }

    public int getTotal() {
        return rounds.size();
    }

    public boolean isFinished() {
        return index >= rounds.size();
    }

    public void advance() {
        if (index < rounds.size()) {
            index++;
        }
    }

    public Set<String> getCompletedLocationIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < index && i < rounds.size(); i++) {
            ids.add(rounds.get(i).getLocation().getId());
        }
        return ids;
    }

    public boolean discover(String locationId) {
        return discovered.add(locationId);
    }

    public Set<String> getDiscoveredIds() {
        return new HashSet<>(discovered);
    }

    public void registerWrongAttempt() {
        wrongAttempts++;
    }

    public int getWrongAttempts() {
        return wrongAttempts;
    }

    public int getPenaltyPoints() {
        return wrongAttempts * ScoreRules.WRONG_PLACE_PENALTY;
    }

    public RoundScore recordTask(boolean solved, int mistakes, int secondsLeft,
                                 int limitSeconds, double findSeconds) {
        RoundScore score = ScoreRules.score(difficulty, solved, mistakes, secondsLeft,
                limitSeconds, findSeconds, streak);
        roundScores.add(score);
        streak = solved && mistakes == 0 ? streak + 1 : 0;
        return score;
    }

    public List<RoundScore> getRoundScores() {
        return Collections.unmodifiableList(roundScores);
    }

    public int getTasksSolved() {
        int solved = 0;
        for (RoundScore score : roundScores) {
            if (score.isSolved()) {
                solved++;
            }
        }
        return solved;
    }

    public int getTaskMistakes() {
        int total = 0;
        for (RoundScore score : roundScores) {
            total += score.getMistakes();
        }
        return total;
    }

    public int getEarnedPoints() {
        int sum = 0;
        for (RoundScore score : roundScores) {
            sum += score.getTotal();
        }
        return sum;
    }

    public boolean isPerfect() {
        return isFinished() && !rounds.isEmpty()
                && getTasksSolved() == rounds.size() && wrongAttempts == 0;
    }

    public int getPerfectBonus() {
        return isPerfect() ? ScoreRules.perfectBonus(difficulty) : 0;
    }

    public int getTotalScore() {
        return Math.max(0, getEarnedPoints() - getPenaltyPoints()) + getPerfectBonus();
    }

    public Rank getRank() {
        return Rank.forScore(getTotalScore(),
                ScoreRules.maxPoints(difficulty, rounds.size()));
    }
}