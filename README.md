# 桌面空间灵感墙

> 把文字、图片和链接永久锚定到真实墙面的 PICO 空间灵感看板。

| 项目 | 内容 |
| --- | --- |
| 作品名称 | 桌面空间灵感墙 |
| 分类 | 效率工具 / 创意与生产力 |
| 一句话介绍 | 将文字、图片与链接永久锚定到真实墙面的 PICO 空间灵感看板。 |
| 应用类型 | PICO Shared Space 共享空间应用 |
| 包名 | `com.spatialapps.inspirationwall` |
| 版本 | `1.0` |
| PICO Spatial SDK BOM | `0.13.3` |
| 项目地址 | <https://github.com/ultimetamr/pico-spatial-inspiration-wall> |

## 应用介绍

桌面空间灵感墙是一款面向 PICO Shared Space 的空间创意收集工具。用户可以把文字、图片和链接制作成具有纸张质感的空间卡片，自由拖拽、缩放、旋转和调整层级，并通过分组与多墙管理建立不同主题的灵感版面。

应用使用竖直平面检测寻找真实墙面，以 Persistent Spatial Anchor 保存整面灵感墙的位置。墙面、分组、卡片内容、二维变换、层级和 Anchor UUID 通过 Room 与本地文件持久化；重新进入应用后可恢复原有空间布局。应用还支持凝视高亮、删除撤销，以及可重复打开的三步新手引导。

## 内容截图

### 历史空间运行效果（按钮配色调整前）

![桌面空间灵感墙运行效果](verification/inspiration-wall-final-running.png)

## 核心功能

- 文字、图片、链接三种卡片；图片数据保存到应用私有目录。
- 卡片点击选中、拖拽、双指缩放与旋转、惯性收尾、大小调整、置顶置底、删除和 5 秒撤销。
- PICO `spatialHoverEffect` 凝视高亮，以及系统按钮 Hover/Haptic 反馈。
- Room 持久化墙面、分组、卡片、二维变换、层级和 Anchor UUID；支持多墙切换。
- Shared Space Volumetric 工作台，以及按需打开的 Full Space 锚定 Stage。
- `PlaneTrackingManager` 竖直平面检测和 `WorldTrackingManager.createAnchor` Persistent World Anchor 流程。
- 首次启动自动显示三步新手引导，并可从顶部“使用指南”再次打开。
- 视锥剔除、低缩放级别 LOD 和 Z-Order 排序。
- Spatial Editor 打包的 `inspiration-wall-stage.bundle` 场景资源。

## 技术结构

- `app/src/main/java/com/spatialapps/inspirationwall/Main.kt`：Shared/Full Space 容器注册。
- `content/InspirationWallApp.kt`：工作台状态与功能编排。
- `content/WallComponents.kt`：空间卡片、操作栏、编辑器与锚定 Stage UI。
- `data/WallStore.kt`：Room 数据、卡片操作和惯性落盘。
- `platform/AnchorRuntime.kt`：Plane Detection 与 Persistent World Anchor。

## 构建与运行

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline --console=plain
pico-cli app install .\app\build\outputs\apk\debug\app-debug.apk -d <device-id>
pico-cli app launch com.spatialapps.inspirationwall -d <device-id>
```

## 设备验证边界

PICO 模拟器不提供真实墙面网格和可持久化 World Anchor，因此模拟器只能验证锚定 Stage、按钮状态和 API 接线。真实墙面贴合、重定位精度、重启后的物理 Anchor 恢复，以及稳定 60fps 需要在支持的 PICO 头显上完成最终验收。

图片卡片通过系统选择器导入，校验后复制到应用私有目录长期保存。

详细验证记录见 [`verification/verification-report.md`](verification/verification-report.md)。
