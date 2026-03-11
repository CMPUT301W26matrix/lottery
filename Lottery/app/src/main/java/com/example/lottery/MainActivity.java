package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

    private static final String KEY_IS_ANONYMOUS = "isAnonymous";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_FID = "fid";
    private Button signInButton;
    private Button entrantButton;
    private Button organizerButton;
    private Button adminButton;
    private TextView chooseRoleText;
    private TextView signInPrompt;
    private SharedPreferences sharedPreferences;

    /**
     * Initializes the activity, sets up UI components,
     * and loads events from Firestore.
     *
     * @param savedInstanceState the saved state of the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        entrantButton = findViewById(R.id.entrant_login_button);
        organizerButton = findViewById(R.id.organizer_login_button);
        adminButton = findViewById(R.id.admin_login_button);
        signInButton = findViewById(R.id.btnSignIn);

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
        chooseRoleText = findViewById(R.id.tvChooseRole);
        signInPrompt = findViewById(R.id.tvSignInHint);

        entrantButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, EntrantRegistrationActivity.class)));

        organizerButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, OrganizerRegistrationActivity.class)));

        adminButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, AdminSignInActivity.class)));

        signInButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, GeneralSignInActivity.class)));
    }

    /**
     * Reloads event data whenever the activity resumes.
     */
    @Override
    protected void onStart() {
        super.onStart();
        checkAnonymousSession();
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
    private void checkAnonymousSession() {
        boolean isAnonymous = sharedPreferences.getBoolean(KEY_IS_ANONYMOUS, false);
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String userName = sharedPreferences.getString(KEY_USER_NAME, "Anonymous User");
        String fid = sharedPreferences.getString(KEY_FID, null);

        if (isAnonymous && userId != null && userId.startsWith("anon_") && fid != null) {
            navigateToEntrantMain(userId, userName, true);
        }
    }

    private void navigateToEntrantMain(String userId, String userName, boolean isAnonymous) {
        Intent intent = new Intent(MainActivity.this, EntrantMainActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        intent.putExtra("isRegistered", false);
        intent.putExtra("isAnonymous", isAnonymous);
        intent.putExtra("fid", sharedPreferences.getString(KEY_FID, ""));

        startActivity(intent);
        finish();
    }
}
