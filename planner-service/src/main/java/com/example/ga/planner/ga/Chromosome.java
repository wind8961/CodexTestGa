package com.example.ga.planner.ga;

import com.example.ga.planner.api.TaskAssignment;

import java.util.List;

public record Chromosome(List<TaskAssignment> genes, double fitness) {
}
