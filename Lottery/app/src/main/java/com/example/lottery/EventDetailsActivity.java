package com.example.lottery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";

    private ImageView ivEventPoster;
    private TextView tvEventTitle, tvScheduledDate, tvRegistrationDeadline, tvEventDetails, tvLocationRequirement;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

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
        tvLocationRequirement = findViewById(R.id.tvLocationRequirement);

        db = FirebaseFirestore.getInstance();

        // Setup common navigation listeners
        setupNavigation();

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
     * Sets up click listeners for the custom bottom navigation bar included in the layout.
     */
    private void setupNavigation() {
        View btnHome = findViewById(R.id.nav_home);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View btnCreate = findViewById(R.id.nav_create_container);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                startActivity(new Intent(this, CreateEventActivity.class));
                finish();
            });
        }

        // Other buttons can be mapped to Toasts or future activities
        View btnHistory = findViewById(R.id.nav_calendar);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> Toast.makeText(this, "History Coming Soon", Toast.LENGTH_SHORT).show());
        }
    }

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

    private void updateUI(Event event) {
        tvEventTitle.setText(event.getTitle());
        tvEventDetails.setText(event.getDetails());
        
        if (event.getScheduledDateTime() != null) {
            tvScheduledDate.setText(dateFormat.format(event.getScheduledDateTime()));
        }
        
        if (event.getRegistrationDeadline() != null) {
            tvRegistrationDeadline.setText(dateFormat.format(event.getRegistrationDeadline()));
        }

        if (tvLocationRequirement != null) {
            if (event.isRequireLocation()) {
                tvLocationRequirement.setText("Location Verification Required");
                tvLocationRequirement.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                tvLocationRequirement.setVisibility(View.VISIBLE);
            } else {
                tvLocationRequirement.setVisibility(View.GONE);
            }
        }

        String posterUriString = event.getPosterUri();
        if (posterUriString != null && !posterUriString.isEmpty()) {
            try {
                Uri posterUri = Uri.parse(posterUriString);
                ivEventPoster.setImageURI(null); 
                ivEventPoster.setImageURI(posterUri);
            } catch (Exception e) {
                ivEventPoster.setImageResource(R.drawable.event_placeholder);
            }
        } else {
            ivEventPoster.setImageResource(R.drawable.event_placeholder);
        }
    }
}
