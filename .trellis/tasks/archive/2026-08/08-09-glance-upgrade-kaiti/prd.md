# Glance 升级接入楷体(霞鹜文楷)

## Goal

升级 androidx.glance 使小组件词文使用与主应用一致的手写楷体(霞鹜文楷 LXGW WenKai,已随 composeResources 打包),同时保持 4 规格布局、交互与已踩坑修正不回归。

## 背景

- Glance 1.1.1 的 `FontFamily(String)` 仅解析系统字体名,无法加载自定义 TTF(已查证)
- 版本线(Google Maven 实测):1.1.1(stable)→ 1.2.0-alpha01/beta01/rc01 → 1.3.0-alpha01/02;**无 1.2.0 stable**
- 候选:1.2.0-rc01(rc,优先)或 1.3.0-alpha02(alpha,兜底)
- 主应用词文字体:composeResources/font/lxgw_wenkai_{regular,medium}.ttf(theme/Theme.kt)

## Acceptance Criteria

- AC1:4 规格小组件词文渲染为楷体(霞鹜文楷),与主应用一致
- AC2:布局/交互零回归:整卡点击、↻/♡、四规格条目、圆角边框背景、色板、截断
- AC3:已踩坑防回归:ColorProvider 包 Color()、AppContextHolder 冷启动兜底、SizeMode/背景/图标 API 变化适配
- AC4:desktopTest + assembleDebug 全绿

## 风险与回退

- 1.2.0-rc01/1.3.0-alpha02 均为预发布,API 可能变化;若候选版本均无自定义字体 API → 回退 1.1.1,记录"字体差距"
- 编译失败/行为异常 → git 回退版本号即可

## 非目标

- 不迁移其他平台 widget(macOS WidgetKit 用系统字体,不动)
- 不引入字体子集化工具(ttf 已打包,直接复用)

## 任务结论(2026-08-09,不达成 AC1,已回退 1.1.1)

**平台级限制,非版本问题**:Glance widget 文本由 launcher 进程渲染(RemoteViews 跨进程),
字体必须系统可见——app 内自定义 TTF 无法跨进程传递。`FontFamily` API 从 1.1.1 到
1.3.0-alpha02 未变(仅 String + Serif/SansSerif/Monospace/Cursive 系统常量),
**任何 Glance 版本都无法使用应用内楷体**。

实测记录:
- 1.2.0-rc01:编译零破坏(API 兼容),但 FontFamily 无自定义字体构造器
- 1.3.0-alpha02:需 compileSdk 37(项目 36),且仍为 alpha,未验证(API 大概率同前)

唯一可行路径(均不采纳,记录备查):
- 用户设备安装系统字体(霞鹜文楷经系统字体商店)后 familyName 可用——依赖用户操作,非产品方案
- 词文渲染为位图——成本高、清晰度差,放弃

现有 FontFamily.Serif(系统衬线)保持为 widget 词文的最终字体。

