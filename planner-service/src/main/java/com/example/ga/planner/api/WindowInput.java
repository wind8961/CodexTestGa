package com.example.ga.planner.api;

import jakarta.validation.constraints.Min;

// 窗口输入：窗口开始结束时间戳，附带任务收益/能耗/站点等参数
public record WindowInput(
        @Min(0) long startEpochSecond,
        @Min(1) long endEpochSecond,
        String targetId,
        String stationId,
        @Min(0) double profit,
        @Min(0) double energyCost,
        @Min(0) double attitudeCost,
        @Min(0) double dataVolumeMb,
        @Min(0) double downlinkRateMbps
) {
    public WindowInput(long startEpochSecond, long endEpochSecond) {
        this(startEpochSecond, endEpochSecond, null, null, 0, 0, 0, 0, 0);
    }
}
