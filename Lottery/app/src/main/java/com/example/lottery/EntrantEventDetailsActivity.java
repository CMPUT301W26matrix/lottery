package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EntrantEventDetailsActivity extends AppCompatActivity {

    private TextView tvEventTitle;
    private TextView tvWaitlistCount;
    private TextView tvNotificationBadge;
    private Button btnWaitlistAction;
    private ImageButton btnNotifications;

    private FirebaseFirestore db;

    private final String eventId = "10029a98-836a-49f3-92e9-67678efc19d1";
    private final String entrantId = "6xygP8FXpxATgAkKmj27";

    private boolean isInWaitlist = false;
    private int waitlistCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_event_details);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvWaitlistCount = findViewById(R.id.tvWaitlistCount);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnWaitlistAction = findViewById(R.id.btnWaitlistAction);
        btnNotifications = findViewById(R.id.btnNotifications);

        tvEventTitle.setText("Swimming Lessons");

        checkWaitlistStatus();
        loadWaitlistCount();
        checkUnreadNotifications();

        btnWaitlistAction.setOnClickListener(v -> {
            if (!isInWaitlist) {
                joinWaitlist();
            } else {
                leaveWaitlist();
            }
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(EntrantEventDetailsActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadNotifications();
        checkWaitlistStatus();
        loadWaitlistCount();
    }

    private void checkWaitlistStatus() {
        DocumentReference entrantRef = db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(entrantId);

        entrantRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                isInWaitlist = true;
                btnWaitlistAction.setText("Leave Wait List");
            } else {
                isInWaitlist = false;
                btnWaitlistAction.setText("Join Wait List");
            }
        });
    }

    private void loadWaitlistCount() {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    waitlistCount = queryDocumentSnapshots.size();
                    tvWaitlistCount.setText("People in Waitlist: " + waitlistCount);
                });
    }

    private void checkUnreadNotifications() {
        db.collection("users")
                .document(entrantId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void joinWaitlist() {
        Map<String, Object> entrantData = new HashMap<>();
        entrantData.put("entrantId", entrantId);
        entrantData.put("status", "WAITLISTED");

        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(entrantId)
                .set(entrantData)
                .addOnSuccessListener(unused -> {
                    isInWaitlist = true;
                    loadWaitlistCount();
                    updateWaitlistUI();
                    Toast.makeText(this, "Joined waitlist", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to join waitlist", Toast.LENGTH_SHORT).show()
                );
    }

    private void leaveWaitlist() {
        db.collection("events")
                .document(eventId)
                .collection("entrants")
                .document(entrantId)
                .delete()
                .addOnSuccessListener(unused -> {
                    isInWaitlist = false;
                    loadWaitlistCount();
                    updateWaitlistUI();
                    Toast.makeText(this, "Left waitlist", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to leave waitlist", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateWaitlistUI() {
        tvWaitlistCount.setText("People in Waitlist: " + waitlistCount);

        if (isInWaitlist) {
            btnWaitlistAction.setText("Leave Wait List");
        } else {
            btnWaitlistAction.setText("Join Wait List");
        }
    }
}