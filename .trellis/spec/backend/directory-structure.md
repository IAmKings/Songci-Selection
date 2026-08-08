# Directory Structure

> 本项目非传统 web 后端——数据层 = SQLite 库 + Python 生成脚本管线。

---

## Overview

- 运行时代码（Kotlin Multiplatform）在 `app/composeApp/src/`，按 target 分层
- 数据处理管线（Python）在 `app/data/tools/` 与 `db/`、`data/`（源数据）
- 数据源外部化：不随 repo 分发，构建时从本机路径读取（`--source` 参数）

---

## Directory Layout

```
repo/
├── data/            # 源数据(ci.json 21,340 首 / ciauthor.json 名录 / 生成清单)
├── db/              # db/build.py: 源数据 → SQLite(db/songci.db, gitignore)
├── app/
│   ├── data/tools/  # Python 生成脚本(单一职责、幂等、可重跑)
│   │   ├── prepare_db.py      # db → 应用资源副本(user_version=1)
│   │   ├── dynasty.py         # 作者 → 朝代映射(dynasty_map.json)
│   │   ├── gen_app_icons.py   # 设计图 → 三端图标(单一源可复现)
│   │   ├── rhythmic_map.py    # 词牌清洗 + 钦定词谱格律映射(8 字段扁平 JSON)
│   │   ├── restore.py         # ⿰ 还原回填(CSV → ci.json, 校验/备份/幂等)
│   │   ├── jinyuan_audit.py / jinyuan_merge.py  # 金元词人审计与补全
│   │   └── restore_manifest.py # ⿰ 清单生成
│   └── composeApp/src/
│       ├── commonMain/kotlin/com/songci/app/
│       │   ├── ui/            # 导航(10 路由) + screens/ + components/ + theme/
│       │   ├── data/          # SQLDelight 查询、Dynasty/Rhythmic 加载、expect 驱动
│       │   └── App.kt
│       ├── desktopMain|androidMain|iosMain/  # 平台壳(数据库驱动、设置存储)
│       ├── commonMain/composeResources/
│       │   ├── files/         # 预建库 + 生成 JSON(dynasty/rhythmic map,入 git)
│       │   └── font/          # Noto Serif SC + Inter
│       └── desktopTest/       # 数据层基准测试(对照 SQL 基准)
└── .trellis/spec/             # 本项目开发规范(本文件所在)
```

---

## 约定

- 生成物策略:脚本产出 JSON 入 `composeResources/files/` 并提交 git(可复现);`songci.db` 生成物 gitignore
- 外部数据源(ci.json/词谱)不入 repo,脚本 `--source` 指定或固定本机路径(报错提示 clone 命令)
- 新增生成脚本放 `app/data/tools/`,对齐 dynasty.py 模式(ROOT/DB/OUT 路径约定)
