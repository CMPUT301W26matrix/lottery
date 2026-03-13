package com.example.lottery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class EntrantProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private Button btnLogout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = getIntent().getStringExtra("userId");

        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        btnLogout = findViewById(R.id.btn_log_out);

        loadUserProfile();
        setupNavigation();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.rl_edit_profile).setOnClickListener(v -> {
            Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.rl_notification_settings).setOnClickListener(v -> {
            Toast.makeText(this, "Notification settings coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserProfile() {
        if (userId == null) return;
        
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvName.setText(documentSnapshot.getString("name"));
                tvEmail.setText(documentSnapshot.getString("email"));
            }
        });
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantMainActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.nav_history).setOnClickListener(v -> 
            Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.nav_qr_scan).setOnClickListener(v -> 
            Toast.makeText(this, "QR Scan coming soon", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            // Already here
        });
    }
}
