# LimitUsage MICFamily 前端重建背景

## 背景

当前工作区是在一个更大项目基础上的阶段性重建。原项目主要包含三个部分：

- evetor-ui：前端页面与页面层逻辑。
- ui-middle-tiers：位于 UI 与后端之间的 API 与编排层。
- model：共享 protobuf 模型库。

当前阶段只重建截图中可见的前端部分，重点是用于创建 margin usage alert 的 LimitUsage 页面。在原项目中，margin 与 LimitUsage 在部分模块里会交替使用。

## 当前范围

- 只重建有截图支撑的前端文件，以及最小必要的支撑桩代码。
- 不创建截图中未出现的非必要业务文件。
- 在现有 MIC 选择框旁新增一个 MICFamily 下拉框。
- 对 MIC 与 MICFamily 应用互斥逻辑：
- 选择其中一个时，清空并禁用另一个。
- 清空当前激活字段后，另一个字段重新可用。

## 命名规则

- 所有 aviator 一律替换为 evetor。
- 所有 com.gs 一律替换为 com.xx。

## 本阶段数据假设

- MIC 与 MICFamily 都来自 ES 配置返回的枚举选项。
- MIC 使用 MIC 这个 key。
- MICFamily 使用 MICFamily 这个 key。
- 在后端与 model 代码尚未提供之前，前端使用本地 mock 枚举数据与本地 submit stub。

## 后续集成原则

当你后续提供后端与 protobuf 的截图后，继续沿用相同重建原则：

- 只保留截图里明确出现的结构。
- 不臆造未展示的业务模块。
- 持续应用同样的命名替换规则。
- 让当前的 MICFamily payload 与校验行为逐步演进到真实 API 与 proto 定义。