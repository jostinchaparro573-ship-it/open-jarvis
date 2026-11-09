package com.openjarvis.automation

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase

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
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE automations ADD COLUMN scheduleAtMs INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile private var INSTANCE: AutomationDB? = null

        fun getInstance(context: Context): AutomationDB {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AutomationDB::class.java,
                    "automations.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

internal fun AutomationManager.Automation.toEntity(): AutomationEntity {
    val type: String
    var hour = 0
    var minute = 0
    var dayOfWeek = 0
    var intervalMs = 0L
    var atMs = 0L

    when (val value = schedule) {
        is AutomationManager.AutomationSchedule.Daily -> {
            type = "daily"
            hour = value.hour
            minute = value.minute
        }
        is AutomationManager.AutomationSchedule.Weekly -> {
            type = "weekly"
            hour = value.hour
            minute = value.minute
            dayOfWeek = value.dayOfWeek
        }
        is AutomationManager.AutomationSchedule.Interval -> {
            type = "interval"
            intervalMs = value.intervalMs
        }
        is AutomationManager.AutomationSchedule.Once -> {
            type = "once"
            atMs = value.atMs
        }
    }

    return AutomationEntity(
        id = id,
        name = name,
        command = command,
        scheduleType = type,
        scheduleHour = hour,
        scheduleMinute = minute,
        scheduleDayOfWeek = dayOfWeek,
        scheduleIntervalMs = intervalMs,
        scheduleAtMs = atMs,
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


class AutomationWorker(
    context: Context,
    params: androidx.work.WorkerParameters
) : androidx.work.CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val id = inputData.getString("automation_id") ?: return Result.failure()

        return try {
            val dao = AutomationDB.getInstance(applicationContext).automationDao()
            val automation = dao.getById(id) ?: return Result.failure()
            if (!automation.enabled) return Result.failure()

            // Deliberately fail closed: no permission-checked command executor is connected yet.
            dao.update(
                automation.copy(
                    lastRun = System.currentTimeMillis(),
                    lastResult = "not_executed: safe command executor is not implemented"
                )
            )
            Result.failure()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}
