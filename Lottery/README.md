# Lottery Android Project - Event Management Feature

This project implements an event management system using Android (Java) and Firebase Firestore.

## Architecture Overview

The Event feature follows a model-activity pattern where data is persisted in a remote Firestore database.

### UML Class Diagram

```mermaid
classDiagram
    class Event {
        -String eventId
        -String title
        -Date scheduledDateTime
        -Date registrationDeadline
        -Integer maxCapacity
        -String details
        -String posterUri
        -String qrCodeContent
        -String organizerId
        +Event()
        +Event(String, String, Date, Date, Integer, String, String, String, String)
        +getters_setters()
    }

    class CreateEventActivity {
        -FirebaseFirestore db
        -String eventId
        -String qrCodeContent
        -Date eventDate
        -Date deadlineDate
        -String posterUriString
        -ActivityResultLauncher getContentLauncher
        -ImageView ivPosterPreview
        -ImageView ivQRCodePreview
        #onCreate(Bundle)
        -initializeViews()
        -generateAndDisplayQRCode()
        -setupImagePicker()
        -showDateTimePicker(Button, boolean)
        -createEvent()
    }

    class EventDetailsActivity {
        -FirebaseFirestore db
        -SimpleDateFormat dateFormat
        -ImageView ivEventPoster
        -TextView tvEventTitle
        -TextView tvScheduledDate
        -TextView tvRegistrationDeadline
        -TextView tvEventDetails
        #onCreate(Bundle)
        -fetchEventDetails(String)
        -updateUI(Event)
    }

    class FirebaseFirestore {
        <<Service>>
        +collection(String)
        +document(String)
        +get()
        +set(Object)
    }

    CreateEventActivity ..> Event : creates & persists
    EventDetailsActivity ..> Event : retrieves & displays
    CreateEventActivity ..> EventDetailsActivity : navigates to (via Intent)
    CreateEventActivity --> FirebaseFirestore : set()
    EventDetailsActivity --> FirebaseFirestore : get()
```

## Implemented User Stories

- **US 02.01.01**: Create Event with a unique promotional QR code (ZXing integrated).
- **US 02.01.04**: Registration deadline management (with chronological validation).
- **US 02.04.01**: Upload and view event posters (Local URI implementation).

## Implementation Details

- **Backend**: Firebase Firestore (Collection: `events`).
- **Navigation**: CreateEventActivity automatically redirects to EventDetailsActivity upon success.
- **Validation**: Strict validation for required fields and logical date sequences.
- **Documentation**: Professional Javadoc provided for all model and activity classes.
