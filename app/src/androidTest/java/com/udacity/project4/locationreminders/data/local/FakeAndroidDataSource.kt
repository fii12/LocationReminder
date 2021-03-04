package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeAndroidDataSource : ReminderDataSource {

    //    Done: Create a fake data source to act as a double to the real data source
    val dataList = HashMap<String, ReminderDTO>()
    var shouldReturnError: Boolean = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError && dataList.isEmpty()) {
            return Result.Error("Error. List is empty.")
        }
        return Result.Success(ArrayList(dataList.values))
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        dataList.put(reminder.id, reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = dataList.get(id)
        reminder?.let {
            return Result.Success<ReminderDTO>(dataList.get(id)!!)
        }
        return Result.Error("data not found.")
    }

    override suspend fun deleteAllReminders() {
        dataList.clear()
    }


}