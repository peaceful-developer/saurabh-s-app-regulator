package com.saurabh.sar.data

import androidx.room.withTransaction
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.Run
import com.saurabh.sar.core.model.ScheduleEntry
import com.saurabh.sar.core.modes.ModeBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Everything about modes in one object, for the UI. */
data class ModeWithDetails(val mode: Mode, val schedule: List<ScheduleEntry>, val rules: List<AppRule>)

class SarRepository(private val db: SarDatabase) {
    private val modeDao = db.modeDao()
    private val runDao = db.runDao()

    val modes: Flow<List<ModeWithDetails>> = combine(
        modeDao.observeModes(), modeDao.observeSchedules(), modeDao.observeRules(),
    ) { modes, schedules, rules ->
        val s = schedules.groupBy { it.modeId }
        val r = rules.groupBy { it.modeId }
        modes.map { m ->
            ModeWithDetails(m.toModel(), s[m.id].orEmpty().map { it.toModel() }, r[m.id].orEmpty().map { it.toModel() })
        }
    }

    val activeRun: Flow<Run?> = runDao.observeActiveRun().map { it?.toModel() }

    suspend fun allModes(): List<Mode> = modeDao.modes().map { it.toModel() }
    suspend fun mode(id: Long): Mode? = modeDao.mode(id)?.toModel()
    suspend fun schedulesByMode(): Map<Long, List<ScheduleEntry>> =
        modeDao.schedules().map { it.toModel() }.groupBy { it.modeId }
    suspend fun rulesFor(modeId: Long): List<AppRule> = modeDao.rulesFor(modeId).map { it.toModel() }
    suspend fun modeCount() = modeDao.count()

    suspend fun saveMode(bundle: ModeBundle): Long = modeDao.saveBundle(
        bundle.mode.toEntity(),
        bundle.schedule.map { it.toEntity(bundle.mode.id) },
        bundle.rules.map { it.toEntity(bundle.mode.id) },
    )

    suspend fun saveModes(bundles: List<ModeBundle>) = db.withTransaction { bundles.forEach { saveMode(it) } }

    suspend fun deleteMode(id: Long) = modeDao.deleteMode(id)
    suspend fun removePackage(pkg: String) = modeDao.deleteRulesForPackage(pkg)
    suspend fun pruneUninstalled(installed: List<String>) {
        if (installed.isNotEmpty()) modeDao.deleteRulesNotIn(installed)
    }

    suspend fun activeRunNow(): Run? = runDao.activeRun()?.toModel()
    suspend fun lastRunStarts(): Map<Long, Long> = runDao.lastStarts().associate { it.modeId to it.startedAt }
    suspend fun usageFor(runId: Long): List<AppUsage> = runDao.usageFor(runId).map { it.toModel() }
    fun observeUsageFor(runId: Long): Flow<List<AppUsage>> = runDao.observeUsageFor(runId).map { l -> l.map { it.toModel() } }

    suspend fun insertRun(run: Run, modeName: String): Run {
        val id = runDao.insertRun(run.toEntity(modeName).copy(id = 0))
        return run.copy(id = id)
    }

    suspend fun saveRun(run: Run, modeName: String, usage: Collection<AppUsage> = emptyList()) =
        runDao.saveRunAndUsage(run.toEntity(modeName), usage.map { it.toEntity() })

    suspend fun saveUsage(usage: Collection<AppUsage>) = runDao.saveRunAndUsage(null, usage.map { it.toEntity() })

    fun observeRunsSince(from: Long): Flow<List<RunEntity>> = runDao.observeRunsSince(from)
    fun observeUsageSince(from: Long): Flow<List<AppUsage>> = runDao.observeUsageSince(from).map { l -> l.map { it.toModel() } }

    suspend fun deleteHistoryBefore(before: Long) = runDao.deleteEndedBefore(before)
    suspend fun clearHistory() = runDao.deleteAllEnded()
}
