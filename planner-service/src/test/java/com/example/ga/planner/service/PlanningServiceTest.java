package com.example.ga.planner.service;

import com.example.ga.planner.api.PlanRequest;
import com.example.ga.planner.api.SatelliteInput;
import com.example.ga.planner.api.WindowInput;
import com.example.ga.planner.ga.GeneticPlannerEngine;
import com.example.ga.planner.gantt.GanttBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanningServiceTest {

    @Test
    void shouldGeneratePlanForThreeSatellites() {
        PlanningService service = new PlanningService(new GeneticPlannerEngine(), new GanttBuilder());

        PlanRequest request = new PlanRequest(
                List.of(
                        sat("SAT-01", 1000),
                        sat("SAT-02", 2000),
                        sat("SAT-03", 3000)
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
    }

    @Test
    void shouldGenerateFastJsonString() {
        PlanningService service = new PlanningService(new GeneticPlannerEngine(), new GanttBuilder());
        PlanRequest request = new PlanRequest(List.of(sat("SAT-01", 1000), sat("SAT-02", 2000), sat("SAT-03", 3000)), 40, 60, 4, 10);

        String json = service.generatePlanFastJson(request);

        assertFalse(json.isBlank());
        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("ganttPngBase64"));
    }

    private SatelliteInput sat(String id, long base) {
        return new SatelliteInput(id,
                windows(base),
                windows(base + 200),
                windows(base + 400));
    }

    private List<WindowInput> windows(long base) {
        return List.of(
                new WindowInput(base, base + 60),
                new WindowInput(base + 120, base + 220),
                new WindowInput(base + 260, base + 340)
        );
    }
}
