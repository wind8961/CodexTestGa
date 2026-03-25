package com.example.ga.common.model;

import java.util.List;

public record SatelliteWindowSet(
        String satelliteId,
        List<Window> observationWindows,
        List<Window> ttcWindows,
        List<Window> downlinkWindows) {
}
