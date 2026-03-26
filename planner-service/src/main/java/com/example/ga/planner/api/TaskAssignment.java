package com.example.ga.planner.api;

public record TaskAssignment(
        String satelliteId,
        TaskType taskType,
        long startEpochSecond,
        long endEpochSecond,
        int windowIndex,
        long durationSeconds,
        String targetId,
        String stationId,
        double profit,
        double energyCost,
        double attitudeCost,
        double dataVolumeMb,
        double downlinkRateMbps
) {
    public TaskAssignment(String satelliteId,
                          TaskType taskType,
                          long startEpochSecond,
                          long endEpochSecond,
                          int windowIndex,
                          long durationSeconds) {
        this(satelliteId, taskType, startEpochSecond, endEpochSecond, windowIndex, durationSeconds,
                null, null, 0, 0, 0, 0, 0);
    }
}
