# MICFamily on LimitUsage 任务说明

本文是当前工作区里与后续实现任务直接相关的任务说明文档。

配套的链路与项目关系说明见：

- [limit-usage-alert-flow.md](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/limit-usage-alert-flow.md)

除上述两份文档外，其他旧分析文档不再作为权威依据。

## 任务目标

实现因 alert 增加 `MICFamily` 作为“Mic合集”语义而带来的 `LimitUsage Alert` 相关改动。

当前已知前提：

- 前端 alert 展示和新增界面已经完成。
- 本次需要改的是 `model` 与 `ui-middle-tiers` 里“规则表示 / 规则装载 / 规则匹配 / alert 生成”相关部分。
- alert 触发后的下发通知链路与本任务无关，不需要改通知侧。

## 本次实现的范围

只做下面两块：

1. `model`
   - `AlertRule` 或其等价规则模型中与 `MIC` / `MICFamily` 选择器相关的表达能力。
2. `ui-middle-tiers`
   - `LimitUsage Alert` 的规则接入
   - 规则装载
   - 规则匹配
   - threshold / time-based 触发前的 selector 判断

## 明确不在范围内

以下内容不属于本次实现目标：

- 前端页面与前端交互逻辑
- alert 触发后的通知、发送、ack、publisher 下游
- config server / endpoint 层的大类重现
- 与 `LimitUsage Alert` 无关的其他 alert 类型

## 已确认的事实

### 1. 规则如何进入运行态

`AlertRule` 不是直接进入 `LimitUsageAlertSource`。

当前已确认有两条规则进入 `LimitUsageAlertSource.processNewAlertRule(...)` 的路径：

- `AlertRuleConsumer`
  - 负责消费规则变更消息
  - 把 `AlertRule` 写入 `AlertRuleCache`
  - 再调用 `LimitUsageAlertSource.processNewAlertRule(rule)` 或 `processRemoveAlertRule(rule)`
- `CustomAlertsPollingThread`
  - 负责启动或补偿式地批量装载规则
  - 会从 ES / generated rules 拉取规则
  - 再批量灌入 `LimitUsageAlertSource.processNewAlertRule(rule)`

### 2. 运行态规则如何参与 alert 生成

`LimitUsageAlertSource.processNewAlertRule(...)` 会把原始 `AlertRule` 转成运行态 limit usage 规则，并分流到：

- `thresholdRules`
- `scheduledRules`

实时 `ClearingData.LimitUsage` 数据通过：

- `LimitUsageAlertEngineConsumer`
- `FastAlertEngine`
- `AlertGenerator`

进入 `LimitUsageAlertSource` 的运行时匹配流程。

也就是说：

- 不是数据一进入系统就直接发 alert
- 而是数据先与运行态规则匹配
- 命中后才生成 `LimitUsageAlert`

### 3. threshold 与 time-based 的差异

- threshold 规则：由实时 `ClearingData.LimitUsage` 事件驱动
- time-based 规则：由 `LimitUsageAlertSource` 到点后通过 `LimitUsageApi` / `LimitUsageLoader` 拉快照再判断

### 4. 通知链路不属于本次改造重点

`LimitUsageAlertConsumer`、`LimitUsageAlertAckingConsumer`、`ConfigServerDao` 当前已足够确认其角色，但它们位于 alert 生成之后，不属于本次 `MICFamily on LimitUsage` 的核心改动范围。

## 本次要实现的业务语义

### 1. MIC 与 MICFamily 是互斥选择器

对 `LimitUsage Alert` 规则来说：

- 规则要么按 `MIC` 匹配
- 要么按 `MICFamily` 匹配
- 两者不能同时生效

### 2. MICFamily 的含义

`MICFamily` 表示一个 “Mic合集” 维度。

这次最新确认的真实业务前提是：

- `micFamily` 规则的目标账号类型是 `GMI`
- 该账号类型代表多个交易所子账号合并后的聚合账号
- 客户只知道这个聚合账号整体的资金占用量，不知道、也不关心每个具体 `mic` 的拆分占用
- 因此这类新 `LimitUsage` 原型实际到达中台时，是 `micFamily=SFX` 且 `mic=UNKNOWN`
- 对这种聚合账号，系统不会提供可用于告警判断的 `mic level` LimitUsage 数据，只会提供 `micFamily level` 数据

因此在规则匹配阶段，不能只保留旧的 “事件 mic 命中规则 mic 集合” 逻辑，还需要支持并明确约束：

- 事件 `micFamily` 命中规则 `micFamily` 集合
- 当规则是 `micFamily` 规则时，命中的应当是聚合级别的 LimitUsage 数据，也就是 `mic` 为空或 `UNKNOWN`

### 3. 兼容旧规则

老的 `MIC-only` 规则仍然必须继续有效。

因此改造应当满足：

- 旧规则读出来后，仍按原有 `MIC` 逻辑工作
- 新规则可以表达 `MICFamily`
- 运行态根据 selector 类型分支匹配
- `MICFamily` 规则不应因为一条同时带真实 `mic` 和 `micFamily` 的混合 payload 而误触发

## 对实现方式的约束

### 1. 先改规则表达，再改匹配逻辑

建议顺序：

1. 补规则模型表达能力
2. 补规则装载路径
3. 补运行态匹配逻辑
4. 最后检查 threshold / time-based 两条路径是否都覆盖到

### 2. 改动收敛在最小必要范围

不要为了完整性去扩展无关大类。

例如：

- `AlertsRestBase` 只需要作为“`ConfigServerDao` 背后存在 `AlertRule` endpoint”的关系证据
- 不需要在本任务里整份重现

### 2.1 time-based 路径不要吞掉空数据告警语义

review 明确指出：

- `scheduleTimeBasedRule(...)` 里不能因为预过滤后的数据为空就直接 `return`
- 旧链路允许“无有效 limit usage 行”的情况继续往下游走
- 下游会基于空 `message` 触发 ERROR 类提示邮件，帮助发现 limit usage 数据缺失问题

因此后续实现中：

- 不能用一个新的上游过滤分支把这类问题静默吃掉
- 要保留 time-based 空数据 / 无有效行时继续生成 alert 的语义

### 2.2 优先扩展 Source 里已有的 JSON 解析与表格生成链路

review 明确指出：

- `LimitUsageAlertSource` 已有 `getTableContent` / `getBody` / `isValidLimitUsage` 这条 time-based JSON 处理链路
- selector 相关的 snapshot 解析应优先在这条已有链路上扩展
- 不应在 `LimitUsageRule` 里额外长出一套 map 级校验 / 过滤函数，把 time-based 逻辑分叉出去

因此后续实现中：

- `Source` 负责 snapshot row 的解析、校验、筛选和表格内容生成
- `Rule` 负责 selector 元信息、规则匹配和基于规则本身的 helper

### 2.3 规则本身的 helper 应尽量收回到 LimitUsageRule

review 给出的重构方向是：

- 接近 data parsing / snapshot row handling 的函数留在 `LimitUsageAlertSource`
- 接近 `AlertRule` / `LimitUsageRule` 本身属性和 alert 组装的函数，尽量迁移到 `LimitUsageRule`

典型例子包括：

- `getTimeToTriggerForRule`
- `getTimeBasedAlertId`
- `getTimeBasedLimitUsageAlert`
- `getTimeBasedLimitUsageAlertActivity`

### 3. 缺事实时不要猜

如果在实现过程中缺少关键事实：

- 不要根据旧猜测继续扩展
- 直接指出需要我去完整源库里查哪条 `find usage`
- 或指出需要我补哪个类、哪个方法、哪段局部代码

## 推荐关注的代码位置

后续实现任务优先关注这些文件：

- [AlertRule.java](/Users/fortunebian/Downloads/futures_ui/model/alert-rule/src/main/java/com/xx/jetstream/model/alert/AlertRule.java)
- [LimitUsageAlertSource.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/sources/LimitUsageAlertSource.java)
- [LimitUsageRule.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/rules/LimitUsageRule.java)
- [LimitUsageAlertEngineConsumer.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/alertengineconsumer/LimitUsageAlertEngineConsumer.java)
- [FastAlertEngine.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/FastAlertEngine.java)
- [AlertGeneratorModule.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/guice/AlertGeneratorModule.java)
- [LimitUsageLoader.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageloaderserver/loader/LimitUsageLoader.java)
- [LimitUsageApi.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageloaderserver/rest/LimitUsageApi.java)

## 当前最重要的判断标准

本任务成功的标准不是“把所有周边类都重现出来”，而是：

1. `LimitUsage Alert` 规则能够表达 `MICFamily`
2. `MIC` 与 `MICFamily` 在规则层保持互斥
3. 规则装载后，实时路径和 time-based 路径都能按 selector 正确匹配
4. 旧 `MIC-only` 规则不被破坏
