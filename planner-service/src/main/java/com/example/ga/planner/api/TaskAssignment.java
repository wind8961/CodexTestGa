package com.example.ga.planner.api;

public record TaskAssignment(
        String satelliteId,
        TaskType taskType,
        long startEpochSecond,
        long endEpochSecond,
        int windowIndex,
        long durationSeconds) {
}
