# PRD:词作朗诵(TTS)— Android 先行

- 任务目录:`.trellis/tasks/08-19-poem-tts-android`
- 状态:**已终止(2026-08-19)**

## 终止结论

**没有合理的技术方案达到当前需求**。原因链:

1. **系统 TTS 在目标真机(OPPO/ColorOS)不可用**:唯一引擎 `com.oplus.ttsaccessibilityengine` 是**无障碍(TalkBack)朗读引擎**,系统 `AppsFilter` 明确阻止第三方 app 绑定(`interaction BLOCKED`,`onInit status=-1`);系统 `tts_default_synth` 缺失,无 Google TTS。代码经诊断日志确认是设备限制,非代码缺陷。
2. **内置 TTS 引擎方案权衡不成立**:
   - espeak-ng(共振峰合成,~3MB):机械音(MOS≈3.2),**古诗词平仄/韵脚/多音字(衰→cuī)完全读不出韵味**,需大规模文本注音修正
   - sherpa-onnx + VITS(神经引擎):中文模型 100-180MB,APK 从 ~40MB 暴增至 150-250MB,违背项目轻量定位;且仍是现代普通话,非古音
   - 预渲染词库音频:2 万首 × 分钟级音频 = 数 GB,不可行
3. **结论**:系统 TTS 被厂商限制、内置引擎音质/体积均不达标,"词作朗诵"在现有约束(零新依赖、轻量 APK、三端)下无合理实现路径,终止任务。

**遗留资产**:代码实现(commonMain `PoemSpeaker` 接口 + Android/Desktop/iOS actual + 详情页播放按钮 + 诊断日志)已完成且编译通过,保留在 `feat/poem-tts-android` 分支未合并;若未来出现可行的轻量引擎(如更小的中文神经模型)或目标设备更换,可复用此接口层。

## Goal(原)

在词作详情页提供「朗诵」功能:点击播放按钮,用系统 TTS 朗读当前词作全文;再次点击暂停/停止。**MVP 仅 Android**,验证交互效果正常后,再开展多端(Desktop/iOS)同步。

## Background / 已确认事实(代码勘查)

- 项目为 Compose Multiplatform(Android / iOS / Desktop 三端 commonMain),`app/composeApp`。
- 零新依赖硬约束(frontend spec 多处明文):方案必须用**系统内置 TTS**(Android `android.speech.tts.TextToSpeech`),不引入第三方库。
- Android 端已有全局 context:`SongciApp.onCreate` 中 `AppContextHolder.context = applicationContext`(`app/composeApp/src/androidMain/kotlin/com/songci/app/SongciApp.kt`)。
- 详情操作区 `DetailActions`(`DetailScreen.kt:439`):Row 内现有「收藏」按钮 + 右侧「作者/词牌」链接组;播放按钮可加在此操作行。
- `AppViewModel`(`AppViewModel.kt`)用 `mutableStateOf(...)` + `private set` 管理 UI 状态(收藏/竖排/通知等),是放置播放状态(播放中/已暂停/空闲)的合理位置。
- 三端已有 `expect/actual` 桥接先例(`Settings.kt` + 各端 `Settings.*.kt`)——MVP 可先只实现 androidMain 的 actual,commonMain 定义接口为多端预留。

## Requirements

- R1(播放):详情页提供播放按钮,点击用系统 TTS 朗读当前词作——**词牌名 + 作者 + 正文**(顺序朗读,中间适当停顿)。
- R2(暂停/停止):播放中点击按钮切换为停止;按钮图标/文案随状态变化(播放中 → 显示停止/暂停)。
- R3(状态):播放状态在 AppViewModel 中管理(Idle / Speaking / Stopped),切换词作时自动停止上一首。
- R4(零依赖):只用 Android 系统 `TextToSpeech`,不新增任何第三方依赖。
- R5(生命周期):离开详情页/词作切换时释放或停止 TTS,避免后台发声;Activity/VM 清理时 `shutdown()`。
- R6(平台隔离):commonMain 定义朗诵接口(expect),androidMain 实现(actual),为后续 Desktop/iOS 预留同接口。

## Out of Scope(MVP 不包含)

- Desktop / iOS 端朗诵(多端同步,待 Android 验收后另行任务)。
- 逐句高亮跟随(朗诵时当前句高亮)——需平台回调,列为后续增强。
- 语速/音调/音色设置项。
- 预录音频/云端 TTS。
- 通知栏播放控制。

## Acceptance Criteria

- AC1:Android 真机,详情页(横排与竖排均可用)点击播放按钮,系统 TTS 朗读当前词作全文,声音清晰可辨。
- AC2:播放中按钮状态变化(显示停止/暂停语义),点击即停止,再次点击重新播放。
- AC3:切换词作(进入另一首)或退出详情页,上一首朗读自动停止,不串音。
- AC4:不新增任何第三方依赖(`build.gradle.kts` 依赖块无变化)。
- AC5:commonMain 存在朗诵接口定义(expect),androidMain 有对应 actual 实现,代码结构为多端预留。
- AC6:desktopTest 通过,assembleDebug 通过,装机后交互验收。

## Open Questions(阻塞规划)

- (已决)朗读内容范围:词牌名 + 作者 + 正文,顺序朗读,中间适当停顿(2026-08-19 用户确认)。
