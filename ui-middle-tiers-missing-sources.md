# ui-middle-tiers 补拍清单

这份清单的目标不是让你把整个中台都拍完，而是只补齐 LimitUsage Alert 生成与运行链路中还缺失的关键证据。下面每一项都按“为什么需要”和“你要拍到什么”为准。

## 一、创建规则入口链路

### 1. LimitUsage 的 REST Controller 或 Resource 类

为什么需要：

- 现在只能确定前端提交后会进入中台，但还无法确认真实入口类名、请求路径、参数对象以及调用到哪个 service。

你要拍到什么：

- 类声明与注解。
- create limit usage rule 的方法签名。
- 请求路径、HTTP 方法。
- 入参 DTO 类型。
- 返回类型。
- 方法体里调用的下游 service 名称。

### 2. 接收前端请求的 DTO 类

为什么需要：

- 这是确认 MIC、MICFamily、AccountIds、Threshold、Time、Timezone 等字段真实命名的唯一可靠来源。

你要拍到什么：

- 全部字段定义。
- Jackson 或校验注解。
- getter/setter 或 builder。
- 是否已有 mic 字段、是否已经预留 family 相关字段。

### 3. Create Rule 的 Service 或 Orchestrator 类

为什么需要：

- 这是从前端请求转换成 ES 配置、AlertRule proto、运行时 rule/filter 的核心位置。

你要拍到什么：

- create 或 submit 方法完整实现。
- 该方法里调用的 mapper、repository、publisher、validator。
- 对 threshold rule 与 time rule 的分支逻辑。

## 二、ES 持久化链路

### 4. 持久化到 ES 的 document/entity 类

为什么需要：

- 只有看到 ES 文档结构，才能确定 micFamily 应该新增在哪个字段，是否需要兼容历史 mic-only 文档。

你要拍到什么：

- 类的全部字段。
- ES index/type 注解或 mapping 注解。
- 与 mic 相关字段的命名。
- serialize/deserialize 相关代码。

### 5. ES Repository 或 DAO 类

为什么需要：

- 需要确认创建规则时如何写入 ES，以及 alert engine 启动时如何读回规则。

你要拍到什么：

- save、findActiveRules、findByType、loadRules 之类的方法。
- 查询条件。
- 反序列化目标类型。

### 6. ES 与 proto 之间的 mapper/converter 类

为什么需要：

- 这里通常决定 proto 字段与 ES 文档字段的一一对应关系，也是新增 micFamily 时最容易漏改的点。

你要拍到什么：

- doc -> proto。
- proto -> doc。
- request -> doc 或 request -> proto。

## 三、Alert Engine 启动与装载链路

### 7. LimitUsageAlertSource 完整代码

为什么需要：

- 你给的截图里已经出现了这个类，但还看不到完整构造逻辑、调度逻辑、从哪读 ES、如何注册 rule。

你要拍到什么：

- 类头部字段区。
- 构造函数。
- startup 或 init 方法。
- 从 ES 拉取规则的方法。
- 生成或刷新 schedule rules 的方法。

### 8. AlertEngine 的启动配置类

为什么需要：

- 需要知道 LimitUsageAlertSource 是如何被注入、注册到 engine、以及何时启动加载的。

你要拍到什么：

- engine bootstrap/configuration 类。
- bean 装配。
- source 注册代码。
- refresh/scheduler 触发点。

### 9. rule source provider 或 source registry 类

为什么需要：

- 很多系统不会直接让 source 自己启动，而是通过 provider/registry 管理不同 alert source。

你要拍到什么：

- registry/provider 的类声明。
- 注册 LimitUsageAlertSource 的代码。
- engine 获取 source 的入口。

## 四、运行时过滤与触发链路

### 10. LimitUsage 对应的运行时 AlertRule 或 Filter 类

为什么需要：

- 目前只知道存在基于 mic 的过滤逻辑，但不知道真实项目里是单独 filter、predicate、condition 还是 rule object。

你要拍到什么：

- matches、test、apply、shouldTrigger 这类方法。
- 事件中 mic 的读取方式。
- 若已有 venue/account 相关过滤，也要一并拍到。

### 11. 阈值触发与时间触发的 evaluator 类

为什么需要：

- MICFamily 只是 venue selector 的新增，真正生效还取决于 threshold rule 与 time rule 的触发器如何接在过滤器后面。

你要拍到什么：

- threshold evaluator。
- time evaluator。
- rule type 分发逻辑。

### 12. 进入引擎的数据事件模型类

为什么需要：

- 只有看清数据流事件里是否本来就带 micFamily，才能判断中台是直接匹配还是需要从 mic 映射出 family。

你要拍到什么：

- 事件 model 或 message 类。
- mic 字段定义。
- 若有 exchange、venue、instrument、market 字段也一起拍。

### 13. 从事件中提取 mic 的 normalizer 或 util 类

为什么需要：

- 若原始事件没有 micFamily，系统很可能通过 instrument/exchange 映射得到 family，这一步是 MicFamily 改造的关键。

你要拍到什么：

- 从事件提取 mic 的方法。
- venue/exchange 到 mic 的映射。
- 若已有 config lookup，也要拍到对应代码。

## 五、枚举与配置链路

### 14. 提供 MIC 枚举给前端的 config service

为什么需要：

- 你已说明 MIC 与 MICFamily 都来自 ES config。要补齐完整逻辑，必须知道当前 MIC 枚举是由哪个 service 提供、字段 key 是什么、返回结构是什么。

你要拍到什么：

- 获取 enum options 的 service。
- ES config key 常量。
- 返回前端的 response 结构。

### 15. MIC 与 MICFamily 的配置常量类

为什么需要：

- 需要确认 ES 中到底使用 MIC、MICFamily、VENUE_FAMILY 还是别的命名，避免后续 proto 和 ES 字段名不一致。

你要拍到什么：

- 常量定义类。
- 与 MIC、exchange、venue family 相关的 key。

## 六、model / protobuf 还缺的证据

### 16. AlertRule 相关 proto 的现有定义

为什么需要：

- 我现在给的是合理草案，但不是原项目真实 proto。要把方案从“设计建议”推进到“接近原项目结构”，必须看到现有 proto 的 message 层级与 oneof 组织方式。

你要拍到什么：

- AlertRule 主 message。
- LimitUsage 或 MarginUsage 对应 message。
- 现有 mic 字段所在位置。
- trigger rule 的 message。

### 17. proto 生成后的 Java library 适配类

为什么需要：

- 有些项目不会直接在业务层用 proto message，而会包一层 converter 或 builder；新增字段时这层非常容易遗漏。

你要拍到什么：

- builder/converter/adapter 类。
- setMic 或同类方法。
- 构造 AlertRule 的完整方法。

## 七、建议你优先补拍的最小集合

如果你这轮只想拍最少数量、但足够让我把中台逻辑补完整，优先拍下面 8 个：

1. LimitUsage 创建接口的 controller 或 resource
需要看到真实入口方法、请求路径、入参 DTO、返回类型，以及它调用了哪个 service。
2. 创建请求 DTO
需要确认 mic、micFamily、accountIds、threshold、time、timezone、internalEmail 这些字段的真实命名和注解。
3. create rule 的主 service 方法
这是前端请求如何变成 ES 文档、proto 或运行时 rule/filter 的核心。

4. 写入 ES 的 document 或 entity 类
这是决定 micFamily 应该落在哪个字段、如何兼容旧 mic-only 文档的关键。
5. ES repository 或 DAO
至少需要 save 和 loadActiveRules 这两个方向的方法，才能把“创建”和“引擎启动装载”串起来。
6. LimitUsageAlertSource.java 对应的原项目完整代码
你现在的截图已经证明这个类存在，但还缺字段区、构造函数、启动装载、调度刷新、从 ES 拉规则这些完整实现。
7. 运行时匹配的 filter 或 rule 类
需要看到真实的 matches、test、apply、shouldTrigger 之类的方法，确认 mic 是怎么从事件里取出来参与匹配的。
8. AlertRule 或 LimitUsage 相关 proto
这是把 MicFamily 从“合理设计”推进到“接近原项目真实协议”的关键证据。


## 八、拍摄要求

为了让我能继续精确重建，请你拍的时候尽量满足这几个条件：

- 每个类至少覆盖类头、字段区、核心方法头、核心方法体。
- 同一个方法尽量从签名一直拍到 return，不要只拍中间片段。
- 如果某个方法调用了另一个明显关键的类，请把被调类名一并拍进来。
- 目录树截图要能看清包路径和类名。
- 优先拍源码，不要只拍反编译后的 outline。