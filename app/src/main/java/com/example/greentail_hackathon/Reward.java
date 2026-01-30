package com.example.greentail_hackathon;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class Reward extends AppCompatActivity {

    private TextView taliPointsTextView;
    private DatabaseReference databaseRef;
    private DatabaseReference suggestionsRef; // Reference for suggestions
    private String userId;

    private EditText editSuggestion;
    private ImageView btnSendSuggestion;

    private final String DB_URL = "https://greentail-hackathon-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.reward);

        taliPointsTextView = findViewById(R.id.taliPoints);
        editSuggestion = findViewById(R.id.editSuggestion);
        btnSendSuggestion = findViewById(R.id.btnSendSuggestion);

        // Charity Card Navigation
        View card1 = findViewById(R.id.card1);
        if (card1 != null) {
            card1.setOnClickListener(v -> {
                Intent intent = new Intent(Reward.this, Reforest.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Suggestion Logic with Firebase Push
        if (btnSendSuggestion != null) {
            btnSendSuggestion.setOnClickListener(v -> {
                String suggestionText = editSuggestion.getText().toString().trim();

                if (!suggestionText.isEmpty()) {
                    if (userId != null) {
                        // Create a unique key for each suggestion
                        String suggestionId = suggestionsRef.push().getKey();

                        // Prepare the data
                        Map<String, Object> suggestionData = new HashMap<>();
                        suggestionData.put("text", suggestionText);
                        suggestionData.put("timestamp", System.currentTimeMillis());

                        // Save to Firebase: suggestions -> userId -> uniqueID
                        suggestionsRef.child(suggestionId).setValue(suggestionData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(Reward.this, "Suggestion sent! Thank you.", Toast.LENGTH_SHORT).show();
                                    editSuggestion.setText("");
                                    hideKeyboard(v);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(Reward.this, "Failed to send. Try again.", Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    Toast.makeText(Reward.this, "Please enter a suggestion first", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Footer Navigation
        setupFooterNavigation();

        // Firebase Setup
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            databaseRef = FirebaseDatabase.getInstance(DB_URL).getReference("users").child(userId);
            // Initialize suggestions reference
            suggestionsRef = FirebaseDatabase.getInstance(DB_URL).getReference("suggestions").child(userId);

            loadTailpointsData();
        } else {
            taliPointsTextView.setText("0");
        }
    }

    private void loadTailpointsData() {
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer taliValue = dataSnapshot.child("taliPoints").getValue(Integer.class);
                    taliPointsTextView.setText(taliValue != null ? String.valueOf(taliValue) : "0");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error: " + databaseError.getMessage());
            }
        });
    }

    private void setupFooterNavigation() {
        ImageView home = findViewById(R.id.home);
        ImageView search = findViewById(R.id.search);
        ImageView progress = findViewById(R.id.progress);
        ImageView connect = findViewById(R.id.connect);

        if (home != null) home.setOnClickListener(v -> startActivity(new Intent(this, Main1.class)));
        if (search != null) search.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        if (progress != null) progress.setOnClickListener(v -> startActivity(new Intent(this, Progress.class)));
        if (connect != null) connect.setOnClickListener(v -> startActivity(new Intent(this, connect.class)));
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}