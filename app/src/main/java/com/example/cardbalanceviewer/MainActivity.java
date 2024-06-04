package com.example.cardbalanceviewer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity{

    static ArrayList<String> card = new ArrayList<>();
    static ArrayList<String> balance = new ArrayList<>();
    static ArrayList<String> dateChanged = new ArrayList<>();
    static ArrayAdapter arrayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("Select Card");

        ListView list = findViewById(R.id.cardList);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.example.cardbalanceviewer", Context.MODE_PRIVATE);
        HashSet<String> _cards = (HashSet<String>) sharedPreferences.getStringSet("setCards", null);
        HashSet<String> _balances = (HashSet<String>) sharedPreferences.getStringSet("setBalances", null);
        HashSet<String> _dates = (HashSet<String>) sharedPreferences.getStringSet("setDates", null);

        if(_cards != null) {
            card = new ArrayList(_cards);
        }
        if(_balances != null) {
            balance = new ArrayList(_balances);
        }
        if(_dates != null) {
            dateChanged = new ArrayList(_dates);
        }

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, card);
        list.setAdapter(arrayAdapter);

        //View Card
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ViewCardActivity.class);
                intent.putExtra("cardId", position);
                startActivity(intent);
            }
        });

        //Delete Card Popup
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int cardToDelete = position;
                new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Delete Card").setMessage("Delete this card?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                card.remove(cardToDelete);
                                balance.remove(cardToDelete);
                                dateChanged.remove(cardToDelete);

                                arrayAdapter.notifyDataSetChanged(); //Update Card List

                                HashSet<String> putCard = new HashSet(MainActivity.card);
                                HashSet<String> putBalance = new HashSet(MainActivity.balance);
                                HashSet<String> putDate = new HashSet(MainActivity.dateChanged);
                                sharedPreferences.edit().putStringSet("setCards", putCard).apply();
                                sharedPreferences.edit().putStringSet("setBalances", putBalance).apply();
                                sharedPreferences.edit().putStringSet("setDates", putDate).apply();
                            }
                        }).setNegativeButton("No", null).show();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    //Main Menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        //Menu item 1: Add card
        if (item.getItemId() == R.id.addCard) {
            Intent intent = new Intent(getApplicationContext(), ViewCardActivity.class);
            startActivity(intent);
            return true;
        }
        //Menu item 2: Delete all
        if(item.getItemId() == R.id.deleteAllCards) {
            new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Reset All").setMessage("Delete all cards?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            card.clear();
                            balance.clear();
                            dateChanged.clear();

                            arrayAdapter.notifyDataSetChanged(); //Update Card List

                            HashSet<String> putCard = new HashSet(MainActivity.card);
                            HashSet<String> putBalance = new HashSet(MainActivity.balance);
                            HashSet<String> putDate = new HashSet(MainActivity.dateChanged);
                            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.example.cardbalanceviewer", Context.MODE_PRIVATE);
                            sharedPreferences.edit().putStringSet("setCards", putCard).apply();
                            sharedPreferences.edit().putStringSet("setBalances", putBalance).apply();
                            sharedPreferences.edit().putStringSet("setDates", putDate).apply();
                        }
                    }).setNegativeButton("No", null).show();
            return true;
        }

        return false;
    }
}
