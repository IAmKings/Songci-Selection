# Android 小组件(Glance)

## Goal

Android App Widget(Glance)实现设计稿 4 规格小组件:随机宋词(词牌+作者+词句),刷新/收藏/阅读全文交互,同进程直读应用 db。

## Background(父任务 design.md 设计)

- 前置验证已完成:deep link(songci://poem/{id})注册+解析 ✓,Glance 原型(2x2 同进程直读 db)构建通过 ✓
- 数据通道:Glance 渲染在应用进程 → 直读 SQLDelight db(21,340 首),零复制
- 交互:Android 无系统版本限制(API 24+),全交互(刷新按钮/收藏按钮/点击 deep link)

## Requirements

- **R1 四规格**:2x2(极简:标题+图标)/4x1(词牌+作者+词句横条)/4x2(词牌+作者+词句+刷新/收藏)/4x4(全功能+阅读全文入口);配色沿用 SongciColors(纸张底/书生蓝/墨色)
- **R2 数据**:复用 SongciRepository 随机词(randomPoems)与收藏(addFavorite/removeFavorite);渲染时打开 db 驱动(低频可接受)
- **R3 交互**:刷新按钮 → updateAll 重新随机;收藏按钮 → 写 favorites 表 + 状态切换;整卡/阅读全文点击 → deep link 打开应用对应词作
- **R4 刷新策略**:手动刷新为主;updatePeriodMillis=0(Glance 同进程自动刷新由应用触发)

## Acceptance Criteria

- AC1:四规格 widget 均可添加并正确渲染(词牌+作者+词句)
- AC2:刷新换词生效(同进程直读 db 随机)
- AC3:收藏写回应用 favorites(应用内收藏列表可见)+ 状态持久
- AC4:点击打开应用对应词作(deep link)
- AC5:主应用功能不受影响(desktopTest + assembleDebug 全绿)

## Out of Scope

- Windows/Apple 小组件(父任务其他子任务)
- 小组件主题定制

## 依赖

- 依赖父任务前置验证(已完成:deep link + Glance 依赖);实施顺序:规格 → 交互 → 验证
