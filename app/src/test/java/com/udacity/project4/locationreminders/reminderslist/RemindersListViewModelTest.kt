package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest : KoinTest {

    //Done: provide testing to the RemindersListViewModel and its live data objects

    /* using the maincoroutinerule to use testcoroutineDispatcher in
    testing. it allows to avoid some boilerplate code as it manages
    the setting and resetting of dispatchers.main which is not required
    in the @before and @after functions of the test class. */
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var viewModel: RemindersListViewModel
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

    //testing the livedata in the reminderlistviewmodel when loadreminders is called.
    @Test
    fun loadReminders_liveData_checkLoading() = mainCoroutineRule.runBlockingTest {
        //currently "showNoData" livedata is not assingned anything.

        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()

        //at first livedata value should be true . so checking that is the case.

        //extension function in livedatatestutil is used as discussed in the lecture videos.
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        //after executing the code inside viewmodelscope the livedata value should be false.
        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    //test with livedata value indicating an error.
    @Test
    fun loadReminders_withEmptyData_snackBarlivedata_showasError() {
        //the following line ensures that if list is empty it returns error.
        dataSource.shouldReturnError = true

        viewModel.loadReminders()

        assertThat(viewModel.showSnackBar.getOrAwaitValue(), not(nullValue()))
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Error. List is empty."))
    }

    //testing the livedata in the reminderlistviewmodel when loadreminders is called.
    @Test
    fun callInvalidatefunction_liveDataValueisNotNull() = mainCoroutineRule.runBlockingTest {
        //currently "showNoData" livedata is not assingned anything.
        viewModel.loadReminders()

        //now livedata value should not be null. so checking that is the case.

        //extension function in livedatatestutil is used as discussed in the lecture videos.
        assertThat(viewModel.showNoData.getOrAwaitValue(), not(nullValue()))

    }


    @Test
    fun saveReminderList_loadReminders_bothHaveSameData() = mainCoroutineRule.runBlockingTest {
        //inserting a list of(1 item) ReminderDTO items to the data source.
        val reminderDatalist = listOf(
            ReminderDTO(
                "title",
                "description", "location", 10.0, 10.0, "id1"
            )
        )
        dataSource.saveReminder(reminderDatalist[0])

        //check if viewmodel.loadReminders gets the same list of Reminders.
        viewModel.loadReminders()
        val viewmodelListData = viewModel.remindersList.getOrAwaitValue()

        //check if both lists have same data.
        assertThat(viewmodelListData[0].id, `is`(reminderDatalist[0].id))
        assertThat(viewmodelListData[0].title, `is`(reminderDatalist[0].title))
        assertThat(
            viewmodelListData[0].description,
            `is`(reminderDatalist[0].description)
        )
        assertThat(viewmodelListData[0].latitude, `is`(reminderDatalist[0].latitude))
        assertThat(viewmodelListData[0].longitude, `is`(reminderDatalist[0].longitude))
        assertThat(viewmodelListData[0].location, `is`(reminderDatalist[0].location))

    }

    @After
    fun cleanUp() {
        stopKoin()
    }

}