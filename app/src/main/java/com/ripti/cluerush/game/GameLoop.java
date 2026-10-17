package com.ripti.cluerush.game;

import android.view.Choreographer;

public final class GameLoop implements Choreographer.FrameCallback {

    public interface FrameListener {
        void onFrame(float deltaSeconds);
    }

    private static final float MAX_DELTA_SECONDS = 0.05f;

    private final Choreographer choreographer = Choreographer.getInstance();
    private final FrameListener listener;
    private long lastFrameNanos;
    private boolean running;

    public GameLoop(FrameListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        lastFrameNanos = 0L;
        choreographer.postFrameCallback(this);
    }

    public void stop() {
        running = false;
        choreographer.removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        float delta = lastFrameNanos == 0L
                ? 0f
                : (frameTimeNanos - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = frameTimeNanos;
        listener.onFrame(Math.min(delta, MAX_DELTA_SECONDS));
        choreographer.postFrameCallback(this);
    }
}