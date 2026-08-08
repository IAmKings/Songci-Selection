# 宋词选粹 · Songci Selection

高保真数字手稿风格的宋词鉴赏应用与数据仓库。21,340 首词作(Kotlin Multiplatform 三端应用 + Python 数据管线 + 钦定词谱格律体系)。

## 仓库导航

| 路径 | 内容 |
|---|---|
| [app/](app/README.md) | Compose Multiplatform 应用:技术栈、构建、关键决策、数据治理记录 |
| [PRD.md](PRD.md) | 产品需求文档(Classical Manuscript 设计哲学、核心功能) |
| [DESIGN.md](DESIGN.md) | 「Classical Manuscript」设计系统 token 定义 |
| [design/](design/README.md) | UI 视觉设计资产(屏幕设计、应用图标) |
| [data/](data/) | 源数据(ci.json 21,340 首 / ciauthor.json 名录 / 生成清单) |
| [db/](db/) | 数据管线(db/build.py:源数据 → SQLite) |
| [.trellis/](.trellis/) | Trellis 开发工作流(任务、spec 规范、journal) |

## 项目状态(2026-08-08)

**应用能力**
- 三端(Android/iOS/macOS)+ 自适应(768dp 宽屏双栏)/ 搜索 / 收藏 / 字号持久化 / 三端图标
- 词作详情:上下阕分段(格律段边界,74.9% 精确)+ 格律卡片(多体切换 2,306 体、异名显示、韵脚下划线)

**数据体系**
- 词库 21,340 首(含金元词人补全 290 首);格律映射 84.2% 词牌 / 95.4% 词作
- 词牌异名关联(565 别名 + 3 策展)与异名搜索展开
- ⿰ 缺失:词牌名层清零,内容层 3,035 处(最低优先级,忠实显示)

**开发规范**
- .trellis/spec 已填充项目真实约定(数据管线/UI 模式/零新依赖原则)
- 全部工作经 Trellis 任务驱动(14 项归档),trellis-check 质量闭环

## 快速开始

```bash
# 应用构建(详见 app/README.md)
cd app && ./gradlew :composeApp:desktopTest :composeApp:packageDmg

# 数据重建(源数据变更后)
python3 db/build.py && python3 app/data/tools/prepare_db.py
```

> 网络注意:本机官方源不可达,gradle wrapper 指向腾讯镜像;外部数据源(chinese_word_rhyme/poetry-source)按 data/tools 脚本提示 clone 到本机路径。
