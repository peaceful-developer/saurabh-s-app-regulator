package com.saurabh.sar.platform

import android.net.Uri
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.EmergencyEffort
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.model.ScheduleEntry
import com.saurabh.sar.core.modes.ModeBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Export / import modes as JSON through the system file picker (FSD 9.3). No network. */
object Backup {
    suspend fun export(app: SarApp, uri: Uri) {
        val modes = app.repository.modes.first()
        val arr = JSONArray()
        modes.forEach { m ->
            val o = JSONObject()
                .put("name", m.mode.name).put("type", m.mode.type.name).put("message", m.mode.message)
                .put("icon", m.mode.icon).put("image", m.mode.imagePath?.takeIf { it.startsWith("builtin:") } ?: JSONObject.NULL)
                .put("emergencyEnabled", m.mode.emergencyEnabled).put("emergencyMinutes", m.mode.emergencyMinutes)
                .put("emergencyUses", m.mode.emergencyUses).put("emergencyEffort", m.mode.emergencyEffort.name)
                .put("emergencyWaitSec", m.mode.emergencyWaitSec).put("emergencyPhrase", m.mode.emergencyPhrase)
                .put("schedule", JSONArray().apply {
                    m.schedule.forEach { put(JSONObject().put("days", it.daysMask).put("start", it.startMinute).put("end", it.endMinute)) }
                })
                .put("rules", JSONArray().apply {
                    m.rules.forEach { r ->
                        put(JSONObject().put("package", r.packageName).put("type", r.type.name)
                            .put("allowanceMin", r.allowanceMin ?: JSONObject.NULL).put("capMin", r.capMin ?: JSONObject.NULL)
                            .put("breakMin", r.breakMin ?: JSONObject.NULL))
                    }
                })
            arr.put(o)
        }
        val text = JSONObject().put("app", "SAR").put("version", 1).put("modes", arr).toString(2)
        withContext(Dispatchers.IO) {
            app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
        }
    }

    /** Adds the file's modes; a name that already exists gets " (2)" etc. Returns how many were added. */
    suspend fun import(app: SarApp, uri: Uri): Int {
        val text = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
        val root = JSONObject(text)
        require(root.optString("app") == "SAR") { "not a SAR backup" }
        val existing = app.repository.allModes().map { it.name.lowercase() }.toMutableSet()
        val installed = app.installedApps.list().map { it.packageName }.toSet()
        val arr = root.getJSONArray("modes")
        val bundles = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            var name = o.getString("name").take(Mode.NAME_MAX)
            var n = 2
            while (name.lowercase() in existing) { name = "${o.getString("name").take(Mode.NAME_MAX - 4)} ($n)"; n++ }
            existing += name.lowercase()
            fun intOrNull(j: JSONObject, k: String) = if (j.isNull(k)) null else j.getInt(k)
            val mode = Mode(
                name = name, type = ModeType.valueOf(o.getString("type")), message = o.getString("message").take(Mode.MESSAGE_MAX),
                imagePath = if (o.isNull("image")) "builtin:study" else o.getString("image"), icon = o.optString("icon", "layers"),
                emergencyEnabled = o.getBoolean("emergencyEnabled"), emergencyMinutes = o.getInt("emergencyMinutes"),
                emergencyUses = o.getInt("emergencyUses"), emergencyEffort = EmergencyEffort.valueOf(o.getString("emergencyEffort")),
                emergencyWaitSec = o.getInt("emergencyWaitSec"), emergencyPhrase = o.getString("emergencyPhrase"),
                sortOrder = 100 + i,
            )
            val sched = o.getJSONArray("schedule").let { s -> (0 until s.length()).map { s.getJSONObject(it) }.map {
                ScheduleEntry(daysMask = it.getInt("days"), startMinute = it.getInt("start"), endMinute = it.getInt("end"))
            } }
            val rules = o.getJSONArray("rules").let { s -> (0 until s.length()).map { s.getJSONObject(it) }.map {
                AppRule(packageName = it.getString("package"), type = RuleType.valueOf(it.getString("type")),
                    allowanceMin = intOrNull(it, "allowanceMin"), capMin = intOrNull(it, "capMin"), breakMin = intOrNull(it, "breakMin"))
            }.filter { it.packageName in installed } }
            ModeBundle(mode, sched, rules)
        }
        app.repository.saveModes(bundles)
        app.controller.onModesChanged()
        return bundles.size
    }
}
