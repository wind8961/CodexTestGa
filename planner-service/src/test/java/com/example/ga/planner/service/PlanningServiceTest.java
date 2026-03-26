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

        // 模拟卫星窗口输入：SAT-01, SAT-02, SAT-03，每个卫星有3组窗口
        PlanRequest request = new PlanRequest(
                List.of(
                        sat("SAT-01", 1000),
                        sat("SAT-02", 2000),
                        sat("SAT-03", 3000)
                ),    // 卫星输入窗口列表
                80,   // 种群大小
                120,  // 迭代次数
                8,    // 精英数量
                12);  // 突变率

        // 生成计划并验证结果
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

    // 生成卫星输入：卫星ID、观测窗口、TTC窗口和下行链接窗口
    private SatelliteInput sat(String id, long base) {
        return new SatelliteInput(id,
                observationWindows(base),
                stationWindows(base + 2200),
                stationWindows(base + 2400));
    }

    // 生成3组窗口输入
    private List<WindowInput> stationWindows(long base) {
        return List.of(
                new WindowInput(base, base + 60),
                new WindowInput(base + 120, base + 220)
        );
    }

    // 生成3组窗口输入
    private List<WindowInput> observationWindows(long base) {
        return List.of(
                new WindowInput(base, base + 60),
                new WindowInput(base + 120, base + 220),
                new WindowInput(base + 260, base + 340),
                new WindowInput(base + 530, base + 640),
                new WindowInput(base + 850, base + 940),
                new WindowInput(base + 1120, base + 1220),
                new WindowInput(base + 1260, base + 1340),
                new WindowInput(base + 1530, base + 1640),
                new WindowInput(base + 1850, base + 1940)
        );
    }
}
