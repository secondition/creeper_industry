# Creeper Industry

`Creeper Industry` 是一个基于 **NeoForge 1.21.1** 的 Minecraft 模组，核心主题是“苦力怕、爆炸、信号工程与危险工业化”。

项目当前已经有可运行原型，重点不在传统的“发电 -> 输电 -> 用电”，而在一套以“信号”为核心媒介的工业系统：

- 爆炸事件可以直接产生脉冲信号
- 机器可以产生连续信号
- 信号会在空气、方块和导管中传播、衰减、聚合
- 机器读取的是目标位置上的最终聚合结果

## 文档

- 设计草案：[docs/design_draft.md](docs/design_draft.md)

## 当前重点系统

### 信号系统

当前仓库里已经实现了信号系统的基础骨架：

- 爆炸脉冲信号
- 连续信号源与更新链
- 接收端刷新与局部重算
- 检测器调试显示
- 导管网络接入传播计算

信号的关键概念：

- 幅值：信号强度
- 周期：离散 tick 周期
- 波形：当前为方波
- 相位：决定当前 tick 采样结果

### 导管系统

导管不是六面漏风的通用信号方块，而是“内部无损传输网络”：

- 信号在普通介质中按曼哈顿距离衰减
- 信号在导管网络内部无损传播
- 导管只能通过“接口”与外界交换信号
- 接口不是独立方块，而是安装在导管六个面的同格挂件

当前规则下：

- 没有接口的导管，不会和空气或普通方块直接连通信号
- 只有装了接口的那一面，才能作为导管的输入/输出面
- 接口增删会触发导管网络缓存刷新与相关接收端重算

## 当前连续信号规则

连续信号当前采用离散方波采样，只支持偶数周期：

- `2 tick`
- `4 tick`
- `6 tick`
- `8 tick`

采样规则统一为：

- 前半周期为 `+A`
- 后半周期为 `-A`

例子：

- `2 tick`: `+A, -A`
- `4 tick`: `+A, +A, -A, -A`
- `6 tick`: `+A, +A, +A, -A, -A, -A`

为了避免全偶数周期下所有信号天然锁相，连续信号源现在正式带有相位：

- 相位是 `0 ~ period-1` 的离散 tick 偏移
- 相位由机器位置和源 id 稳定生成
- 同一台机器在同一位置时，相位稳定
- 不同机器通常不会默认同相

这套规则的目的：

- 保持方波定义纯净，不再为奇数周期打补丁
- 让连续信号叠加真正受相位影响
- 避免等效值长期退化成单一锁相结果

## 检测器读数

`Signal Update Detector` 当前会显示三类值：

- `equivalent_value`：等效值，表示聚合结果的等效强度
- `instantaneous`：当前 tick 的瞬时值
- `last_non_zero`：最近一次非零等效值

其中：

- 纯连续信号时，等效值来自公共周期内叠加波形绝对值的平均
- 若当前 tick 混入脉冲信号，则该 tick 按瞬时叠加结果结算

## 开发环境

- Minecraft `1.21.1`
- NeoForge `21.1.219`
- Java `21`
- Gradle Wrapper

## 常用命令

```powershell
.\gradlew runClient
```

```powershell
.\gradlew runServer
```

```powershell
.\gradlew runData
```

```powershell
.\gradlew build
```

## 代码目录

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

重点目录：

- `content/energy/signal`：信号源、传播、聚合、刷新与接收
- `content/energy/transmission`：导管、接口与导管网络
- `docs/design_draft.md`：当前规则、约束与设计取舍
