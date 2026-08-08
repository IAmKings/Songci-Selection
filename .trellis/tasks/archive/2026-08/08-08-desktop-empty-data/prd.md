# 修复桌面端打开无词数据

## Goal

修复打包版桌面应用启动后词库全空白。实际为双重问题叠加:①jlink 精简运行时缺 `java.sql` 模块(SQLDelight JDBC 查询全抛 NoClassDefFoundError)→ 全空白主因;②gradle 增量缓存损坏导致 desktopJar 缺 composeResources(songci.db)→ 首启复制即失败。顺带处理图标显示异常(macOS 图标缓存)。

## Background

- M1 打磨验证时用开发模式(完整 JDK),从未真正验证过 jlink 打包运行时 → java.sql 缺失从首个 DMG 起就存在
- 图标任务期间首次 packageDmg(13:17)后 desktopJar 资源缺失,增量缓存误判 up-to-date
- 数据层本身无问题:源库/持久库均 21,050 首,MD5 一致;Android/iOS 驱动(Native/AndroidSqliteDriver)不依赖 java.sql,无此风险

## Requirements

- **R1**:`nativeDistributions` 加 `modules("java.sql")`(CMP 1.11 DSL,位于 nativeDistributions 块内)
- **R2**:强制重跑打包链修复缓存损坏产物;记录兜底命令 `./gradlew :composeApp:clean :composeApp:packageDmg`
- **R3**:图标"丢失"定位为 macOS LaunchServices/Dock 缓存,修复手段 = `lsregister -f` + `killall Dock`

## Acceptance Criteria

- AC1:打包版应用启动无 NoClassDefFoundError,词数据正常显示(用户目视确认)
- AC2:runtime modules 文件经 jimage 验证含 `java.sql`
- AC3:bundle 内 jar 含 songci.db + dynasty 文件;图标 icns MD5 与生成产物一致
- AC4:Android/iOS 驱动确认不依赖 java.sql(代码审查)

## Out of Scope

- Android/iOS 端无需改动(驱动不依赖 java.sql,资源打包已验证完整)
- gradle 缓存损坏根因深挖(状态性损坏,非代码缺陷)

## 关键决策

- `modules("java.sql")` 放 `nativeDistributions` 块(application 块内该 API 不存在,CMP 1.11 新 DSL)
- 图标缓存问题不做代码修复(打包无误,属系统行为)
