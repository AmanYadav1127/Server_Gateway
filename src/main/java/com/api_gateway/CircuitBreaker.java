package com.api_gateway;

public class CircuitBreaker {

    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private State state = State.CLOSED;

    private final int failureThreshold = 3;
    private final long resetTimeout = 10_000;

    private int failureCount = 0;
    private long lastFailureTime = 0;

    private boolean probeInProgress = false;

    public synchronized boolean allowRequest() {

        if (state == State.CLOSED) {
            return true;
        }

        if (state == State.OPEN) {

            long elapsed =
                    System.currentTimeMillis() - lastFailureTime;

            if (elapsed < resetTimeout) {
                return false;
            }

            state = State.HALF_OPEN;
        }

        if (state == State.HALF_OPEN && !probeInProgress) {
            probeInProgress = true;
            return true;
        }

        return false;
    }

    public synchronized void recordSuccess() {

        failureCount = 0;
        state = State.CLOSED;
        probeInProgress = false;
    }

    public synchronized void recordFailure() {

        lastFailureTime = System.currentTimeMillis();

        if (state == State.HALF_OPEN) {
            state = State.OPEN;
            probeInProgress = false;
            return;
        }

        failureCount++;

        if (failureCount >= failureThreshold) {
            state = State.OPEN;
        }
    }

    public synchronized String getState() {
        return state.name();
    }
}