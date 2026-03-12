package com.example.lottery;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lottery.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/*
 * BrowseProfilesActivity
 * Allows administrator to browse all user profiles in the system.
 */
public class BrowseProfilesActivity extends AppCompatActivity {

    private ListView lvProfiles;
    private TextView tvEmptyProfiles;

    private ArrayList<User> users;
    private ProfileAdapter profileAdapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_profiles);

        lvProfiles = findViewById(R.id.lvProfiles);
        tvEmptyProfiles = findViewById(R.id.tvEmptyProfiles);

        db = FirebaseFirestore.getInstance();

        users = new ArrayList<>();
        profileAdapter = new ProfileAdapter(this, users);
        lvProfiles.setAdapter(profileAdapter);

        loadProfiles();
    }

    /*
     * Loads all user profiles from Firestore
     */
    private void loadProfiles() {

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    users.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvEmptyProfiles.setVisibility(View.VISIBLE);
                        lvProfiles.setVisibility(View.GONE);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        String name = doc.getString("username");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone_number");

                        users.add(new User(name, email, phone));
                    }

                    profileAdapter.notifyDataSetChanged();
                });
    }
}