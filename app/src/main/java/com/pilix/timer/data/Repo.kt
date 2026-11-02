package com.pilix.timer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 轻量本地存储：方案列表、当前方案、设置、以及可恢复的计时快照。
 * 使用 SharedPreferences + org.json，避免引入额外依赖。
 */
class Repo private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pilix_timer", Context.MODE_PRIVATE)

    // ---------------- 方案 ----------------

    fun loadPlans(): List<Plan> {
        val raw = prefs.getString(KEY_PLANS, null)
        if (raw.isNullOrEmpty()) return listOf(defaultPlan())
        return try {
            val arr = JSONArray(raw)
            val list = (0 until arr.length()).map { i -> planFromJson(arr.getJSONObject(i)) }
            if (list.isEmpty()) listOf(defaultPlan()) else list
        } catch (_: Exception) {
            listOf(defaultPlan())
        }
    }

    fun savePlans(plans: List<Plan>) {
        val arr = JSONArray()
        plans.forEach { arr.put(planToJson(it)) }
        prefs.edit().putString(KEY_PLANS, arr.toString()).apply()
    }

    fun activePlanId(): String? = prefs.getString(KEY_ACTIVE_PLAN, null)

    fun setActivePlanId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_PLAN, id).apply()
    }

    // ---------------- 设置 ----------------

    fun loadSettings(): AppSettings = AppSettings(
        soundOn = prefs.getBoolean(KEY_SOUND, true),
        vibrateOn = prefs.getBoolean(KEY_VIBRATE, true),
        keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN, true),
        immersive = prefs.getBoolean(KEY_IMMERSIVE, true),
        themeMode = prefs.getInt(KEY_THEME, 0),
    )

    fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_SOUND, s.soundOn)
            .putBoolean(KEY_VIBRATE, s.vibrateOn)
            .putBoolean(KEY_KEEP_SCREEN, s.keepScreenOn)
            .putBoolean(KEY_IMMERSIVE, s.immersive)
            .putInt(KEY_THEME, s.themeMode)
            .apply()
    }

    // ---------------- 计时快照（进程被杀后可恢复） ----------------

    fun saveSnapshot(s: SessionSnapshot) {
        prefs.edit()
            .putString(KEY_SNAPSHOT, JSONObject().apply {
                put("phase", s.phase.name)
                put("segIndex", s.segIndex)
                put("segElapsedMs", s.segElapsedMs)
                put("activeMs", s.activeMs)
                put("overtimeMs", s.overtimeMs)
                put("totalMs", s.totalMs)
                put("totalArmed", s.totalArmed)
                put("planId", s.planId)
                put("savedAtElapsed", s.savedAtElapsed)
            }.toString())
            .apply()
    }

    fun loadSnapshot(): SessionSnapshot? {
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        return try {
            val o = JSONObject(raw)
            SessionSnapshot(
                phase = Phase.valueOf(o.getString("phase")),
                segIndex = o.getInt("segIndex"),
                segElapsedMs = o.getLong("segElapsedMs"),
                activeMs = o.getLong("activeMs"),
                overtimeMs = o.getLong("overtimeMs"),
                totalMs = o.getLong("totalMs"),
                totalArmed = o.getBoolean("totalArmed"),
                planId = o.optString("planId"),
                savedAtElapsed = o.optLong("savedAtElapsed", 0L),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearSnapshot() {
        prefs.edit().remove(KEY_SNAPSHOT).apply()
    }

    // ---------------- JSON ----------------

    private fun planToJson(p: Plan): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("builtin", p.builtin)
        put("segments", JSONArray().apply {
            p.segments.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("minutes", s.minutes)
                    put("colorIndex", s.colorIndex)
                    put("endAlert", s.endAlert.name)
                    put("startMode", s.startMode.name)
                })
            }
        })
    }

    private fun planFromJson(o: JSONObject): Plan {
        val segsArr = o.optJSONArray("segments") ?: JSONArray()
        val segs = (0 until segsArr.length()).map { i ->
            val s = segsArr.getJSONObject(i)
            Segment(
                id = s.optString("id", UUID.randomUUID().toString()),
                name = s.optString("name", "分段"),
                minutes = s.optInt("minutes", 1).coerceIn(1, 120),
                colorIndex = s.optInt("colorIndex", 0),
                endAlert = runCatching { EndAlert.valueOf(s.optString("endAlert")) }
                    .getOrDefault(EndAlert.SOUND),
                startMode = runCatching { StartMode.valueOf(s.optString("startMode")) }
                    .getOrDefault(StartMode.AUTO),
            )
        }
        return Plan(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", "方案"),
            segments = segs,
            builtin = o.optBoolean("builtin", false),
        )
    }

    companion object {
        private const val KEY_PLANS = "plans"
        private const val KEY_ACTIVE_PLAN = "active_plan"
        private const val KEY_SOUND = "sound_on"
        private const val KEY_VIBRATE = "vibrate_on"
        private const val KEY_KEEP_SCREEN = "keep_screen_on"
        private const val KEY_IMMERSIVE = "immersive"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_SNAPSHOT = "session_snapshot"

        @Volatile
        private var instance: Repo? = null

        fun get(context: Context): Repo =
            instance ?: synchronized(this) {
                instance ?: Repo(context).also { instance = it }
            }
    }
}
