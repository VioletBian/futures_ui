你要求的思考任务结论

evetor dra 与前端交互逻辑
前端 LimitUsage 页面加载枚举配置。
用户在 MIC 与 MICFamily 二选一，互斥逻辑在前端立即执行。
提交时构造 payload，包含 mic 或 micFamily（不可同时有值）。
中台接口接收并进行服务端 XOR 校验，生成规则过滤器并持久化到 ES。
alert engine 启动后从 ES 生成 alert 的逻辑流
建议/重建链路为：Controller -> Service -> Mapper -> ES 持久化。
引擎启动时通过 Source 从 ES 拉取有效规则并装载到内存。
Source 提供当前活动过滤器集合给运行时匹配模块。
生成后的 alert 如何作用于数据流
新数据流事件进入引擎后，逐条与活动过滤器匹配。
规则为 MIC 模式时，用事件 mic 匹配规则 mic 集合。
规则为 MICFamily 模式时，用事件 micFamily 匹配规则 micFamily 集合。
匹配成功后再判断阈值/时间触发条件，最终触发告警动作。
增加 MicFamily 字段后的改造方案
4.1 model 的 AlertRule protobuf 如何改
在 rule 中引入 venue_selector 的 oneof：mic_selector 与 mic_family_selector。
这样协议层就保证互斥，避免 mic 与 micFamily 同时生效的歧义。
你现在仓库里已给出该草案实现，见 alert_rule.proto。
4.2 ui-middle-tiers 的 alert 生成代码如何改

请求 DTO 增加 micFamily。
Service 层强制 XOR 校验。
Mapper 将 mic 或 micFamily 映射到统一 Filter 对象。
ES 存储结构支持 micFamily，且保持对旧 mic-only 规则的兼容读取。
运行时 Filter 匹配逻辑新增 event.micFamily 分支。