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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lottery.model.Event;
import com.example.lottery.util.QRCodeUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Activity for organizers to create new events.
 * 
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Provides a form to input event details (title, capacity, description).</li>
 *   <li>Manages date and time selection for the event and its registration deadline.</li>
 *   <li>Handles event poster selection (stored as local URI for prototype phase).</li>
 *   <li>Generates and displays a unique promotional QR code.</li>
 *   <li>Validates and persists event data to Firebase Firestore.</li>
 * </ul>
 * </p>
 * 
 * <p>NOTE:
 * For the prototype checkpoint, posters are stored as local URIs.
 * Cross-device poster sharing will require Firebase Storage upload
 * in a future implementation.
 * </p>
 * 
 * <p>Satisfies requirements for:
 * US 02.01.01: Event creation with promotional QR code.
 * US 02.01.04: Registration deadline management.
 * US 02.04.01: Event poster support (Local URI).
 * </p>
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    // UI Components
    private TextInputEditText etEventTitle, etMaxCapacity, etEventDetails;
    private Button btnEventDateTime, btnRegistrationDeadline, btnUploadPoster, btnGenerateQRCode, btnCreateEvent;
    private ImageView ivPosterPreview, ivQRCodePreview;
    private TextView tvQRCodeLabel;
    private MaterialCardView cvQRCode;
    
    // Core data variables for the new event
    private final String eventId = UUID.randomUUID().toString();
    private String qrCodeContent = "";
    private Date eventDate;
    private Date deadlineDate;
    
    /** 
     * URI of the selected poster image. 
     * Initialized as null to indicate no selection (Satisfies non-mandatory AC).
     */
    private Uri selectedPosterUri = null;

    private ActivityResultLauncher<String> getContentLauncher;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

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
        setupImagePicker();

        // US 02.01.01: Generate and display QR code
        btnGenerateQRCode.setOnClickListener(v -> generateAndDisplayQRCode());

        // Launch Event action
        btnCreateEvent.setOnClickListener(v -> createEvent());

        // Date selection listeners
        btnEventDateTime.setOnClickListener(v -> showDateTimePicker(btnEventDateTime, true));
        btnRegistrationDeadline.setOnClickListener(v -> showDateTimePicker(btnRegistrationDeadline, false));
    }

    private void initializeViews() {
        etEventTitle = findViewById(R.id.etEventTitle);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etEventDetails = findViewById(R.id.etEventDetails);
        btnEventDateTime = findViewById(R.id.btnEventDateTime);
        btnRegistrationDeadline = findViewById(R.id.btnRegistrationDeadline);
        btnUploadPoster = findViewById(R.id.btnUploadPoster);
        btnGenerateQRCode = findViewById(R.id.btnGenerateQRCode);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        
        ivQRCodePreview = findViewById(R.id.ivQRCodePreview);
        tvQRCodeLabel = findViewById(R.id.tvQRCodeLabel);
        cvQRCode = findViewById(R.id.cvQRCode);
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
     * Initializes the Activity Result Launcher for selecting an event poster.
     */
    private void setupImagePicker() {
        getContentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPosterUri = uri;
                        ivPosterPreview.setImageURI(uri);
                        btnUploadPoster.setVisibility(View.GONE);
                    }
                });
        btnUploadPoster.setOnClickListener(v -> getContentLauncher.launch("image/*"));
        ivPosterPreview.setOnClickListener(v -> getContentLauncher.launch("image/*"));
    }

    private void showDateTimePicker(Button button, boolean isEventTime) {
        final Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (v, hour, min) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, min);
                Date date = selected.getTime();
                button.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d", year, month + 1, day, hour, min));
                if (isEventTime) eventDate = date; else deadlineDate = date;
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Logic for US 02.01.01 and US 02.04.01.
     * Validates input and persists event metadata (including local poster URI) to Firestore.
     */
    private void createEvent() {
        String title = Objects.requireNonNull(etEventTitle.getText()).toString().trim();
        String capacityStr = Objects.requireNonNull(etMaxCapacity.getText()).toString().trim();
        String details = Objects.requireNonNull(etEventDetails.getText()).toString().trim();

        // US 02.01.04 Validation: Chronological check
        // Event must have title
        if (title.isEmpty()) {
            Toast.makeText(this, "Event title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Event must have start date and time
        if (eventDate == null) {
            Toast.makeText(this, "Event date and time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Event must have registration deadline
        if (deadlineDate == null) {
            Toast.makeText(this, "Registration deadline is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!deadlineDate.before(eventDate)) {
            Toast.makeText(this, "Registration deadline must be before the event start time", Toast.LENGTH_LONG).show();
            return;
        }

        if (qrCodeContent.isEmpty()) {
            qrCodeContent = QRCodeUtils.generateUniqueQrContent(eventId);
        }

        int maxCapacity = capacityStr.isEmpty() ? 0 : Integer.parseInt(capacityStr);

        /**
         * NOTE:
         * For the prototype checkpoint, we store the local content:// URI as a string.
         * Cross-device viewing will fail until Firebase Storage is implemented.
         */
        String posterUriToSave = (selectedPosterUri != null) ? selectedPosterUri.toString() : "";

        // Create the model instance
        Event newEvent = new Event(
                eventId,
                title,
                eventDate,
                deadlineDate,
                maxCapacity,
                details,
                posterUriToSave,
                qrCodeContent,
                "organizer_current_user"
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

    /*
    /**
     * Placeholder for future cloud storage integration (US 02.04.01 Production).
     * This method will handle uploading the local file to Firebase Storage
     * and saving the public download URL to Firestore.
     *
    private void uploadPosterToFirebase(Uri uri, String eventId) {
        // StorageReference storageRef = FirebaseStorage.getInstance()
        //    .getReference("event_posters/" + eventId + ".jpg");
        //
        // storageRef.putFile(uri)
        //    .addOnSuccessListener(taskSnapshot -> 
        //        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
        //            // Future: update Firestore with downloadUri.toString()
        //        })
        //    );
    }
    */
}
