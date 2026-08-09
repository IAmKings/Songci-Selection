# Android 小组件对齐设计稿(design/widgets)

## Goal

Android 4 规格小组件的视觉与布局对齐 `design/widgets/{2x2,4x1,4x2,4x4}/code.html` 设计稿,保留全部现有交互(整卡点击深链、↻/♡、四规格独立条目)。

## 设计规范(摘自 design/widgets)

| 规格 | 布局 | 视觉元素 |
|---|---|---|
| 2x2 | 居中:书本图标 + 词牌 + 首句 | 12px 圆角 + warm-sand 边框,primary-container 文字 |
| 4x1 | 三段式:词牌/作者(左)| 词句斜体(中)| 刷新按钮(右) | 顶部 4px primary 色条 |
| 4x2 | 左 1/4 品牌区(logo+名称)+ 右 3/4 词区(右上 ↻/♡) | 12px 圆角 + 边框 + 顶部色条 |
| 4x4 | 头部(品牌+印章)+ 竖排主区(词牌/作者/词句两列)+ 底部操作栏(刷新/收藏/阅读全文) | 28px 圆角 + 边框 + 顶部色条 |

色板:bg #F5F4ED、primary #002046、primary-container #1B365D、secondary #605E59、warm-sand #E8E6DC、on-surface #1B1C18、error #BA1A1A(印章)。

## 实施范围

- A 布局+配色(必做):四规格按上表重构,颜色 token 对齐
- B 图标(必做):book/refresh/favorite/bookmark 矢量 drawable + Glance Image
- C 圆角+边框(必做):shape drawable 背景(2x2/4x2 12px、4x4 28px;4x1 无圆角)
- D 竖排(尽力):4x4 词牌/作者竖排(每字一行);词句竖排若工作量过大则降级为横排两列,记录差距
- 印章(4x4):border drawable + "宋"字,可做则做
- 不做:羊皮纸纹理(F)、衬线字体(E,Glance 1.1.1 无 fontFamily,记录差距)

## Acceptance Criteria

- AC1:4 规格布局与设计稿一致(结构/配色/图标),实机截图对比无明显偏差
- AC2:现有交互全部保留:整卡点击深链、↻/♡、四规格独立添加
- AC3:desktopTest + assembleDebug 全绿
- AC4:未实现项(纹理/衬线/竖排词句)明确记录差距于 prd.md

## 非目标

- ~~不升 Glance 版本~~ → 已决策升级(用户 2026-08-09 确认),转任务 08-09-glance-upgrade-kaiti
- 不引入位图纹理资源(羊皮纸纹理,记录差距)

## 差距记录(AC4)

- [x] 衬线:已用 FontFamily.Serif + Italic(1.1.1 实际支持,与 design.md 初判相反)
- [x] 截断:maxLines 裁剪 + 字符级 take 双保险(词牌最长 15 字/首句最长 77 字实测)
- [ ] 楷体:Glance 1.1.1 无法加载自定义 TTF → 升级后接入霞鹜文楷(新任务)
- [ ] 羊皮纸纹理:需位图资源,暂不做
- [ ] 竖排词句:4x4 词句降级为横排两列(词牌/作者已竖排)
