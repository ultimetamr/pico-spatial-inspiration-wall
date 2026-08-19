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
| 4 类卡片 | PASS | 默认数据及 UI 树含文字、图片、链接、涂鸦卡片 |
| 50+ 卡片 | PASS（功能） | 实际触发后 Room/UI 树为 56 张，54 张处于当前视锥 |
| 重启持久化 | PASS（Room） | stop/launch 后仍为 56 张卡片 |
| 整墙导出 | PASS | MediaStore 生成 2048×1152 PNG |
| 锚定 Stage | PASS（接线） | UI 树含扫描、永久锚定、返回共享工作台；确认按钮在无平面时禁用 |
| SpatialUI 风格检查 | PASS | 0 errors，3 lifecycle 建议 warnings |
| 工作流/布局/架构检查 | PASS | artifact、layout、implementation、architecture 均通过；architecture 0 warnings |

## 截图

- `inspiration-wall-workbench.png`：初始 5 张卡片工作台。
- `inspiration-wall-final-running.png`：最终 APK，56 张卡片 LOD 运行状态。
- `inspiration-wall-50-plus.png`：50+ 压力样例。
- `exported-full-wall-final.png`：完整墙面导出文件。
- `anchor-stage-emulator.png`：进入 Full Space 后的模拟器合成画面；锚定 Stage 的完整控件由 UI 树验证。

## 尚需物理头显验收

1. Persistent World Anchor 的真实创建、UUID 重新加载、遮挡后的 Relocalization 和重启定位误差。
2. 真实竖直墙面 Plane Detection、卡片法线贴合和多墙物理切换。
3. 56 张卡片持续拖拽/缩放时的 Perfetto 帧时间与稳定 60fps。模拟器启动 56 张卡片时出现过启动阶段 skipped frames，因此不能据此签署 60fps 验收。
4. 头显麦克风权限、中文 on-device SpeechRecognizer 可用性。
5. 图片卡片当前使用本地示例资产，涂鸦卡片当前使用预置 JSON 笔迹；系统选图与自由手绘编辑器尚未完成生产级验收。

## 实现中解决的问题

- CLI 模板默认指向不可用的 Spatial SDK 6.0.0；改为与本机缓存及模拟器匹配的 0.13.3。
- Room 2.6.1 与 Kotlin 2.1 metadata 不兼容；升级到 Room 2.7.2。
- 主机 Gradle 配置含失效代理；最终采用已缓存依赖的 offline 构建，未永久修改用户级配置。
- 模拟器复用旧 Volumetric 容器尺寸时画面为黑色；将默认尺寸收敛到 960×624×192，并卸载重装容器后恢复正常合成。
- Spatial Editor 的运行时 CPython 插件上下文未初始化；没有手改 USD，而是保留官方打包流程生成最小 Stage bundle，产品视觉由 Compose/SpatialUI 实现。
