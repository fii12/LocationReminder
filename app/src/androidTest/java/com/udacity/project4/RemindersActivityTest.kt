package com.udacity.project4

import android.app.Application
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
//END TO END test to black box test the app
//this test includes navigation between fragments, testing snackbar and toast,view visibility checks etc.
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var decorView: View

    //rule is used to get decorview required to test whether toast is shown.
    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(RemindersActivity::class.java)

    @Before
    fun setUp() {
        activityScenarioRule.scenario.onActivity { activity ->
            decorView = activity.window.decorView
        }
    }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            //androidLogger(Level.DEBUG)
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        viewModel = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    //seems like the idling resource is not required for this test. But added it for safety.
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


    //    Done: add End to End testing to the app
    /*the following is a large test that performs the following.
    1.check views are shown in reminderlist fragment, navigate to savereminder fragment
    by clicking on fab.
    2.enter details in to the edittext,sets custom values to the livedata, and saves the
    reminder by clicking savefab.
     3.navigate back to reminderlist fragment to check if new saved data is visible
     4.snackbar test - navigate to savereminder fragment and click save without full data
     to trigger the snackbar message
     5.toast test - navigate to mapfragment and click on save button without selecting pio to
     trigger toast message*/
    @Test
    fun navigateTo_selectLocation_clickSave_showToast() = runBlocking {
        repository.saveReminder(
            ReminderDTO(
                "test title",
                "test description", "test location", 10.0, 10.0, "id1"
            )
        )

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        //seems like the idling resource is not required for this test. But added it for safety.
        dataBindingIdlingResource.monitorActivity(activityScenario)

        //inside reminderlistfragment.
        onView(withText("test title")).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        //inside savereminderfragment.
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).perform(typeText("new title"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(typeText("new description"))
            .perform(closeSoftKeyboard())

        viewModel.latitude.postValue(20.0)
        viewModel.longitude.postValue(20.0)
        viewModel.reminderSelectedLocationStr.postValue("new location")

        //navigate to reminderlist fragment after saving data.
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withText("new title")).check(matches(isDisplayed()))
        onView(withText("new description")).check(matches(isDisplayed()))

        onView(withId(R.id.addReminderFAB)).perform(click())

        //checks if savereminderfragment views and snackbars shown
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))

        onView(withId(R.id.saveReminder)).perform(click())

        //checks if snackbar shown.
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))

        onView(withId(R.id.reminderTitle)).perform(typeText("new title"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(R.id.reminderDescription)).perform(typeText("new description"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.save_button)).check(matches(isDisplayed()))

        onView(withId(R.id.save_button)).perform(click())

        //testing for toast.
        onView(withText(R.string.select_poi)).inRoot(withDecorView(not(decorView))).check(
            matches(
                isDisplayed()
            )
        )

        activityScenario.close()
    }

}
