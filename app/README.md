# 宋词选粹 · Compose Multiplatform 应用

「宋词选粹」跨平台应用:Kotlin + Compose Multiplatform,Android / iOS / macOS 三端,内置 21,050 首宋词数据库,遵循 `DESIGN.md`「Classical Manuscript」设计系统。

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
│   │   ├── files/rhythmic_map.json # 词牌格律映射(84% 词牌/95% 词作,生成物)
│   │   └── font/                  # Noto Serif SC + Inter(Google Fonts,OFL 许可)
│   └── src/desktopTest/   # 数据层基准测试(对照 db/songci.db SQL 基准)
├── iosApp/                # Xcode 壳(构建时经 embedAndSignAppleFrameworkForXcode 调 gradle)
├── data/tools/
│   ├── prepare_db.py      # db/songci.db → 应用资源副本(user_version=1)
│   ├── dynasty.py         # authors.long_desc → dynasty_map.json
│   ├── gen_app_icons.py   # design/app-icon/screen.png → 三端图标产物(可复现,改图标重跑)
│   └── rhythmic_map.py    # 词牌名清洗 + 钦定词谱格律映射 → rhythmic_map.json + 未映射清单
└── gradle/
```

## 构建

```bash
# 数据准备(首次或 db 更新后;生成物已 gitignore)
python3 data/tools/prepare_db.py
python3 data/tools/dynasty.py
# 格律映射(需先 clone 数据源: git clone --depth 1 https://github.com/charlesix59/chinese_word_rhyme /tmp/cwr)
python3 data/tools/rhythmic_map.py

# 桌面(macOS)
/tmp/gradle-9.7.0/bin/gradle :composeApp:desktopTest :composeApp:desktopJar
./gradlew :composeApp:run            # 直接运行(wrapper 需先成功下载发行包)

# Android
./gradlew :composeApp:assembleDebug

# iOS
open iosApp/iosApp.xcodeproj         # Xcode 构建运行
```

> 本机无系统 gradle;若 `./gradlew` 下载发行包失败,使用 `/tmp/gradle-9.7.0/bin/gradle`(或自行安装)。

## 关键设计决策

- **搜索用 LIKE 而非 FTS5**:21k 行全表扫描实测 <20ms;FTS5 unicode61 对中文是整段分词,`MATCH '明月'` 仅命中 1 行,索引基本失效(详见任务 prd)
- **预建库 + user_version=1**:SQLDelight 驱动版本匹配时跳过建表,避免 DROP 重建
- **朝代推导**:`dynasty.py` 关键词 + 年号 + 生卒年回退,覆盖率约 15%(数据本身缺失朝代信息);未覆盖作者归「未知」
- **格律**:词牌/格律合并为单一入口,词牌详情页内置格律卡片(句式摘要 + 逐字平仄谱按句分行、阕间空行、韵脚下划线;段边界由钦定词谱 shift 推断,单调/双调/三叠/四叠适配);词作内容分段(切上下阕)待数据治理后实施
- **自适应**:宽度 ≥768dp 详情双栏(上下阕并置),<768dp 单栏 + 底部导航
- **图标**:三端已接入 `design/app-icon/screen.png`(卷轴+毛笔),唯一源 + `data/tools/gen_app_icons.py` 生成全部产物。iOS 用全出血 RGB(App Store 拒 alpha,圆角由系统蒙版);macOS/Windows 用预烘焙圆角卡面。注意 CMP 1.11 桌面 DSL 用按平台 `macOS/windows/linux { iconFile }` 块,旧版 `icon(vararg)` 已移除

## 未来选项(已评估,当前不做)

- **核心库/用户库分离(零复制)**:首启直接只读打开 asset/bundle 中的核心库,favorites 独立小库。评估:收益 = Android 首启省 100–300ms(一次性)+ 磁盘 17MB;成本 = 跨库 JOIN 消失(收藏查询二次查 + 内存合并)+ 三端只读 SQLDelight 驱动适配 + 双库版本管理。当前 favorites 单表极简,ROI 差;**待核心库升级/多应用表出现时再拆** —— 届时「只读核心 + 独立用户库」是正确形态。
- **字号持久化用 DataStore**:当前用各平台原生键值存储(SharedPreferences/NSUserDefaults/Properties,~40 行零依赖);设置项增多(亮度/通知)时可换 DataStore(需引入 okio 依赖)。
- **格律多体切换**:格律卡片仅展示首体(正格);钦定词谱 826 调共 2,306 体,数据源 Ci_Tunes.json 的 formats 数组已含全部体(生成脚本只提取首体)。待 UI 需要时扩展 rhythmic_map.py 输出多体 + 卡片切换。
- **词作内容分段(切上下阕)**:按用户决策排在数据治理(⿰ 还原/金元补全)后进行;段边界数据(segEnds)已在 rhythmic_map.json 就绪,复用同一份边界切词作文本;需处理变体字数差异(多体匹配)与无格律兜底(行数对半+单段回退)。

## 测试

`./gradlew :composeApp:desktopTest` —— 数据层基准测试:搜索(对照 LIKE 基准 648 行)、词牌过滤(743 行)、收藏往返、朝代抽样、随机取词。
