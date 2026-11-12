package com.openjarvis.automation

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class AutomationManager(
    private val context: Context,
    private val profileId: String = "legacy-local"
) {
    init { require(profileId.isNotBlank()) { "Profile ID must not be blank" } }
    private fun workName(id: String) = "$profileId:$id"

    private val db = AutomationDB.getInstance(context)
    private val dao = db.automationDao()
    private val _automationsFlow = MutableStateFlow<List<Automation>>(emptyList())
    val automationsFlow: StateFlow<List<Automation>> = _automationsFlow

    suspend fun loadAutomations() {
        _automationsFlow.value = dao.getAll(profileId).map { it.toAutomation() }
    }

    suspend fun createAutomation(automation: Automation): String = withContext(Dispatchers.IO) {
        dao.insert(automation.toEntity().copy(profileId = profileId))
        if (automation.enabled) scheduleAutomation(automation)
        _automationsFlow.value = dao.getAll(profileId).map { it.toAutomation() }
        automation.id
    }

    suspend fun updateAutomation(automation: Automation) = withContext(Dispatchers.IO) {
        if (dao.getById(automation.id, profileId) == null) return@withContext
        dao.update(automation.toEntity().copy(profileId = profileId))
        cancelAutomation(automation.id)
        if (automation.enabled) scheduleAutomation(automation)
        _automationsFlow.value = dao.getAll(profileId).map { it.toAutomation() }
    }

    suspend fun deleteAutomation(id: String) = withContext(Dispatchers.IO) {
        cancelAutomation(id)
        dao.delete(id, profileId)
        _automationsFlow.value = dao.getAll(profileId).map { it.toAutomation() }
    }

    suspend fun toggleAutomation(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val stored = dao.getById(id, profileId) ?: return@withContext
        val updated = stored.copy(enabled = enabled)
        dao.update(updated)
        if (enabled) scheduleAutomation(updated.toAutomation()) else cancelAutomation(id)
        _automationsFlow.value = dao.getAll(profileId).map { it.toAutomation() }
    }

    suspend fun runNow(id: String) = withContext(Dispatchers.IO) {
        val stored = dao.getById(id, profileId) ?: return@withContext
        recordNotExecuted(stored)
    }

    private suspend fun scheduleAutomation(automation: Automation) {
        require(automation.enabled) { "Cannot schedule a disabled automation" }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val inputData = workDataOf(
            "automation_id" to automation.id,
            "profile_id" to profileId,
            "automation_command" to automation.command
        )

        val workManager = WorkManager.getInstance(context)
        when (val schedule = automation.schedule) {
            is AutomationSchedule.Daily -> {
                val request = PeriodicWorkRequestBuilder<AutomationWorker>(
                    24, TimeUnit.HOURS, 15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(calculateDelay(schedule.hour, schedule.minute), TimeUnit.MILLISECONDS)
                    .addTag(workName(automation.id))
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    workName(automation.id), ExistingPeriodicWorkPolicy.REPLACE, request
                )
            }
            is AutomationSchedule.Weekly -> {
                val request = PeriodicWorkRequestBuilder<AutomationWorker>(
                    7, TimeUnit.DAYS, 15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(calculateWeeklyDelay(schedule.dayOfWeek, schedule.hour, schedule.minute), TimeUnit.MILLISECONDS)
                    .addTag(workName(automation.id))
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    workName(automation.id), ExistingPeriodicWorkPolicy.REPLACE, request
                )
            }
            is AutomationSchedule.Interval -> {
                require(schedule.intervalMs >= 15 * 60 * 1000L) {
                    "WorkManager requires periodic intervals of at least 15 minutes"
                }
                val request = PeriodicWorkRequestBuilder<AutomationWorker>(
                    schedule.intervalMs, TimeUnit.MILLISECONDS
                )
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .addTag(workName(automation.id))
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    workName(automation.id), ExistingPeriodicWorkPolicy.REPLACE, request
                )
            }
            is AutomationSchedule.Once -> {
                val delay = schedule.atMs - System.currentTimeMillis()
                if (delay <= 0) {
                    recordNotExecuted(automation.toEntity().copy(profileId = profileId))
                    return
                }
                val request = OneTimeWorkRequestBuilder<AutomationWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(workName(automation.id))
                    .build()
                workManager.enqueueUniqueWork(
                    workName(automation.id), ExistingWorkPolicy.REPLACE, request
                )
            }
        }
    }

    private fun cancelAutomation(id: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(workName(id))
    }

    private suspend fun recordNotExecuted(automation: AutomationEntity) {
        dao.update(
            automation.copy(
                lastRun = System.currentTimeMillis(),
                lastResult = "not_executed: safe command executor is not implemented",
                runCount = automation.runCount
            )
        )
        _automationsFlow.value = dao.getAll(profileId).map { it.toAutomation() }
    }

    private fun calculateDelay(targetHour: Int, targetMinute: Int): Long {
        require(targetHour in 0..23 && targetMinute in 0..59) { "Invalid daily schedule time" }
        val cal = java.util.Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
        cal.set(java.util.Calendar.MINUTE, targetMinute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        var delay = cal.timeInMillis - now
        if (delay <= 0) delay += 24 * 60 * 60 * 1000L
        return delay
    }

    private fun calculateWeeklyDelay(dayOfWeek: Int, targetHour: Int, targetMinute: Int): Long {
        require(dayOfWeek in java.util.Calendar.SUNDAY..java.util.Calendar.SATURDAY) {
            "Invalid weekly schedule day"
        }
        require(targetHour in 0..23 && targetMinute in 0..59) { "Invalid weekly schedule time" }
        val cal = java.util.Calendar.getInstance()
        val now = cal.timeInMillis
        cal.set(java.util.Calendar.DAY_OF_WEEK, dayOfWeek)
        cal.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
        cal.set(java.util.Calendar.MINUTE, targetMinute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        var delay = cal.timeInMillis - now
        if (delay <= 0) delay += 7 * 24 * 60 * 60 * 1000L
        return delay
    }

    fun parseSchedule(input: String): AutomationSchedule? {
        val lower = input.lowercase()

        val dailyMatch = Regex(
            """every day at (\d{1,2})(?::(\d{2}))?\s*(am|pm)?""",
            RegexOption.IGNORE_CASE
        ).find(lower)
        if (dailyMatch != null) {
            var hour = dailyMatch.groupValues[1].toInt()
            val minute = dailyMatch.groupValues[2].toIntOrNull() ?: 0
            val isPM = dailyMatch.groupValues[3].lowercase() == "pm"
            if (hour !in 1..12 || minute !in 0..59) return null
            if (isPM && hour != 12) hour += 12
            if (!isPM && hour == 12) hour = 0
            return AutomationSchedule.Daily(hour, minute)
        }

        val intervalMatch = Regex(
            """every (\d+)\s*(minute|hour|day)s?""",
            RegexOption.IGNORE_CASE
        ).find(lower)
        if (intervalMatch != null) {
            val value = intervalMatch.groupValues[1].toLongOrNull() ?: return null
            if (value <= 0) return null
            val unit = intervalMatch.groupValues[2]
            val multiplier = when (unit) {
                "minute" -> 60_000L
                "hour" -> 60 * 60_000L
                "day" -> 24 * 60 * 60_000L
                else -> return null
            }
            return AutomationSchedule.Interval(value * multiplier)
        }

        return null
    }

    data class Automation(
        val id: String,
        val name: String,
        val command: String,
        val schedule: AutomationSchedule,
        val enabled: Boolean = true,
        val lastRun: Long? = null,
        val lastResult: String? = null,
        val runCount: Int = 0
    )

    sealed class AutomationSchedule {
        data class Daily(val hour: Int, val minute: Int) : AutomationSchedule()
        data class Weekly(val dayOfWeek: Int, val hour: Int, val minute: Int) : AutomationSchedule()
        data class Interval(val intervalMs: Long) : AutomationSchedule()
        data class Once(val atMs: Long) : AutomationSchedule()
    }
}
