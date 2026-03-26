package com.example.ga.planner.api;

public record ObjectiveBreakdown(
        double profitScore,
        double energyCost,
        double attitudeCost,
        double latencyCost,
        double conflictPenalty,
        double weightedFitness
) {
}
