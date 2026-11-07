
package com.ripti.cluerush.model;

public class LobbyPlayer {

    public static final String TEAM_A = "A";
    public static final String TEAM_B = "B";

    private final String uid;
    private final String name;
    private final boolean ready;
    private final boolean host;
    private final String team;

    public LobbyPlayer(String uid, String name, boolean ready, boolean host) {
        this(uid, name, ready, host, null);
    }

    public LobbyPlayer(String uid, String name, boolean ready, boolean host, String team) {
        this.uid = uid;
        this.name = name;
        this.ready = ready;
        this.host = host;
        this.team = team;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isHost() {
        return host;
    }

    public String getTeam() {
        return team;
    }

    public boolean isTeamA() {
        return TEAM_A.equals(team);
    }

    public boolean isTeamB() {
        return TEAM_B.equals(team);
    }
}
