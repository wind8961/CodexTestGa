package com.example.ga.planner.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlanRequest(
        @NotEmpty List<@Valid SatelliteInput> satellites,
        @Min(20) @Max(2000) int populationSize,
        @Min(10) @Max(3000) int generations,
        @Min(1) @Max(50) int eliteCount,
        @Min(0) @Max(100) int mutationRatePercent) {
}
