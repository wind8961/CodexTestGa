# 卫星联合任务规划（GA）Spring Boot 微服务示例

本项目提供一个工程化的 Java 17 + Spring Boot 多模块项目，用遗传算法在**目标区域观测窗口**、**测控站窗口（TTC）**、**数传站窗口（Downlink）**约束下，自动生成联合最优解：

- 卫星观测计划
- 卫星测控计划
- 卫星数传计划
- 甘特图数据（JSON + Mermaid Gantt 文本 + PNG(Base64)）

支持卫星规模 **3~20 星**。

## 项目结构

```text
satellite-ga-platform
├── pom.xml                       # 父工程（Maven multi-module）
├── common                        # 公共模型层
│   └── src/main/java/.../model
└── planner-service               # 规划微服务（GA + API + 甘特图）
    ├── src/main/java/.../api
    ├── src/main/java/.../ga
    ├── src/main/java/.../service
    ├── src/main/java/.../gantt
    └── src/test/java/.../service
```

## 核心设计

1. **编码方式（Chromosome）**
   - 每颗卫星包含 3 个基因位：观测、TTC、数传。
   - 每个位选择对应窗口集合中的一个窗口。
2. **适应度函数（Fitness）**
   - 优先最大化任务总时长。
   - 对同星任务时间重叠冲突进行大额惩罚。
3. **进化策略**
   - 精英保留（Elite）
   - 锦标赛选择（Tournament）
   - 单点交叉（Single-point Crossover）
   - 随机突变（Mutation）
4. **输出结果**
   - 按星输出三类计划。
   - 输出甘特图任务列表。
   - 输出 Mermaid Gantt 文本，可直接在 Mermaid 渲染器生成图。

## 快速启动

### 1) 构建与测试

```bash
mvn clean test
```

### 2) 启动规划服务

```bash
mvn -pl planner-service spring-boot:run
```

服务默认端口 `8081`。

### 3) 调用规划接口

`POST /api/plans/optimize`

已提供可直接在 IntelliJ IDEA / VS Code REST Client 使用的 HTTP 测试文件：`planner-service/http/planner-api.http`。

额外提供 Fastjson 字符串接口：`POST /api/plans/optimize/fastjson`（返回 fastjson 序列化后的 JSON 字符串）。

示例请求：

```json
{
  "satellites": [
    {
      "satelliteId": "SAT-01",
      "observationWindows": [{"startEpochSecond": 1000, "endEpochSecond": 1060}, {"startEpochSecond": 1120, "endEpochSecond": 1200}],
      "ttcWindows": [{"startEpochSecond": 1300, "endEpochSecond": 1380}, {"startEpochSecond": 1420, "endEpochSecond": 1490}],
      "downlinkWindows": [{"startEpochSecond": 1600, "endEpochSecond": 1680}, {"startEpochSecond": 1700, "endEpochSecond": 1770}]
    },
    {
      "satelliteId": "SAT-02",
      "observationWindows": [{"startEpochSecond": 2000, "endEpochSecond": 2070}, {"startEpochSecond": 2140, "endEpochSecond": 2220}],
      "ttcWindows": [{"startEpochSecond": 2300, "endEpochSecond": 2360}, {"startEpochSecond": 2400, "endEpochSecond": 2490}],
      "downlinkWindows": [{"startEpochSecond": 2600, "endEpochSecond": 2680}, {"startEpochSecond": 2720, "endEpochSecond": 2790}]
    },
    {
      "satelliteId": "SAT-03",
      "observationWindows": [{"startEpochSecond": 3000, "endEpochSecond": 3070}, {"startEpochSecond": 3140, "endEpochSecond": 3220}],
      "ttcWindows": [{"startEpochSecond": 3300, "endEpochSecond": 3370}, {"startEpochSecond": 3420, "endEpochSecond": 3490}],
      "downlinkWindows": [{"startEpochSecond": 3600, "endEpochSecond": 3680}, {"startEpochSecond": 3720, "endEpochSecond": 3790}]
    }
  ],
  "populationSize": 120,
  "generations": 300,
  "eliteCount": 12,
  "mutationRatePercent": 10
}
```


## 本次增强能力（联合规划）

- 支持**多目标加权优化**：`objectiveWeights` 可配置收益、能耗、姿态切换、时延权重。
- 支持**同星多观测窗口**：单颗卫星可同时选取多个观测窗口。
- 支持**共享测控/数传窗口下传**：每星选择一个 TTC + Downlink 窗口，并将多个观测任务聚合到同一数传窗口评估容量。
- 支持**跨星地面站约束**：当不同卫星占用同一 `stationId` 且时间重叠时进行冲突惩罚。

扩展输入字段（`WindowInput`）示例：`targetId`、`stationId`、`profit`、`energyCost`、`attitudeCost`、`dataVolumeMb`、`downlinkRateMbps`。

## 甘特图生成

返回中 `mermaidGantt` 字段可直接复制到 Mermaid 渲染工具。

返回中 `ganttPngBase64` 为 PNG 图片的 Base64，可直接在前端 `<img src="data:image/png;base64,...">` 展示。

你也可以用前端（ECharts / Plotly / D3）直接消费 `ganttTasks` 生成交互式甘特图。

## 后续工程扩展建议

- 引入轨道动力学和地面站容量约束（站点并发/天线资源）。
- 多目标优化（收益、能耗、姿态切换、时延）改造为 NSGA-II。
- 将窗口输入改为消息总线（Kafka）实时流式更新。
- 微服务拆分：`window-service`、`optimizer-service`、`visualization-service`。
- 加入持久化与可追溯审计（PostgreSQL + Flyway）。
