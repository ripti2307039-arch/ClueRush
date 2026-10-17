package com.ripti.cluerush.model;

public class LobbyPlayer {

    private final String uid;
    private final String name;
    private final boolean ready;
    private final boolean host;

    public LobbyPlayer(String uid, String name, boolean ready, boolean host) {
        this.uid = uid;
        this.name = name;
        this.ready = ready;
        this.host = host;
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
}