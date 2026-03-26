package com.example.ga.planner.ga;

import com.example.ga.common.model.Window;
import com.example.ga.planner.api.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class GeneticPlannerEngine {

    public Chromosome solve(PlanRequest request) {
        if (request.satellites().size() < 3 || request.satellites().size() > 20) {
            throw new IllegalArgumentException("satellite count must be within 3~20");
        }

        List<Chromosome> population = IntStream.range(0, request.populationSize())
                .mapToObj(i -> randomChromosome(request.satellites(), request.objectiveWeights()))
                .toList();

        Chromosome best = population.stream().max(Comparator.comparingDouble(Chromosome::fitness)).orElseThrow();

        for (int generation = 0; generation < request.generations(); generation++) {
            List<Chromosome> sorted = population.stream()
                    .sorted(Comparator.comparingDouble(Chromosome::fitness).reversed())
                    .toList();

            if (!sorted.isEmpty() && sorted.get(0).fitness() > best.fitness()) {
                best = sorted.get(0);
            }

            List<Chromosome> next = new ArrayList<>();
            int elite = Math.min(request.eliteCount(), sorted.size());
            next.addAll(sorted.subList(0, elite));

            while (next.size() < request.populationSize()) {
                Chromosome p1 = tournament(sorted);
                Chromosome p2 = tournament(sorted);
                Chromosome child = crossover(p1, p2, request.satellites(), request.objectiveWeights());
                if (ThreadLocalRandom.current().nextInt(100) < request.mutationRatePercent()) {
                    child = mutate(child, request.satellites(), request.objectiveWeights());
                }
                next.add(reScore(child, request.objectiveWeights()));
            }
            population = next;
        }
        return best;
    }

    private Chromosome randomChromosome(List<SatelliteInput> satellites, ObjectiveWeights weights) {
        List<TaskAssignment> genes = new ArrayList<>();
        for (SatelliteInput sat : satellites) {
            int ttcIndex = ThreadLocalRandom.current().nextInt(sat.ttcWindows().size());
            int downlinkIndex = ThreadLocalRandom.current().nextInt(sat.downlinkWindows().size());

            TaskAssignment ttc = toAssignment(sat.satelliteId(), TaskType.TTC, ttcIndex, sat.ttcWindows().get(ttcIndex));
            TaskAssignment downlink = toAssignment(sat.satelliteId(), TaskType.DOWNLINK, downlinkIndex, sat.downlinkWindows().get(downlinkIndex));
            genes.add(ttc);
            genes.add(downlink);

            List<Integer> orderedIndexes = IntStream.range(0, sat.observationWindows().size()).boxed()
                    .sorted(Comparator.comparingLong(i -> sat.observationWindows().get(i).startEpochSecond()))
                    .toList();

            int targetObsCount = ThreadLocalRandom.current().nextInt(1, Math.max(2, sat.observationWindows().size() / 2));
            long previousEnd = Long.MIN_VALUE;
            int chosen = 0;
            for (int idx : orderedIndexes) {
                if (chosen >= targetObsCount) {
                    break;
                }
                WindowInput obs = sat.observationWindows().get(idx);
                if (obs.endEpochSecond() > downlink.startEpochSecond() || obs.startEpochSecond() < previousEnd) {
                    continue;
                }
                genes.add(toAssignment(sat.satelliteId(), TaskType.OBSERVATION, idx, obs));
                previousEnd = obs.endEpochSecond();
                chosen++;
            }
            if (chosen == 0) {
                int fallback = ThreadLocalRandom.current().nextInt(sat.observationWindows().size());
                genes.add(toAssignment(sat.satelliteId(), TaskType.OBSERVATION, fallback, sat.observationWindows().get(fallback)));
            }
        }
        return score(genes, weights);
    }

    private TaskAssignment toAssignment(String satelliteId, TaskType type, int index, WindowInput w) {
        Window window = new Window(w.startEpochSecond(), w.endEpochSecond());
        long duration = window.durationSeconds();
        return new TaskAssignment(
                satelliteId,
                type,
                window.startEpochSecond(),
                window.endEpochSecond(),
                index,
                duration,
                w.targetId(),
                w.stationId(),
                valueOrDefault(w.profit(), duration),
                valueOrDefault(w.energyCost(), duration * 0.25),
                valueOrDefault(w.attitudeCost(), 1.0),
                valueOrDefault(w.dataVolumeMb(), duration),
                valueOrDefault(w.downlinkRateMbps(), 5.0)
        );
    }

    private double valueOrDefault(double value, double defaultValue) {
        return value <= 0 ? defaultValue : value;
    }

    private Chromosome tournament(List<Chromosome> population) {
        Random random = ThreadLocalRandom.current();
        Chromosome a = population.get(random.nextInt(population.size()));
        Chromosome b = population.get(random.nextInt(population.size()));
        return a.fitness() >= b.fitness() ? a : b;
    }

    private Chromosome crossover(Chromosome p1, Chromosome p2, List<SatelliteInput> satellites, ObjectiveWeights weights) {
        Map<String, List<TaskAssignment>> parent1 = p1.genes().stream().collect(Collectors.groupingBy(TaskAssignment::satelliteId));
        Map<String, List<TaskAssignment>> parent2 = p2.genes().stream().collect(Collectors.groupingBy(TaskAssignment::satelliteId));

        List<TaskAssignment> genes = new ArrayList<>();
        for (SatelliteInput sat : satellites) {
            boolean chooseFirst = ThreadLocalRandom.current().nextBoolean();
            genes.addAll(chooseFirst ? parent1.getOrDefault(sat.satelliteId(), List.of()) : parent2.getOrDefault(sat.satelliteId(), List.of()));
        }
        return score(genes, weights);
    }

    private Chromosome mutate(Chromosome source, List<SatelliteInput> satellites, ObjectiveWeights weights) {
        List<TaskAssignment> genes = new ArrayList<>(source.genes());
        SatelliteInput sat = satellites.get(ThreadLocalRandom.current().nextInt(satellites.size()));

        genes.removeIf(t -> t.satelliteId().equals(sat.satelliteId()) && t.taskType() == TaskType.OBSERVATION);
        int newObsCount = ThreadLocalRandom.current().nextInt(1, Math.max(2, sat.observationWindows().size() / 2));
        List<Integer> shuffled = new ArrayList<>(IntStream.range(0, sat.observationWindows().size()).boxed().toList());
        java.util.Collections.shuffle(shuffled);
        for (int i = 0; i < Math.min(newObsCount, shuffled.size()); i++) {
            int idx = shuffled.get(i);
            genes.add(toAssignment(sat.satelliteId(), TaskType.OBSERVATION, idx, sat.observationWindows().get(idx)));
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            genes.removeIf(t -> t.satelliteId().equals(sat.satelliteId()) && t.taskType() == TaskType.TTC);
            int idx = ThreadLocalRandom.current().nextInt(sat.ttcWindows().size());
            genes.add(toAssignment(sat.satelliteId(), TaskType.TTC, idx, sat.ttcWindows().get(idx)));
        } else {
            genes.removeIf(t -> t.satelliteId().equals(sat.satelliteId()) && t.taskType() == TaskType.DOWNLINK);
            int idx = ThreadLocalRandom.current().nextInt(sat.downlinkWindows().size());
            genes.add(toAssignment(sat.satelliteId(), TaskType.DOWNLINK, idx, sat.downlinkWindows().get(idx)));
        }

        return score(genes, weights);
    }

    private Chromosome reScore(Chromosome chromosome, ObjectiveWeights weights) {
        return score(chromosome.genes(), weights);
    }

    private Chromosome score(List<TaskAssignment> genes, ObjectiveWeights weights) {
        Map<String, List<TaskAssignment>> satTasks = genes.stream().collect(Collectors.groupingBy(TaskAssignment::satelliteId));

        double profitScore = 0;
        double energyCost = genes.stream().mapToDouble(TaskAssignment::energyCost).sum();
        double attitudeCost = 0;
        double latencyCost = 0;
        double conflictPenalty = 0;
        Set<String> observedTargets = new HashSet<>();

        for (List<TaskAssignment> tasks : satTasks.values()) {
            List<TaskAssignment> obs = tasks.stream()
                    .filter(t -> t.taskType() == TaskType.OBSERVATION)
                    .sorted(Comparator.comparingLong(TaskAssignment::startEpochSecond))
                    .toList();
            TaskAssignment ttc = tasks.stream().filter(t -> t.taskType() == TaskType.TTC).findFirst().orElse(null);
            TaskAssignment downlink = tasks.stream().filter(t -> t.taskType() == TaskType.DOWNLINK).findFirst().orElse(null);

            for (int i = 0; i < obs.size(); i++) {
                TaskAssignment current = obs.get(i);
                boolean firstTimeTarget = current.targetId() == null || observedTargets.add(current.targetId());
                profitScore += firstTimeTarget ? current.profit() : current.profit() * 0.6;
                attitudeCost += current.attitudeCost();

                if (i > 0) {
                    TaskAssignment previous = obs.get(i - 1);
                    attitudeCost += Math.abs(current.startEpochSecond() - previous.endEpochSecond()) / 60.0;
                }

                if (ttc == null || ttc.endEpochSecond() > current.startEpochSecond()) {
                    conflictPenalty += 3000;
                }

                if (downlink != null) {
                    latencyCost += Math.max(0, downlink.startEpochSecond() - current.endEpochSecond());
                    if (current.endEpochSecond() > downlink.startEpochSecond()) {
                        conflictPenalty += 8000;
                    }
                } else {
                    conflictPenalty += 5000;
                }
            }

            if (downlink != null) {
                double totalData = obs.stream().mapToDouble(TaskAssignment::dataVolumeMb).sum();
                double capacity = downlink.durationSeconds() * downlink.downlinkRateMbps();
                if (totalData > capacity) {
                    conflictPenalty += (totalData - capacity) * 8;
                }
            }

            conflictPenalty += overlapPenalty(tasks);
        }

        conflictPenalty += stationConflictPenalty(genes.stream().filter(t -> t.taskType() != TaskType.OBSERVATION).toList());

        double fitness = weights.profitWeight() * profitScore
                - weights.energyWeight() * energyCost
                - weights.attitudeWeight() * attitudeCost
                - weights.latencyWeight() * latencyCost
                - conflictPenalty;

        ObjectiveBreakdown breakdown = new ObjectiveBreakdown(profitScore, energyCost, attitudeCost, latencyCost, conflictPenalty, fitness);
        return new Chromosome(genes, breakdown, fitness);
    }

    private double overlapPenalty(List<TaskAssignment> tasks) {
        double penalty = 0;
        for (int i = 0; i < tasks.size(); i++) {
            for (int j = i + 1; j < tasks.size(); j++) {
                TaskAssignment a = tasks.get(i);
                TaskAssignment b = tasks.get(j);
                if (a.startEpochSecond() < b.endEpochSecond() && a.endEpochSecond() > b.startEpochSecond()) {
                    penalty += 10_000;
                }
            }
        }
        return penalty;
    }

    private double stationConflictPenalty(List<TaskAssignment> stationTasks) {
        Map<String, List<TaskAssignment>> byStation = new HashMap<>();
        for (TaskAssignment task : stationTasks) {
            if (task.stationId() == null || task.stationId().isBlank()) {
                continue;
            }
            byStation.computeIfAbsent(task.stationId(), key -> new ArrayList<>()).add(task);
        }

        double penalty = 0;
        for (List<TaskAssignment> stationWindowList : byStation.values()) {
            penalty += overlapPenalty(stationWindowList) * 0.6;
        }
        return penalty;
    }
}
