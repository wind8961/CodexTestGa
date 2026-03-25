package com.example.ga.planner.api;

public record GanttTask(String lane, String label, long startEpochSecond, long endEpochSecond, String color) {
}
