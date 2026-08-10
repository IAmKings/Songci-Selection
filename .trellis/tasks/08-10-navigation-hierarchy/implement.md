# Implement — 导航分层重构

## 执行清单

1. **阅读现状**:SongciApp.kt 全文(Scaffold/tab 逻辑/路由表)、各 Screen 的 onOpenPoem 回调接线
2. **openPoem 统一入口**:在 SongciApp 内建 `openPoem(id)`(design.md §2 逻辑),替换所有 `nav.navigate("detail/$id")` 调用点(首页/搜索/收藏/词牌/作者/深链)
3. **bottomBar 显隐**:按路由前缀隐藏(§1)
4. **tab 切换清详情**:修改 tab onClick,内容层不随 tab 恢复(§3,以行为验证为准)
5. **深链核对**:LaunchedEffect 用 openPoem 语义
6. **验证**:
   - 三症状复现验证(无限循环/双详情并存/tab 常驻)
   - 返回路径逐条:首页→详情→词牌→选词→返回×2→首页
   - 深链直达 + 返回
   - 宽屏(>768dp 窗口)双栏不回归
   - desktopTest 全绿
7. **提交**:detect_changes → commit

## 验证命令

```bash
cd app && ./gradlew :composeApp:desktopTest
# 桌面运行目视验证:窗口拖窄(<768dp)测窄屏路径;拖宽测宽屏
```

## 回滚点

- 步骤 4 前:改动集中在 SongciApp.kt 单文件,git checkout 即回退

## Review Gate

- 步骤 6:三症状消除 + 返回路径正确 + 深链/宽屏不回归 → 提交
