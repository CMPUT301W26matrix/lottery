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
import android.widget.ImageButton;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Activity for organizers to create or edit events.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li>Provides UI for entering event details (Title, Date, Capacity, etc.).</li>
 *   <li>Handles both creation of new events and editing of existing ones.</li>
 *   <li>Enforces business rules such as US 02.01.04 (Registration deadline validation) 
 *       and US 02.03.01 (Waiting list limit enforcement during edit).</li>
 *   <li>Generates promotional QR codes and handles poster selection callbacks.</li>
 * </ul>
 * </p>
 * Activity for organizers to create new events.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Collect event metadata, schedule, and organizer-facing options.</li>
 *   <li>Handle poster selection through {@link UploadPosterDialogFragment}.</li>
 *   <li>Generate and preview a promotional QR code before saving.</li>
 *   <li>Validate date relationships before persisting to Firestore.</li>
 * </ul>
 * </p>
 *
 * <p>For the current prototype, poster images are copied into app-local storage and the
 * resulting URI is stored with the event record.</p>
 */
public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";

    // UI Components
    private TextInputEditText etEventTitle, etMaxCapacity, etEventDetails, etWaitingListLimit;
    private TextInputEditText etEventStart, etEventEnd, etRegStart, etRegEnd, etDrawDate;
    private TextInputLayout tilWaitingListLimit;
    private Button btnOpenUploadDialog, btnGenerateQRCode, btnCreateEvent;
    private ImageButton btnBack;
    private ImageView ivQRCodePreview, ivPosterPreview;
    private TextView tvQRCodeLabel, tvPosterStatus, tvHeader;
    private MaterialCardView cvQRCode;
    private SwitchMaterial swRequireLocation, swLimitWaitingList;
    
    // Core data variables
    private String eventId = UUID.randomUUID().toString();
    private String qrCodeContent = "";
    private Date eventStartDate, eventEndDate, regStartDate, regEndDate, drawDate;
    private boolean isEditMode = false;
    
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
    
    /** URI returned from the poster picker dialog. */
    private Uri selectedPosterUri = null;
    private FirebaseFirestore db;

    /**
     * Initializes Firebase, binds the form views, and wires action handlers.
     *
     * @param savedInstanceState previously saved activity state, if any
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
        
        // Check for edit mode
        String existingEventId = getIntent().getStringExtra("eventId");
        if (existingEventId != null) {
            isEditMode = true;
            this.eventId = existingEventId;
            tvHeader.setText("Edit Event");
            btnCreateEvent.setText("Update Event");
            loadEventData(existingEventId);
        }

        setupDialogCallback();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnOpenUploadDialog.setOnClickListener(v -> {
            UploadPosterDialogFragment dialog = new UploadPosterDialogFragment();
            dialog.show(getSupportFragmentManager(), "upload_poster");
        });

        btnGenerateQRCode.setOnClickListener(v -> generateAndDisplayQRCode());

        btnCreateEvent.setOnClickListener(v -> validateAndSaveEvent());

        swLimitWaitingList.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilWaitingListLimit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                etWaitingListLimit.setText("");
            }
        });
    }

    /**
     * Initializes UI references.
     */
    private void initializeViews() {
        tvHeader = findViewById(R.id.tvHeader);
        etEventTitle = findViewById(R.id.etEventTitle);
        etMaxCapacity = findViewById(R.id.etMaxCapacity);
        etEventDetails = findViewById(R.id.etEventDetails);
        
        etEventStart = findViewById(R.id.etEventStart);
        etEventEnd = findViewById(R.id.etEventEnd);
        etRegStart = findViewById(R.id.etRegStart);
        etRegEnd = findViewById(R.id.etRegEnd);
        etDrawDate = findViewById(R.id.etDrawDate);

        etEventStart.setOnClickListener(v -> showDateTimePicker(etEventStart, "eventStart"));
        etEventEnd.setOnClickListener(v -> showDateTimePicker(etEventEnd, "eventEnd"));
        etRegStart.setOnClickListener(v -> showDateTimePicker(etRegStart, "regStart"));
        etRegEnd.setOnClickListener(v -> showDateTimePicker(etRegEnd, "regEnd"));
        etDrawDate.setOnClickListener(v -> showDateTimePicker(etDrawDate, "drawDate"));

        btnOpenUploadDialog = findViewById(R.id.btnOpenUploadDialog);
        btnGenerateQRCode = findViewById(R.id.btnGenerateQRCode);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnBack = findViewById(R.id.btnBack);
        
        ivQRCodePreview = findViewById(R.id.ivQRCodePreview);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        tvQRCodeLabel = findViewById(R.id.tvQRCodeLabel);
        tvPosterStatus = findViewById(R.id.tvPosterStatus);
        cvQRCode = findViewById(R.id.cvQRCode);

        swRequireLocation = findViewById(R.id.swRequireLocation);
        swLimitWaitingList = findViewById(R.id.swLimitWaitingList);
        tilWaitingListLimit = findViewById(R.id.tilWaitingListLimit);
        etWaitingListLimit = findViewById(R.id.etWaitingListLimit);
    }

    /**
     * Loads existing event data from Firestore if in Edit Mode.
     * @param eventId The unique identifier of the event to load.
     */
    private void loadEventData(String eventId) {
        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Event event = doc.toObject(Event.class);
                if (event != null) {
                    etEventTitle.setText(event.getTitle());
                    etMaxCapacity.setText(String.valueOf(event.getMaxCapacity()));
                    etEventDetails.setText(event.getDetails());
                    swRequireLocation.setChecked(event.isRequireLocation());
                    
                    if (event.getWaitingListLimit() != null) {
                        swLimitWaitingList.setChecked(true);
                        etWaitingListLimit.setText(String.valueOf(event.getWaitingListLimit()));
                        tilWaitingListLimit.setVisibility(View.VISIBLE);
                    }

                    if (event.getPosterUri() != null && !event.getPosterUri().isEmpty() && ivPosterPreview != null) {
                        selectedPosterUri = Uri.parse(event.getPosterUri());
                        ivPosterPreview.setImageURI(selectedPosterUri);
                        ivPosterPreview.setVisibility(View.VISIBLE);
                        tvPosterStatus.setText("Poster selected");
                        tvPosterStatus.setTextColor(getResources().getColor(R.color.primary_blue));
                    }
                    
                    this.qrCodeContent = event.getQrCodeContent();
                    this.eventStartDate = event.getScheduledDateTime();
                    this.regEndDate = event.getRegistrationDeadline();
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
                    if (eventStartDate != null) etEventStart.setText(sdf.format(eventStartDate));
                    if (regEndDate != null) etRegEnd.setText(sdf.format(regEndDate));
                }
            }
        });
    }

    /**
     * Sets up the listener for receiving results from the poster upload dialog.
     */
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

    /**
     * Generates a unique QR code for the event and displays it.
    * Copies the selected image into internal storage to avoid losing URI access later. 
    */
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
     * Helper to show a combined date and time picker.
     */
    private void showDateTimePicker(final TextInputEditText editText, final String fieldType) {
        final Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (v, hour, min) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, min);
                Date date = selected.getTime();
                String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d %02d:%02d", 
                                                     month + 1, day, year, hour, min);
                editText.setText(formattedDate);
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
     * Validates all inputs before saving to Firestore.
     * Enforces US 02.01.04 and US 02.03.01.
     */
    private void validateAndSaveEvent() {
        String title = Objects.requireNonNull(etEventTitle.getText()).toString().trim();
        String capacityStr = Objects.requireNonNull(etMaxCapacity.getText()).toString().trim();
        String details = Objects.requireNonNull(etEventDetails.getText()).toString().trim();
        String waitingLimitStr = Objects.requireNonNull(etWaitingListLimit.getText()).toString().trim();

        if (title.isEmpty() || eventStartDate == null || regEndDate == null) {
            Toast.makeText(this, "Event title, Start Date, and Registration End are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!EventValidationUtils.isRegistrationDeadlineValid(regEndDate, eventStartDate)) {
            Toast.makeText(this, "Registration must end before the event starts", Toast.LENGTH_LONG).show();
            return;
        }

        Integer waitingListLimit = null;
        if (swLimitWaitingList.isChecked()) {
            if (waitingLimitStr.isEmpty()) {
                Toast.makeText(this, "Please enter a waiting list limit", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                waitingListLimit = Integer.parseInt(waitingLimitStr);
                if (waitingListLimit <= 0) {
                    Toast.makeText(this, "Limit must be a positive integer (>0)", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number for waiting list limit", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final Integer finalWaitingListLimit = waitingListLimit;

        // AC #4 Check: New limit cannot be strictly lower than current entrants
        if (isEditMode && finalWaitingListLimit != null) {
            db.collection("events").document(eventId).collection("entrants").get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.size() > finalWaitingListLimit) {
                        Toast.makeText(this, "New limit (" + finalWaitingListLimit + ") cannot be less than current entrants (" + snapshots.size() + ")", Toast.LENGTH_LONG).show();
                    } else {
                        saveEventToFirestore(title, capacityStr, details, finalWaitingListLimit);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking entrants", e);
                    saveEventToFirestore(title, capacityStr, details, finalWaitingListLimit);
                });
        } else {
            saveEventToFirestore(title, capacityStr, details, finalWaitingListLimit);
        }
    }

    /**
     * Performs the actual Firestore write operation.
     */
    private void saveEventToFirestore(String title, String capacityStr, String details, Integer waitingListLimit) {
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
                .set(event)
                .addOnSuccessListener(aVoid -> {
                    String msg = isEditMode ? "Event Updated Successfully!" : "Event Launched Successfully!";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing document", e);
                    Toast.makeText(this, "Failed to save event", Toast.LENGTH_SHORT).show();
                });
    }
}
