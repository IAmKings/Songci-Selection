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

private fun allocInit(className: String): Pointer? = send(send(cls(className), "alloc"), "init")

// ---- ObjC block 手工构造(最小布局:isa/flags/reserved/invoke/descriptor;调用方只走 invoke)----
class ObjCBlock(fn: Callback) : Structure() {
    @JvmField var isa: Pointer? = objcLib.getGlobalVariableAddress("_NSConcreteStackBlock")
    @JvmField var flags: Int = 0
    @JvmField var reserved: Int = 0
    @JvmField var invoke: Pointer? = CallbackReference.getFunctionPointer(fn)
    @JvmField var descriptor: Pointer? = null

    override fun getFieldOrder(): List<String> = listOf("isa", "flags", "reserved", "invoke", "descriptor")
}

/** 授权回调: void^(BOOL granted, NSError*);block invoke 首参为 block 自身。 */
class GrantBlock : Callback {
    // @JvmField:公开字段不算 public 方法(JNA 要求 Callback 类仅一个 public 方法=invoke)
    @JvmField var granted = false
    fun invoke(block: Pointer?, grantedFlag: Byte, error: Pointer?) {
        granted = grantedFlag != 0.toByte()
    }
}

/** 设置回调: void^(UNNotificationSettings*);授权状态读出(同步回调)。 */
class SettingsBlock : Callback {
    @JvmField var status = -1L
    fun invoke(block: Pointer?, settings: Pointer?) {
        if (settings != null) status = send(settings, "authorizationStatus")?.getLong(0) ?: -1L
    }
}

private val settingsCb = SettingsBlock()
private val settingsBlock = ObjCBlock(settingsCb)

private fun center(): Pointer? = send(cls("UNUserNotificationCenter"), "currentNotificationCenter")

actual fun requestNotificationPermission() {
    // 用户主动开启开关时调用(系统弹授权框)
    val grantCb = GrantBlock()
    send(center(), "requestAuthorizationWithOptions:completionHandler:", 7L, ObjCBlock(grantCb).pointer)
}

/** UNAuthorizationStatus.Authorized = 2。 */
private fun authorizationGranted(): Boolean {
    send(center(), "getNotificationSettingsWithCompletionHandler:", settingsBlock.pointer)
    return settingsCb.status == 2L
}

private fun epochDay(): Long = Date().time / 86_400_000L

private fun triggerDate(daysFromNow: Int, hour: Int, minute: Int): Pointer? {
    val cal = send(cls("NSCalendar"), "currentCalendar")
    val future = send(cal, "dateByAddingUnit:value:toDate:options:", 1L, daysFromNow.toLong(), send(cls("NSDate"), "date"), 0L)
    val comps = send(cal, "components:fromDate:", (0x0E or 0x10).toLong(), future)   // NSYear|NSMonth|NSDay
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
        val content = send(response, "request")?.let { send(it, "content") } ?: return
        val userInfo = send(content, "userInfo") ?: return
        val poemId = send(userInfo, "objectForKey:", nsString("poemId"))?.let { send(it, "longLongValue")?.getLong(0) } ?: 0L
        if (poemId > 0) macDeepLinkChannel?.trySend(poemId)
        // ponytail: completionHandler 不回调(block 调用桥未做,先跑通主链路;系统日志警告可接受)
    }
}

private var delegateRegistered = false

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
            CallbackReference.getFunctionPointer(ResponseCallback()),
            "v@:@@@",
        ),
    )
    objcLib.getFunction("objc_registerClassPair").invoke(arrayOf(delegate))
    send(center(), "setDelegate:", send(delegate, "new"))
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
        val current = loadNotificationPrefs()
        var scheduled = current.lastScheduledDay
        val today = epochDay()
        while (scheduled < today + WINDOW_DAYS) {
            val day = scheduled + 1
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
            scheduled = day
        }
        saveNotificationPrefs(current.copy(lastScheduledDay = scheduled))
    }
}

private fun nsDictOf(key: String, value: Long): Pointer? {
    val dict = send(cls("NSDictionary"), "dictionaryWithObject:forKey:", nsNumber(value), nsString(key))
    return send(dict, "retain")
}
