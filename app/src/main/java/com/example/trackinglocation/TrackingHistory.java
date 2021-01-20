package com.example.trackinglocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TrackingHistory extends AppCompatActivity {

    ImageView close, deleteAll;
    RecyclerView dataList;
    TextView emptyMessage;

    String storageType;

    ArrayList<String> keyList = new ArrayList<>();
    ArrayList<String> valueList = new ArrayList<>();

    SharedPreferences sharedPreferences;
    RecycleviewDataListAdapter myAdapter;

    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_history);

        //getting intent
        Intent intent = getIntent();
        //getting string extra from intent "StorageType"
        storageType = intent.getStringExtra("StorageType");

        close = findViewById(R.id.close_history);
        deleteAll = findViewById(R.id.delete_all);
        dataList = findViewById(R.id.tracking_datas);
        emptyMessage = findViewById(R.id.empty_message);

        // read data from SharedPreferences/FirebaseDatabase
        assert storageType != null;
        if (storageType.equals("Local")) {
            sharedPreferences = getSharedPreferences("MAP_ROUTS", MODE_PRIVATE);
            java.util.Map<String, ?> keys = sharedPreferences.getAll();
            keyList.addAll(keys.keySet());
            for (int i = 0; i < keyList.size(); i++) {
                valueList.add(String.valueOf(keys.get(keyList.get(i))));
            }
            if (empty(keyList)) {
                // setting adapter
                myAdapter = new RecycleviewDataListAdapter
                        (TrackingHistory.this, keyList, valueList);
                dataList.setAdapter(myAdapter);
                dataList.setLayoutManager(new LinearLayoutManager(this));
            }

        } else {
            databaseReference = FirebaseDatabase.getInstance()
                    .getReference().child("MAP_ROUTS");
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot childDataSnapshot : snapshot.getChildren()) {
                        String key = childDataSnapshot.getKey();
                        String value = String.valueOf(childDataSnapshot.getValue());
                        valueList.add(value);
                        keyList.add(key);
                    }
                    if (empty(keyList)) {
                        myAdapter = new RecycleviewDataListAdapter
                                (TrackingHistory.this, keyList, valueList);
                        dataList.setAdapter(myAdapter);
                        dataList.setLayoutManager(new LinearLayoutManager(TrackingHistory.this));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

        // close button listener
        close.setOnClickListener(v -> finish());

        // deleteAll button listener
        // creating an alertDialog for confirmation
        deleteAll.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("From " + storageType + " Storage")
                .setIcon(R.drawable.ic_delete_all)
                .setMessage(R.string.delete_diolge)
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.cancel();
                    // delete item from local storage(shared Preferences)
                    if (storageType.equals("Local")) {
                        sharedPreferences.edit().clear().apply();
                        keyList.clear();
                        // modify here if you want to delete selectively
                        // (edit.remove("key_name").apply();)
                    } else if (storageType.equals("Firebase")) {
                        // delete item from online storage(Firebase Database)
                        databaseReference = FirebaseDatabase.getInstance()
                                .getReference().child("MAP_ROUTS");
                        databaseReference.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot childDataSnapshot : snapshot.getChildren()) {
                                    childDataSnapshot.getRef().removeValue();
                                }
                                keyList.clear();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }

                    dataList.setVisibility(View.GONE);
                    emptyMessage.setVisibility(View.VISIBLE);
                })
                .setNegativeButton("No", (dialog, which) -> dialog.cancel())
                .show());
    }

    private boolean empty(ArrayList<String> kl) {
        if (kl.isEmpty()) {
            dataList.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
            deleteAll.setEnabled(false);
            return false;
        } else {
            return true;
        }
    }
}