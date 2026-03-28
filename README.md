# Creeper Industry

`Creeper Industry` 是一个基于 **NeoForge 1.21.1** 的 Minecraft 模组，围绕“苦力怕、爆炸、信号工程与危险工业化”展开设计。

## 文档导航

- 仓库首页概览：当前文件
- 设计草案与规则沉淀：[docs/design_draft.md](docs/design_draft.md)

## 项目定位

这个模组不打算把工业化简单做成“发电 -> 输电 -> 用电”。

当前设计方向更接近：

- 以爆炸事件作为早期脉冲信号来源
- 以机器产生的连续信号作为中后期扩展
- 让方块、机器和传输介质围绕“信号传播与聚合”协同工作
- 把物流、生产和自动化都建立在这套信号系统之上

## 当前状态

目前仓库已经进入可运行原型阶段，内容大致分成两类。

### 已经有实际代码支撑的部分

- 爆炸脉冲信号的基础传播链路
- 连续信号源的数据结构与更新流程
- 信号接收端索引与统一刷新逻辑
- 多个信号在目标点的聚合/读取框架
- `Continuous Signal Emitter`：可产生连续信号的调试方块
- `Signal Update Detector`：可显示当前信号读数的调试/验证方块
- 防爆导管、防爆框架、防爆玻璃等基础内容注册

### 已注册但仍偏占位的部分

- `Precision Dropper`
- `Rocket Launcher`
- `Guided Firework Rocket`
- `3D Printer`
- `Botanical / Zoological / Monster Biosphere`

这些内容已经有注册、方块实体或菜单结构，但大多还不是完整玩法实现。

## 已有内容概览

当前创意标签页中已经包含以下主要内容：

- `Catnip`
- `Guided Firework Rocket`
- `Blastproof Duct`
- `Blastproof Frame`
- `Blastproof Glass`
- `Continuous Signal Emitter`
- `Signal Update Detector`
- `Precision Dropper`
- `Botanical Biosphere`
- `Zoological Biosphere`
- `Monster Biosphere`
- `3D Printer`
- `Rocket Launcher`

## 信号系统概念

这是当前仓库最重要、也最值得关注的部分。

### 核心思路

- 系统以“信号”作为机器交互媒介，而不是 FE 一类通用能量
- 早期信号可来自爆炸事件
- 后续机器可持续产生离散化的连续信号
- 接收端读取的是目标位置处聚合后的最终结果

### 当前实现特征

- 信号具备振幅、周期等定义
- 连续信号源会被注册到仓库中，并在变化时触发更新
- 接收端不会盲目每 tick 全量轮询，而是走变化驱动刷新
- 检测器方块可以观察当前聚合信号值，便于调试和验证

如果你要继续开发这个仓库，建议先阅读 `content/energy/signal` 目录，再结合 [docs/design_draft.md](docs/design_draft.md) 查看原始设计约束。

## 技术栈

- Minecraft `1.21.1`
- NeoForge `21.1.219`
- Java `21`
- Gradle Wrapper
- Parchment Mappings `2024.11.17`

## 本地开发

### 环境要求

- JDK 21
- 能正常运行 Gradle Wrapper

### 常用命令

启动客户端：

```powershell
.\gradlew runClient
```

启动服务端：

```powershell
.\gradlew runServer
```

运行数据生成：

```powershell
.\gradlew runData
```

构建模组：

```powershell
.\gradlew build
```

## 项目结构

```text
src/main/java/com/secondition/creeperindustry
├─ content
│  ├─ energy
│  │  ├─ signal
│  │  └─ transmission
│  ├─ logistics
│  └─ production
├─ foundation
└─ CreeperIndustry.java
```

各目录职责大致如下：

- `content/energy/signal`：信号源、传播、聚合、接收与刷新机制
- `content/energy/transmission`：防爆导管及相关传输方块
- `content/logistics`：火箭、发射器、定点投掷等物流内容
- `content/production`：生态球、3D 打印机等生产内容
- `foundation`：基础方块/菜单封装

## 开发建议

如果你准备继续迭代这个项目，优先级建议如下：

1. 完成连续信号系统与导管网络的整合
2. 把调试方块扩展成真正参与玩法的机器
3. 为物流与生产机器补齐实际业务逻辑，而不只是方块实体占位
4. 增加配方、数据生成、本地化与测试验证内容

## 许可证

本项目当前在 `gradle.properties` 中声明为 `MIT`。
