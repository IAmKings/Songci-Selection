# Android 小组件视觉重对齐 design/widgets(二轮)

## Goal

修正 `app/composeApp/src/androidMain/kotlin/com/songci/app/widget/SongciWidget.kt` 四规格(2x2/4x1/4x2/4x4)与 `design/widgets/{2x2,4x1,4x2,4x4}/code.html` 的视觉偏离:布局结构(2x2 居中、4x4 竖排词句、4x4 底部操作栏)、字号梯度、图标形态与颜色。保留全部交互(整卡深链、↻/♡、四规格独立条目)与已踩坑修正(ColorProvider 包 Color、db 连接关闭、截断双保险)。

## 背景

上一轮对齐任务 `08-09-android-widget-design-alignment` 已落地结构/配色/图标,但遗留差距与实现偏离导致当前 UI 与设计稿"偏离非常严重":

- 已知差距(AC4 记录):4x4 词句降级横排、羊皮纸纹理未做、楷体不可接入(launcher 跨进程限制,已归档)
- 实现时引入的新偏离:2x2 顶部对齐(design.md 原定居中)、字号整体偏小 2~8sp、4x2 刷新图标颜色硬编码 #1B365D(设计稿 stone)、4x4 底部操作栏缺失背景/边框/胶囊按钮、四角装饰线缺失

## 偏离清单(设计稿 → 当前实现,逐规格)

### 2x2(Small)
| # | 设计稿 | 当前实现 | 严重度 |
|---|---|---|---|
| S1 | 内容垂直水平居中(justify-center items-center) | `verticalAlignment = Alignment.Top` 顶部对齐 | 高 |
| S2 | menu_book **实心**(FILL 1)24px | ic_book 描边 22dp | 中 |
| S3 | 词牌 24px | 16sp | 高 |
| S4 | 首句 14px(80% 透明度) | 12sp 全不透明 | 中 |

### 4x1(Banner)
| # | 设计稿 | 当前实现 | 严重度 |
|---|---|---|---|
| B1 | 作者 14px(label-metadata-mobile) | 10sp | 高 |
| B2 | 词句 14px 斜体 opacity-90 | 12sp 斜体 | 中 |
| B3 | 词牌 14px bold | 13sp bold | 低 |
| B4 | 左列 min-w 80px + 右 padding 16px | 76dp | 低 |

### 4x2(Medium)
| # | 设计稿 | 当前实现 | 严重度 |
|---|---|---|---|
| M1 | 品牌区:48px 圆角 logo 图 + "宋词选粹" 14px primary | ic_book 26dp + 11sp | 中 |
| M2 | action icons **stone #6B6A64** | ic_refresh 硬编码 fill #1B365D | 中 |
| M3 | 词牌 14px bold + tracking-widest,作者 secondary 独立 | 14sp bold,作者合并进同行 " · 作者" | 低 |
| M4 | 词句 14px on-surface(两行) | 11sp on-surface(三行) | 高 |
| M5 | 容器 padding 16px(p-4) | 12px | 低 |

### 4x4(Large)
| # | 设计稿 | 当前实现 | 严重度 |
|---|---|---|---|
| L1 | 头部品牌 24px「宋词精选」 | 18sp「宋词选粹」 | 高 |
| L2 | 词牌**竖排** 28px(vertical-text) | 竖排 24sp + take(8) 截断 | 中 |
| L3 | 词句**竖排两栏**(vertical-text,#1B365D) | **横排**两列 12sp | 高(结构偏离) |
| L4 | 底部操作栏:h-52px bg-surface/80 + border-t warm-sand + 圆形按钮 + 「阅读全文」胶囊按钮(border-warm-sand + open_in_new) | 无背景无边框:2 icon + "阅读全文 ›" 文本 | 高 |
| L5 | 四角装饰线(warm-sand 50%) | 无 | 中 |
| L6 | 羊皮纸纹理(parchment-texture) | 纯色 #F5F4ED | 中(已知差距) |
| L7 | 印章:6x6 边框 4px 圆角 opacity-80 + 宋字 | ic_seal(4dp 圆角)22dp + 10sp | 低 |

### 全局
| # | 设计稿 | 当前实现 | 严重度 |
|---|---|---|---|
| G1 | 字体 Noto Serif SC(宋体) | FontFamily.Serif(系统 serif,Android 多为 Noto Serif CJK,接近) | 低(跨进程限制,楷体不可行,已归档) |
| G2 | 字号梯度整体 | 偏小 2~8sp(见上表) | 高(综合) |

## 实施范围

- **P0 布局结构**(必做,先做):
  1. S1:2x2 改垂直居中
  2. L3:4x4 词句改竖排两栏(Column 逐字,字高适配 4x4 高度,超限截断)——失败再降级横排并记录
  3. L4:4x4 底部操作栏重做(半透明 surface 背景 + border-t + 圆形 ↻/♡ 按钮 + 阅读全文胶囊按钮)
  4. L5:4x4 四角装饰线(4 个 Box,warm-sand)
- **P1 视觉细节**:
  5. G2/S3/S4/B1/B2/M4:字号梯度整体对齐设计稿(见 design.md 字号表)
  6. M2:4x2 action icons 改 stone 色(新 vector 或 Glance 无 tint → 直接调 vector fillColor/新增资源)
  7. S2:2x2 书本图标实心化(fill)
  8. L1:4x4 头部文字统一「宋词选粹」(与产品名一致;设计稿 4x4 写"精选"疑笔误,见假设)
  9. M1:4x2 品牌区字号/间距微调
- **P2 装饰/记录差距**:
  10. L6:羊皮纸纹理——程序化近似(浅色斑点 vector)或记录差距跳过(见决策)
  11. G1:字体差距记录,不处理

## 验收标准

- AC1:四规格实机截图与 design/widgets 对应 screen.png 对比,无高严重度偏离项(上表"高"清零)
- AC2:现有交互全部保留:整卡点击深链、↻/♡、四规格独立添加
- AC3:`desktopTest` + `assembleDebug` 全绿
- AC4:无法达成项(如 4x4 竖排词句若溢出降级)明确记录差距于本文件

## 假设(用户可推翻)

- 4x4 头部文字统一「宋词选粹」(产品名),不采设计稿 4x4 的「宋词精选」(与 4x2 品牌区及产品名矛盾)
- 羊皮纸纹理:先做程序化近似尝试,失败则记录差距(需位图资源,与上轮结论一致)
- 字体不处理:Glance widget 跨进程渲染,应用内 TTF 不可达(已归档验证)

## 非目标

- 不升 Glance 版本(1.1.1 已回退定版,字体 API 无自定义构造器)
- 不动 macOS/iOS WidgetKit 实现(用户指定安卓)
- 不引入位图纹理资源
- 不改 manifest/receiver/数据流

## 差距记录(AC4,2026-08-13 实施后)

- [x] 2x2 垂直居中、4x4 竖排词句两栏、4x4 底部操作栏(半透明 surface + border-t + 胶囊按钮)、四角装饰线 —— 本轮已实现
- [x] 羊皮纸纹理:用户决策跳过(Glance 单层背景,需位图资源),记录差距
- [x] tracking-widest(letterSpacing):Glance 1.1.1 `TextStyle` 无 letterSpacing API,无法实现,记录差距
- [x] 4x1 作者字号:设计稿 14px,因 4x1 高度 60dp 限制降为 12sp,记录差距
- [x] 四角装饰线半透明(warm-sand 50%):Glance 无半透明背景,用固定色 #E8E6DC 近似
- [x] 4x2 词牌/作者 baseline 对齐:Glance 无 baseline 对齐,用 CenterVertically 近似
- [x] 楷体:launcher 跨进程渲染,应用内 TTF 不可达(已归档验证),系统 Serif 近似
- [ ] 实机截图对比(AC1):当前 adb 无设备连接,需用户接入设备后执行

## 验证命令

```bash
cd app && ./gradlew :composeApp:assembleDebug && ./gradlew :composeApp:desktopTest
# 实机(需设备):adb install + 手动添加四规格 widget + screencap 对比
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb exec-out screencap -p > /tmp/widget.png
```

> 当前 adb 无设备连接;实机验证步骤需用户接入设备/模拟器后执行。
