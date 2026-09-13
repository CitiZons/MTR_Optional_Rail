# MTR Optional Rail Addon

![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-62a35a?style=flat-square) ![Forge 47.4.18](https://img.shields.io/badge/Forge-47.4.18-f59e0b?style=flat-square) ![License MIT](https://img.shields.io/badge/license-MIT-8b5cf6?style=flat-square)

为 MTR 轨道节点提供可编辑的 XYZ 偏移、节点旋转和轨道超高效果，让弯道和倾斜路段的轨道外观保持连续。

MTR Optional Rail Addon adds editable XYZ offsets, node rotation and rail cant to MTR rail nodes, keeping curved and banked track sections visually continuous.

## 功能 / Features

| 中文 | English |
|---|---|
| **节点偏移**——调整节点的 X、Y、Z 视觉位置。 | **Node offsets** — adjust the visual X, Y and Z position of each node. |
| **节点旋转**——为节点添加平滑的旋转过渡。 | **Node rotation** — add a smooth rotation transition between nodes. |
| **轨道超高**——在曲线路段插值外轨倾斜角度。 | **Rail cant** — interpolate banking angles along curved rail sections. |
| **服务端同步**——编辑结果由服务器验证并同步到客户端。 | **Server synchronization** — edits are validated by the server and synchronized to clients. |
| **独立撤销**——偏移、旋转和超高分别保存历史并可撤销。 | **Independent undo** — offsets, rotation and cant keep separate undo history. |

本模组只修改 MTR 节点和轨道的视觉表现，不添加新的轨道类型，也不改变列车限速或寻路规则。

The addon changes the visual presentation of MTR nodes and rails. It adds no rail types and does not alter train speed limits or pathfinding rules.

## 安装 / Install

需要 Minecraft 1.20.1、Forge 47.4.18 和对应版本的 MTR Forge。将 `mtr_optional_rail_addon-0.1.0.jar` 放入客户端和服务器的 `mods/` 文件夹。

Requires Minecraft 1.20.1, Forge 47.4.18 and a matching MTR Forge build. Put `mtr_optional_rail_addon-0.1.0.jar` in the `mods/` folder on both the client and server.

## 使用 / Usage

手持刷子右键 MTR 轨道节点，打开编辑面板。修改字段后点击 Apply；每一行都有独立的 Undo 按钮。偏移范围为 -1 至 1，旋转范围为 -90 至 90 度，超高范围为 -45 至 45 度。

Hold the MTR brush and right-click an MTR rail node to open the editor. Click Apply after editing a field; each row has its own Undo button. Offsets range from -1 to 1, rotation from -90 to 90 degrees, and cant from -45 to 45 degrees.

## 构建 / Build

需要 JDK 17。运行以下命令构建模组并执行回归测试：

JDK 17 is required. Run the following commands to build the addon and execute regression tests:

```powershell
.\gradlew.bat build --no-daemon
.\gradlew.bat regression
```

构建产物位于 `build/libs/`。

The built jars are written to `build/libs/`.

## 许可 / License

源码采用 MIT License。

The source code is released under the MIT License.
