package com.example.lottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WaitingListActivityTest {

    private void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test that WaitingListActivity launches successfully.
     * Since the activity always shows either the list or the empty message,
     * we verify the activity itself loads by checking the empty message view exists.
     */
    @Test
    public void waitingListActivity_launchesSuccessfully() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                WaitingListActivity.class
        );
        intent.putExtra("eventId", "nonexistent_event_id_12345");

        try (ActivityScenario<WaitingListActivity> scenario = ActivityScenario.launch(intent)) {
            waitFor(3000);
            // After load, one of these two views must always be visible
            onView(withId(R.id.emptyMessage))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }

    /**
     * Test that the empty message is shown when there are no entrants.
     */
    @Test
    public void waitingListActivity_displaysEmptyMessageView() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                WaitingListActivity.class
        );
        intent.putExtra("eventId", "nonexistent_event_id_12345");

        try (ActivityScenario<WaitingListActivity> scenario = ActivityScenario.launch(intent)) {
            waitFor(3000);
            onView(withId(R.id.emptyMessage))
                    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        }
    }
}