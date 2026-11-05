package com.saurabh.sar.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.EmergencyEffort
import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.Run
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.model.ScheduleEntry

@Entity(tableName = "mode")
data class ModeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: ModeType,
    val message: String,
    val imagePath: String?,
    val icon: String,
    val emergencyEnabled: Boolean,
    val emergencyMinutes: Int,
    val emergencyUses: Int,
    val emergencyEffort: EmergencyEffort,
    val emergencyWaitSec: Int,
    val emergencyPhrase: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "schedule_entry",
    foreignKeys = [ForeignKey(entity = ModeEntity::class, parentColumns = ["id"], childColumns = ["modeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("modeId")],
)
data class ScheduleEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeId: Long,
    val daysMask: Int,
    val startMinute: Int,
    val endMinute: Int,
)

@Entity(
    tableName = "app_rule",
    primaryKeys = ["modeId", "packageName"],
    foreignKeys = [ForeignKey(entity = ModeEntity::class, parentColumns = ["id"], childColumns = ["modeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("packageName")],
)
data class AppRuleEntity(
    val modeId: Long,
    val packageName: String,
    val type: RuleType,
    val allowanceMin: Int?,
    val capMin: Int?,
    val breakMin: Int?,
)

/** Runs keep their mode id but no foreign key, so history survives deleting a mode. */
@Entity(tableName = "run", indices = [Index("modeId"), Index("endedAt")])
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeId: Long,
    /** Copy of the mode's name at start, for history after the mode is deleted or renamed. */
    val modeName: String,
    val startedAt: Long,
    val plannedEndAt: Long?,
    val locked: Boolean,
    val scheduled: Boolean,
    val endedAt: Long?,
    val endReason: EndReason?,
    val emergencyUsesUsed: Int,
)

@Entity(
    tableName = "run_app_usage",
    primaryKeys = ["runId", "packageName"],
    foreignKeys = [ForeignKey(entity = RunEntity::class, parentColumns = ["id"], childColumns = ["runId"], onDelete = ForeignKey.CASCADE)],
)
data class RunAppUsageEntity(
    val runId: Long,
    val packageName: String,
    val usedMs: Long,
    val stretchUsedMs: Long,
    val lastForegroundEndAt: Long?,
    val breakUntil: Long?,
    val emergencyUntil: Long?,
)

fun ModeEntity.toModel() = Mode(id, name, type, message, imagePath, icon, emergencyEnabled, emergencyMinutes,
    emergencyUses, emergencyEffort, emergencyWaitSec, emergencyPhrase, sortOrder)
fun Mode.toEntity() = ModeEntity(id, name.trim(), type, message.trim(), imagePath, icon, emergencyEnabled, emergencyMinutes,
    emergencyUses, emergencyEffort, emergencyWaitSec, emergencyPhrase, sortOrder)
fun ScheduleEntryEntity.toModel() = ScheduleEntry(id, modeId, daysMask, startMinute, endMinute)
fun ScheduleEntry.toEntity(modeId: Long) = ScheduleEntryEntity(0, modeId, daysMask, startMinute, endMinute)
fun AppRuleEntity.toModel() = AppRule(modeId, packageName, type, allowanceMin, capMin, breakMin)
fun AppRule.toEntity(modeId: Long) = AppRuleEntity(modeId, packageName, type,
    allowanceMin.takeIf { type == RuleType.LIMIT }, capMin.takeIf { type == RuleType.LIMIT }, breakMin.takeIf { type == RuleType.LIMIT })
fun RunEntity.toModel() = Run(id, modeId, startedAt, plannedEndAt, locked, scheduled, endedAt, endReason, emergencyUsesUsed)
fun Run.toEntity(modeName: String) = RunEntity(id, modeId, modeName, startedAt, plannedEndAt, locked, scheduled, endedAt, endReason, emergencyUsesUsed)
fun RunAppUsageEntity.toModel() = AppUsage(runId, packageName, usedMs, stretchUsedMs, lastForegroundEndAt, breakUntil, emergencyUntil)
fun AppUsage.toEntity() = RunAppUsageEntity(runId, packageName, usedMs, stretchUsedMs, lastForegroundEndAt, breakUntil, emergencyUntil)
