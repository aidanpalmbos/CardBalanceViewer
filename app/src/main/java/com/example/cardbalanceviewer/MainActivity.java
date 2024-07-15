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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity{

    static ArrayList<String> privateCardList = new ArrayList<>();
    static ArrayList<String> publicCardList = new ArrayList<>();
    static ArrayList<String> privateBalanceList = new ArrayList<>();
    static ArrayList<String> publicBalanceList = new ArrayList<>();
    static ArrayList<String> privateDateChangedList = new ArrayList<>();
    static ArrayList<String> publicDateChangedList = new ArrayList<>();
    static ArrayAdapter privateCardAdapter;
    static ArrayAdapter publicCardAdapter;
    static SharedPreferences sharedPrefs;
    RequestQueue queue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPrefs = this.getSharedPreferences("com.example.cardbalanceviewer", Context.MODE_PRIVATE);
        queue = Volley.newRequestQueue(this);

        //Show current user activity:
        this.setTitle("Select Card");

        //
        // Public Cards
        //
        ListView publicCardListView = findViewById(R.id.publicCardList);

        //
        //Private Cards
        //
        ListView privateCardListView = findViewById(R.id.privateCardList);
        loadPrivateLists();

        privateCardAdapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, MainActivity.privateCardList);
        privateCardListView.setAdapter(privateCardAdapter);

        publicCardAdapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, MainActivity.publicCardList);
        publicCardListView.setAdapter(publicCardAdapter);

        privateCardAdapter.notifyDataSetChanged();
        publicCardAdapter.notifyDataSetChanged();

        //Delete Card Popup
        privateCardListView.setOnItemLongClickListener(deleteCardOnLongClick());

        //View Card
        privateCardListView.setOnItemClickListener(privateCardOnClick());
        publicCardListView.setOnItemClickListener(publicCardOnClick());
    }

    @Override
    public void onResume() {
        if(RESTClass.checkConnection()) {
            loadPublicLists();
        }
        else {
            clearPublicListsOnError(this,"No Connection");
        }

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    //Main Menu:
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        //Menu Option 1: Add a private card
        if (item.getItemId() == R.id.addPrivateCard) {
            menuCreatePrivate();
        }
        //Menu Option 2: Add a public card
        if(item.getItemId() == R.id.addPublicCard) {
            menuCreatePublic();
        }
        //Menu Option 3: Delete all cards
        if(item.getItemId() == R.id.deleteAllPrivateCards) {
            menuDeleteAll();
        }

        //Nothing else was selected:
        return false;
    }

    @Override
    public void onBackPressed() {
        this.finishAffinity();
        super.onBackPressed();
    }

    /**Load private cards from sharedPreferences.*/
    public void loadPrivateLists() {
        HashSet<String> _cards = (HashSet<String>) sharedPrefs.getStringSet("setCards", null);
        HashSet<String> _balances = (HashSet<String>) sharedPrefs.getStringSet("setBalances", null);
        HashSet<String> _dates = (HashSet<String>) sharedPrefs.getStringSet("setDates", null);

        //Use Pre-existing cards:
        if(_cards != null) {
            privateCardList = new ArrayList(_cards);
        }
        if(_balances != null) {
            privateBalanceList = new ArrayList(_balances);
        }
        if(_dates != null) {
            privateDateChangedList = new ArrayList(_dates);
        }
    }
    /**Load shared cards from the server.*/
    public void loadPublicLists() {
        try {
            StringRequest getRequest = new StringRequest(Request.Method.POST, RESTClass.SetupString("loadCards"), response -> {
                clearPublicLists();
                publicCardAdapter.notifyDataSetChanged();

                if(!RESTClass.checkResponse(response)) {
                    Toast.makeText(this, "Error with Server", Toast.LENGTH_SHORT).show();
                    return;
                }

                stringToPublicCards(response);
                publicCardAdapter.notifyDataSetChanged();
            }, error -> {
                clearPublicListsOnError(this, "Error with Server");
            });
            queue.add(getRequest);
        }
        catch (Exception error){
            clearPublicListsOnError(this, "Error with Server");
        }
    }
    /**Format the Server Response into their respective lists.*/
    public void stringToPublicCards(String response) {
        String tempString;
        while (response.length() > 1) {

            tempString = response.substring(0, response.indexOf("|"));
            publicCardList.add(tempString);
            response = response.substring(tempString.length() + 1);

            tempString = response.substring(0, response.indexOf("|"));
            publicBalanceList.add(String.format("%.2f", Float.valueOf(tempString)));
            response = response.substring(tempString.length() + 1);

            tempString = response.substring(0, response.indexOf("|"));
            SimpleDateFormat databaseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date;
            try {
                date = databaseFormat.parse(tempString);
            } catch (ParseException e) { //Something dumb happened, so just skip this
                publicDateChangedList.add(tempString);
                response = response.substring(tempString.length() + 1);
                continue;
            }
            SimpleDateFormat programFormat = new SimpleDateFormat("MMMM dd, yyyy h:mm a");
            publicDateChangedList.add("Date Last Saved: " + programFormat.format(date));
            response = response.substring(tempString.length() + 1);
        }
    }

    public static void clearPrivateLists() {
        privateCardList.clear();
        privateBalanceList.clear();
        privateDateChangedList.clear();
    }
    public static void clearPublicLists() {
        publicCardList.clear();
        publicBalanceList.clear();
        publicDateChangedList.clear();
    }
    /**In an error, clear the public list and make a Toast Popup.*/
    public static void clearPublicListsOnError(Context context, String error) {
        clearPublicLists();
        publicCardAdapter.notifyDataSetChanged();
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
    }

    public AdapterView.OnItemLongClickListener deleteCardOnLongClick() {
        return new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int cardToDelete = position;
                new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Delete Card").setMessage("Delete this card?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                privateCardList.remove(cardToDelete);
                                privateBalanceList.remove(cardToDelete);
                                privateDateChangedList.remove(cardToDelete);

                                privateCardAdapter.notifyDataSetChanged(); //Update Card List

                                HashSet<String> putCard = new HashSet(MainActivity.privateCardList);
                                HashSet<String> putBalance = new HashSet(MainActivity.privateBalanceList);
                                HashSet<String> putDate = new HashSet(MainActivity.privateDateChangedList);
                                sharedPrefs.edit().putStringSet("setCards", putCard).apply();
                                sharedPrefs.edit().putStringSet("setBalances", putBalance).apply();
                                sharedPrefs.edit().putStringSet("setDates", putDate).apply();
                            }
                        }).setNegativeButton("No", null).show();
                return true;
            }
        };
    }

    public AdapterView.OnItemClickListener privateCardOnClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent newCardIntent = new Intent(getApplicationContext(), ViewCardActivity.class);
                newCardIntent.putExtra("isPublic", false);
                newCardIntent.putExtra("cardId", position);
                startActivity(newCardIntent);
            }
        };
    }
    public AdapterView.OnItemClickListener publicCardOnClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!RESTClass.checkConnection()) {
                    clearPublicListsOnError(getApplicationContext(), "No Connection");
                    return;
                }

                loadPublicLists();
                try {
                    Thread.sleep(RESTClass.threadSleep);
                } catch (InterruptedException e) {
                    return;
                }

                Intent newCardIntent = new Intent(getApplicationContext(), ViewCardActivity.class);
                newCardIntent.putExtra("isPublic", true);
                newCardIntent.putExtra("cardId", position);
                startActivity(newCardIntent);
            }
        };
    }

    public boolean menuCreatePrivate() {
        Intent addCardIntent = new Intent(this, ViewCardActivity.class);
        startActivity(addCardIntent);
        return true;
    }
    public boolean menuCreatePublic() {
        if(!RESTClass.checkConnection()) {
            clearPublicListsOnError(this, "No Connection");
            return false;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        try {
            StringRequest getRequest = new StringRequest(Request.Method.POST, RESTClass.SetupString("createCard"), response -> {
                if(RESTClass.checkResponse(response)) {
                    publicCardList.add(response);
                    publicBalanceList.add("0");
                    publicDateChangedList.add("");
                    publicCardAdapter.notifyDataSetChanged();

                    Intent newCardIntent = new Intent(this, ViewCardActivity.class);
                    newCardIntent.putExtra("isPublic", true);
                    newCardIntent.putExtra("cardId", publicCardList.size() - 1);
                    startActivity(newCardIntent);
                }
                else {
                    clearPublicListsOnError(this, "Error on Server");
                }
            }, error -> {
                clearPublicListsOnError(this, "Error on Server");
            });
            queue.add(getRequest);
            return true;
        }
        catch (Exception error) {
            clearPublicListsOnError(this, "Error on Server");
            return false;
        }
    }
    public boolean menuDeleteAll() {
        new AlertDialog.Builder(MainActivity.this).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Delete All")
                .setMessage("This will delete all of your cards, but the shared ones will remain.\n\nAre you sure?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearPrivateLists();
                        privateCardAdapter.notifyDataSetChanged(); //Update Card List

                        HashSet<String> putCard = new HashSet(MainActivity.privateCardList);
                        HashSet<String> putBalance = new HashSet(MainActivity.privateBalanceList);
                        HashSet<String> putDate = new HashSet(MainActivity.privateDateChangedList);
                        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.example.cardbalanceviewer", Context.MODE_PRIVATE);
                        sharedPreferences.edit().putStringSet("setCards", putCard).apply();
                        sharedPreferences.edit().putStringSet("setBalances", putBalance).apply();
                        sharedPreferences.edit().putStringSet("setDates", putDate).apply();
                    }
                }).setNegativeButton("No", null).show();
        return true;
    }
}
