package com.openjarvis.automation

import android.content.Context
import androidx.room.*

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val command: String,
    val scheduleType: String,
    val scheduleHour: Int = 0,
    val scheduleMinute: Int = 0,
    val scheduleDayOfWeek: Int = 0,
    val scheduleIntervalMs: Long = 0,
    val scheduleAtMs: Long = 0,
    val enabled: Boolean = true,
    val lastRun: Long? = null,
    val lastResult: String? = null,
    val runCount: Int = 0
)

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY name")
    suspend fun getAll(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getById(id: String): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(automation: AutomationEntity)

    @Update
    suspend fun update(automation: AutomationEntity)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [AutomationEntity::class], version = 2, exportSchema = false)
abstract class AutomationDB : RoomDatabase() {
    abstract fun automationDao(): AutomationDao

    companion object {
        @Volatile private var INSTANCE: AutomationDB? = null

        fun getInstance(context: Context): AutomationDB {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AutomationDB::class.java,
                    "automations.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

internal fun AutomationManager.Automation.toEntity(): AutomationEntity {
    val scheduleFields = when (val value = schedule) {
        is AutomationManager.AutomationSchedule.Daily ->
            listOf("daily", value.hour, value.minute, 0, 0L, 0L)
        is AutomationManager.AutomationSchedule.Weekly ->
            listOf("weekly", value.hour, value.minute, value.dayOfWeek, 0L, 0L)
        is AutomationManager.AutomationSchedule.Interval ->
            listOf("interval", 0, 0, 0, value.intervalMs, 0L)
        is AutomationManager.AutomationSchedule.Once ->
            listOf("once", 0, 0, 0, 0L, value.atMs)
    }

    return AutomationEntity(
        id = id,
        name = name,
        command = command,
        scheduleType = scheduleFields[0] as String,
        scheduleHour = scheduleFields[1] as Int,
        scheduleMinute = scheduleFields[2] as Int,
        scheduleDayOfWeek = scheduleFields[3] as Int,
        scheduleIntervalMs = scheduleFields[4] as Long,
        scheduleAtMs = scheduleFields[5] as Long,
        enabled = enabled,
        lastRun = lastRun,
        lastResult = lastResult,
        runCount = runCount
    )
}

internal fun AutomationEntity.toAutomation(): AutomationManager.Automation {
    val parsedSchedule = when (scheduleType) {
        "daily" -> AutomationManager.AutomationSchedule.Daily(scheduleHour, scheduleMinute)
        "weekly" -> AutomationManager.AutomationSchedule.Weekly(
            scheduleDayOfWeek, scheduleHour, scheduleMinute
        )
        "interval" -> AutomationManager.AutomationSchedule.Interval(scheduleIntervalMs)
        "once" -> AutomationManager.AutomationSchedule.Once(scheduleAtMs)
        else -> throw IllegalStateException("Unknown automation schedule type: $scheduleType")
    }

    return AutomationManager.Automation(
        id = id,
        name = name,
        command = command,
        schedule = parsedSchedule,
        enabled = enabled,
        lastRun = lastRun,
        lastResult = lastResult,
        runCount = runCount
    )
}
