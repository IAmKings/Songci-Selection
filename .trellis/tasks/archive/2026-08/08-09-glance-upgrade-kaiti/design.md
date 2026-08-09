# Design — Glance 升级接入楷体

## 技术路线

```
libs.versions.toml: glance 1.1.1 → 1.2.0-rc01(优先)/1.3.0-alpha02(兜底)
→ gradle 编译(自动从 Google Maven 下载)
→ 本地缓存 javap 验证 FontFamily 自定义 API
→ 接入 Res.font.lxgw_wenkai(compose resources)或 res/font + FontFamily 资源引用
→ 实机验证 4 规格
```

## 自定义字体接入方式(按候选版本 API 决定)

| 方式 | 适用 | 说明 |
|---|---|---|
| A. `FontFamily(androidx.compose.ui.text.font.FontFamily)` 构造器 | Glance 1.2+ 若支持 | 直接传 compose `FontFamily(Res.font.lxgw_wenkai_regular)` |
| B. `FontFamily(String)` + res/font 资源名 | 若 familyName 走资源解析 | 需把 ttf 复制到 androidMain/res/font + font family XML |
| C. 均不支持 | 回退 | 保持 FontFamily.Serif,记录差距,任务结论注明 |

## API 变化核对清单(1.1.1 → 候选版)

编译通过为准,重点核对:
- `ColorProvider`(Int/Long/Color 构造器是否变化)—— 已踩坑,变化需同步改
- `SizeMode.Single` / `SizeMode.Exact`
- `GlanceModifier.background(ImageProvider)`
- `Text(maxLines=)` / `TextStyle(fontFamily/fontStyle)`
- `Image` / `ImageProvider(Int)`
- `provideContent` / `GlanceAppWidgetReceiver`

## 验证矩阵

- 4 规格渲染(楷体生效)
- 点击/刷新/收藏
- ColorProvider 渲染无 "No package ID" 类错误
- 冷启动(SongciApp)不回归

## 回退

- 版本号 git 回退即可;若 API 破坏面大(>2 处构造器签名变化),评估放弃升级
