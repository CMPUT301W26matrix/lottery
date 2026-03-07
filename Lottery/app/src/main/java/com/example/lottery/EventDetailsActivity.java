package com.example.lottery;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
// import com.github.bumptech.glide.Glide; // Required for future cloud storage loading
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity to display the details of a specific event.
 * 
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Retrieves event metadata from Firebase Firestore based on a passed event ID.</li>
 *   <li>Displays event information including title, description, and dates.</li>
 *   <li>Renders the event poster image or a placeholder if none exists.</li>
 * </ul>
 * </p>
 * 
 * <p>Known Limitations:
 * <ul>
 *   <li>Poster images are loaded using local URIs, which only works if the image is present on the current device.</li>
 * </ul>
 * </p>
 * 
 * <p>Satisfies requirements for:
 * US 02.04.01: Event poster visualization for entrants.
 * </p>
 * 
 * @see com.example.lottery.model.Event
 */
public class EventDetailsActivity extends AppCompatActivity {

    /** Log tag for debugging. */
    private static final String TAG = "EventDetailsActivity";

    /** ImageView for displaying the event poster. */
    private ImageView ivEventPoster;
    
    /** TextViews for title, dates, and event description. */
    private TextView tvEventTitle, tvScheduledDate, tvRegistrationDeadline, tvEventDetails;
    
    /** Firestore database instance for fetching event data. */
    private FirebaseFirestore db;
    
    /** Formatter for displaying dates in a human-readable format. */
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    /**
     * Initializes the activity, sets up the UI components, and retrieves the event ID from the intent.
     * Starts the Firestore fetch process if a valid ID is provided.
     * 
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Initialize UI components
        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvScheduledDate = findViewById(R.id.tvScheduledDate);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvEventDetails = findViewById(R.id.tvEventDetails);

        db = FirebaseFirestore.getInstance();

        // Get eventId from intent passed by the caller
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) {
            fetchEventDetails(eventId);
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Fetches the event document from the "events" collection in Firestore.
     * On success, it triggers the UI update with the retrieved {@link Event} object.
     *
     * @param eventId The unique document ID of the event to fetch.
     */
    private void fetchEventDetails(String eventId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            updateUI(event);
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event details", e);
                    Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Populates the UI fields with data from the provided Event object.
     * Includes logic to handle local URI parsing for the event poster and fallback to a placeholder.
     *
     * @param event The {@link Event} object containing metadata to display.
     */
    private void updateUI(Event event) {
        tvEventTitle.setText(event.getTitle());
        tvEventDetails.setText(event.getDetails());
        
        if (event.getScheduledDateTime() != null) {
            tvScheduledDate.setText(dateFormat.format(event.getScheduledDateTime()));
        }
        
        if (event.getRegistrationDeadline() != null) {
            tvRegistrationDeadline.setText(dateFormat.format(event.getRegistrationDeadline()));
        }

        /**
         * Poster Display Logic:
         * 
         * Prototype Design:
         * If a posterUri exists (as a local content:// string), it is converted back 
         * to a Uri and set to the ImageView. 
         */
        String posterUriString = event.getPosterUri();
        if (posterUriString != null && !posterUriString.isEmpty()) {
            
            // --- ACTUAL PROTOTYPE IMPLEMENTATION (Local URI) ---
            try {
                Uri posterUri = Uri.parse(posterUriString);
                ivEventPoster.setImageURI(null); 
                ivEventPoster.setImageURI(posterUri);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing local poster URI. Using placeholder fallback.", e);
                ivEventPoster.setImageResource(R.drawable.event_placeholder);
            }

            /*
            // --- FUTURE PRODUCTION IMPLEMENTATION (Cloud Storage URL) ---
            // To enable cross-device loading, replace the local URI logic above with Glide:
            //
            // Glide.with(this)
            //    .load(posterUriString) // Now treated as a https:// URL
            //    .placeholder(R.drawable.event_placeholder)
            //    .error(R.drawable.event_placeholder)
            //    .centerCrop()
            //    .into(ivEventPoster);
            */

        } else {
            // Default placeholder if no poster is available
            ivEventPoster.setImageResource(R.drawable.event_placeholder);
        }
    }
}
