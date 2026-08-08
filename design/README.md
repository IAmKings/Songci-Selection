# design/ 目录说明

「宋词选粹」UI 视觉设计资产。屏幕设计、排版样张、应用图标集中于此;产品文档(PRD、设计系统)位于仓库根目录。

## 结构

| 路径 | 内容 |
|---|---|
| `../PRD.md` | 产品需求文档(宋词选粹:Kami/古籍手稿设计哲学,探索/沉浸阅读/收藏/设置,页面清单) |
| `../DESIGN.md` | **Classical Manuscript 设计系统** —— 完整 token(色彩/字体/间距/形状)+ 品牌与组件规范 |
| `screens/mobile-home/` | 首页(HTML + 截图) |
| `screens/mobile-detail/` | 词作详情 · 手机 |
| `screens/mobile-search/` | 搜索结果 |
| `screens/mobile-settings/` | 设置 |
| `screens/mobile-favorites/` | 我的收藏 |
| `screens/tablet/` | 词作展示 · 平板 |
| `app-icon/screen.png` | 应用图标(1024×1024,卷轴+毛笔,书生蓝+纸张配色) |
| `widgets/2x2/` `4x1/` `4x2/` `4x4/` | **桌面小组件设计稿**(宋词选粹 Widget:网格布局规格,HTML + 截图) |
| `specimens/` | 早期排版样张(随机宋词,手机 375×812 / 平板 768×1024) |

## 屏幕 ↔ PRD 页面映射

| PRD 页面 | 位置 |
|---|---|
| 首页(手机)· SCREEN_10 | `screens/mobile-home/` |
| 词作详情(手机)· SCREEN_8 | `screens/mobile-detail/` |
| 词作详情(平板)· SCREEN_9 | `screens/tablet/` |
| 搜索结果(手机)· SCREEN_7 | `screens/mobile-search/` |
| 收藏(手机)· SCREEN_4 | `screens/mobile-favorites/` |
| 设置(手机)· SCREEN_6 | `screens/mobile-settings/` |

> 根目录 `PRD.md` 中 `{{DATA:SCREEN:...}}` 占位符按原样保留,未改动源文档。

## 设计体系脉络

- **来源**:早期 `specimens/` 样张是 kami skill 设计语言(kami token:羊皮纸 `#f5f4ed`、书生蓝 `#1B365D`)在宋词排版上的直接应用。
- **演进**:根目录 `DESIGN.md` 将其系统化为完整设计系统 —— 增加 Material 式 surface 分层、Inter 界面字体、间距/形状/组件规范;`specimens/` 的排版数值(标题 36/46px、正文 18/20px、行高 2.05/2.1、kicker、上下阕双栏)被正式收纳为 typography/spacing token。
- **实现**:`screens/*` 为设计系统的 HTML 实现(notoSerif 文学内容 + Inter 界面标注),截图即设计定稿。

## 复现与查看

- 截图:`open design/screens/mobile-home/screen.png`
- HTML:浏览器直接打开 `code.html`,devtools 设备模拟查看

## 数据来源

- 导入自 `~/Downloads/stitch_songci_digital_archive`(2026-08-07),`specimens/` 中同名 `phone.html`/`tablet.html` 与仓库内版本一致,未重复导入。
- `widgets/` 导入自 `~/Downloads/stitch_songci_digital_archive2`(2026-08-09);同目录 `classical_manuscript/DESIGN.md` 与仓库根完全一致,未重复导入。
