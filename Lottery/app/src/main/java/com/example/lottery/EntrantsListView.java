package com.example.lottery;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lottery.model.Event;
import com.example.lottery.util.EventValidationUtils;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class EntrantsListView extends AppCompatActivity{
    private static final String TAG = "CreateEventActivity";
    private Button btnSwitchSignedUp, btnSwitchCancelled, btnSwitchWaitedList, btnSendNotification, btnViewLocation, btnSampleWinners;
    private FirebaseFirestore db;

    /**
     * Initializes the activity, sets up Firebase, bind views,
     * and click button listeners for QR code generation and event creation.
     *
     * @param savedInstanceState If the activity is initialized again after being shut down,
     *                           this contains the most recent data, in other case it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrants_list);

        // Initialize Firestore
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "Service Unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupDialogCallback();

        // switch to signed up component to display the entrants list that have signed up
        btnSwitchSignedUp.setOnClickListener(v -> {
            UploadPosterDialogFragment dialog = new UploadPosterDialogFragment();
            dialog.show(getSupportFragmentManager(), "upload_poster");
        });

        // switch to signed up component to display the entrants list that have signed up
        btnSwitchCancelled.setOnClickListener(v -> generateAndDisplayQRCode());

        // switch to signed up component to display the entrants list that have signed up
        btnSwitchWaitedList.setOnClickListener(v -> createEvent());
    }

    /**
     * Initialize view for the create event activity.
     */
    private void initializeViews() {
        btnSwitchWaitedList = findViewById(R.id.entrants_list_waited_list_btn);
        btnSwitchCancelled = findViewById(R.id.entrants_list_cancelled_btn);
        btnSwitchSignedUp = findViewById(R.id.entrants_list_signed_up_btn);
        btnViewLocation =findViewById(R.id.entrants_list_view_location_btn);
        btnSampleWinners =findViewById(R.id.entrants_list_sample_btn);
        btnSendNotification = findViewById(R.id.entrants_list_send_notification_btn);
    }

    /**
     * Sets up the listener to receive the poster URI from the UploadPosterDialogFragment.
     */
    private void setupDialogCallback() {
        getSupportFragmentManager().setFragmentResultListener("posterRequest", this, (requestKey, bundle) -> {
            String uriString = bundle.getString("posterUri");
            if (uriString != null) {
                selectedPosterUri = Uri.parse(uriString);
                tvPosterStatus.setText("Poster selected");
                tvPosterStatus.setTextColor(getResources().getColor(R.color.primary_blue));
                ivPosterPreview.setImageURI(selectedPosterUri);
                ivPosterPreview.setVisibility(View.VISIBLE);
                Log.d(TAG, "Poster URI received from dialog: " + uriString);
            }
        });
    }

    /**
     * US 02.01.01: Generates QR content and displays its Bitmap in the UI.
     */
    private void generateAndDisplayQRCode() {
        qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(qrCodeContent);

        if (qrBitmap != null) {
            ivQRCodePreview.setImageBitmap(qrBitmap);
            tvQRCodeLabel.setVisibility(View.VISIBLE);
            cvQRCode.setVisibility(View.VISIBLE);
            Toast.makeText(this, "QR Code Generated!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Standard Date and Time picker for form fields.
     */
    private void showDateTimePicker(final TextInputEditText editText, final String fieldType) {
        final Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (v, hour, min) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, min);
                Date date = selected.getTime();

                // Format: MM/dd/yyyy HH:mm
                String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d %02d:%02d",
                        month + 1, day, year, hour, min);
                editText.setText(formattedDate);

                // Store value based on field type
                switch (fieldType) {
                    case "eventStart": eventStartDate = date; break;
                    case "eventEnd": eventEndDate = date; break;
                    case "regStart": regStartDate = date; break;
                    case "regEnd": regEndDate = date; break;
                    case "drawDate": drawDate = date; break;
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Logic for US 02.01.01, US 02.04.01, and US 02.02.03.
     * Validates input and persists event metadata (including local poster URI and location requirement) to Firestore.
     */
    private void createEvent() {
        String title = Objects.requireNonNull(etEventTitle.getText()).toString().trim();
        String capacityStr = Objects.requireNonNull(etMaxCapacity.getText()).toString().trim();
        String details = Objects.requireNonNull(etEventDetails.getText()).toString().trim();

        // 1. Mandatory Field Validation (US 02.01.04 Requirement)
        if (title.isEmpty()) {
            Toast.makeText(this, "Event title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventStartDate == null) {
            Toast.makeText(this, "Event start date and time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (regEndDate == null) {
            Toast.makeText(this, "Registration end date is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Business Rule Validation (US 02.01.04 Requirement)
        // Uses EventValidationUtils for testable business logic
        if (!EventValidationUtils.isRegistrationDeadlineValid(regEndDate, eventStartDate)) {
            Toast.makeText(this, "Registration must end before the event starts", Toast.LENGTH_LONG).show();
            return;
        }

        if (qrCodeContent.isEmpty()) {
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        }

        int maxCapacity = capacityStr.isEmpty() ? 0 : Integer.parseInt(capacityStr);

        /**
         * NOTE:
         * For the prototype checkpoint, we store the local content:// URI as a string.
         */
        String posterUriToSave = (selectedPosterUri != null) ? selectedPosterUri.toString() : "";

        // US 02.02.03: Get the value of the geolocation toggle
        boolean requireLocation = swRequireLocation.isChecked();

        // Create the model instance
        Event newEvent = new Event(
                eventId,
                title,
                eventStartDate, // Using start as the primary scheduled time
                regEndDate,     // Using reg end as the primary deadline
                maxCapacity,
                details,
                posterUriToSave,
                qrCodeContent,
                "organizer_current_user",
                requireLocation
        );

        // Save to Firestore using eventId as document path
        db.collection("events").document(eventId)
                .set(newEvent)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event Launched Successfully!", Toast.LENGTH_SHORT).show();

                    // Navigate to details screen to show the created event
                    Intent intent = new Intent(CreateEventActivity.this, EventDetailsActivity.class);
                    intent.putExtra("eventId", eventId);
                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing document", e);
                    Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show();
                });
    }
}
