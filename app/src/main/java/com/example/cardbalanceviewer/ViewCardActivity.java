package com.example.cardbalanceviewer;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.widget.TextView;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

public class ViewCardActivity extends AppCompatActivity {

    int cardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_view);

        this.setTitle("Edit Card"); //Title is used to show the user what they are doing/need to do

        //Load necessary components:
        TextView dateEdited = findViewById(R.id.dateEdited);
        TextView unsavedChanges = findViewById(R.id.labelUnsavedChanges);
        EditText editName = findViewById(R.id.editCardName);
        EditText editCardValue = findViewById(R.id.editCardValue);
        EditText changeValue = findViewById(R.id.changeValue);
        Button addButton = findViewById(R.id.addButton);
        Button subButton = findViewById(R.id.subButton);

        Intent editCardIntent = getIntent();
        cardId = editCardIntent.getIntExtra("cardId", -1);
        if(cardId != -1) {
            //Card has been made before
            editName.setText(MainActivity.card.get(cardId));
            dateEdited.setText(MainActivity.dateChanged.get(cardId));
            editCardValue.setText(MainActivity.balance.get(cardId));
        }

        //Warn user of unsaved changes made if user changes value manually:
        TextWatcher unsavedChangesWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                unsavedChanges.setText("Unsaved Changes Made");
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        //Anytime the Value or Name is updated, use the watcher:
        editCardValue.addTextChangedListener(unsavedChangesWatcher);
        editName.addTextChangedListener(unsavedChangesWatcher);

        //Perform mathematics to adjust value automatically:
        addButton.setOnClickListener(v -> {
            if(Float.parseFloat(changeValue.getText().toString()) == 0.00f) { return; }
            editCardValue.setText(updateValue(editCardValue.getText().toString(), changeValue.getText().toString(), true));
            unsavedChanges.setText("Unsaved Changes Made");
        });
        subButton.setOnClickListener(v -> {
            if(Float.parseFloat(changeValue.getText().toString()) == 0.00f) { return; }
            editCardValue.setText(updateValue(editCardValue.getText().toString(), changeValue.getText().toString(), false));
            unsavedChanges.setText("Unsaved Changes Made");
        });

        //Save Button
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            dateEdited.setText(updateDate());
            unsavedChanges.setText("");

            if(cardId == -1) { //New Card
                MainActivity.card.add(editName.getText().toString());
                MainActivity.balance.add(editCardValue.getText().toString());
                MainActivity.dateChanged.add(dateEdited.getText().toString());
                cardId = MainActivity.card.size() - 1;
            } else { //View Old Card
                MainActivity.card.set(cardId, editName.getText().toString());
                MainActivity.balance.set(cardId, editCardValue.getText().toString());
                MainActivity.dateChanged.set(cardId, dateEdited.getText().toString());
            }

            MainActivity.adapter.notifyDataSetChanged();

            SharedPreferences sharedPreferences = getApplication().getSharedPreferences("com.example.cardbalanceviewer", Context.MODE_PRIVATE);
            HashSet<String> putCard = new HashSet(MainActivity.card);
            HashSet<String> putBalance = new HashSet(MainActivity.balance);
            HashSet<String> putDate = new HashSet(MainActivity.dateChanged);
            sharedPreferences.edit().putStringSet("setCards", putCard).apply();
            sharedPreferences.edit().putStringSet("setBalances", putBalance).apply();
            sharedPreferences.edit().putStringSet("setDates", putDate).apply();

            //Go back to main menu:
            //Intent transfer = new Intent(getApplicationContext(), MainActivity.class);
            //startActivity(transfer);
        });

    }

    public String updateValue(String currString, String changeString, boolean add) {
        float curr = Float.parseFloat(currString);
        float change = Float.parseFloat(changeString);
        if(add) return String.format("%.2f", curr + change); //Add if add is true
        return String.format("%.2f", curr - change); //Subtract instead
    }
    public String updateDate() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy h:mm a");
        return "Date Last Saved: " + format.format(date);
    }
}
