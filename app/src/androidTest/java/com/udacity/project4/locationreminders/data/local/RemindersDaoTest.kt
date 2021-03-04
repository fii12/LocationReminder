package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantexecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminder_RetrieveFromDb_BothareSame() = runBlockingTest {
        //GIVEN- REMINDER IS INSERTED IN TO DATABASE.
        val reminderDTO = ReminderDTO(
            "test title",
            "test description", "test location", 10.0, 10.0, "id1"
        )
        database.reminderDao().saveReminder(reminderDTO)

        //WHEN- DATA IS RETRIEVED FROM DATABASE.
        val dataFromSource = database.reminderDao().getReminderById(reminderDTO.id)

        //THEN- BOTH DATA ARE SAME.
        assertThat<ReminderDTO>(dataFromSource as ReminderDTO, not(nullValue()))
        assertThat(dataFromSource.id, `is`(reminderDTO.id))
        assertThat(dataFromSource.title, `is`(reminderDTO.title))
        assertThat(dataFromSource.description, `is`(reminderDTO.description))
        assertThat(dataFromSource.location, `is`(reminderDTO.location))
        assertThat(dataFromSource.latitude, `is`(reminderDTO.latitude))
        assertThat(dataFromSource.longitude, `is`(reminderDTO.longitude))

    }

    @Test
    fun retrieveInvalidDataFromDb_Error_returnsNull() = runBlockingTest {
        //GIVEN A DATABASE.

        //WHEN- INVALID DATA IS RETRIEVED FROM DATABASE.
        val dataFromSource = database.reminderDao().getReminderById("sample_id")

        //THEN- NULL VALUE IS RETURNED.
        assertThat(dataFromSource, `is`(nullValue()))

    }

}