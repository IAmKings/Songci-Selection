# Android 小组件四规格独立定制

## Goal

Android 桌面小组件从"1 个 provider + 拖拽 resize"改为**4 种独立添加条目**,与 macOS WidgetKit 4 规格交互一致(添加面板直接选规格,无需拖拽)。

## 背景

- 现状:1 个 `SongciWidgetReceiver` + `SizeMode.Responsive` 4 尺寸;添加面板仅 1 条,其余规格靠拖拽
- macOS 端:4 种大小(small/medium/large/extraLarge)分别添加
- 实机已验证:Glance 渲染、整卡点击深链、刷新/收藏、AppContextHolder 兜底均可用

## 规格定义(macOS ↔ Android 映射)

| 条目 | Android 规格 | 布局定制 |
|---|---|---|
| 宋词选粹·小 | 2x2(110×110dp, target 2×2) | 词牌·作者 + 首句(紧凑) |
| 宋词选粹·横幅 | 4x1(250×110dp, target 4×1) | 横幅:词牌·作者 + 一句 |
| 宋词选粹·中 | 4x2(250×250dp, target 4×2) | 词牌·作者 + 多行词句 + ↻/♡ |
| 宋词选粹·大 | 4x4(250×520dp, target 4×4) | 全文多行 + ↻/♡ + 阅读全文提示 |

## 交互要求(全部规格)

- 整卡点击 → `songci://poem/{id}` 直达对应词详情(深链已实现)
- 中/大规格:↻ 刷新换词、♡ 收藏
- 数据:同进程直读 db,随机一词(现状保留)

## Acceptance Criteria

- AC1:添加面板出现 4 个独立条目(各自 label/预览),均可添加
- AC2:4 规格渲染正确(内容与规格匹配,不重叠不截断)
- AC3:整卡点击直达对应词详情;↻/♡ 生效
- AC4:旧 2x2 路径兼容:已添加的旧组件不破坏(卸载重装后由新条目替代,不视为问题)
- AC5:主应用功能不受影响(desktopTest + assembleDebug 全绿)

## 非目标

- 不做多语言、不做主题适配、不做配置界面
- 不引入 Glance 新版本(1.1.1 已踩坑,保持)
