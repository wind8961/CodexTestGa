package com.example.ga.planner.ga;

import com.example.ga.planner.api.ObjectiveBreakdown;
import com.example.ga.planner.api.TaskAssignment;

import java.util.List;

// 染色体的数据结构
public record Chromosome(
        List<TaskAssignment> genes,
        ObjectiveBreakdown objectiveBreakdown,
        double fitness
) {
}
