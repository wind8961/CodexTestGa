package com.example.ga.planner.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SatelliteInput(
        @NotBlank String satelliteId,
        @NotEmpty List<@Valid WindowInput> observationWindows,
        @NotEmpty List<@Valid WindowInput> ttcWindows,
        @NotEmpty List<@Valid WindowInput> downlinkWindows) {
}
