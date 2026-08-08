# 宋词选粹 · Compose Multiplatform 应用

「宋词选粹」跨平台应用:Kotlin + Compose Multiplatform,Android / iOS / macOS 三端,内置 21,340 首宋词数据库(含金元词人补全 290 首),遵循 `DESIGN.md`「Classical Manuscript」设计系统。

## 技术栈与版本

| 组件 | 版本 |
|---|---|
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 |
| AGP | 9.3.1(含 KMP bypass 属性,见 `gradle.properties`) |
| Gradle | 9.7.0(wrapper 指向腾讯镜像,本机网络官方源不可达) |
| SQLDelight | 2.3.2 |
| Navigation (CMP) | 2.9.2 |

## 目录结构

```
app/
├── composeApp/
│   ├── src/commonMain/kotlin/com/songci/app/
│   │   ├── ui/            # 导航(10 路由)+ 6 屏 + 自适应(768dp 断点)
│   │   ├── theme/         # DESIGN.md token → Material3 主题
│   │   ├── data/          # SQLDelight 查询、朝代映射、平台驱动 expect
│   │   └── App.kt
│   ├── src/androidMain/   # Android 壳(assets 复制预建库)
│   ├── src/iosMain/       # iOS 壳(NSBundle 复制预建库)
│   ├── src/desktopMain/   # 桌面壳(macOS)
│   ├── src/commonMain/composeResources/
│   │   ├── files/songci.db        # 预建库副本(user_version=1,生成物,不入 git)
│   │   ├── files/dynasty_map.json # 朝代映射(生成物)
│   │   ├── files/rhythmic_map.json    # 词牌格律首体映射(84.2% 词牌/95.4% 词作,8 字段,生成物)
│   │   ├── files/rhythmic_bodies.json # 格律全部体(多体切换/分段匹配,生成物)
│   │   ├── files/unmapped_rhythmics.json # 未映射清单活档案(226 条数据源缺失)
│   │   └── font/                  # LXGW WenKai 霞鹜文楷 + Inter(OFL 许可,随附 OFL.txt)
│   └── src/desktopTest/   # 数据层基准测试(对照 db/songci.db SQL 基准)
├── iosApp/                # Xcode 壳(构建时经 embedAndSignAppleFrameworkForXcode 调 gradle)
├── data/tools/
│   ├── prepare_db.py      # db/songci.db → 应用资源副本(user_version=1)
│   ├── dynasty.py         # authors.long_desc → dynasty_map.json
│   ├── gen_app_icons.py   # design/app-icon/screen.png → 三端图标产物(可复现,改图标重跑)
│   ├── rhythmic_map.py    # 词牌清洗+格律映射(含策展表) → map/bodies/未映射清单
│   ├── restore_manifest.py # ⿰ 缺失清单生成(按作者聚合)
│   ├── restore.py         # ⿰ 还原回填(CSV 校验/备份/幂等)
│   ├── jinyuan_audit.py   # 金元词人缺失审计(97 人分类)
│   └── jinyuan_merge.py   # 金元词作补全(名录内作者,别名解析)
└── gradle/
```

## 构建

```bash
# 数据准备(首次或 db 更新后;生成物已 gitignore)
python3 data/tools/prepare_db.py
python3 data/tools/dynasty.py
# 格律映射(需先 clone 数据源: git clone --depth 1 https://github.com/charlesix59/chinese_word_rhyme /tmp/cwr)
python3 data/tools/rhythmic_map.py

# 三端验证(commonMain 改动后必修)
./gradlew :composeApp:desktopTest :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64

# 桌面(macOS)
/tmp/gradle-9.7.0/bin/gradle :composeApp:desktopTest :composeApp:desktopJar
./gradlew :composeApp:run            # 直接运行(wrapper 需先成功下载发行包)

# iOS 完整构建
open iosApp/iosApp.xcodeproj         # Xcode 构建运行
```

> 本机无系统 gradle;若 `./gradlew` 下载发行包失败,使用 `/tmp/gradle-9.7.0/bin/gradle`(或自行安装)。

## 关键设计决策

- **字体 = LXGW WenKai 霞鹜文楷(SIL OFL-1.1)**:仓耳今楷因商业许可未授权弃用(2026-08-09,字体内嵌声明须获书面许可);替代评估——霞鹜文楷楷体贴合手稿设计、覆盖严格优于 Noto Serif SC(词库 5,379 字符缺 1 个扩展区字 vs Noto 缺 3 个,替换零损失),全量替换(不做子集化:搜索框任意输入需全字符集)。OFL.txt 随字体分发。
- **搜索用 LIKE 而非 FTS5**:21k 行全表扫描实测 <20ms;FTS5 unicode61 对中文是整段分词,`MATCH '明月'` 仅命中 1 行,索引基本失效(详见任务 prd)
- **预建库 + user_version=1**:SQLDelight 驱动版本匹配时跳过建表,避免 DROP 重建
- **朝代推导**:`dynasty.py` 关键词 + 年号 + 生卒年回退,覆盖率约 15%(数据本身缺失朝代信息);未覆盖作者归「未知」
- **格律体系**:词牌/格律合并单一入口,详情页内置格律卡片——句式摘要 + 逐字平仄谱(平蓝/仄红/中灰,按句分行、阕间空行、韵脚下划线)、**多体切换**(钦定词谱 2,306 体,体作者名标注:毛滂体/苏轼体,点选切换)、**异名显示与搜索展开**(出塞→谒金门同调全部词,单键组别名亦命中);段边界由 shift 推断(单调/双调/三叠/四叠)
- **词作分段**:`Segmenter` 按格律段边界切上下阕——词作字数匹配任一体 → 该体段边界精确切分(74.9%,已映射内 87.9%),变体/未映射兜底行数对半;宽屏双栏/窄屏段间分隔按真实阕界
- **自适应**:宽度 ≥768dp 详情双栏(上下阕并置),<768dp 单栏 + 底部导航
- **图标**:三端已接入 `design/app-icon/screen.png`(卷轴+毛笔),唯一源 + `data/tools/gen_app_icons.py` 生成全部产物。iOS 用全出血 RGB(App Store 拒 alpha,圆角由系统蒙版);macOS/Windows 用预烘焙圆角卡面。注意 CMP 1.11 桌面 DSL 用按平台 `macOS/windows/linux { iconFile }` 块,旧版 `icon(vararg)` 已移除

## 数据治理决策记录(2026-08-08)

- **驱动缓存策略**:桌面/Android/iOS 首启复制预建库到用户目录,**版本标记判新**(prepare_db 生成 db_version.txt = 源库内容哈希,缓存旁版本文件不一致即重新复制;曾因旧库残留致词库缺失/空白词牌名,现以版本标记根治);词牌显示层:含 ⿰ 词牌归并到主词牌(源数据保留原貌),异名搜索自动展开同调(出塞→谒金门全部词)。

- **⿰ 内容层还原 = 最低优先级**:3,035 处缺失(850 首/4.0%)经评估——chinese-poetry/snowtraces 两大开源数据源同源缺失(均用 □ 占位),搜韵等权威校对网抽查 5 首同样缺字。自动化不可行,人工对照 ROI 极低。**维持现状**:缺失字符以 ⿰ 显示(忠实传递),清单 `data/restore_manifest.json` + `app/data/tools/restore.py` 管道保留,未来若有高质量权威数字源可重评。
- 词牌名层 ⿰ 已清零(6 个贺铸遗缺词牌归并显示,源数据保留原貌)。
- **金元词人补全(+290 首/15 人)**:snowtraces/poetry-source(MIT)金 919 + 元 5,004 首中筛名录内无词作作者(蔡松年 64/元好问 57/李俊民 49/段克己 32 等),title 词牌解析(·取主+最长前缀+别名),内容级去重,词库 21,050→21,340;名录无词作 97→82。
- **alias 策展(律体校验双条件)**:25 条形近候选——5 条消解(水调歌→水调歌头/木兰花→玉楼春/雨中花→雨中花慢,字数命中证据),20 条确认独立调重归类;策展门槛 = 名字相似 + 律体吻合,防误配优先于覆盖率。未映射 226 条全为数据源缺失。

## 有意决策(非技术债,触发条件驱动,2026-08-08 澄清)

> 以下为**评估后有意不做**的决策,非技术债:触发条件出现时才实施,提前做是 YAGNI 浪费。

- **核心库/用户库分离(零复制)**:收益 = Android 首启省 100–300ms(一次性)+ 磁盘 17MB;成本 = 跨库 JOIN 消失 + 三端只读驱动适配 + 双库版本管理。当前 favorites 单表极简,ROI 差;**触发条件:核心库升级/多应用表出现**。
- **字号持久化用 DataStore**:原生键值(SharedPreferences/NSUserDefaults/Properties,~40 行零依赖)已满足单枚举需求,DataStore 引入 okio 依赖是**反向优化**;**触发条件:设置项增多(亮度/通知)且需要响应式订阅**。
- **数据源扩充 = 不值得(已检验)**:见下「未来选项」区「数据源扩充 = 不值得(已检验 2026-08-08)」;**触发条件:含完整句读+韵脚+段边界的钦定数字化源出现**。

## 未来选项(已评估,当前不做)

- **数据源扩充 = 不值得(已检验 2026-08-08)**:钦定词谱数字化源(cipai_with_statistics_qdcp.csv, 809 调名/2,268 体)对未映射 226 词牌仅覆盖 10 条/103 首(调笑令系 77/剔银灯 10 等, 失调名 372 首非调名、六么令等钦定未收、套曲不入谱均无效);且 CSV 仅平仄串+逗读序列,**无韵脚/段边界字段**——扩充词牌将无韵脚下划线/阕间空行/精确分段(功能降级),分段精确率零增益。判定:维持 818 调数据源,**待含完整句读+韵脚+段边界的钦定数字化源出现再重评**。

## 测试

`./gradlew :composeApp:desktopTest` —— 数据层基准测试(对照 db/songci.db SQL 基准):
- 搜索 LIKE 基准(明月 100 行)、异名搜索回归(青衫湿→人月圆)、词牌过滤、收藏往返、朝代抽样、随机取词
- 格律解析器:parseMap/parseSpec 8 字段、expand 异名展开、bodies 多体匹配
- 分段器:精确双调/单调单段/变体兜底/无格律兜底/去标点

全部测试为纯函数/数据层基准,UI 层不测(目视验收)。
