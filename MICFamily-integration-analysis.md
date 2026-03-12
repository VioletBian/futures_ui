# MICFamily 集成分析

## 1. 前端与 DRA 的交互逻辑

1. 前端页面 LimitUsage.tsx 初始化表单状态，并通过 limitUsageConfigService.fetchLimitUsageEnumOptions 拉取枚举配置。
2. 用户只能在 MIC 与 MICFamily 之间二选一。
3. 互斥逻辑在前端事件处理中完成：
- 选择 MIC 时会清空 MICFamily。
- 选择 MICFamily 时会清空 MIC。
- 当前字段被清空后，另一字段重新可选。
4. 点击提交后，页面调用 LimitUsageUtil.buildPayload 组装请求，再发送到 createLimitUsageRule。
5. DRA 中台入口接收请求，并校验 venue selector 必须且只能有一种。

## 2. Alert Engine 启动后从 ES 生成规则的逻辑流

按当前重建结果，可归纳为以下链路：

1. LimitUsageRuleController.createLimitUsageRule
2. LimitUsageRuleService.createRule
3. AlertRuleFilterMapper.toLimitUsageFilter
4. 将序列化后的规则或过滤器配置持久化到 ES
5. 引擎启动时由 AlertEngineBootstrap.start 拉起装载流程
6. LimitUsageAlertSource.loadActiveFilters 从 ES 读取有效规则并装入内存

运行时由 Source 维护内存中的活动过滤器集合，供匹配阶段使用。

## 3. 已生成 Alert 如何作用于数据流

1. 新的数据事件进入 alert engine 流水线。
2. 引擎向 LimitUsageAlertSource.getActiveFilters 获取当前活动过滤器。
3. 针对每条规则调用 LimitUsageAlertFilter.matches(event) 进行匹配：
- MIC 规则使用事件中的 mic 与规则中的 mic 集合匹配。
- MICFamily 规则使用事件中的 micFamily 与规则中的 micFamily 集合匹配。
4. 过滤器匹配成功后，再继续判断阈值型或时间型触发条件；满足条件时执行告警动作，例如发信。

## 4. 增加 MICFamily 后需要的改造

### 4.1 model 中 AlertRule protobuf 的修改方式

建议把 venue selector 建模为 oneof：

- MicSelector mic_selector
- MicFamilySelector mic_family_selector

这样可以在协议层保证 MIC 与 MICFamily 互斥，避免同时有值造成语义歧义。

当前仓库中的重建草案位于：

- model/alert-rule/src/main/proto/com/xx/futures/evetor/alert/alert_rule.proto

### 4.2 ui-middle-tiers 中 alert 生成逻辑的修改方式

1. 请求 DTO 必须同时支持 mic 与 micFamily 两个字段。
2. Service 层必须做 XOR 校验，即两者必须且只能有一个非空。
3. Mapper 层需要把 mic 或 micFamily 映射到统一的运行时过滤器模型。
4. ES 持久化结构需要新增 micFamily，并保持对旧 mic-only 文档的兼容读取。
5. 运行时过滤逻辑需要支持 event.micFamily，并按 selector 类型分支匹配。

## 5. 命名替换约束

当前工作区内新增或重建的包路径统一遵循：

- 用 com.xx 替换 com.gs
- 用 evetor 替换 aviator
