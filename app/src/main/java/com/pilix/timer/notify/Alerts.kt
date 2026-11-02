package com.pilix.timer.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.pilix.timer.R

/**
 * 提示音播放器。
 *
 * 使用 USAGE_ALARM（闹钟流）而非 USAGE_NOTIFICATION：
 * 闹钟流不受系统勿扰模式静音影响，保证会议现场一定能听到分段结束提示。
 */
class SoundPlayer(context: Context) {

    private val pool: SoundPool
    private var soundId: Int = 0

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()
        soundId = pool.load(context.applicationContext, R.raw.chime, 1)
    }

    fun play() {
        if (soundId == 0) return
        pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}

/** 震动反馈，同样以闹钟用途登记，尽量穿透勿扰模式 */
object Buzz {

    enum class Style { LIGHT, STRONG }

    fun play(context: Context, style: Style) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val effect = when (style) {
            Style.LIGHT -> VibrationEffect.createOneShot(45, 110)
            Style.STRONG -> VibrationEffect.createWaveform(
                longArrayOf(0, 90, 70, 150),
                intArrayOf(0, 210, 0, 255),
                -1,
            )
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        } catch (_: Exception) {
            // 忽略震动失败
        }
    }

    private fun vibrator(context: Context): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }
}
