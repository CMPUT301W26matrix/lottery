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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
 *   <li>Handles event poster selection via a dedicated dialog (US 02.04.01).</li>
 *   <li>Generates and displays a unique promotional QR code.</li>
 *   <li>Configures event-specific requirements such as geolocation (US 02.02.03).</li>
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
 * US 02.04.01: Event poster support (Dialog-based flow).
 * US 02.02.03: Geolocation requirement toggle.
 * </p>
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    // UI Components
    private TextInputEditText etEventTitle, etMaxCapacity, etEventDetails;
    private TextInputEditText etEventStart, etEventEnd, etRegStart, etRegEnd, etDrawDate;
    private Button btnOpenUploadDialog, btnGenerateQRCode, btnCreateEvent;
    private ImageView ivQRCodePreview, ivPosterPreview;
    private TextView tvQRCodeLabel, tvPosterStatus;
    private MaterialCardView cvQRCode;
    private SwitchMaterial swRequireLocation;
    
    // Core data variables for the new event
    private final String eventId = UUID.randomUUID().toString();
    private String qrCodeContent = "";
    private Date eventStartDate, eventEndDate, regStartDate, regEndDate, drawDate;
    
    /** 
     * URI of the selected poster image. 
     * Received from UploadPosterDialogFragment.
     */
    private Uri selectedPosterUri = null;

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
        setContentView(R.layout.activity_create_event);

        // Initialize Firestore early so we can fail fast if services are unavailable.
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

        // Launch the poster picker dialog.
        btnOpenUploadDialog.setOnClickListener(v -> {
            UploadPosterDialogFragment dialog = new UploadPosterDialogFragment();
            dialog.show(getSupportFragmentManager(), "upload_poster");
        });

        // Generate the QR code lazily when the organizer requests a preview.
        btnGenerateQRCode.setOnClickListener(v -> generateAndDisplayQRCode());
        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    /** Binds UI controls and sets up the date/time field triggers. */
    private void initializeViews() {
        etEventTitle = findViewById(R.id.etEventTitle);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etEventDetails = findViewById(R.id.etEventDetails);
        
        // Refactored Date Inputs
        etEventStart = findViewById(R.id.etEventStart);
        etEventEnd = findViewById(R.id.etEventEnd);
        etRegStart = findViewById(R.id.etRegStart);
        etRegEnd = findViewById(R.id.etRegEnd);
        etDrawDate = findViewById(R.id.etDrawDate);

        // Date Picker Click Listeners
        etEventStart.setOnClickListener(v -> showDateTimePicker(etEventStart, "eventStart"));
        etEventEnd.setOnClickListener(v -> showDateTimePicker(etEventEnd, "eventEnd"));
        etRegStart.setOnClickListener(v -> showDateTimePicker(etRegStart, "regStart"));
        etRegEnd.setOnClickListener(v -> showDateTimePicker(etRegEnd, "regEnd"));
        etDrawDate.setOnClickListener(v -> showDateTimePicker(etDrawDate, "drawDate"));

        btnOpenUploadDialog = findViewById(R.id.btnOpenUploadDialog);
        btnGenerateQRCode = findViewById(R.id.btnGenerateQRCode);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        
        ivQRCodePreview = findViewById(R.id.ivQRCodePreview);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        tvQRCodeLabel = findViewById(R.id.tvQRCodeLabel);
        tvPosterStatus = findViewById(R.id.tvPosterStatus);
        cvQRCode = findViewById(R.id.cvQRCode);

        swRequireLocation = findViewById(R.id.swRequireLocation);
    }

    /** Receives the selected poster URI from {@link UploadPosterDialogFragment}. */
    private void setupDialogCallback() {
        getSupportFragmentManager().setFragmentResultListener("posterRequest", this, (requestKey, bundle) -> {
            String uriString = bundle.getString("posterUri");
            if (uriString != null) {
                selectedPosterUri = Uri.parse(uriString);
                tvPosterStatus.setText("Poster selected");
                tvPosterStatus.setTextColor(getResources().getColor(R.color.primary_blue));
                
                // Show the chosen poster immediately so the organizer can confirm the selection.
                if (ivPosterPreview != null) {
                    ivPosterPreview.setImageURI(selectedPosterUri);
                    ivPosterPreview.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /** Copies the selected image into internal storage to avoid losing URI access later. */
    private String saveImageToInternalStorage(Uri uri) {
        try {
            String fileName = "poster_" + UUID.randomUUID().toString() + ".jpg";
            File file = new File(getFilesDir(), fileName);
            
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.close();
            inputStream.close();
            
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save image locally", e);
            return uri.toString(); // Fallback to original URI
        }
    }

    /** Generates a QR code preview for the event being drafted. */
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

        int maxCapacity;
        if (capacityStr.isEmpty()) {
            maxCapacity = 0;
        } else {
            maxCapacity = Integer.parseInt(capacityStr);
        }
        
        // Persist the poster locally before storing the URI in Firestore.
        String posterUriToSave = "";
        if (selectedPosterUri != null) {
            posterUriToSave = saveImageToInternalStorage(selectedPosterUri);
        }
        
        boolean requireLocation = swRequireLocation.isChecked();

        Event newEvent = new Event(
                eventId, title, eventStartDate, regEndDate, maxCapacity, 
                details, posterUriToSave, qrCodeContent, "organizer_current_user", requireLocation
        );

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
