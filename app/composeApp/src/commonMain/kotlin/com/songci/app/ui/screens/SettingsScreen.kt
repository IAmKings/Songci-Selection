package com.songci.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.songci.app.data.NotificationPrefs
import com.songci.app.data.notificationPermissionGranted
import com.songci.app.data.requestNotificationPermission
import com.songci.app.theme.SongciColors
import com.songci.app.ui.AppViewModel
import com.songci.app.ui.FontScale
import com.songci.app.ui.FontStyle
import com.songci.app.ui.components.SimpleListScreen
import com.songci.app.ui.components.TimePickerDialog

/** 设置:阅读设置(字号)+ 关于;账号/通知/退出登录为占位。 */
@Composable
fun SettingsScreen(vm: AppViewModel) {
    SimpleListScreen(title = "设置") {
        Text(
            "阅读设置",
            style = MaterialTheme.typography.labelMedium,
            color = SongciColors.stone,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            FontScale.entries.forEach { scale ->
                val selected = vm.fontScale == scale
                Text(
                    scale.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) SongciColors.onPrimary else SongciColors.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .border(1.dp, SongciColors.primary)
                        .background(if (selected) SongciColors.primary else SongciColors.surfaceContainerLow)
                        .clickable { vm.updateFontScale(scale) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        Text(
            "字体风格",
            style = MaterialTheme.typography.labelMedium,
            color = SongciColors.stone,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            FontStyle.entries.forEach { style ->
                val selected = vm.fontStyle == style
                Text(
                    style.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) SongciColors.onPrimary else SongciColors.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .border(1.dp, SongciColors.primary)
                        .background(if (selected) SongciColors.primary else SongciColors.surfaceContainerLow)
                        .clickable { vm.updateFontStyle(style) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        Text(
            "仅应用内生效(小组件使用系统字体)",
            style = MaterialTheme.typography.labelSmall,
            color = SongciColors.stone,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Text(
            "每日一词",
            style = MaterialTheme.typography.labelMedium,
            color = SongciColors.stone,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        val prefs = vm.notificationPrefs
        var showTimePicker by remember { mutableStateOf(false) }
        // 进入页面时判断通知权限;系统授权框返回(ON_RESUME)后刷新
        var permissionGranted by remember { mutableStateOf(notificationPermissionGranted()) }
        var pendingEnable by remember { mutableStateOf(false) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    permissionGranted = notificationPermissionGranted()
                    if (pendingEnable && permissionGranted) {   // 授权成功 → 补开开关(开启即等待授权的意图),内部触发 reschedule
                        pendingEnable = false
                        vm.updateNotificationPrefs(vm.notificationPrefs.copy(enabled = true))
                    } else {
                        pendingEnable = false   // 拒绝授权:开关保持关闭,引导行提示
                    }
                    // 注意:不再无条件 reschedule —— 每次聚焦(ON_RESUME)重排会与通知点击循环,造成重复/立即触发
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            // 开关(与字号按钮同款形态:边框 + 选中 primary 填充)
            Text(
                if (prefs.enabled) "开启" else "关闭",
                style = MaterialTheme.typography.labelLarge,
                color = if (prefs.enabled) SongciColors.onPrimary else SongciColors.primary,
                modifier = Modifier
                    .border(1.dp, SongciColors.primary)
                    .background(if (prefs.enabled) SongciColors.primary else SongciColors.surfaceContainerLow)
                    .clickable {
                        if (prefs.enabled) {
                            vm.updateNotificationPrefs(prefs.copy(enabled = false))
                        } else if (permissionGranted) {
                            vm.updateNotificationPrefs(prefs.copy(enabled = true))
                        } else {
                            pendingEnable = true   // 未授权:先弹系统授权框,授权返回后自动开启
                            requestNotificationPermission()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                "每日 ${prefs.hour.toString().padStart(2, '0')}:${prefs.minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelLarge,
                color = if (prefs.enabled) SongciColors.nearBlack else SongciColors.stone,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .border(1.dp, if (prefs.enabled) SongciColors.line else SongciColors.stone.copy(alpha = 0.4f))
                    .clickable(enabled = prefs.enabled) { showTimePicker = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (!permissionGranted) {
            // 未授权引导:可点击请求权限(进入页面即可见)
            Text(
                if (prefs.enabled) "通知权限未授予,无法推送 · 点击授予" else "未授予通知权限 · 点击请求,授权后即可开启",
                style = MaterialTheme.typography.labelSmall,
                color = if (prefs.enabled) SongciColors.error else SongciColors.stone,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable {
                        if (!prefs.enabled) pendingEnable = true
                        requestNotificationPermission()
                    },
            )
        } else {
            Text(
                "通知权限已授予",
                style = MaterialTheme.typography.labelSmall,
                color = SongciColors.stone,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        if (showTimePicker) {
            TimePickerDialog(
                initialHour = prefs.hour,
                initialMinute = prefs.minute,
                onConfirm = { hour, minute ->
                    showTimePicker = false
                    vm.updateNotificationPrefs(prefs.copy(hour = hour, minute = minute))
                },
                onDismiss = { showTimePicker = false },
            )
        }
        Text(
            "关于宋词选粹",
            style = MaterialTheme.typography.labelMedium,
            color = SongciColors.stone,
            modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 8.dp),
        )
        Text(
            "版本 0.1.0 · 词库 21,050 首 · 诗人 1,564 位",
            style = MaterialTheme.typography.bodyMedium,
            color = SongciColors.nearBlack,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "账号设置 / 通知设置 / 退出登录 —— 后续版本提供",
            style = MaterialTheme.typography.labelSmall,
            color = SongciColors.stone,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp),
        )
    }
}
