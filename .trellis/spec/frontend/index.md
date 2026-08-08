# Frontend Development Guidelines

> Compose Multiplatform UI 层约定(三端共用 commonMain)。

---

## Overview

UI 为 Compose Multiplatform(Android/iOS/Desktop 共用),「Classical Manuscript」设计系统(DESIGN.md)。约定围绕:复用组件、单一 ViewModel、主题 token、自适应断点、零新依赖。

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | UI 分层:导航/屏幕/组件/主题 | ✅ 已填充 |
| [Component Guidelines](./component-guidelines.md) | SimpleListScreen/TextRowList/PoemCard 复用、屏幕内私有组件 | ✅ 已填充 |
| [Hook Guidelines](./hook-guidelines.md) | Compose 状态获取模式(React hooks 对应表) | ✅ 已填充 |
| [State Management](./state-management.md) | 单 AppViewModel + StateFlow/mutableStateOf | ✅ 已填充 |
| [Quality Guidelines](./quality-guidelines.md) | 768dp 自适应、主题一致性、LazyColumn 滚动、验收闭环 | ✅ 已填充 |
| [Type Safety](./type-safety.md) | 不可变数据类、防御解析、空安全 | ✅ 已填充 |

---

## 核心模式速查

- 屏幕复用 `SimpleListScreen`(标题栏 + 内容槽);整页滚动用单一 LazyColumn(头部为首 item)
- 数据加载:`LaunchedEffect + mutableStateOf(null)` → 空态「加载中…」
- 词牌相关显示走归并逻辑(`cleanRhythmic`:⿰ 词牌 → 主词牌),源数据保留原貌

**Language**: 中文(与项目文档一致)。
