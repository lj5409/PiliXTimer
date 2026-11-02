package com.pilix.timer

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pilix.timer.data.AlertKind
import com.pilix.timer.data.AppSettings
import com.pilix.timer.data.EndAlert
import com.pilix.timer.data.Palette
import com.pilix.timer.data.Phase
import com.pilix.timer.data.Plan
import com.pilix.timer.data.Repo
import com.pilix.timer.data.Segment
import com.pilix.timer.data.SessionSnapshot
import com.pilix.timer.data.StartMode
import com.pilix.timer.data.defaultPlan
import com.pilix.timer.notify.Buzz
import com.pilix.timer.notify.SoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 计时引擎。
 *
 * 时间基准使用 [SystemClock.elapsedRealtime]，所有状态都是「按时间戳推算」而非「每次 tick 减一」，
 * 因此切后台、掉帧都不会造成漂移；切回前台后立即校正。
 */
class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repo.get(app)
    private val player = SoundPlayer(app)

    // ---------------- 对外状态 ----------------

    var plan by mutableStateOf(defaultPlan())
        private set

    /** 全部已保存方案 */
    var plans by mutableStateOf(repo.loadPlans())
        private set

    var phase by mutableStateOf(Phase.IDLE)
        private set
    var segIndex by mutableStateOf(0)
        private set

    /** 当前分段剩余毫秒 */
    var segRemainingMs by mutableStateOf(0L)
        private set

    /** 本轮已用时（毫秒），暂停时不走 */
    var activeMs by mutableStateOf(0L)
        private set

    /** 超出时间（毫秒），正计时 */
    var overtimeMs by mutableStateOf(0L)
        private set

    /** 总用时（毫秒），独立连续累计，从不暂停 */
    var totalMs by mutableStateOf(0L)
        private set

    var alert by mutableStateOf<AlertKind?>(null)
        private set

    var settings by mutableStateOf(repo.loadSettings())
        private set

    /** 供 UI 使用的当前分段 */
    val currentSegment: Segment? get() = plan.segments.getOrNull(segIndex)

    val currentAccent: Int get() = Palette.of(currentSegment?.colorIndex ?: 0)

    // ---------------- 引擎内部 ----------------

    private var segElapsedMs = 0L
    private var totalArmed = false
    private var lastTick = SystemClock.elapsedRealtime()
    private var tickerJob: Job? = null
    private var alertJob: Job? = null
    private var tickCount = 0

    init {
        val plans = repo.loadPlans()
        val activeId = repo.activePlanId()
        plan = plans.firstOrNull { it.id == activeId }
            ?: plans.firstOrNull { it.builtin }
            ?: defaultPlan()
        restore(repo.loadSnapshot())
        publish()
        startTicker()
    }

    // ---------------- 时钟 ----------------

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val dt = (now - lastTick).coerceAtLeast(0L)
                lastTick = now
                if (dt > 0) tick(dt)
                delay(20)
            }
        }
    }

    private fun tick(dt: Long) {
        // 总用时：一旦启用就永不停歇
        if (totalArmed) totalMs += dt

        when (phase) {
            Phase.RUNNING -> {
                segElapsedMs += dt
                activeMs += dt
                val seg = currentSegment
                if (seg != null && segElapsedMs >= seg.durationMs) {
                    onSegmentFinished(seg)
                }
            }

            Phase.WAITING -> {
                // 等待手动开始下一段：本轮仍在继续，超时正计时同时走
                activeMs += dt
                overtimeMs += dt
            }

            else -> Unit
        }

        publish()

        if (++tickCount % 100 == 0) persist()
    }

    private fun onSegmentFinished(seg: Segment) {
        when (seg.endAlert) {
            EndAlert.PULSE -> {
                if (settings.vibrateOn) Buzz.play(getApplication(), Buzz.Style.LIGHT)
                showAlert(AlertKind.Pulse())
            }

            EndAlert.SOUND -> {
                if (settings.soundOn) {
                    player.play()
                    if (settings.vibrateOn) Buzz.play(getApplication(), Buzz.Style.STRONG)
                } else {
                    // 静音状态：用 5 秒急促醒目的屏幕变化代替声音
                    if (settings.vibrateOn) Buzz.play(getApplication(), Buzz.Style.STRONG)
                    showAlert(AlertKind.ScreenFlash())
                }
            }

            EndAlert.NONE -> Unit
        }
        advance()
    }

    /** 进入下一分段 */
    private fun advance() {
        val next = segIndex + 1
        if (next >= plan.segments.size) {
            phase = Phase.DONE
            publish()
            persist()
            return
        }
        val nextSeg = plan.segments[next]
        segIndex = next
        segElapsedMs = 0
        if (nextSeg.startMode == StartMode.AUTO) {
            phase = Phase.RUNNING
        } else {
            // 需手动点击开始（如共鸣时间），此时显示正计时
            phase = Phase.WAITING
            overtimeMs = 0
        }
        publish()
        persist()
    }

    private fun publish() {
        val seg = currentSegment
        val duration = seg?.durationMs ?: 0L
        segRemainingMs = when (phase) {
            Phase.RUNNING, Phase.PAUSED -> (duration - segElapsedMs).coerceAtLeast(0L)
            else -> duration
        }
    }

    // ---------------- 用户操作 ----------------

    /** 开始 / 暂停 按钮 */
    fun onPrimary() {
        when (phase) {
            Phase.IDLE -> startSegment(0)
            Phase.RUNNING -> {
                phase = Phase.PAUSED
                publish()
                persist()
            }

            Phase.PAUSED -> {
                phase = Phase.RUNNING
                publish()
                persist()
            }

            Phase.WAITING -> startSegment(segIndex)
            Phase.DONE -> resetToReady()
        }
    }

    private fun startSegment(index: Int) {
        segIndex = index.coerceIn(0, (plan.segments.size - 1).coerceAtLeast(0))
        segElapsedMs = 0
        phase = Phase.RUNNING
        totalArmed = true
        publish()
        persist()
    }

    /** 刷新：回到计时起始准备状态（总用时不清零） */
    fun resetToReady() {
        phase = Phase.IDLE
        segIndex = 0
        segElapsedMs = 0
        activeMs = 0
        overtimeMs = 0
        alert = null
        alertJob?.cancel()
        publish()
        persist()
    }

    /** 点击总用时：归零并重新计时 */
    fun resetTotal() {
        totalMs = 0
        totalArmed = true
        persist()
    }

    // ---------------- 提醒 ----------------

    private fun showAlert(kind: AlertKind) {
        alert = kind
        alertJob?.cancel()
        alertJob = viewModelScope.launch {
            delay(kind.durationMs)
            if (alert === kind) alert = null
        }
    }

    fun dismissAlert() {
        alertJob?.cancel()
        alert = null
    }

    // ---------------- 方案 ----------------

    fun selectPlan(target: Plan) {
        plan = target
        repo.setActivePlanId(target.id)
        resetToReady()
    }

    /** 保存方案：同 id 覆盖，否则新增 */
    fun savePlan(target: Plan) {
        val all = repo.loadPlans().toMutableList()
        val index = all.indexOfFirst { it.id == target.id }
        if (index >= 0) all[index] = target else all.add(target)
        repo.savePlans(all)
        plans = all
        plan = target
        repo.setActivePlanId(target.id)
        resetToReady()
    }

    fun deletePlan(id: String) {
        val all = repo.loadPlans()
        if (all.size <= 1) return
        val target = all.firstOrNull { it.id == id } ?: return
        if (target.builtin) return
        val rest = all.filterNot { it.id == id }
        repo.savePlans(rest)
        plans = rest
        if (plan.id == id) {
            plan = rest.firstOrNull { it.builtin } ?: rest.first()
            repo.setActivePlanId(plan.id)
            resetToReady()
        }
    }

    fun duplicatePlan(source: Plan, newName: String): Plan {
        val copy = source.copy(
            id = "plan-" + System.currentTimeMillis(),
            name = newName,
            builtin = false,
            segments = source.segments.map {
                it.copy(id = "seg-" + System.currentTimeMillis() + "-" + it.name)
            },
        )
        savePlan(copy)
        return copy
    }

    // ---------------- 设置 ----------------

    fun updateSettings(next: AppSettings) {
        settings = next
        repo.saveSettings(next)
    }

    fun toggleSound() = updateSettings(settings.copy(soundOn = !settings.soundOn))

    fun toggleVibrate() = updateSettings(settings.copy(vibrateOn = !settings.vibrateOn))

    // ---------------- 持久化 ----------------

    private fun persist() {
        repo.saveSnapshot(
            SessionSnapshot(
                phase = phase,
                segIndex = segIndex,
                segElapsedMs = segElapsedMs,
                activeMs = activeMs,
                overtimeMs = overtimeMs,
                totalMs = totalMs,
                totalArmed = totalArmed,
                planId = plan.id,
                savedAtElapsed = SystemClock.elapsedRealtime(),
            )
        )
    }

    private fun restore(snapshot: SessionSnapshot?) {
        if (snapshot == null) return
        if (snapshot.planId != plan.id) return
        phase = snapshot.phase
        segIndex = snapshot.segIndex.coerceIn(0, (plan.segments.size - 1).coerceAtLeast(0))

        // 进程被杀死期间流失的时间要补回来，否则恢复后会比真实时间慢
        val gap = if (snapshot.savedAtElapsed > 0L) {
            (SystemClock.elapsedRealtime() - snapshot.savedAtElapsed).coerceAtLeast(0L)
        } else {
            0L
        }

        segElapsedMs = snapshot.segElapsedMs + if (phase == Phase.RUNNING) gap else 0L
        activeMs = snapshot.activeMs +
            if (phase == Phase.RUNNING || phase == Phase.WAITING) gap else 0L
        overtimeMs = snapshot.overtimeMs + if (phase == Phase.WAITING) gap else 0L
        totalMs = snapshot.totalMs + if (snapshot.totalArmed) gap else 0L
        totalArmed = snapshot.totalArmed
    }

    override fun onCleared() {
        tickerJob?.cancel()
        player.release()
        super.onCleared()
    }
}
