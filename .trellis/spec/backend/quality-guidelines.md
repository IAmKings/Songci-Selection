# Quality Guidelines

> 数据层质量约定:基准测试、幂等、最小依赖。

---

## 测试模式

- **desktopTest 数据层基准测试**(`SongciRepositoryTest.kt`):对照 `db/songci.db` 的 SQL 基准(sqlite3 CLI 实测),如 `search("明月")` = 100 行
- **纯函数单测**:可测逻辑下沉数据层纯函数(`Rhythmic.tuneLines`/`parseSpec`/`expand`),UI 层不测
- 测试文件同包可访问 `internal` 构造(`Rhythmic internal constructor`)
- **三端构建验证(必修)**:任何 commonMain 改动后必须 `desktopTest + assembleDebug + compileKotlinIosSimulatorArm64` 全绿——曾因只跑 desktop/android 漏检 `putIfAbsent`(JVM 解析 java.util 默认方法, Kotlin/Native 无此 API)致 iOS 编译挂。平台差异 API(如 MutableMap.putIfAbsent)禁用,用 `getOrPut` 等原生等价物。

---

## 生成物与可复现

- 所有生成脚本:单一职责、幂等(重跑 git diff 干净)、输出入 git(JSON)或 gitignore(db)
- 数据变更走管线:源数据 → `db/build.py` → `prepare_db.py` → 应用资源,不手工改生成物

---

## 依赖原则(ponytail)

- **不引新依赖**,能用 stdlib/原生/已有依赖解决就不加:JSON 用手写轻量解析(kotlinx.serialization 不引)、设置用原生键值存储(DataStore 不引)、搜索用 LIKE(FTS5 对中文分词失效)
- 复用项目既有模式,不发明新架构

---

## 数据准确性

- 数据修复只信外部权威源(纸本/专业校对站),不靠模型猜测补字
- 未映射/缺失清单为活档案(`unmapped_rhythmics.json`/`restore_manifest.json`),带原因分类与状态,治理后重跑消解
