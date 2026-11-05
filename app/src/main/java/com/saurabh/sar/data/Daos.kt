package com.saurabh.sar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {
    @Query("SELECT * FROM mode ORDER BY sortOrder, id")
    fun observeModes(): Flow<List<ModeEntity>>

    @Query("SELECT * FROM mode ORDER BY sortOrder, id")
    suspend fun modes(): List<ModeEntity>

    @Query("SELECT * FROM mode WHERE id = :id")
    suspend fun mode(id: Long): ModeEntity?

    @Query("SELECT * FROM schedule_entry")
    fun observeSchedules(): Flow<List<ScheduleEntryEntity>>

    @Query("SELECT * FROM schedule_entry")
    suspend fun schedules(): List<ScheduleEntryEntity>

    @Query("SELECT * FROM schedule_entry WHERE modeId = :modeId")
    suspend fun schedulesFor(modeId: Long): List<ScheduleEntryEntity>

    @Query("SELECT * FROM app_rule")
    fun observeRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rule WHERE modeId = :modeId")
    suspend fun rulesFor(modeId: Long): List<AppRuleEntity>

    @Insert
    suspend fun insertMode(mode: ModeEntity): Long

    @Upsert
    suspend fun upsertMode(mode: ModeEntity): Long

    @Query("DELETE FROM mode WHERE id = :id")
    suspend fun deleteMode(id: Long)

    @Query("DELETE FROM schedule_entry WHERE modeId = :modeId")
    suspend fun clearSchedules(modeId: Long)

    @Insert
    suspend fun insertSchedules(entries: List<ScheduleEntryEntity>)

    @Query("DELETE FROM app_rule WHERE modeId = :modeId")
    suspend fun clearRules(modeId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<AppRuleEntity>)

    @Query("DELETE FROM app_rule WHERE packageName = :packageName")
    suspend fun deleteRulesForPackage(packageName: String)

    @Query("DELETE FROM app_rule WHERE packageName NOT IN (:installed)")
    suspend fun deleteRulesNotIn(installed: List<String>)

    @Query("SELECT COUNT(*) FROM mode")
    suspend fun count(): Int

    /** Saves a mode with its full schedule and rule set in one transaction. Returns the id. */
    @Transaction
    suspend fun saveBundle(mode: ModeEntity, schedule: List<ScheduleEntryEntity>, rules: List<AppRuleEntity>): Long {
        val id = if (mode.id == 0L) insertMode(mode) else { upsertMode(mode); mode.id }
        clearSchedules(id)
        if (schedule.isNotEmpty()) insertSchedules(schedule.map { it.copy(id = 0, modeId = id) })
        clearRules(id)
        if (rules.isNotEmpty()) insertRules(rules.map { it.copy(modeId = id) })
        return id
    }
}

@Dao
interface RunDao {
    @Query("SELECT * FROM run WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeRun(): RunEntity?

    @Query("SELECT * FROM run WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveRun(): Flow<RunEntity?>

    @Query("SELECT modeId, MAX(startedAt) AS startedAt FROM run GROUP BY modeId")
    suspend fun lastStarts(): List<LastStart>

    @Query("SELECT * FROM run WHERE startedAt >= :from ORDER BY startedAt DESC")
    fun observeRunsSince(from: Long): Flow<List<RunEntity>>

    @Query("SELECT * FROM run_app_usage WHERE runId IN (SELECT id FROM run WHERE startedAt >= :from)")
    fun observeUsageSince(from: Long): Flow<List<RunAppUsageEntity>>

    @Query("SELECT * FROM run_app_usage WHERE runId = :runId")
    suspend fun usageFor(runId: Long): List<RunAppUsageEntity>

    @Query("SELECT * FROM run_app_usage WHERE runId = :runId")
    fun observeUsageFor(runId: Long): Flow<List<RunAppUsageEntity>>

    @Insert
    suspend fun insertRun(run: RunEntity): Long

    @Upsert
    suspend fun upsertRun(run: RunEntity)

    @Upsert
    suspend fun upsertUsage(usage: List<RunAppUsageEntity>)

    @Query("DELETE FROM run WHERE endedAt IS NOT NULL AND endedAt < :before")
    suspend fun deleteEndedBefore(before: Long)

    @Query("DELETE FROM run WHERE endedAt IS NOT NULL")
    suspend fun deleteAllEnded()

    /** Run end (or an app switch) writes everything in one transaction (FSD 11.3). */
    @Transaction
    suspend fun saveRunAndUsage(run: RunEntity?, usage: List<RunAppUsageEntity>) {
        if (run != null) upsertRun(run)
        if (usage.isNotEmpty()) upsertUsage(usage)
    }
}

data class LastStart(val modeId: Long, val startedAt: Long)
