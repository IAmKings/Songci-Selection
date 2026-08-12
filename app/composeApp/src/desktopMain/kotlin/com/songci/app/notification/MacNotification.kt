package com.songci.app.data

import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * macOS 每日一词通知:JNA 直绑 ObjC runtime(无 com.sun.jna.objc 包——JNA 5.x 已移除;
 * 用 Function 调 objc_msgSend + 手工 block 结构 + Callback 当 IMP)。
 * 滚动窗口同 iOS(启动/设置变更时补排未来 7 天,调度时各随机选词);
 * 点击回调经 macDeepLinkChannel(main.kt 的现有深链通道)直达词详情。
 */
private const val WINDOW_DAYS = 7L

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** 深链通道(main.kt 注入):点击通知 → 打开词详情。 */
var macDeepLinkChannel: Channel<Long>? = null

// ---- ObjC runtime(纯 JNA)----
private val objcLib: NativeLibrary = NativeLibrary.getInstance("objc")

private fun sel(name: String): Pointer = objcLib.getFunction("sel_registerName").invokePointer(arrayOf(name))

private fun cls(name: String): Pointer = objcLib.getFunction("objc_getClass").invokePointer(arrayOf(name))

private fun send(receiver: Pointer?, selector: String, vararg args: Any?): Pointer? {
    val fn = objcLib.getFunction("objc_msgSend")
    val all = if (args.isEmpty()) arrayOf(receiver, sel(selector)) else arrayOf(receiver, sel(selector), *args)
    return fn.invokePointer(all)
}

/** NSInteger 返回值走 invokeLong:invokePointer 会把值当指针,再 getLong(0) 解引用即段错误(实测 si_addr=0x1)。 */
private fun sendLong(receiver: Pointer?, selector: String, vararg args: Any?): Long {
    val fn = objcLib.getFunction("objc_msgSend")
    val all = if (args.isEmpty()) arrayOf(receiver, sel(selector)) else arrayOf(receiver, sel(selector), *args)
    return fn.invokeLong(all)
}

private fun allocInit(className: String): Pointer? = send(send(cls(className), "alloc"), "init")

/** 最小 Block_descriptor_1:reserved + size;_Block_copy 读 descriptor->size 确定拷贝大小。 */
class BlockDescriptor : Structure() {
    @JvmField var reserved: Long = 0
    @JvmField var blockSize: Long = 0
    override fun getFieldOrder(): List<String> = listOf("reserved", "blockSize")
}

// ---- ObjC block 手工构造(最小布局:isa/flags/reserved/invoke/descriptor;调用方只走 invoke)----
class ObjCBlock(fn: Callback) : Structure() {
    @JvmField var isa: Pointer? = objcLib.getGlobalVariableAddress("_NSConcreteStackBlock")
    @JvmField var flags: Int = 0
    @JvmField var reserved: Int = 0
    @JvmField var invoke: Pointer? = CallbackReference.getFunctionPointer(fn)
    @JvmField var descriptor: Pointer? = null

    /** 常驻 descriptor(private 字段不进 JNA 布局);随 block 一起被顶层 val 持有。 */
    private val descriptorStruct = BlockDescriptor()

    override fun getFieldOrder(): List<String> = listOf("isa", "flags", "reserved", "invoke", "descriptor")

    init {
        // JNA 只在结构体作为参数传给 native 时 autoWrite;这里只取 .pointer 传给 objc_msgSend,
        // 不显式 write 则 native 内存全零,系统收到 isa=NULL 的 block 直接崩溃。
        write()
        // descriptor 为 NULL 同样必崩:_Block_copy 读 descriptor->size(段错误,已实测复现)。
        descriptorStruct.blockSize = size().toLong()
        descriptorStruct.write()
        descriptor = descriptorStruct.pointer
        write()
    }
}

/** 授权回调: void^(BOOL granted, NSError*);block invoke 首参为 block 自身。 */
class GrantBlock : Callback {
    // @JvmField:公开字段不算 public 方法(JNA 要求 Callback 类仅一个 public 方法=invoke)
    @JvmField var granted = false
    fun invoke(block: Pointer?, grantedFlag: Byte, error: Pointer?) {
        granted = grantedFlag != 0.toByte()
    }
}

/** 设置回调: void^(UNNotificationSettings*);授权状态写入 + 放行等待方(回调在系统线程异步触发)。 */
class SettingsBlock : Callback {
    @JvmField var status = -1L
    @JvmField @Volatile var latch: CountDownLatch? = null   // @JvmField:否则生成 public getter 破坏 JNA 单方法回调校验
    fun invoke(block: Pointer?, settings: Pointer?) {
        if (settings != null) status = sendLong(settings, "authorizationStatus") else -1L
        latch?.countDown()
    }
}

private val settingsCb = SettingsBlock()
private val settingsBlock = ObjCBlock(settingsCb)

/** 授权回调常驻实例:block 指针被系统异步持有,局部变量会被 GC 成悬垂指针。 */
private val grantCb = GrantBlock()
private val grantBlock = ObjCBlock(grantCb)

private fun center(): Pointer? = send(cls("UNUserNotificationCenter"), "currentNotificationCenter")

actual fun requestNotificationPermission() {
    // UNAuthorizationStatus:0=未决定(可弹框)/1=拒绝(系统不再弹,引导系统设置)/2=已授权
    when (authorizationStatus()) {
        0L -> send(center(), "requestAuthorizationWithOptions:completionHandler:", 7L, grantBlock.pointer)
        1L -> openNotificationSettings()
    }
}

actual fun notificationPermissionGranted(): Boolean = authorizationGranted()

/** Denied 后系统不再弹授权框:深链打开系统设置的通知页(标准 UX,同 iOS 引导)。 */
private fun openNotificationSettings() {
    try {
        ProcessBuilder("open", "x-apple.systempreferences:com.apple.preference.notifications").start()
    } catch (_: java.io.IOException) {
    }
}

/** UNAuthorizationStatus(0=未决定/1=拒绝/2=已授权/3=临时/4=短暂);回调异步,等最多 5s,超时返回 -1。 */
private fun authorizationStatus(): Long {
    settingsCb.status = -1
    settingsCb.latch = CountDownLatch(1)
    try {
        send(center(), "getNotificationSettingsWithCompletionHandler:", settingsBlock.pointer)
        if (!settingsCb.latch!!.await(5, TimeUnit.SECONDS)) return -1L   // 回调未到:按未知处理
        return settingsCb.status
    } finally {
        settingsCb.latch = null
    }
}

private fun authorizationGranted(): Boolean = authorizationStatus() == 2L

private fun epochDay(): Long = Date().time / 86_400_000L

private fun triggerDate(daysFromNow: Int, hour: Int, minute: Int): Pointer? {
    val cal = send(cls("NSCalendar"), "currentCalendar")
    // NSCalendarUnitDay = 16(NS_OPTIONS 位 4);传 1 是非法 unit,dateByAddingUnit 返回 nil →
    // components nil → UNCalendarNotificationTrigger 抛 NSException(已实测,授权后首次排期即崩)
    val future = send(cal, "dateByAddingUnit:value:toDate:options:", 16L, daysFromNow.toLong(), send(cls("NSDate"), "date"), 0L)
    val comps = send(cal, "components:fromDate:", (0x0E or 0x10).toLong(), future)   // NSYear|NSMonth|NSDay|NSHour
    send(comps, "setHour:", hour.toLong())
    send(comps, "setMinute:", minute.toLong())
    return comps
}

private fun nsString(s: String): Pointer? = send(cls("NSString"), "stringWithUTF8String:", s)

private fun nsNumber(v: Long): Pointer? = send(cls("NSNumber"), "numberWithLongLong:", v)

// ---- 点击回调 delegate(JNA Callback 直接当 IMP,无需 block)----
class ResponseCallback : Callback {
    // IMP 签名: (id self, SEL _cmd, id center, id response, id completionHandler)
    fun invoke(self: Pointer?, cmd: Pointer?, center: Pointer?, response: Pointer?, completion: Pointer?) {
        // macOS 的 UNNotificationResponse 无 request 属性(iOS 才有),链为 response → notification → request → content
        // (直接发 request 消息会 NSInvalidArgumentException: unrecognized selector,已实测崩溃)
        val notification = send(response, "notification") ?: return
        val content = send(notification, "request")?.let { send(it, "content") } ?: return
        val userInfo = send(content, "userInfo") ?: return
        val poemId = send(userInfo, "objectForKey:", nsString("poemId"))?.let { sendLong(it, "longLongValue") } ?: 0L
        if (poemId > 0) macDeepLinkChannel?.trySend(poemId)
        // ponytail: completionHandler 不回调(block 调用桥未做,先跑通主链路;系统日志警告可接受)
    }
}

private var delegateRegistered = false

/** 常驻回调与 delegate 实例:JNA 对 Callback 弱引用,局部实例 GC 后 IMP 悬垂;
 * UNUserNotificationCenter.delegate 是 weak,不持有则实例被释放 → 点击无回调(已实测)。 */
private val responseCb = ResponseCallback()
private var delegateInstance: Pointer? = null

private fun ensureDelegate() {
    if (delegateRegistered) return
    val delegate = objcLib.getFunction("objc_allocateClassPair")
        .invokePointer(arrayOf(send(cls("NSObject"), "class"), "SongciNotificationDelegate", 0))
        ?: return
    val proto = objcLib.getFunction("objc_getProtocol").invokePointer(arrayOf("UNUserNotificationCenterDelegate"))
    if (proto != null) objcLib.getFunction("class_addProtocol").invokeInt(arrayOf(delegate, proto))
    objcLib.getFunction("class_addMethod").invokeInt(
        arrayOf(
            delegate,
            sel("userNotificationCenter:didReceiveNotificationResponse:withCompletionHandler:"),
            CallbackReference.getFunctionPointer(responseCb),
            "v@:@@@",
        ),
    )
    objcLib.getFunction("objc_registerClassPair").invoke(arrayOf(delegate))
    delegateInstance = send(delegate, "new")
    send(center(), "setDelegate:", delegateInstance)
    delegateRegistered = true
}

actual fun rescheduleDailyNotification(prefs: NotificationPrefs) {
    if (!prefs.enabled) {
        send(center(), "removeAllPendingNotificationRequests")
        return
    }
    ensureDelegate()
    scope.launch {
        if (!authorizationGranted()) return@launch   // 未授权:不排(授权请求只在开关开启时由 requestNotificationPermission 触发)
        // 幂等滚动窗口:每次启动/设置变更都清掉重排未来 7 天 —— 改时间/选词即刻生效,旧触发作废
        // (此前按 lastScheduledDay 推进,首次 0 → day=1 → 1970 年 → 全部立即触发,已实测无限通知)
        send(center(), "removeAllPendingNotificationRequests")
        val current = loadNotificationPrefs()
        val today = epochDay()
        // 滚动窗口:每次启动/设置变更从"今天(未过时刻)或明天"起排 7 天 —— 改时间/选词即刻生效;
        // lastScheduledDay 仅记录进度(不参与推进,推进式曾致 1970 立即触发无限通知)
        val now = java.util.Calendar.getInstance()
        val nowMinute = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val firstDay = if (nowMinute < current.hour * 60 + current.minute) today else today + 1
        var day = firstDay
        val lastDay = firstDay + WINDOW_DAYS - 1
        while (day <= lastDay) {
            val poem = pickRandomPoem() ?: return@launch
            val title = if (poem.authorName.isEmpty()) poem.rhythmic else "${poem.rhythmic} · ${poem.authorName}"
            val firstLine = poem.content.lineSequence().firstOrNull() ?: ""
            val content = allocInit("UNMutableNotificationContent")
            send(content, "setTitle:", nsString(title))
            send(content, "setBody:", nsString("「$firstLine」"))
            send(content, "setUserInfo:", nsDictOf("poemId", poem.id))
            val trigger = send(
                cls("UNCalendarNotificationTrigger"),
                "triggerWithDateMatchingComponents:repeats:",
                triggerDate((day - today).toInt(), current.hour, current.minute),
                0L,   // repeats = NO
            )
            val request = send(
                cls("UNNotificationRequest"),
                "requestWithIdentifier:content:trigger:", nsString("daily-poem-$day"), content, trigger,
            )
            send(center(), "addNotificationRequest:withCompletionHandler:", request, Pointer.NULL)
            day++
        }
        saveNotificationPrefs(current.copy(lastScheduledDay = lastDay))
    }
}

private fun nsDictOf(key: String, value: Long): Pointer? {
    val dict = send(cls("NSDictionary"), "dictionaryWithObject:forKey:", nsNumber(value), nsString(key))
    return send(dict, "retain")
}
