package com.example.ga.planner.api;

import jakarta.validation.constraints.Min;

public record ObjectiveWeights(
        @Min(0) double profitWeight,
        @Min(0) double energyWeight,
        @Min(0) double attitudeWeight,
        @Min(0) double latencyWeight) {

    public static ObjectiveWeights defaults() {
        return new ObjectiveWeights(1.0, 0.6, 0.4, 0.5);
    }
}
