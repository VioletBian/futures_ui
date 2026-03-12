# ui-middle-tier-services 重建说明

当前已按截图重建的 LimitUsage Alert 关键链路类包括：
- API 入口：LimitUsageRuleController
- 规则创建：LimitUsageRuleService
- 过滤器映射：AlertRuleFilterMapper
- 运行时过滤：LimitUsageAlertFilter
- 规则装载源：LimitUsageAlertSource
- 引擎启动入口：AlertEngineBootstrap

这些文件目前只保留最小可分析骨架，重点聚焦 MIC 与 MICFamily 的逻辑表达，不扩展未拍到的业务细节。
