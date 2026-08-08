# 词牌名清洗与格律映射(含未映射标记)

## Goal

数据治理第一步:把 db 中 1,423 个词牌名清洗归并,映射到钦定词谱格律数据源(818 调),生成应用可用的 `rhythmic_map.json` + 未映射清单 `unmapped_rhythmics.json`(带原因分类,供还原专项联动)。

## Background

- db 词牌名 1,423 个,数据源 Ci_Tunes.json(钦定词谱系)818 调——已实测:直配 647(45.5%)+ 变体归并 514 = 81.6% 可映射,262 个未映射
- 未映射构成:⿰ 占位词牌名(9 个,数据缺口一)、异名直译差异、数据源未收录调
- 数据源位于 /tmp/cwr/data/Ci_Tunes.json(19.6MB,已 clone);不进 repo,仅作构建输入
- 模式复用:dynasty.py 的「脚本生成 + composeResources + 生成物不入 git」管线

## Requirements

- **R1 清洗规则**:`A·B` → 取 B(主词牌);引号词题剥离(`"僮至"念奴娇` → `念奴娇`);⿰ 前缀剥离(`⿰⿰⿰·七娘子` → `七娘子`);全角空格/符号规范化
- **R2 映射**:清洗后名称精确匹配 Ci_Tunes.json 键;命中输出 `rhythmic_map.json`(词牌名 → 格律摘要:字数/句式 sketch/首体平仄谱/韵脚标记)
- **R3 未映射清单**:`unmapped_rhythmics.json`,每条含 `rhythmic`(原始名)、`cleaned`(清洗后)、`category`(placeholder / alias-mismatch / missing-in-source)、涉及词作数;清单为活档案,还原专项完成后重跑流水线消解 placeholder 类
- **R4 覆盖统计**:输出映射覆盖报告(词牌名覆盖率 + 词作覆盖率),记入任务 notes

## Acceptance Criteria

- AC1:清洗+映射脚本幂等可重跑;`rhythmic_map.json` 生成且经 JSON 校验
- AC2:可映射率 ≥ 81%(以词牌名计,基准 1,161/1,423);未映射清单三个分类齐备,每类有代表性样例可查
- AC3:抽查 5 个已映射词牌(水调歌头/浣溪沙/念奴娇/满江红/鹧鸪天),格律摘要与钦定词谱一致(人工核对)
- AC4:desktopTest 数据层测试通过(如有涉及);`assembleDebug` 不受影响

## Out of Scope

- 格律 UI 展示(词牌详情页平仄谱——后续任务)
- ⿰ 原字还原(数据治理专项,清单联动但独立)
- 金元词人补全(独立专项)
- 数据源准确性全面核对(本任务仅抽查 5 调,全面核对另行立项)

## 关键决策

- 未映射清单带原因分类而非一刀切报告——placeholder 类与还原专项联动可自动消解
- 数据源不入 repo(19.6MB),构建时从本机路径读;生成物 rhythmic_map.json 入 composeResources(同 dynasty_map.json 模式)
