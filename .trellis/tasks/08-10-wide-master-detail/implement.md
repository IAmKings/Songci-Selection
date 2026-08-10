# Implement — 宽屏词牌/作者侧栏化

## 执行清单

1. **DetailBody 抽取**(DetailScreen.kt):NarrowDetail/WideDetail 内容抽为共享 `DetailBody`;DETAIL 路由壳不变
2. **双栏页**(RhythmicPoemsScreen/AuthorPoemsScreen):加 `wide` 参数 + `initialPoemId`;wide 分支 Row[列表 | DetailBody/空态];选中状态内部管理;窄分支零改动
3. **路由参数**(SongciApp.kt):`rhythmic/{rhythmic}?poemId={poemId}`、`author/{authorId}?poemId={poemId}`;详情页回调携带当前 poemId
4. **构建 + 测试**:desktopTest/assembleDebug/lintDebug
5. **实机验证**(桌面,拖宽 >768dp):
   - 详情 → 词牌 → 双栏,右侧初始=当前词;点列表切换;作者链接 → 作者双栏
   - 返回路径;索引/首页进入词牌 → 空态
   - 窄屏(拖窄)全回归
6. **提交**:detect_changes → commit(分支 feature/wide-master-detail)

## 验证命令

```bash
cd app && ./gradlew :composeApp:desktopTest
./gradlew :composeApp:assembleDebug
```

## 回滚点

- 分支工作,`git checkout master` 即整体回退
- DetailBody 抽取前后各跑一次 desktopTest 确认零回归

## Review Gate

- 步骤 4:构建/测试绿
- 步骤 5:AC1-AC5 验收通过 → 合并 master(用户确认)
