package com.example.ga.planner.api;

import java.util.List;

public record PlanResponse(
        double fitness,
        int generationReached,
        List<SatellitePlan> satellitePlans,
        List<GanttTask> ganttTasks,
        String mermaidGantt,
        String ganttPngBase64) {
}
