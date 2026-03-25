package com.example.ga.planner.api;

import jakarta.validation.constraints.Min;

public record WindowInput(@Min(0) long startEpochSecond, @Min(1) long endEpochSecond) {
}
