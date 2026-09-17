package com.songci.app.ui.components

/**
 * iOS:kheti 渲染启用。
 * 回退开关:kheti iOS target 未实测(kheti 仓库唯一剩余里程碑);若真机/模拟器出现渲染异常,
 * 将本行改为 `= false`,详情页自动回退 DetailLegacy.kt 旧路径(改动仅此一行)。
 */
actual fun useKhetiRendering(): Boolean = true
