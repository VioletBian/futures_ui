# LimitUsage Alert 链路梳理

本文只基于当前已落地代码、`directory_structure`、以及你后续补拍的 `round1/round2/round3` 代码图整理。

## 当前结论

就“梳理 LimitUsage 相关 Alert 的整条数据流和逻辑链”这个目标来说，当前代码条件已经够用了，可以形成一条相对稳定的主链路。

但如果目标升级为“所有相关类 1:1 精确转写”，现在还不算完全收敛，主要缺口集中在：

- `AlertRule` 尾部未拍全的 getter/setter、builder 尾段
- `LimitUsageAlertConsumer`
- `LimitUsageAlertAckingConsumer`
- `ConfigServerDao`
- `AlertPublisher` 或下游发布目标类
- 前端/配置侧把 `AlertRule` 写入并刷新到 `aviator-dra` 的那段入口代码

也就是说：

- 业务链路已经足够完整，可以写 flow 文档。
- 源码级 1:1 复现还存在局部缺口，尤其是 model 尾部和通知侧若干类。

## 资料可信级别

本次判断采用的可信级别顺序如下：

1. `directory_structure`
2. 近距离单类代码图
3. 同目录下其他已确认类之间的直接调用关系
4. 当前工作区里已重建代码

像旧的分析 md、早期错误转录、或者仅凭命名推断的内容，本次都没有作为事实依据。

## 模块职责

从当前目录结构和代码看，LimitUsage Alert 至少跨了这几个模块：

- `recap-server/limitusageloaderserver`
  负责订阅 `ClearingData.LimitUsage`、做本地缓存、对外提供查询接口。
- `aviator-dra`
  负责接收 limit usage 数据、持有运行中规则、在主 alert engine 内生成 LimitUsageAlert。
- `recap-server/limitusageserver`
  负责消费生成后的 `Coverage.Alert`，回查 `AlertRule`，然后发邮件、Symphony 等通知。
- `recap-server/recapserver`
  当前能确认它提供了 `ConfigServerDao`，负责通过 HTTP 回查 `AlertRule`。
- `model/alert-rule`
  当前已补出 `AlertRule` 的可见字段段和调用必需的 bean getter；尾部仍不是完整 1:1。

## 主链路

### 1. Limit usage 数据进入系统

来自 [LimitUsageLoader.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageloaderserver/loader/LimitUsageLoader.java)：

- `LimitUsageLoader` 实现了 `DataConsumer<ClearingData.LimitUsage>`。
- `consume(...)` 直接调用 `updateCache(...)`。
- `configureAndStartKafkaSubscription()` 负责做 hydration，然后启动 Kafka subscriber。
- `updateCache(...)` 会把 limit usage 按两种 key 缓存：
  - `clientRefId`
  - 第一条 GMI account synonym

这意味着 loader 侧维护的是“当前最新 limit usage 快照缓存”，而不是简单透传。

### 2. loader server 对外提供 limit usage 查询

来自 [LimitUsageApi.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageloaderserver/rest/LimitUsageApi.java)：

- 暴露了三类查询：
  - 全量 `/all`
  - 单账户 `/account`
  - 多账户 `/accounts`
- 所有查询最终都回到 `LimitUsageLoader` 的缓存。

来自 [LimitUsageLoaderServer.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageloaderserver/server/LimitUsageLoaderServer.java)：

- 服务启动后会初始化 `LimitUsageLoader`
- 然后执行 `configureAndStartKafkaSubscription()`

所以这一段的职责很清晰：

- 一边持续吃 limit usage 数据
- 一边通过 REST 暴露最新快照

### 3. aviator-dra 把 LimitUsage 接进主 alert engine

来自 [AlertGeneratorModule.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/guice/AlertGeneratorModule.java)：

- `LimitUsageAlertSource` 会在 `processLimitUsageAlert` 打开时被注册为 `AlertGeneratorSource`
- `LimitUsageAlertEngineConsumer` 也会在同一开关下被绑定
- `AlertEngine` 绑定到 `FastAlertEngine`

这个 wiring 很关键，说明：

- LimitUsage 不是一个独立旁路服务
- 它是直接挂进 `aviator-dra` 主 alert engine 的

### 4. event-driven 入口已经确认

来自 [AlertEngineConsumer.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/alertengineconsumer/AlertEngineConsumer.java) 和 [LimitUsageAlertEngineConsumer.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/alertengineconsumer/LimitUsageAlertEngineConsumer.java)：

- `AlertEngineConsumer` 是 `DataConsumer<Object>`
- `LimitUsageAlertEngineConsumer.consume(Object)` 会判断输入是不是 `ClearingData.LimitUsage`
- 如果是，就调用 `alertEngine.process(limitUsage)`
- `setLiveMode()` 直接转发到 `alertEngine.setLiveMode()`

这段现在已经不是推断，而是直接确认了：

- 实时 limit usage 数据会通过 `LimitUsageAlertEngineConsumer`
- 直接进入 `FastAlertEngine.process(ClearingData.LimitUsage)`

### 5. 规则在 LimitUsageAlertSource 中被持有和分流

来自 [LimitUsageAlertSource.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/sources/LimitUsageAlertSource.java)：

- 运行态规则分成两类：
  - `thresholdRules`
  - `scheduledRules`
- `processNewAlertRule(AlertRule)` 会：
  - 先做旧版本移除
  - 再判断是否为 limit usage rule
  - 再做权限/entitlement 检查
  - 最后根据规则类型进入 threshold 或 time-based 路径

这说明前端或配置侧落下来的 `AlertRule`，到了 `aviator-dra` 后会被转成运行时的 `LimitUsageRule`。

### 6. threshold 路径

来自 [LimitUsageRule.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/rules/LimitUsageRule.java)：

- 每条运行时规则持有：
  - `AlertRule`
  - `Common.Application`
  - `ActiveAlerts`
- 核心判断至少包括：
  - 当前 alert 是否已在 `activeAlerts` 中存在
  - venue 是否命中
  - account 是否命中
  - threshold operator/value 是否 breach

当前已确认的 account 命中逻辑是：

- 既看 `clientRefId`
- 也看 GMI synonym

当前已确认的 alert id 构造逻辑里包含：

- `clientRefId`
- `GMI`
- `ruleVersion`
- `ruleId`

所以同一账户在规则版本升级后，可以重新形成新 alert。

### 7. time-based 路径

来自 [LimitUsageAlertSource.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/sources/LimitUsageAlertSource.java)：

- `scheduleTimeBasedRule(...)` 会在指定时刻触发
- 触发时通过 `/limitusage/accounts` 拉当前快照
- 然后生成 time-based 的 limit usage alert

所以 time-based 规则不是等实时事件自然撞上，而是：

- 到点后主动拉 snapshot
- 再基于 snapshot 做一次规则判断

### 8. 统一通过 FastAlertEngine 出 alert

来自 [FastAlertEngine.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/FastAlertEngine.java)：

- `process(ClearingData.LimitUsage)` 会走 `generateNewAlerts(limitUsage)`
- `processUnpublishedAlerts(...)` 会把结果分成：
  - `ImmediateAlert`
  - `TimePendingAlert`
- `raiseActiveAlert(...)` 对 `LimitUsageAlert` 会走 `activeAlerts.addLimitUsageAlert(alert)`

再结合 [AlertEngineTicker.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/aviator-dra/src/main/java/com/xx/futures/evetor/alert/generator/AlertEngineTicker.java)：

- engine ticker 每秒调用一次 `checkTimePendingAlerts()`

因此可以确认：

- LimitUsageAlert 最终还是走统一 engine
- 不是在外面自己维护一套独立调度器

### 9. 生成后的 alert 在通知侧再次过滤、处理、校验

当前直接拍到并可确认的通知侧基础类有：

- [LimitUsageAlertFilter.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageserver/alerts/LimitUsageAlertFilter.java)
- [LimitUsageAlertProcessor.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageserver/alerts/LimitUsageAlertProcessor.java)
- [LimitUsageAlertRuleValidation.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageserver/alerts/LimitUsageAlertRuleValidation.java)

可确认逻辑：

- `LimitUsageAlertFilter.shouldBeProcessed(...)`
  - 只放行 `AlertType.LimitUsage`
  - 且按 `alertIdSet` 去重
- `LimitUsageAlertProcessor.process(...)`
  - 直接把 `GeneratedMessage` cast 成 `Coverage.Alert`
- `LimitUsageAlertRuleValidation`
  - 校验 generic email 是否该发
  - 校验 symphony 是否该发
  - 校验 symphony room 是否该发
  - 校验 alertId 与 ruleId、message 与 rule.message 是否一致

这里要注意一点：

- `LimitUsageAlertRuleValidation` 这次补拍确认了类名就是带 `Rule` 的版本
- 之前 workspace 里的 `LimitUsageAlertValidation` 是旧的错误命名，已修正

### 10. 通知侧消费 alert 并回查 AlertRule

来自 [LimitUsageAlertConsumer.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/limitusageserver/alerts/LimitUsageAlertConsumer.java) 和 [ConfigServerDao.java](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/ui-middle-tier-services/recap-server/src/main/java/com/xx/futures/evetor/recapserver/dao/ConfigServerDao.java)：

- `LimitUsageAlertConsumer` 收到的是 `Coverage.Alert`
- 它会先从 alert id 反推出 `alertRuleId`
- 然后通过 `ElasticUtils.getElasticDocument(..., alertRuleCache, configServerDao)` 回查 `AlertRule`
- 再根据 `AlertRule` 的通知配置走：
  - external client email
  - generic email
  - symphony room
  - symphony team room

来自 `ConfigServerDao`：

- 通过 HTTP 从 config/elasticsearch 风格接口回查 `AlertRule`

所以通知侧不是只靠 alert payload 自己发，而是会重新拉一遍规则配置。

## 一条相对完整的链路

当前可以把主链路收敛成下面这 10 步：

1. `limitusageloaderserver` 消费 `ClearingData.LimitUsage`
2. `LimitUsageLoader` 按 `clientRefId` / GMI synonym 更新内存缓存
3. `LimitUsageApi` 暴露缓存查询接口
4. `AlertGeneratorModule` 把 `LimitUsageAlertSource` 和 `LimitUsageAlertEngineConsumer` 接入主 engine
5. 实时 limit usage 通过 `LimitUsageAlertEngineConsumer` 进入 `FastAlertEngine`
6. `LimitUsageAlertSource` 持有运行中规则，并把规则分成 threshold/time-based 两条路径
7. `LimitUsageRule` 对实时数据或定时拉取的 snapshot 做命中判断
8. `FastAlertEngine` 生成 `ImmediateAlert` / `TimePendingAlert`，并把 LimitUsageAlert 纳入 `activeAlerts`
9. `limitusageserver` 侧对 `Coverage.Alert` 做 filter/process/validation
10. `LimitUsageAlertConsumer` 回查 `AlertRule` 后发通知

## 当前可以确认的关键逻辑点

- LimitUsage 是主 alert engine 的一部分，不是外挂旁路。
- 实时 limit usage 的 engine 入口已经拍实，就是 `LimitUsageAlertEngineConsumer -> AlertEngine.process(limitUsage)`。
- time-based rule 不是等实时消息，而是到点主动向 loader server 拉 snapshot。
- 运行时规则在 `LimitUsageAlertSource` 内部区分成 threshold 和 scheduled 两套集合。
- alert 去重不止发生在通知侧，engine 里还有 `activeAlerts` 的 active alert 去重。
- 通知侧会重新回查 `AlertRule`，说明 alert payload 本身不是唯一事实源。

## 还不够 1:1 的地方

如果你的目标是“逻辑链已经够清楚”，当前状态够了。

如果你的目标是“源码也要尽量 1:1”，现在还有几个明显缺口：

### A. AlertRule 仍然不是完整尾部

当前 [AlertRule.java](/Users/fortunebian/Downloads/futures_ui/model/alert-rule/src/main/java/com/xx/jetstream/model/alert/AlertRule.java) 已经具备：

- 可见字段段
- `isAlgoRule`
- `isValid`
- `isLimitUsageRule`
- `isTimeBasedLimitUsageRule`
- 当前调用面需要的若干 getter

但还不是完整 1:1：

- builder 尾段没拍全
- getter/setter 尾段没拍全
- 某些通知字段的声明位置不在当前可见区域，只能按调用做结构性补全

### B. 通知侧几个类还没被这轮近拍直接验证

仍建议补拍：

1. `LimitUsageAlertConsumer`
2. `LimitUsageAlertAckingConsumer`
3. `ConfigServerDao`

理由：

- 这些类会决定最终通知分叉、rule 回查方式、以及 ack 行为
- 目前虽然已有转写，但可信度不如这轮精拍过的类

### C. 规则写入/刷新入口还没拍到

如果你想把“前端创建 LimitUsage 规则”到“aviator-dra 开始执行该规则”这一段彻底打通，还需要：

1. `LimitUsageRuleController`
2. `LimitUsageRuleService`
3. `AlertRuleFilterMapper`
4. 任何把 `AlertRule` 刷进 `LimitUsageAlertSource.processNewAlertRule(...)` 的 registry/provider/source 类
5. config server 里负责持久化或查询 `AlertRule` 的 endpoint/service 类

这是当前链路里最大的事实缺口。

## 当前最稳妥的判断

我现在的判断是：

- “LimitUsage Alert 是怎么从 limit usage 数据走到通知”的主链路，已经足够完整。
- “规则是怎么从前端写入并刷新到运行态”的前半段，还没有 solid source。
- “AlertRule model 的完整尾部”也还没完全拍到，所以 model 目前是“足够支撑链路分析和当前调用”，但不是完整源文件级复刻。

如果你下一轮继续补图，我建议优先级是：

1. `LimitUsageRuleController`
2. `LimitUsageRuleService`
3. `AlertRuleFilterMapper`
4. `LimitUsageAlertConsumer`
5. `ConfigServerDao`
6. `AlertRule` 尾部 getter/setter/builder 区域

这样就能把“规则进入系统”到“规则驱动通知”的整段闭环补全得更扎实。
