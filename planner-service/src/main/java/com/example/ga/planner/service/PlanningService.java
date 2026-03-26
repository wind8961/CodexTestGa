package com.example.ga.planner.service;

import com.alibaba.fastjson2.JSON;
import com.example.ga.planner.api.GanttTask;
import com.example.ga.planner.api.PlanRequest;
import com.example.ga.planner.api.PlanResponse;
import com.example.ga.planner.api.SatellitePlan;
import com.example.ga.planner.api.TaskAssignment;
import com.example.ga.planner.ga.Chromosome;
import com.example.ga.planner.ga.GeneticPlannerEngine;
import com.example.ga.planner.gantt.GanttBuilder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlanningService {
    private final GeneticPlannerEngine geneticPlannerEngine;
    private final GanttBuilder ganttBuilder;

    public PlanningService(GeneticPlannerEngine geneticPlannerEngine, GanttBuilder ganttBuilder) {
        this.geneticPlannerEngine = geneticPlannerEngine;
        this.ganttBuilder = ganttBuilder;
    }

    public PlanResponse generatePlan(PlanRequest request) {
        Chromosome best = geneticPlannerEngine.solve(request);

        Map<String, List<TaskAssignment>> grouped = best.genes().stream()
                .sorted(Comparator.comparing(TaskAssignment::startEpochSecond))
                .collect(Collectors.groupingBy(TaskAssignment::satelliteId));

        List<SatellitePlan> plans = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String sat = entry.getKey();
                    List<TaskAssignment> all = entry.getValue();
                    return new SatellitePlan(
                            sat,
                            all.stream().filter(t -> t.taskType().name().equals("OBSERVATION")).toList(),
                            all.stream().filter(t -> t.taskType().name().equals("TTC")).toList(),
                            all.stream().filter(t -> t.taskType().name().equals("DOWNLINK")).toList()
                    );
                }).toList();

        List<GanttTask> ganttTasks = best.genes().stream().map(task ->
                new GanttTask(task.satelliteId(), task.taskType().name(), task.startEpochSecond(), task.endEpochSecond(), color(task.taskType().name()))).toList();

        return new PlanResponse(
                best.fitness(),
                best.objectiveBreakdown(),
                request.generations(),
                plans,
                ganttTasks,
                ganttBuilder.toMermaid(ganttTasks),
                ganttBuilder.toPngBase64(ganttTasks));
    }

    public String generatePlanFastJson(PlanRequest request) {
        return JSON.toJSONString(generatePlan(request));
    }

    private String color(String type) {
        return switch (type) {
            case "OBSERVATION" -> "#1f77b4";
            case "TTC" -> "#ff7f0e";
            default -> "#2ca02c";
        };
    }
}
