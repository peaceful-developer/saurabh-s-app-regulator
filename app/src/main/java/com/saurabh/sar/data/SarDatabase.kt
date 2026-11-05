package com.saurabh.sar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.saurabh.sar.core.model.EmergencyEffort
import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType

class Converters {
    @TypeConverter fun modeType(v: ModeType): String = v.name
    @TypeConverter fun modeType(v: String): ModeType = ModeType.valueOf(v)
    @TypeConverter fun ruleType(v: RuleType): String = v.name
    @TypeConverter fun ruleType(v: String): RuleType = RuleType.valueOf(v)
    @TypeConverter fun effort(v: EmergencyEffort): String = v.name
    @TypeConverter fun effort(v: String): EmergencyEffort = EmergencyEffort.valueOf(v)
    @TypeConverter fun endReason(v: EndReason?): String? = v?.name
    @TypeConverter fun endReasonOf(v: String?): EndReason? = v?.let { EndReason.valueOf(it) }
}

@Database(
    entities = [ModeEntity::class, ScheduleEntryEntity::class, AppRuleEntity::class, RunEntity::class, RunAppUsageEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SarDatabase : RoomDatabase() {
    abstract fun modeDao(): ModeDao
    abstract fun runDao(): RunDao

    companion object {
        fun create(context: Context): SarDatabase =
            Room.databaseBuilder(context, SarDatabase::class.java, "sar.db").build()
    }
}
