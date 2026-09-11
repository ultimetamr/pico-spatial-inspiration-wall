# 验证报告

验证日期：2026-08-16（Asia/Shanghai）

目标：PICO Emulator `Pico_Emulator_0_13` / `emulator-5554` / x86_64

安装包：`com.spatialapps.inspirationwall` 1.0

## 自动验证结果

| 项目 | 结果 | 证据 |
|---|---|---|
| Debug 构建 | PASS | `:app:assembleDebug`，offline build successful |
| JVM 单元测试 | PASS | `CardLayoutEngineTest`：剔除、缩放边界、Z-Order、惯性 |
| 安装与启动 | PASS | PID 创建、`pico-cli app info` 显示 running=true |
| 崩溃检查 | PASS | 过滤 `AndroidRuntime/FATAL EXCEPTION` 无应用崩溃 |
| 3 类卡片 | PASS | 默认数据及 UI 树含文字、图片和链接卡片 |
| 重启持久化 | PASS（Room） | stop/launch 后仍为 56 张卡片 |
| 锚定 Stage | PASS（接线） | UI 树含扫描、永久锚定、返回共享工作台；确认按钮在无平面时禁用 |
| SpatialUI 风格检查 | PASS | 0 errors，3 lifecycle 建议 warnings |
| 工作流/布局/架构检查 | PASS | artifact、layout、implementation、architecture 均通过；architecture 0 warnings |

## 截图

- `inspiration-wall-workbench.png`：初始 5 张卡片工作台。
- `inspiration-wall-final-running.png`：最终 APK，56 张卡片 LOD 运行状态。
- `anchor-stage-emulator.png`：进入 Full Space 后的模拟器合成画面；锚定 Stage 的完整控件由 UI 树验证。

## 尚需物理头显验收

1. Persistent World Anchor 的真实创建、UUID 重新加载、遮挡后的 Relocalization 和重启定位误差。
2. 真实竖直墙面 Plane Detection、卡片法线贴合和多墙物理切换。
3. 56 张卡片持续拖拽/缩放时的 Perfetto 帧时间与稳定 60fps。模拟器启动 56 张卡片时出现过启动阶段 skipped frames，因此不能据此签署 60fps 验收。
4. 图片卡片当前使用本地示例资产；系统选图尚未完成生产级验收。

## 实现中解决的问题

- 2026-09-04：卡片曾可被拖到画布外而无法选中；现按实时画布尺寸、屏幕密度、缩放和旋转外接范围约束位置，并自动回收历史越界卡片。新增 4 组边界单元测试。
- 2026-09-04：图片卡片接入 Android 本地图片选择器，选中后校验并复制到应用私有目录；移除 `50+ 测试`入口、生成逻辑及其历史测试卡片。
- 2026-09-04：按产品调整移除 PNG 导出入口及实现；新增首次启动自动出现、完成状态持久化且可从顶部重新打开的三步新手引导。
- CLI 模板默认指向不可用的 Spatial SDK 6.0.0；改为与本机缓存及模拟器匹配的 0.13.3。
- Room 2.6.1 与 Kotlin 2.1 metadata 不兼容；升级到 Room 2.7.2。
- 主机 Gradle 配置含失效代理；最终采用已缓存依赖的 offline 构建，未永久修改用户级配置。
- 模拟器复用旧 Volumetric 容器尺寸时画面为黑色；将默认尺寸收敛到 960×624×192，并卸载重装容器后恢复正常合成。
- Spatial Editor 的运行时 CPython 插件上下文未初始化；没有手改 USD，而是保留官方打包流程生成最小 Stage bundle，产品视觉由 Compose/SpatialUI 实现。
