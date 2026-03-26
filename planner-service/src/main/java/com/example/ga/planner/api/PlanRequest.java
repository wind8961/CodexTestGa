package com.example.ga.planner.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 任务规划请求参数，包含卫星窗口列表、种群大小、迭代次数、精英数量、变异率等配置
public record PlanRequest(
        @NotEmpty List<@Valid SatelliteInput> satellites,
        @Min(20) @Max(2000) int populationSize,
        @Min(10) @Max(3000) int generations,
        @Min(1) @Max(50) int eliteCount,
        @Min(0) @Max(100) int mutationRatePercent,
        @Valid ObjectiveWeights objectiveWeights
) {
    public PlanRequest(List<SatelliteInput> satellites,
                       int populationSize,
                       int generations,
                       int eliteCount,
                       int mutationRatePercent) {
        this(satellites, populationSize, generations, eliteCount, mutationRatePercent, ObjectiveWeights.defaults());
    }

    public PlanRequest {
        objectiveWeights = objectiveWeights == null ? ObjectiveWeights.defaults() : objectiveWeights;
    }
}
