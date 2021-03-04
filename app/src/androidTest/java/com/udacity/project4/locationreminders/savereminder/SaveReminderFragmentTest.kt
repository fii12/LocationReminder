package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.local.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
import org.mockito.Mockito

@MediumTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderFragmentTest : KoinTest {

    private lateinit var repository: FakeAndroidDataSource
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
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
        repository = get()

//        clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    //Navigation test using mockito.
    @Test
    fun onClickselectLocation_NavigateToMapFragment() = runBlocking<Unit> {
        //WHEN FRAGMENT IS STARTED
        val navController = Mockito.mock(NavController::class.java)

        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        //WHEN selectLocation IS CLICKED
        Espresso.onView(ViewMatchers.withId(R.id.selectLocation)).perform(ViewActions.click())

        //CORRECT NAVIGATION METHOD IS CALLED.
        Mockito.verify(navController)
            .navigate(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
    }

    @Test
    fun onStartFragment_itemsAreVisible() = runBlocking<Unit> {

        launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.reminderTitle))
            .check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.reminderDescription))
            .check(matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(R.id.selectLocation))
            .check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.saveReminder))
            .check(matches(ViewMatchers.isDisplayed()))
    }

    //testing for snackbar display on attempting invalid data save.
    @Test
    fun onClickSavewithInvalidData_snackBarIsVisible() = runBlocking<Unit> {

        launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.reminderTitle))
            .check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.reminderTitle))
            .perform(typeText("sample title"), closeSoftKeyboard())
        onView(withId(R.id.saveReminder))
            .perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))

        onView(withId(R.id.reminderDescription))
            .check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.saveReminder))
            .check(matches(ViewMatchers.isDisplayed()))
    }

}