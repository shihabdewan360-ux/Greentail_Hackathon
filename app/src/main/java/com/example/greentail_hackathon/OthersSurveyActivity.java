package com.example.greentail_hackathon;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OthersSurveyActivity extends AppCompatActivity {

    private static final double[] SCREEN_TIME_FACTORS = {0, 0, 0.3, 0.3, 0.6, 0.6, 1.0, 1.0, 1.5, 1.5, 2.0, 2.0, 3.0};
    private static final double[] SHOPPING_FREQ_FACTORS = {0.5, 1.5, 4.0, 8.0};
    private static final double[] RECYCLE_FACTORS = {0, 0.3, 0.7, 1.5};
    private static final double[] PLASTIC_FACTORS = {1.5, 0.8, 0.2, 0};
    private static final double[] ECO_BRAND_FACTORS = {0, 1.0, 0.5};
    private static final double[] COMPOST_FACTORS = {0, 0.5, 0.2};
    private static final double[] DISPOSAL_FACTORS = {0.1, 0.2, 1.0, 0.1};

    private SeekBar screenHoursSeekBar;
    private TextView screenHoursValue;
    private RadioGroup ecoBrandsGroup, shoppingFrequencyGroup, recycleGroup, plasticGroup, compostGroup, disposalGroup;
    private Button submitButton;

    private double annualEmissions = 0;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_othersservey);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        mDatabase = FirebaseDatabase.getInstance("https://greentail-hackathon-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        screenHoursValue = findViewById(R.id.screenHoursValue);
        screenHoursSeekBar = findViewById(R.id.screenHoursSeekBar);
        ecoBrandsGroup = findViewById(R.id.ecoBrandsGroup);
        shoppingFrequencyGroup = findViewById(R.id.shoppingFrequencyGroup);
        recycleGroup = findViewById(R.id.recycleGroup);
        plasticGroup = findViewById(R.id.plasticGroup);
        compostGroup = findViewById(R.id.compostGroup);
        disposalGroup = findViewById(R.id.disposalGroup);
        submitButton = findViewById(R.id.submitButton);

        screenHoursSeekBar.setMax(SCREEN_TIME_FACTORS.length - 1);
        screenHoursSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                screenHoursValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        submitButton.setOnClickListener(v -> {
            if (validateSelections()) {
                calculateFootprint();
                saveSurveyData();
            } else {
                Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateSelections() {
        return ecoBrandsGroup.getCheckedRadioButtonId() != -1 &&
                shoppingFrequencyGroup.getCheckedRadioButtonId() != -1 &&
                recycleGroup.getCheckedRadioButtonId() != -1 &&
                plasticGroup.getCheckedRadioButtonId() != -1 &&
                compostGroup.getCheckedRadioButtonId() != -1 &&
                disposalGroup.getCheckedRadioButtonId() != -1;
    }

    private void calculateFootprint() {
        double weeklyEmissions = 0;
        int screenHours = screenHoursSeekBar.getProgress();
        weeklyEmissions += (screenHours < SCREEN_TIME_FACTORS.length) ? SCREEN_TIME_FACTORS[screenHours] : SCREEN_TIME_FACTORS[SCREEN_TIME_FACTORS.length - 1];

        int ecoId = ecoBrandsGroup.getCheckedRadioButtonId();
        if (ecoId == R.id.yesEcoOption) weeklyEmissions += ECO_BRAND_FACTORS[0];
        else if (ecoId == R.id.noEcoOption) weeklyEmissions += ECO_BRAND_FACTORS[1];
        else if (ecoId == R.id.sometimesEcoOption) weeklyEmissions += ECO_BRAND_FACTORS[2];

        int shopId = shoppingFrequencyGroup.getCheckedRadioButtonId();
        if (shopId == R.id.rarelyOption) weeklyEmissions += SHOPPING_FREQ_FACTORS[0];
        else if (shopId == R.id.monthlyOption) weeklyEmissions += SHOPPING_FREQ_FACTORS[1];
        else if (shopId == R.id.weeklyOption) weeklyEmissions += SHOPPING_FREQ_FACTORS[2];
        else if (shopId == R.id.frequentlyOption) weeklyEmissions += SHOPPING_FREQ_FACTORS[3];

        int recId = recycleGroup.getCheckedRadioButtonId();
        if (recId == R.id.alwaysRecycleOption) weeklyEmissions += RECYCLE_FACTORS[0];
        else if (recId == R.id.oftenRecycleOption) weeklyEmissions += RECYCLE_FACTORS[1];
        else if (recId == R.id.sometimesRecycleOption) weeklyEmissions += RECYCLE_FACTORS[2];
        else if (recId == R.id.neverRecycleOption) weeklyEmissions += RECYCLE_FACTORS[3];

        int plasId = plasticGroup.getCheckedRadioButtonId();
        if (plasId == R.id.alwaysPlasticOption) weeklyEmissions += PLASTIC_FACTORS[0];
        else if (plasId == R.id.oftenPlasticOption) weeklyEmissions += PLASTIC_FACTORS[1];
        else if (plasId == R.id.sometimesPlasticOption) weeklyEmissions += PLASTIC_FACTORS[2];
        else if (plasId == R.id.neverPlasticOption) weeklyEmissions += PLASTIC_FACTORS[3];

        int compId = compostGroup.getCheckedRadioButtonId();
        if (compId == R.id.yesCompostOption) weeklyEmissions += COMPOST_FACTORS[0];
        else if (compId == R.id.planningCompostOption) weeklyEmissions += COMPOST_FACTORS[1];
        else if (compId == R.id.noCompostOption) weeklyEmissions += COMPOST_FACTORS[2];

        int dispId = disposalGroup.getCheckedRadioButtonId();
        if (dispId == R.id.donateOption) weeklyEmissions += DISPOSAL_FACTORS[0];
        else if (dispId == R.id.recycleOption) weeklyEmissions += DISPOSAL_FACTORS[1];
        else if (dispId == R.id.throwOption) weeklyEmissions += DISPOSAL_FACTORS[2];
        else if (dispId == R.id.saleOption) weeklyEmissions += DISPOSAL_FACTORS[3];

        annualEmissions = (weeklyEmissions * 52) / 1000;
    }

    private void saveSurveyData() {
        OthersSurveyData surveyData = new OthersSurveyData(
                screenHoursSeekBar.getProgress(),
                getSelectedRadioText(ecoBrandsGroup),
                getSelectedRadioText(shoppingFrequencyGroup),
                getSelectedRadioText(recycleGroup),
                getSelectedRadioText(plasticGroup),
                getSelectedRadioText(compostGroup),
                getSelectedRadioText(disposalGroup),
                annualEmissions
        );

        // 1. Save Category Data
        mDatabase.child("surveys").child("others").child(userId).setValue(surveyData)
                .addOnSuccessListener(aVoid -> {
                    // 2. Mark Survey as Complete
                    mDatabase.child("users").child(userId).child("survey_completed").setValue(true)
                            .addOnSuccessListener(aVoid2 -> updateTotalFootprint()); // Move to total calculation
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Save Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateTotalFootprint() {
        mDatabase.child("surveys").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                double total = 0;
                if (snapshot.child("home").child(userId).exists()) total += getVal(snapshot.child("home").child(userId));
                if (snapshot.child("food").child(userId).exists()) total += getVal(snapshot.child("food").child(userId));
                if (snapshot.child("travel").child(userId).exists()) total += getVal(snapshot.child("travel").child(userId));
                if (snapshot.child("others").child(userId).exists()) total += getVal(snapshot.child("others").child(userId));

                mDatabase.child("users").child(userId).child("total_footprint").setValue(total)
                        .addOnCompleteListener(task -> {
                            Intent intent = new Intent(OthersSurveyActivity.this, Overall.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears activity stack
                            startActivity(intent);
                            finish();
                        });
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private double getVal(DataSnapshot snapshot) {
        Double d = snapshot.child("annualEmissions").getValue(Double.class);
        return (d != null) ? d : 0;
    }

    private String getSelectedRadioText(RadioGroup group) {
        int selectedId = group.getCheckedRadioButtonId();
        return (selectedId != -1) ? ((RadioButton) findViewById(selectedId)).getText().toString() : "N/A";
    }

    public static class OthersSurveyData {
        public int screenHours;
        public String ecoBrands, shoppingFrequency, recycling, plasticUsage, composting, disposalMethod;
        public double annualEmissions;
        public long timestamp = System.currentTimeMillis();

        public OthersSurveyData() {}
        public OthersSurveyData(int screenHours, String ecoBrands, String shoppingFrequency, String recycling, String plasticUsage, String composting, String disposalMethod, double annualEmissions) {
            this.screenHours = screenHours;
            this.ecoBrands = ecoBrands;
            this.shoppingFrequency = shoppingFrequency;
            this.recycling = recycling;
            this.plasticUsage = plasticUsage;
            this.composting = composting;
            this.disposalMethod = disposalMethod;
            this.annualEmissions = annualEmissions;
        }
    }
}