package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    //    Done: Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun close() = database.close()

    //runBlocking used due to the error-"runBlockingTest fails with "This job has not completed yet""
    @Test
    fun insertReminder_RetrieveFromRepositoryById_BothAreSame() = runBlocking {
        //GIVEN- REMINDER IS INSERTED IN TO DATABASE.
        val reminderDTO = ReminderDTO(
            "test title",
            "test description", "test location", 10.0, 10.0, "id1"
        )
        repository.saveReminder(reminderDTO)

        //WHEN- DATA IS RETRIEVED FROM DATABASE.
        val dataFromSource = repository.getReminder(reminderDTO.id)

        //THEN- BOTH DATA ARE SAME.
        assertThat(
            dataFromSource,
            not(nullValue())
        )
        dataFromSource as Result.Success

        assertThat(dataFromSource.data.id, `is`(reminderDTO.id))
        assertThat(dataFromSource.data.title, `is`(reminderDTO.title))
        assertThat(dataFromSource.data.description, `is`(reminderDTO.description))
        assertThat(dataFromSource.data.location, `is`(reminderDTO.location))
        assertThat(dataFromSource.data.latitude, `is`(reminderDTO.latitude))
        assertThat(dataFromSource.data.longitude, `is`(reminderDTO.longitude))
    }

    //runBlocking used due to the error-"runBlockingTest fails with "This job has not completed yet""
    @Test
    fun retrieveInvalidData_returnsResultError() = runBlocking {
        //GIVEN THE REPOSITORY.

        //WHEN- INVALID DATA IS RETRIEVED FROM REPOSITORY.
        val dataFromSource = repository.getReminder("sample id")

        //THEN- RETURNS RESULT.ERROR.
        assertThat(
            dataFromSource,
            not(nullValue())
        )
        dataFromSource as Result.Error
        assertThat(
            dataFromSource,
            `is`(Result.Error("Reminder not found!"))
        )
    }

}