package com.example.ga.planner.service;

import com.example.ga.planner.api.PlanRequest;
import com.example.ga.planner.api.SatelliteInput;
import com.example.ga.planner.api.WindowInput;
import com.example.ga.planner.ga.GeneticPlannerEngine;
import com.example.ga.planner.gantt.GanttBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanningServiceTest {

    @Test
    void shouldGeneratePlanForThreeSatellites() {
        PlanningService service = new PlanningService(new GeneticPlannerEngine(), new GanttBuilder());

        PlanRequest request = new PlanRequest(
                List.of(
                        sat("SAT-01", 1000, "GS-A"),
                        sat("SAT-02", 2000, "GS-B"),
                        sat("SAT-03", 3000, "GS-A")
                ),
                80,
                120,
                8,
                12);

        var response = service.generatePlan(request);

        assertEquals(3, response.satellitePlans().size());
        assertFalse(response.ganttTasks().isEmpty());
        assertFalse(response.mermaidGantt().isBlank());
        assertFalse(response.ganttPngBase64().isBlank());
        assertNotNull(response.objectiveBreakdown());
        assertTrue(response.satellitePlans().stream().allMatch(p -> p.observationPlan().size() >= 2));
        assertTrue(response.satellitePlans().stream().flatMap(p -> p.observationPlan().stream()).allMatch(o -> o.profit() > 0));
        assertTrue(response.satellitePlans().stream().flatMap(p -> p.downlinkPlan().stream()).allMatch(d -> d.downlinkRateMbps() > 0));
    }

    @Test
    void shouldGenerateFastJsonString() {
        PlanningService service = new PlanningService(new GeneticPlannerEngine(), new GanttBuilder());
        PlanRequest request = new PlanRequest(List.of(sat("SAT-01", 1000, "GS-A"), sat("SAT-02", 2000, "GS-B"), sat("SAT-03", 3000, "GS-C")), 40, 60, 4, 10);

        String json = service.generatePlanFastJson(request);

        assertFalse(json.isBlank());
        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("ganttPngBase64"));
        assertTrue(json.contains("objectiveBreakdown"));
    }

    private SatelliteInput sat(String id, long base, String stationId) {
        return new SatelliteInput(id,
                observationWindows(id, base),
                stationWindows(base + 2200, stationId, 8),
                stationWindows(base + 2400, stationId, 12));
    }

    private List<WindowInput> stationWindows(long base, String stationId, double rateMbps) {
        return List.of(
                new WindowInput(base, base + 60, null, stationId, 10, 6, 1, 150, rateMbps),
                new WindowInput(base + 120, base + 220, null, stationId, 12, 8, 1, 220, rateMbps)
        );
    }

    private List<WindowInput> observationWindows(String satelliteId, long base) {
        return List.of(
                new WindowInput(base, base + 60, satelliteId + "-T1", null, 80, 22, 2, 100, 0),
                new WindowInput(base + 120, base + 220, satelliteId + "-T2", null, 95, 30, 3, 140, 0),
                new WindowInput(base + 260, base + 340, satelliteId + "-T3", null, 90, 26, 2, 130, 0),
                new WindowInput(base + 530, base + 640, satelliteId + "-T4", null, 110, 36, 4, 180, 0),
                new WindowInput(base + 850, base + 940, satelliteId + "-T5", null, 98, 31, 3, 150, 0),
                new WindowInput(base + 1120, base + 1220, satelliteId + "-T6", null, 108, 33, 4, 165, 0),
                new WindowInput(base + 1260, base + 1340, satelliteId + "-T7", null, 92, 25, 2, 120, 0),
                new WindowInput(base + 1530, base + 1640, satelliteId + "-T8", null, 118, 37, 4, 190, 0),
                new WindowInput(base + 1850, base + 1940, satelliteId + "-T9", null, 100, 29, 3, 145, 0)
        );
    }
}
