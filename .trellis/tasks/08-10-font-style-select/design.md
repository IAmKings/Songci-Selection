# Design — 字体风格选择 + 子集化

## 总览

```
composeResources/font/:
  lxgw_wenkai_{regular,medium}.ttf(子集化后)
  lxgw_neozhisong_screen.ttf(新字库,子集化后)
  IPA-Font-License.txt(授权)
设置: fontStyle(kaikti/songti)持久化 → Theme.kt 按选择加载 FontFamily
```

## 1. 子集化流程(scripts/subset-fonts.sh,fonttools)

```bash
# 1. 从 db 提取词库字符集(5275 字)+ 词牌/作者名
# 2. 追加 UI 文本字符(设置项文案等,从代码/资源提取或人工维护白名单)
# 3. pyftsubset 每个 ttf:--unicodes=<字符集> --no-hinting --layout-features='*'
# 4. 输出覆盖到 composeResources/font/(替换原文件)
```

- 词库变更后重跑脚本(幂等)
- 字符集文件固化:scripts/或 ci 产物 `font-charset.txt`
- 桌面与移动端共用同一套子集字体(无需平台差异)

## 2. 字体风格设置(common)

- 持久化:复用现有设置机制(Settings.android.kt 的 SharedPreferences 模式 → 需要 common 抽象)
  - 现状:fontScale 的 actual 实现(Android SharedPreferences / desktop / iOS 各一)
  - 新增 `saveFontStyle(name)` / `loadFontStyle()` 三端 actual,命名 enum FontStyle { KAITI, SONGTI }
- Theme.kt:`PoemFontFamily` 按风格返回 WenKai 或新致宋 FontFamily(@Composable 资源加载)
- 应用点:所有词文 Text 使用 PoemFontFamily(现状 NotoSerifFamily 的消费点替换)
- 设置页:RadioButton/列表项,当前选中高亮 + 「仅应用内生效」说明

## 3. 新致宋接入

- 下载:LXGWNeoZhiSongScreen.ttf(普通版,不含 Full 的混入黑体字符)→ composeResources/font/
- 命名:lxgw_neozhisong_screen.ttf
- 授权:LXGWNeoZhiSongScreen.ttf 对应 IPA Font License v1.0 → IPA-Font-License.txt

## 4. 风险

- 子集化缺字:字符集必须覆盖词库全量(5275 实测)+ UI 文本;验收抽样
- 新致宋下载源网络受限(需实测 GitHub raw);失败则走浏览器手动下载
- 字体切换即时生效:FontFamily 为 @Composable 资源加载,按 state 切换需重新组合
- widget 不受影响(标注即可)
