package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
@RunWith(AndroidJUnit4::class)
class ReminderListFragmentTest : KoinTest {

    private lateinit var repository: FakeAndroidDataSource
    private lateinit var appContext: Application
    private lateinit var fakeAndroidDataSource: FakeAndroidDataSource

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as FakeAndroidDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as FakeAndroidDataSource
                )
            }
            single { FakeAndroidDataSource() }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            androidContext(appContext)
            modules(listOf(myModule))
        }
        //Get our real repository
        fakeAndroidDataSource = get()
        repository = get()

//        clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    //    Done: test the navigation of the fragments.
    //Navigation test using mockito.
    @Test
    fun onClickFab_NavigateToSaveFragment() = runBlocking<Unit> {
        //WHEN FRAGMENT IS STARTED
        val navController = mock(NavController::class.java)

        val reminderDTO = ReminderDTO(
            "test title",
            "test description", "test location", 10.0, 10.0, "id1"
        )
        repository.saveReminder(reminderDTO)
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        //WHEN FAB IS CLICKED
        onView(withId(R.id.addReminderFAB)).perform(click())

        //CORRECT NAVIGATION METHOD IS CALLED.
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    //    Done: test the displayed data on the UI.
    @Test
    fun onStartFragment_itemDetailsVisible() = runBlocking<Unit> {
        //val dataSource:ReminderDataSource by inject()
        val reminderDTO = ReminderDTO(
            "test title",
            "test description", "test location", 10.0, 10.0, "id1"
        )
        repository.saveReminder(reminderDTO)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withText("test title")).check(matches(isDisplayed()))
        onView(withText("test description")).check(matches(isDisplayed()))
        onView(withText("test location")).check(matches(isDisplayed()))
    }


    @Test
    fun onStartFragment_fabVisible() = runBlocking<Unit> {
        //val dataSource:ReminderDataSource by inject()
        val reminderDTO = ReminderDTO(
            "test title",
            "test description", "test location", 10.0, 10.0, "id1"
        )
        repository.saveReminder(reminderDTO)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.addReminderFAB)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
    }

    //    Done: add testing for the error messages.
    @Test
    fun onStartFragment_withNoData_errorIndicatedByNoDataTextView() = runBlocking<Unit> {
        //val dataSource:ReminderDataSource by inject()

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        fakeAndroidDataSource.shouldReturnError = true

        onView(withId(R.id.addReminderFAB)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

}