package com.example.ga.planner.api;

import java.util.List;

public record SatellitePlan(
        String satelliteId,
        List<TaskAssignment> observationPlan,
        List<TaskAssignment> ttcPlan,
        List<TaskAssignment> downlinkPlan) {
}
