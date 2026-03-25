package com.example.ga.common.model;

public record Window(long startEpochSecond, long endEpochSecond) {

    public Window {
        if (endEpochSecond <= startEpochSecond) {
            throw new IllegalArgumentException("window end must be greater than start");
        }
    }

    public long durationSeconds() {
        return endEpochSecond - startEpochSecond;
    }

    public boolean overlap(Window other) {
        return startEpochSecond < other.endEpochSecond && endEpochSecond > other.startEpochSecond;
    }
}
