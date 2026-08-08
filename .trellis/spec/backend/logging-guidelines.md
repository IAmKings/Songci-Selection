# Logging Guidelines

> 本项目零日志体系:应用侧无日志框架,脚本侧 print 报告输出。

---

## 应用侧(Kotlin)

- **不引入日志框架**(无 slf4j/kotlin-logging);错误路径用 UI 空态/闸门表达
- 桌面打包运行时若有异常,进程 stderr 直出(调试用),不写文件

---

## 脚本侧(Python)

- 用 `print` 输出执行报告:数据源统计、覆盖率、写入路径(如 `rhythmic_map.py` 打印词牌/词作覆盖)
- 错误用 `raise SystemExit("提示 + 修复命令")` 退出
- 不写日志文件、不落调试残留

---

## 调试原则

- 应用行为问题优先用桌面测试复现(数据层),不靠运行时日志堆栈
- 性能基准记录在测试注释与 README(如 LIKE 搜索 <20ms)
