package com.example.ga.planner.ga;

import com.example.ga.common.model.Window;
import com.example.ga.planner.api.PlanRequest;
import com.example.ga.planner.api.SatelliteInput;
import com.example.ga.planner.api.TaskAssignment;
import com.example.ga.planner.api.TaskType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Component
public class GeneticPlannerEngine {

    public Chromosome solve(PlanRequest request) {
        if (request.satellites().size() < 3 || request.satellites().size() > 20) {
            throw new IllegalArgumentException("satellite count must be within 3~20");
        }

        List<Chromosome> population = IntStream.range(0, request.populationSize())
                .mapToObj(i -> randomChromosome(request.satellites()))
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
                Chromosome child = crossover(p1, p2);
                if (ThreadLocalRandom.current().nextInt(100) < request.mutationRatePercent()) {
                    child = mutate(child, request.satellites());
                }
                next.add(reScore(child));
            }
            population = next;
        }

        return best;
    }

    private Chromosome randomChromosome(List<SatelliteInput> satellites) {
        List<TaskAssignment> genes = new ArrayList<>();
        for (SatelliteInput sat : satellites) {
            addGene(genes, sat.satelliteId(), TaskType.OBSERVATION, sat.observationWindows());
            addGene(genes, sat.satelliteId(), TaskType.TTC, sat.ttcWindows());
            addGene(genes, sat.satelliteId(), TaskType.DOWNLINK, sat.downlinkWindows());
        }
        return new Chromosome(genes, fitness(genes));
    }

    private void addGene(List<TaskAssignment> genes, String satelliteId, TaskType type, List<com.example.ga.planner.api.WindowInput> windows) {
        int index = ThreadLocalRandom.current().nextInt(windows.size());
        com.example.ga.planner.api.WindowInput w = windows.get(index);
        Window window = new Window(w.startEpochSecond(), w.endEpochSecond());
        genes.add(new TaskAssignment(satelliteId, type, window.startEpochSecond(), window.endEpochSecond(), index, window.durationSeconds()));
    }

    private Chromosome tournament(List<Chromosome> population) {
        Random random = ThreadLocalRandom.current();
        Chromosome a = population.get(random.nextInt(population.size()));
        Chromosome b = population.get(random.nextInt(population.size()));
        return a.fitness() >= b.fitness() ? a : b;
    }

    private Chromosome crossover(Chromosome p1, Chromosome p2) {
        int cut = ThreadLocalRandom.current().nextInt(p1.genes().size());
        List<TaskAssignment> genes = new ArrayList<>(p1.genes().subList(0, cut));
        genes.addAll(p2.genes().subList(cut, p2.genes().size()));
        return new Chromosome(genes, fitness(genes));
    }

    private Chromosome mutate(Chromosome source, List<SatelliteInput> satellites) {
        List<TaskAssignment> genes = new ArrayList<>(source.genes());
        int idx = ThreadLocalRandom.current().nextInt(genes.size());
        TaskAssignment old = genes.get(idx);
        SatelliteInput sat = satellites.stream().filter(s -> s.satelliteId().equals(old.satelliteId())).findFirst().orElseThrow();

        List<com.example.ga.planner.api.WindowInput> pool = switch (old.taskType()) {
            case OBSERVATION -> sat.observationWindows();
            case TTC -> sat.ttcWindows();
            case DOWNLINK -> sat.downlinkWindows();
        };
        int newIndex = ThreadLocalRandom.current().nextInt(pool.size());
        var picked = pool.get(newIndex);
        genes.set(idx, new TaskAssignment(old.satelliteId(), old.taskType(), picked.startEpochSecond(), picked.endEpochSecond(), newIndex,
                picked.endEpochSecond() - picked.startEpochSecond()));

        return new Chromosome(genes, fitness(genes));
    }

    private Chromosome reScore(Chromosome chromosome) {
        return new Chromosome(chromosome.genes(), fitness(chromosome.genes()));
    }

    private double fitness(List<TaskAssignment> genes) {
        long totalDuration = genes.stream().mapToLong(TaskAssignment::durationSeconds).sum();
        long conflicts = 0;
        for (int i = 0; i < genes.size(); i++) {
            for (int j = i + 1; j < genes.size(); j++) {
                TaskAssignment a = genes.get(i);
                TaskAssignment b = genes.get(j);
                if (!a.satelliteId().equals(b.satelliteId())) {
                    continue;
                }
                boolean overlap = a.startEpochSecond() < b.endEpochSecond() && a.endEpochSecond() > b.startEpochSecond();
                if (overlap) {
                    conflicts++;
                }
            }
        }
        return totalDuration - conflicts * 10_000;
    }
}
