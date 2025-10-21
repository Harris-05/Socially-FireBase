package com.example.smd_assignment_i230796

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddingPostTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(add_post::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testAddPostFlow() {
        val imageUri = Uri.parse("android.resource://com.example.smd_assignment_i230796/" + R.drawable.post_picture_screen_1)
        val resultData = Intent().apply {
            putParcelableArrayListExtra("android.intent.extra.STREAM", arrayListOf(imageUri))
            data = imageUri
        }
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)

        intending(not(isInternal())).respondWith(result)

        onView(withId(R.id.btnPickImages)).perform(click())

        Thread.sleep(1500)

        val previewIntent = Intent().apply {
            putStringArrayListExtra("imageUris", arrayListOf(imageUri.toString()))
            putExtra("caption", "My Espresso Test Post 🚀")
        }
        val previewResult = Instrumentation.ActivityResult(Activity.RESULT_OK, previewIntent)
        intending(not(isInternal())).respondWith(previewResult)

        Thread.sleep(3000)
        onView(withId(R.id.btnPost)).perform(click())

        Thread.sleep(2000)
    }
}
