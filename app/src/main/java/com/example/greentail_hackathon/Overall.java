package com.example.greentail_hackathon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.DecimalFormat;

public class Overall extends AppCompatActivity {

    private static final double MAX_BAR_VALUE = 2.50;
    private TextView homeValue, travelValue, foodValue, othersValue, overallText, overallValue, updateButton;
    private View homeProgress, travelProgress, foodProgress, othersProgress;
    private double homeFootprint = 0.0;
    private double travelFootprint = 0.0;
    private double foodFootprint = 0.0;
    private double othersFootprint = 0.0;
    private DecimalFormat decimalFormat = new DecimalFormat("#.#");
    private FirebaseAuth mAuth;
    private int categoriesLoaded = 0;
    private final int TOTAL_CATEGORIES = 4;

    private final String DB_URL = "https://greentail-hackathon-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_overall);

        mAuth = FirebaseAuth.getInstance();

        homeValue = findViewById(R.id.homeValue);
        travelValue = findViewById(R.id.travelValue);
        foodValue = findViewById(R.id.foodValue);
        othersValue = findViewById(R.id.othersValue);
        overallText = findViewById(R.id.overallText);
        overallValue = findViewById(R.id.overallValue);
        updateButton = findViewById(R.id.updateButton);

        homeProgress = findViewById(R.id.homeProgressFill);
        travelProgress = findViewById(R.id.travelProgressFill);
        foodProgress = findViewById(R.id.foodProgressFill);
        othersProgress = findViewById(R.id.othersProgressFill);

        // --- UPDATE SURVEY ANSWERS LOGIC ---
        updateButton.setOnClickListener(v -> {
            String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

            // Set completion to false so Main1 knows to go back to TakeServey
            FirebaseDatabase.getInstance(DB_URL).getReference("users")
                    .child(userId)
                    .child("survey_completed")
                    .setValue(false)
                    .addOnSuccessListener(aVoid -> {
                        Intent intent = new Intent(Overall.this, TakeServey.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error resetting status", Toast.LENGTH_SHORT).show());
        });

        updateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchFootprintData();
    }

    private void fetchFootprintData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        categoriesLoaded = 0;
        homeFootprint = 0.0; travelFootprint = 0.0; foodFootprint = 0.0; othersFootprint = 0.0;

        DatabaseReference dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("surveys");
        fetchCategoryData(dbRef.child("home"), userId, "home");
        fetchCategoryData(dbRef.child("travel"), userId, "travel");
        fetchCategoryData(dbRef.child("food"), userId, "food");
        fetchCategoryData(dbRef.child("others"), userId, "others");
    }

    private void fetchCategoryData(DatabaseReference categoryRef, String userId, String categoryName) {
        categoryRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double footprint = 0.0;
                    if (snapshot.child("annualEmissions").exists()) {
                        footprint = getDoubleValue(snapshot.child("annualEmissions"));
                    } else if (snapshot.child("footprint").exists()) {
                        footprint = getDoubleValue(snapshot.child("footprint"));
                    } else if (snapshot.child("carbon_footprint").exists()) {
                        footprint = getDoubleValue(snapshot.child("carbon_footprint"));
                    }

                    switch (categoryName) {
                        case "home": homeFootprint = footprint; break;
                        case "travel": travelFootprint = footprint; break;
                        case "food": foodFootprint = footprint; break;
                        case "others": othersFootprint = footprint; break;
                    }
                }
                categoriesLoaded++;
                if (categoriesLoaded >= TOTAL_CATEGORIES) {
                    updateUI();
                    updateTotalInDatabase();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { categoriesLoaded++; }
        });
    }

    private double getDoubleValue(DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        if (value instanceof Double) return (Double) value;
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); } catch (Exception e) { return 0.0; }
        }
        return 0.0;
    }

    private void updateUI() {
        homeValue.setText(formatValue(homeFootprint));
        travelValue.setText(formatValue(travelFootprint));
        foodValue.setText(formatValue(foodFootprint));
        othersValue.setText(formatValue(othersFootprint));

        double totalTons = homeFootprint + travelFootprint + foodFootprint + othersFootprint;
        overallText.setText("Overall " + formatValue(totalTons));
        overallValue.setText(getImpactDescription(totalTons));
        updateProgressBarWithDelay();
    }

    private void updateTotalInDatabase() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        if (!userId.equals("anonymous")) {
            double total = homeFootprint + travelFootprint + foodFootprint + othersFootprint;
            FirebaseDatabase.getInstance(DB_URL).getReference("users").child(userId).child("total_footprint").setValue(total);
        }
    }

    private String formatValue(double value) {
        return value == 0 ? "0.0" : decimalFormat.format(value);
    }

    private String getImpactDescription(double totalTons) {
        if (totalTons <= 0) return "Complete surveys to calculate your carbon impact";
        int billboards = (int) Math.max(1, Math.ceil((totalTons * 3) / 18));
        return String.format("%s tons of CO2e would melt an area\nof arctic sea ice the size of %d %s",
                decimalFormat.format(totalTons), billboards, billboards == 1 ? "billboard" : "billboards");
    }

    private void updateProgressBarWithDelay() {
        homeProgress.post(() -> {
            updateProgressBar(homeProgress, homeFootprint);
            updateProgressBar(travelProgress, travelFootprint);
            updateProgressBar(foodProgress, foodFootprint);
            updateProgressBar(othersProgress, othersFootprint);
        });
    }

    private void updateProgressBar(View progressBar, double tonsValue) {
        float percentage = (tonsValue <= 0) ? 0.02f : (float) Math.min(tonsValue / MAX_BAR_VALUE, 1.0f);
        ViewGroup parent = (ViewGroup) progressBar.getParent();
        int parentWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        ViewGroup.LayoutParams params = progressBar.getLayoutParams();
        params.width = (int) (parentWidth * percentage);
        progressBar.setLayoutParams(params);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, Main1.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}