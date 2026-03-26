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

/**
 * 遗传算法引擎核心类
 * <p>
 * 功能：基于遗传算法实现卫星任务智能规划，通过进化迭代生成最优任务分配方案
 * 核心流程：初始化种群 → 选择 → 交叉 → 变异 → 迭代进化 → 输出最优个体
 * </p>
 *
 * @author 自动生成
 * @since 卫星任务规划系统
 */
@Component
public class GeneticPlannerEngine {

    /**
     * 遗传算法主求解方法：入口方法，执行完整遗传算法流程
     *
     * @param request 任务规划请求参数，包含卫星列表、种群大小、迭代次数、精英数量、变异率等配置
     * @return 迭代后适应度最优的染色体（最优任务分配方案）
     * @throws IllegalArgumentException 卫星数量不在 3~20 范围内时抛出异常
     */
    public Chromosome solve(PlanRequest request) {
        // 校验卫星数量合法性，保证算法稳定性
        if (request.satellites().size() < 3 || request.satellites().size() > 20) {
            throw new IllegalArgumentException("satellite count must be within 3~20");
        }

        // 1. 初始化种群：生成指定数量的随机染色体（任务分配方案）
        List<Chromosome> population = IntStream.range(0, request.populationSize())
                .mapToObj(i -> randomChromosome(request.satellites()))
                .toList();

        // 初始化当前最优染色体
        Chromosome best = population.stream().max(Comparator.comparingDouble(Chromosome::fitness)).orElseThrow();

        // 2. 遗传算法迭代进化
        for (int generation = 0; generation < request.generations(); generation++) {
            // 种群按适应度降序排序
            List<Chromosome> sorted = population.stream()
                    .sorted(Comparator.comparingDouble(Chromosome::fitness).reversed())
                    .toList();

            // 更新全局最优解
            if (!sorted.isEmpty() && sorted.get(0).fitness() > best.fitness()) {
                best = sorted.get(0);
            }

            // 3. 生成下一代种群
            List<Chromosome> next = new ArrayList<>();

            // 精英保留策略：直接保留最优的N个个体到下一代
            int elite = Math.min(request.eliteCount(), sorted.size());
            next.addAll(sorted.subList(0, elite));

            // 填充种群至指定大小
            while (next.size() < request.populationSize()) {
                // 锦标赛选择父代
                Chromosome p1 = tournament(sorted);
                Chromosome p2 = tournament(sorted);

                // 交叉生成子代
                Chromosome child = crossover(p1, p2);

                // 概率执行变异操作
                if (ThreadLocalRandom.current().nextInt(100) < request.mutationRatePercent()) {
                    child = mutate(child, request.satellites());
                }

                // 重新计算适应度并加入下一代
                next.add(reScore(child));
            }

            // 种群替换，进入下一轮迭代
            population = next;
        }

        // 返回迭代完成后的最优解
        return best;
    }

    /**
     * 生成随机染色体
     * 为每颗卫星的观测、测控、数传三种任务随机分配一个时间窗口，构成一条染色体
     *
     * @param satellites 卫星输入列表
     * @return 随机生成的染色体（完整任务分配方案）
     */
    private Chromosome randomChromosome(List<SatelliteInput> satellites) {
        List<TaskAssignment> genes = new ArrayList<>();
        // 遍历所有卫星，为每个卫星生成三种任务的基因
        for (SatelliteInput sat : satellites) {
            addGene(genes, sat.satelliteId(), TaskType.OBSERVATION, sat.observationWindows());
            addGene(genes, sat.satelliteId(), TaskType.TTC, sat.ttcWindows());
            addGene(genes, sat.satelliteId(), TaskType.DOWNLINK, sat.downlinkWindows());
        }
        // 构建染色体并计算初始适应度
        return new Chromosome(genes, fitness(genes));
    }

    /**
     * 为指定卫星、任务类型随机选择一个时间窗口，生成对应的任务分配基因
     *
     * @param genes       基因列表（染色体）
     * @param satelliteId 卫星ID
     * @param type        任务类型（观测/测控/数传）
     * @param windows     该任务的可选时间窗口列表
     */
    private void addGene(List<TaskAssignment> genes, String satelliteId, TaskType type, List<com.example.ga.planner.api.WindowInput> windows) {
        // 随机选择一个窗口索引
        int index = ThreadLocalRandom.current().nextInt(windows.size());
        com.example.ga.planner.api.WindowInput w = windows.get(index);
        Window window = new Window(w.startEpochSecond(), w.endEpochSecond());
        // 构建任务分配基因并加入染色体
        genes.add(new TaskAssignment(satelliteId, type, window.startEpochSecond(), window.endEpochSecond(), index, window.durationSeconds()));
    }

    /**
     * 锦标赛选择算法
     * 从种群中随机抽取两个个体，返回适应度更高的个体作为父代
     * 优点：避免早熟，保留优秀基因，稳定性强
     *
     * @param population 排序后的种群
     * @return 被选中的父代染色体
     */
    private Chromosome tournament(List<Chromosome> population) {
        Random random = ThreadLocalRandom.current();
        Chromosome a = population.get(random.nextInt(population.size()));
        Chromosome b = population.get(random.nextInt(population.size()));
        return a.fitness() >= b.fitness() ? a : b;
    }

    /**
     * 单点交叉操作
     * 随机选择一个切割点，前半段继承父代1，后半段继承父代2，生成新子代
     *
     * @param p1 父代1
     * @param p2 父代2
     * @return 交叉后的子代染色体
     */
    private Chromosome crossover(Chromosome p1, Chromosome p2) {
        // 随机生成交叉点
        int cut = ThreadLocalRandom.current().nextInt(p1.genes().size());
        // 拼接基因片段
        List<TaskAssignment> genes = new ArrayList<>(p1.genes().subList(0, cut));
        genes.addAll(p2.genes().subList(cut, p2.genes().size()));
        // 生成新染色体并计算适应度
        return new Chromosome(genes, fitness(genes));
    }

    /**
     * 变异操作
     * 随机选择一个基因，重新为该任务分配一个时间窗口，保持基因结构不变
     * 作用：增加种群多样性，避免算法陷入局部最优
     *
     * @param source     待变异的原始染色体
     * @param satellites 卫星输入列表
     * @return 变异后的新染色体
     */
    private Chromosome mutate(Chromosome source, List<SatelliteInput> satellites) {
        List<TaskAssignment> genes = new ArrayList<>(source.genes());
        // 随机选择一个基因位置
        int idx = ThreadLocalRandom.current().nextInt(genes.size());
        TaskAssignment old = genes.get(idx);

        // 找到对应卫星信息
        SatelliteInput sat = satellites.stream()
                .filter(s -> s.satelliteId().equals(old.satelliteId()))
                .findFirst()
                .orElseThrow();

        // 根据任务类型获取对应可选窗口列表
        List<com.example.ga.planner.api.WindowInput> pool = switch (old.taskType()) {
            case OBSERVATION -> sat.observationWindows();
            case TTC -> sat.ttcWindows();
            case DOWNLINK -> sat.downlinkWindows();
        };

        // 随机重新选择一个窗口，生成新基因
        int newIndex = ThreadLocalRandom.current().nextInt(pool.size());
        var picked = pool.get(newIndex);
        genes.set(idx, new TaskAssignment(
                old.satelliteId(),
                old.taskType(),
                picked.startEpochSecond(),
                picked.endEpochSecond(),
                newIndex,
                picked.endEpochSecond() - picked.startEpochSecond()
        ));

        // 返回变异后的染色体
        return new Chromosome(genes, fitness(genes));
    }

    /**
     * 重新计算染色体适应度
     * 用于交叉/变异后刷新适应度值
     *
     * @param chromosome 待刷新的染色体
     * @return 刷新适应度后的新染色体
     */
    private Chromosome reScore(Chromosome chromosome) {
        return new Chromosome(chromosome.genes(), fitness(chromosome.genes()));
    }

    /**
     * 适应度函数（核心评价标准）
     * 适应度 = 总任务时长 - 冲突数 × 惩罚系数
     * 目标：最大化任务时长，最小化时间冲突
     *
     * @param genes 染色体基因列表（任务分配方案）
     * @return 适应度值（越大表示方案越优）
     */
    private double fitness(List<TaskAssignment> genes) {
        // 总有效任务执行时长
        long totalDuration = genes.stream().mapToLong(TaskAssignment::durationSeconds).sum();

        // 统计同一卫星的任务时间冲突数量
        long conflicts = 0;
        for (int i = 0; i < genes.size(); i++) {
            for (int j = i + 1; j < genes.size(); j++) {
                TaskAssignment a = genes.get(i);
                TaskAssignment b = genes.get(j);

                // 只检查同一颗卫星的任务冲突
                if (!a.satelliteId().equals(b.satelliteId())) {
                    continue;
                }

                // 判断时间窗口是否重叠（冲突）
                boolean overlap = a.startEpochSecond() < b.endEpochSecond() && a.endEpochSecond() > b.startEpochSecond();
                if (overlap) {
                    conflicts++;
                }
            }
        }

        // 冲突惩罚：每个冲突扣除10000时长，大幅降低冲突方案的适应度
        return totalDuration - conflicts * 10_000;
    }
}