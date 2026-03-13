package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottery.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EntrantMainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;
    private View emptyStateContainer;
    private TextView tvActiveCount, tvJoinedCount;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userId = getIntent().getStringExtra("userId");
        db = FirebaseFirestore.getInstance();

        rvEvents = findViewById(R.id.rvEvents);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvJoinedCount = findViewById(R.id.tvJoinedCount);

        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList, this);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        setupNavigation();
        loadEvents();
        loadStats();
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            // Already home
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> 
            Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.nav_qr_scan).setOnClickListener(v -> 
            Toast.makeText(this, "QR Scan coming soon", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
    }

    private void loadStats() {
        db.collection("events").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (tvActiveCount != null) {
                tvActiveCount.setText(String.valueOf(queryDocumentSnapshots.size()));
            }
        });

        if (userId != null) {
            db.collectionGroup("entrants")
                    .whereEqualTo("entrantId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (tvJoinedCount != null) {
                            tvJoinedCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                        }
                    });
        }
    }

    private void updateEmptyState() {
        if (eventList.isEmpty()) {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEventClick(Event event) {
        Intent intent = new Intent(this, EntrantEventDetailsActivity.class);
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        intent.putExtra(EntrantEventDetailsActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }
}
