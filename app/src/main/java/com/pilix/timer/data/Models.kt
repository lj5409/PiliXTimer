package com.pilix.timer.data

import java.util.UUID

/** 分段结束时的提醒方式 */
enum class EndAlert {
    /** 3 秒环形 + 数字高亮脉冲（轻提醒，无声音） */
    PULSE,
    /** 声音提示分段结束 */
    SOUND,
    /** 无提醒 */
    NONE
}

/** 分段开始方式 */
enum class StartMode {
    /** 上一段结束后自动接续 */
    AUTO,
    /** 需手动点击开始（例如共鸣时间） */
    TAP
}

data class Segment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val minutes: Int,
    val colorIndex: Int,
    val endAlert: EndAlert = EndAlert.SOUND,
    val startMode: StartMode = StartMode.AUTO,
) {
    val durationMs: Long get() = minutes * 60_000L
}

data class Plan(
    val id: String,
    val name: String,
    val segments: List<Segment>,
    val builtin: Boolean = false,
) {
    val totalMs: Long get() = segments.sumOf { it.durationMs }
}

object Palette {
    val argb = intArrayOf(
        0xFF3B82F6.toInt(), // 蓝
        0xFFF59E0B.toInt(), // 琥珀
        0xFF10B981.toInt(), // 翠绿
        0xFF8B5CF6.toInt(), // 紫
        0xFFEF4444.toInt(), // 红
        0xFF06B6D4.toInt(), // 青
        0xFFF472B6.toInt(), // 粉
    )

    val names = listOf("蓝", "琥珀", "翠绿", "紫", "红", "青", "粉")

    fun of(index: Int): Int = argb[((index % argb.size) + argb.size) % argb.size]
}

/** 内置默认方案：陈述 5min → 延长 1min → 共鸣 2min */
fun defaultPlan(): Plan = Plan(
    id = "builtin-default",
    name = "标准 Update",
    builtin = true,
    segments = listOf(
        Segment(
            name = "陈述",
            minutes = 5,
            colorIndex = 0,
            endAlert = EndAlert.PULSE,
            startMode = StartMode.AUTO,
        ),
        Segment(
            name = "延长",
            minutes = 1,
            colorIndex = 1,
            endAlert = EndAlert.SOUND,
            startMode = StartMode.AUTO,
        ),
        Segment(
            name = "共鸣",
            minutes = 2,
            colorIndex = 2,
            endAlert = EndAlert.SOUND,
            startMode = StartMode.TAP,
        ),
    ),
)

data class AppSettings(
    val soundOn: Boolean = true,
    val vibrateOn: Boolean = true,
    val keepScreenOn: Boolean = true,
    val immersive: Boolean = true,
    /** 0 = 跟随系统, 1 = 深色, 2 = 浅色 */
    val themeMode: Int = 0,
)
