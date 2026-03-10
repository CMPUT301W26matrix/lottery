package com.example.lottery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class EntrantRegistrationActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private EditText entrantName, entrantEmail, entrantPassword, entrantPassword2, entrantPhone;
    private Button continueButton, anonContinueButton;
    private ImageButton backButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_registration);

        // Initialize firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize button views
        backButton = findViewById(R.id.btnBack);
        continueButton = findViewById(R.id.btnContinue);
        anonContinueButton = findViewById(R.id.btnContinueWithoutRegistering);
        // Initialize text editor views
        entrantName = findViewById(R.id.etName);
        entrantEmail = findViewById(R.id.etEmail);
        entrantPhone = findViewById(R.id.etPhoneNumber);
        entrantPassword = findViewById(R.id.etPassword);
        entrantPassword2 = findViewById(R.id.etReEnterPassword);

        // Set on click listeners for buttons
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(EntrantRegistrationActivity.this, MainActivity.class);
            startActivity(intent);
        });


    }
}
