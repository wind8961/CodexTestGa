package com.example.ga.planner.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 任务规划请求参数，包含卫星窗口列表、种群大小、迭代次数、精英数量、变异率等配置
public record PlanRequest(
        @NotEmpty List<@Valid SatelliteInput> satellites,   // 卫星及观测、测控、数传窗口输入列表
        @Min(20) @Max(2000) int populationSize,             // 种群大小
        @Min(10) @Max(3000) int generations,                // 迭代次数
        @Min(1) @Max(50) int eliteCount,                    // 精英数量
        @Min(0) @Max(100) int mutationRatePercent           // 突变率
) {
}
