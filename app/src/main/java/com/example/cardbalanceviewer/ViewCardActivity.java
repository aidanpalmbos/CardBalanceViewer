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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class ViewCardActivity extends AppCompatActivity {

    int cardId;
    String originalPublicCardName;
    Boolean isPublicCard;

    TextView dateEdited;
    TextView unsavedChanges;
    EditText editName;
    EditText editCardValue;
    EditText changeValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent editCardIntent = getIntent();
        cardId = editCardIntent.getIntExtra("cardId", -1);
        isPublicCard = editCardIntent.getBooleanExtra("isPublic", false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_view);

        //Show current user activity:
        if(isPublicCard) this.setTitle("Edit Shared Card");
        else this.setTitle("Edit Card");

        //Gather necessary components:
        dateEdited = findViewById(R.id.dateEdited);
        unsavedChanges = findViewById(R.id.labelUnsavedChanges);
        editName = findViewById(R.id.editCardName);
        editCardValue = findViewById(R.id.editCardValue);
        changeValue = findViewById(R.id.changeValue);

        //Put correct values from list into activity:
        if(isPublicCard) { //Card is saved to a server
            int publicListSize = MainActivity.publicCardList.size();
            if(cardId > publicListSize || publicListSize <= 0) {
                Intent transfer = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(transfer);
                return;
            }
            originalPublicCardName = MainActivity.publicCardList.get(cardId);
            editName.setText(originalPublicCardName);
            dateEdited.setText(MainActivity.publicDateChangedList.get(cardId));
            editCardValue.setText(MainActivity.publicBalanceList.get(cardId));
        }
        else if(cardId != -1) { //Card is not Public but has been made before
            editName.setText(MainActivity.privateCardList.get(cardId));
            dateEdited.setText(MainActivity.privateDateChangedList.get(cardId));
            editCardValue.setText(MainActivity.privateBalanceList.get(cardId));
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

        //Quick update value:
        Button addButton = findViewById(R.id.addButton);
        Button subButton = findViewById(R.id.subButton);
        addButton.setOnClickListener(v -> {
            quickValueUpdate(true);
        });
        subButton.setOnClickListener(v -> {
            quickValueUpdate(false);
        });

        //Save Button:
        Button saveButton = findViewById(R.id.saveButton);
        if(isPublicCard) saveButton.setOnClickListener(v ->  publicSave());
        else saveButton.setOnClickListener(v -> privateSave());
    }

    /**Change the current value by quickly adding or subtracting a set value.*/
    public void quickValueUpdate(boolean add) {
        if(Float.parseFloat(changeValue.getText().toString()) == 0.00f) { return; }
        float curr = Float.parseFloat(editCardValue.getText().toString());
        float change = Float.parseFloat(changeValue.getText().toString());

        if(add) editCardValue.setText(String.format("%.2f", curr + change));
        else editCardValue.setText(String.format("%.2f", curr - change));

        unsavedChanges.setText("Unsaved Changes Made");
    }

    /**Save the private card to sharedPreferences.*/
    public void privateSave() {
        dateEdited.setText(updateDate());
        unsavedChanges.setText("");

        if(cardId == -1) { //New Card
            MainActivity.privateCardList.add(editName.getText().toString());
            MainActivity.privateBalanceList.add(editCardValue.getText().toString());
            MainActivity.privateDateChangedList.add(dateEdited.getText().toString());
            cardId = MainActivity.privateCardList.size() - 1;
        } else { //View Old Card
            MainActivity.privateCardList.set(cardId, editName.getText().toString());
            MainActivity.privateBalanceList.set(cardId, editCardValue.getText().toString());
            MainActivity.privateDateChangedList.set(cardId, dateEdited.getText().toString());
        }

        MainActivity.privateCardAdapter.notifyDataSetChanged();

        SharedPreferences sharedPreferences = getApplication().getSharedPreferences("com.example.cardbalanceviewer", Context.MODE_PRIVATE);
        HashSet<String> putCard = new HashSet(MainActivity.privateCardList);
        HashSet<String> putBalance = new HashSet(MainActivity.privateBalanceList);
        HashSet<String> putDate = new HashSet(MainActivity.privateDateChangedList);
        sharedPreferences.edit().putStringSet("setCards", putCard).apply();
        sharedPreferences.edit().putStringSet("setBalances", putBalance).apply();
        sharedPreferences.edit().putStringSet("setDates", putDate).apply();

        //Go back to main menu:
        Toast.makeText(this, "Card Saved", Toast.LENGTH_SHORT).show();
        Intent transfer = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(transfer);
    }
    /**Save the public card to the server.*/
    public void publicSave() {
        dateEdited.setText(updateDate());
        unsavedChanges.setText("");

        String name = editName.getText().toString();
        String value = editCardValue.getText().toString();

        if(!RESTHelper.checkConnection()) {
            MainActivity.clearPublicListsOnError(this, "No Connection");
            return;
        }
        Context currentContext = this;
        try {
            String[] cardInfo = new String[]{"apiKey", RESTHelper.apiKey, "cardName", name, "balance", value, "originalName", originalPublicCardName};
            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.newCall(RESTHelper.createRequest("saveCard", cardInfo)).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String validResponse = response.body().string();
                    ViewCardActivity.this.runOnUiThread(() -> {
                        if(RESTHelper.checkResponse(validResponse)) {
                            MainActivity.publicCardList.set(cardId, editName.getText().toString());
                            MainActivity.publicBalanceList.set(cardId, editCardValue.getText().toString());
                            MainActivity.publicDateChangedList.set(cardId, dateEdited.getText().toString());
                            MainActivity.publicCardAdapter.notifyDataSetChanged();


                            Toast.makeText(currentContext, "Shared Card Saved", Toast.LENGTH_SHORT).show();
                            Intent transfer = new Intent(currentContext, MainActivity.class);
                            startActivity(transfer);
                        }
                        else {
                            MainActivity.clearPublicListsOnError(currentContext, "Error with Server");
                            Intent transfer = new Intent(currentContext, MainActivity.class);
                            startActivity(transfer);
                        }
                    });
                }
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    ViewCardActivity.this.runOnUiThread(() -> {
                        MainActivity.clearPublicListsOnError(currentContext, "Error with Server");
                        Intent transfer = new Intent(currentContext, MainActivity.class);
                        startActivity(transfer);
                    });
                }
            });
        }
        catch (Exception error) {
            MainActivity.clearPublicListsOnError(this, "Error");
            Intent transfer = new Intent(this, MainActivity.class);
            startActivity(transfer);
        }
    }

    /**Create and format current date and time. Returns new Date String.*/
    public String updateDate() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy h:mm a");
        return "Date Last Saved: " + format.format(date);
    }
}
