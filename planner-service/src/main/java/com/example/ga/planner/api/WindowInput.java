package com.example.ga.planner.api;

import jakarta.validation.constraints.Min;

// 窗口输入：窗口开始结束时间戳
public record WindowInput(@Min(0) long startEpochSecond, @Min(1) long endEpochSecond) {
}
