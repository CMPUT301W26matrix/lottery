package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MainActivity serves as the main dashboard for organizers.
 *
 * <p>This activity retrieves event data from Firestore and displays it
 * in a RecyclerView. It also shows summary statistics for events
 * including active, closed, pending, and total events.</p>
 *
 * <p>The activity implements {@link EventAdapter.OnEventClickListener}
 * to respond when an event in the list is selected.</p>
 */
public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    /** Log tag used for debugging. */
    private static final String TAG = "MainActivity";

    /** RecyclerView displaying the list of events. */
    private RecyclerView rvEvents;

    /** Adapter used to bind event data to the RecyclerView. */
    private EventAdapter adapter;

    /** List storing events retrieved from Firestore. */
    private List<Event> eventList;

    /** TextView shown when no events exist. */
    private TextView tvNoEvents;

    /** TextViews displaying event statistics. */
    private TextView tvActiveCount, tvClosedCount, tvPendingCount, tvTotalCount;

    /** Firestore database reference. */
    private FirebaseFirestore db;

    /**
     * Initializes the activity, sets up UI components,
     * and loads events from Firestore.
     *
     * @param savedInstanceState the saved state of the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        // Bind UI Components
        rvEvents = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvClosedCount = findViewById(R.id.tvClosedCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);

        // Setup RecyclerView
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList, this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        setupNavigation();
        loadOrganizerEvents();

        /*
        -------------------------------------------------
        TEMPORARY TEST CODE (USED DURING DEVELOPMENT)
        -------------------------------------------------
        This block was used to directly open the entrant
        event details screen for testing waitlist and
        notification functionality.

        It should remain commented in the main branch
        and will be removed once full navigation flow
        is implemented.
        -------------------------------------------------

        Intent intent = new Intent(MainActivity.this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, "27f4180b-d282-41fc-a240-5d2675d4bf59");
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, "7EwFNDGwGyR89nPgfPzwD8wG6jq2");
        startActivity(intent);
        finish();
        */
    }

    /**
     * Sets up navigation actions for the bottom navigation bar.
     */
    private void setupNavigation() {

        View btnCreate = findViewById(R.id.nav_create_container);

        if (btnCreate != null) {
            btnCreate.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, CreateEventActivity.class))
            );
        }

        View btnHome = findViewById(R.id.nav_home);

        if (btnHome != null) {
            btnHome.setOnClickListener(v ->
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
            );
        }
    }

    /**
     * Reloads event data whenever the activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    /**
     * Retrieves events from Firestore and updates the RecyclerView.
     *
     * <p>This method also calculates event statistics including
     * active and closed events.</p>
     */
    private void loadOrganizerEvents() {

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    eventList.clear();
                    int active = 0;
                    int closed = 0;
                    Date now = new Date();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        try {

                            Event event = document.toObject(Event.class);

                            // Compatibility fix for older database fields
                            if (event.getScheduledDateTime() == null) {
                                Date oldDate = document.getDate("eventDate");
                                if (oldDate != null) event.setScheduledDateTime(oldDate);
                            }

                            if (event.getRegistrationDeadline() == null) {
                                Date oldDeadline = document.getDate("deadlineDate");
                                if (oldDeadline != null) event.setRegistrationDeadline(oldDeadline);
                            }

                            eventList.add(event);

                            if (event.getScheduledDateTime() != null &&
                                    event.getScheduledDateTime().after(now)) {
                                active++;
                            } else {
                                closed++;
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping document " + document.getId(), e);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateSummaryStats(active, closed, 0, eventList.size());

                    tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error", e);
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the summary statistics displayed on the dashboard.
     *
     * @param active number of active events
     * @param closed number of closed events
     * @param pending number of pending events
     * @param total total number of events
     */
    private void updateSummaryStats(int active, int closed, int pending, int total) {

        tvActiveCount.setText(String.valueOf(active));
        tvClosedCount.setText(String.valueOf(closed));
        tvPendingCount.setText(String.valueOf(pending));
        tvTotalCount.setText(String.valueOf(total));
    }

    /**
     * Handles event item clicks from the RecyclerView.
     *
     * @param event the selected event
     */
    @Override
    public void onEventClick(Event event) {

        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }
}