# Implement — 字体风格选择 + 子集化

## 执行清单

1. **下载新致宋**:LXGWNeoZhiSongScreen.ttf → composeResources/font/lxgw_neozhisong_screen.ttf(网络受限则用户手动下载)
2. **子集化脚本**:scripts/subset-fonts.sh(fonttools pyftsubset)
   - 提取词库字符集(db 5275 字)+ UI 白名单 → font-charset.txt
   - 对 3 个 ttf(WenKai×2 + 新致宋)子集化 → 覆盖 composeResources
   - 验证:子集后体积 + 无缺字抽样
3. **设置持久化**:FontStyle enum + 三端 actual(save/loadFontStyle,复用 fontScale 模式)
4. **Theme.kt**:PoemFontFamily 按风格返回;替换词文 Text 的字体消费点
5. **设置页**:字体风格选项(楷体/宋体)+ 当前高亮 + 「仅应用内生效」标注
6. **授权文件**:IPA Font License 入库
7. **验证**:desktopTest;桌面运行切换字体即时生效 + 重启持久;APK 体积对比(子集化前后);词库全量抽样无缺字
8. **提交 + 合并**(用户确认验收后)

## 验证命令

```bash
cd app && ./gradlew :composeApp:desktopTest
./gradlew :composeApp:assembleDebug && ls -la composeApp/build/outputs/apk/debug/*.apk  # 体积对比
```

## 回滚点

- 分支工作,git checkout master 整体回退
- 子集化前备份原字体文件(git 历史可恢复,子集化在分支内)

## Review Gate

- 步骤 7:AC1-AC5 全过 → 合并 master
