# ui-middle-tier-services 重建说明

当前目录下的还原内容以 `images/ui-middle-tiers/directory_structure` 和各模块代码图为准。

目前已按代码图明确落下的 LimitUsage Alert 相关类主要包括：

- `aviator-dra`
  - `AlertEngine`
  - `AlertGenerator`
  - `FastAlertEngine`
  - `AlertGeneratorModule`
  - `LimitUsageRule`
  - `AlertGeneratorSource`
  - `LimitUsageAlertSource`
- `recap-server`
  - `ConfigServerDao`
  - `LimitUsageAlertAckingConsumer`
  - `LimitUsageAlertConsumer`
  - `LimitUsageAlertFilter`
  - `LimitUsageAlertProcessor`
  - `LimitUsageAlertRuleValidation`
  - `LimitUsageServer`
  - `LimitUsageServerDebug`
  - `Main`
  - `ApplicationModule`
  - `JerseyModule`
  - `LimitUsageLoader`
  - `LimitUsageApi`
  - `LimitUsageLoaderServer`
  - `Main`

关于当前可确认的数据流和未解决歧义，见 [limit-usage-alert-flow.md](/Users/fortunebian/Downloads/futures_ui/ui-middle-tiers/limit-usage-alert-flow.md)。
