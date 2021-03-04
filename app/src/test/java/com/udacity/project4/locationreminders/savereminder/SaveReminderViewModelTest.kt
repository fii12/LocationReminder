package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest : KoinTest {


    /* using the maincoroutinerule to use testcoroutineDispatcher in
     testing. it allows to avoid some boilerplate code as it manages
     the setting and resetting of dispatchers.main which is not required
     in the @before and @after functions of the test class. */
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var viewModel: SaveReminderViewModel
    lateinit var dataSource: FakeDataSource
    lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as FakeDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as FakeDataSource
                )
            }
            single { FakeDataSource() }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            androidContext(appContext)
            modules(listOf(myModule))
        }

        dataSource = get()
        viewModel = get()

    }

    //Done: provide testing to the SaveReminderView and its live data objects
    @Test
    fun validateData_ReturnsFalse() {
        val reminderDataItem = ReminderDataItem(
            "title",
            "description", null, 10.0, 10.0
        )
        assertThat(viewModel.validateEnteredData(reminderDataItem), `is`(false))

    }

    @Test
    fun validateData_ReturnsTrue() {
        val reminderDataItem = ReminderDataItem(
            "title",
            "description", "a sample location", 10.0, 10.0
        )
        assertThat(viewModel.validateEnteredData(reminderDataItem), `is`(true))
    }

    //testing the livedata in the savereminderviewmodel when savereminder is called.
    @Test
    fun saveReminder_liveData_checkLoading() = mainCoroutineRule.runBlockingTest {
        val reminderDataItem = ReminderDataItem(
            "title",
            "description", "a sample location", 10.0, 10.0
        )
        //currently "showloading" livedata is not assingned anything.

        mainCoroutineRule.pauseDispatcher()
        viewModel.validateAndSaveReminder(reminderDataItem)


        //now livedata value should not be null. so checking that is the case.

        //extension function in livedatatestutil is used as discussed in the lecture videos.

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))

    }

    @Test
    fun saveReminder_retrieveData_bothAreSame() = mainCoroutineRule.runBlockingTest {
        val reminderDataItem = ReminderDataItem(
            "title",
            "description", "a sample location", 10.0, 10.0
        )
        viewModel.validateAndSaveReminder(reminderDataItem)

        val dataFromSource = dataSource.getReminder(reminderDataItem.id)
        var result = ReminderDTO(
            "fake title",
            "fake description", "fake location", 110.0, 110.0, "fake id"
        )
        when (dataFromSource) {
            is Result.Success<*> -> result = (dataFromSource.data as ReminderDTO)

        }
        assertThat(reminderDataItem.id, `is`(result.id))
        assertThat(reminderDataItem.title, `is`(result.title))
        assertThat(reminderDataItem.description, `is`(result.description))
        assertThat(reminderDataItem.latitude, `is`(result.latitude))
        assertThat(reminderDataItem.longitude, `is`(result.longitude))
        assertThat(reminderDataItem.location, `is`(result.location))
    }

    @After
    fun cleanUp() {
        stopKoin()
    }

}