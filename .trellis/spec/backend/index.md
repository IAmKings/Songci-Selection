# Backend Development Guidelines

> 数据层与生成管线约定(本项目「backend」= SQLite 数据 + Python 生成脚本 + KMP 数据访问)。

---

## Overview

项目非传统 web 后端:数据层由三部分组成——`db/songci.db`(SQLite 预建库)、`app/data/tools/*.py`(生成脚本管线)、`composeResources/files/*.json`(运行时加载的生成物)。所有约定围绕:幂等生成、防御解析、最小依赖。

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | 仓库结构:源数据/脚本/运行时分层 | ✅ 已填充 |
| [Database Guidelines](./database-guidelines.md) | SQLite schema、预建库 user_version=1、LIKE 搜索基准 | ✅ 已填充 |
| [Error Handling](./error-handling.md) | 防御式解析、校验拒写、幂等、驱动缓存更新 | ✅ 已填充 |
| [Quality Guidelines](./quality-guidelines.md) | 基准测试、生成物可复现、零新依赖、数据准确性 | ✅ 已填充 |
| [Logging Guidelines](./logging-guidelines.md) | 零日志框架:应用侧无日志,脚本侧 print 报告 | ✅ 已填充 |
| [Notification Guidelines](./notification-guidelines.md) | 三端通知权限/滚动窗口排期/macOS JNA 绑定坑 | ✅ 已填充 |

---

## 核心模式速查

- 数据加载:composeResources 扁平 JSON + 手写解析器(`Dynasty.parseMap` 风格),**不引 kotlinx.serialization**
- 数据变更:源数据 → `db/build.py` → `prepare_db.py`,不手工改生成物
- 外部数据源不入 repo,`--source` 指定;生成物 JSON 入 git(可复现)

**Language**: 中文(与项目文档一致)。
