package com.pilix.timer.data

/** 计时器阶段 */
enum class Phase {
    /** 准备态：等待第一次开始 */
    IDLE,

    /** 倒计时中 */
    RUNNING,

    /** 已暂停 */
    PAUSED,

    /** 等待手动开始下一段（例如共鸣时间），此时显示正计时（超时） */
    WAITING,

    /** 本轮结束 */
    DONE
}

/** 进程被杀后可恢复的计时快照 */
data class SessionSnapshot(
    val phase: Phase,
    val segIndex: Int,
    val segElapsedMs: Long,
    val activeMs: Long,
    val overtimeMs: Long,
    val totalMs: Long,
    val totalArmed: Boolean,
    val planId: String,
    /** 保存快照时的 elapsedRealtime，恢复时用于补齐被中断的那段时间 */
    val savedAtElapsed: Long = 0L,
)

/** 瞬时提醒 */
sealed interface AlertKind {
    /** 提醒持续时长 */
    val durationMs: Long

    /** 3 秒环形与数字高亮脉冲（轻提醒） */
    data class Pulse(override val durationMs: Long = 3_000L) : AlertKind

    /** 静音状态下的 5 秒急促屏幕变化 */
    data class ScreenFlash(override val durationMs: Long = 5_000L) : AlertKind
}
