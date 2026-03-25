# 架构说明

## 微服务边界（当前实现）

- `planner-service`：对外提供优化接口；内部包含 GA 引擎与甘特图转换。
- `common`：跨服务可复用的数据模型。

## 目标规模

- 输入校验限制：3~20 星。
- 每星三个任务类别：观测 / TTC / 数传。

## API 响应关键字段

- `satellitePlans`：按星分组的三类计划。
- `ganttTasks`：标准化甘特任务条目。
- `mermaidGantt`：可视化文本，可直接渲染。
- `ganttPngBase64`：后端直接生成的甘特图 PNG（Base64）。
