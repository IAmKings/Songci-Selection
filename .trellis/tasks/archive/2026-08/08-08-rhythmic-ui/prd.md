# 格律 UI 接入(词牌详情平仄谱展示)

## Goal

把 rhythmic_map.json 的格律数据接入应用 UI:词牌详情页顶部展示格律卡片(句式摘要 + 逐字平仄谱 + 韵脚),词牌列表行显示字数摘要,目录「格律」入口并入「词牌」(同一功能,避免重复入口)。

## Background

- 数据层已就绪:rhythmic_map.json(1,195 词牌,生成物)覆盖 84% 词牌/95.3% 词作
- 现状:RhythmicsScreen 纯词牌名列表 → RhythmicPoemsScreen 词作列表,无格律展示;目录有「词牌」与「格律」两入口指向同一列表(重复)
- 项目模式:Dynasty.kt 用轻量字符串解析器读 composeResources JSON(无 kotlinx.serialization 依赖),UI 直接消费

## Requirements

- **R1 数据格式对齐**:rhythmic_map.py 输出摘要字段(供 UI 消费),保持 dynasty_map.json 的极简解析风格;现有字段保留
- **R2 加载层**:新增 `Rhythmic.kt`(对齐 Dynasty.kt):load() 读 rhythmic_map.json,`of(rhythmic)` 返回格律摘要;解析器对齐 parseMap 风格
- **R3 词牌详情格律卡片**:RhythmicPoemsScreen 顶部展示:sketch(句式摘要)+ 字数 + 体数 + 逐字平仄谱(中/平/仄 三色标记,句/韵分隔);无格律数据(未映射词牌)不显示卡片
- **R4 词牌列表字数摘要**:RhythmicsScreen 词牌行显示字数小标签(有数据才显示)
- **R5 目录入口合并**:删除 IndexScreens 目录「格律 →」入口(词牌已含格律),README 决策记录同步

## Acceptance Criteria

- AC1:desktopTest 新增解析器测试通过(对齐现有测试风格),既有测试全绿
- AC2:桌面运行:水调歌头/浣溪沙详情页显示格律卡片(字数/句式/平仄谱);未映射词牌(如「一井金」)无卡片不崩溃
- AC3:词牌列表行显示字数标签;目录无「格律」重复入口
- AC4:assembleDebug 通过;生成物重新生成后幂等

## Out of Scope

- alias-mismatch 24 条策展补映射(清单 status=open,策展另立)
- ⿰ 还原专项、金元补全
- 韵部/韵脚高亮、多体切换 UI(先展示首体)

## 关键决策

- 平仄谱用色块逐字渲染(中=灰/平=蓝/仄=红),句末空格分隔、韵脚特殊标记——纯 Compose 无新依赖
- 数据格式:生成脚本补输出扁平摘要值(对齐 dynasty 解析风格),不做 kotlinx.serialization 引入
