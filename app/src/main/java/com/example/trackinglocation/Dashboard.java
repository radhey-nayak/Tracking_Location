package com.example.trackinglocation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class Dashboard extends AppCompatActivity {

    CardView start, viewHistory;
    SwitchMaterial storageSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //here we request permission so that user will not face any interference while tracking
        requestPermissions();

        start = findViewById(R.id.start);
        viewHistory = findViewById(R.id.view_history);
        storageSelection = findViewById(R.id.storage_switch);

        // here we select our storage type
        storageSelection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (storageSelection.isChecked()) {
                storageSelection.setText(R.string.firebase);
                Toast.makeText(this, "Switching to firebase database",
                        Toast.LENGTH_SHORT).show();
            } else {
                storageSelection.setText(R.string.local);
                Toast.makeText(this, "Switching to local storage",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // this listener will take user to Map activity
        start.setOnClickListener(view -> {

            // only if all permissions were granted else this'll request permission again
            if (ActivityCompat.checkSelfPermission(Dashboard.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(Dashboard.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Intent i1 = new Intent(Dashboard.this,
                        Map.class);
                i1.putExtra("ExtraIntentType", "NewRout");
                i1.putExtra("StorageType", storageSelection.getText());
                startActivity(i1);

            } else {
                requestPermissions();
            }

        });

        // this listener will take user to TrackingHistory activity
        viewHistory.setOnClickListener(view -> {
            Intent i2 = new Intent(Dashboard.this, TrackingHistory.class);
            i2.putExtra("StorageType", storageSelection.getText());
            startActivity(i2);
        });

    }

    //requesting permission here by the os version
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                    this,
                    "You need to accept location permissions to use this app.",
                    123,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ); // if yes this will take you to onRequestPermissionsResult(Override method)
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "You need to accept location permissions to use this app.",
                    123,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ); // if yes this will take you to onRequestPermissionsResult(Override method)
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 123) {// If request is cancelled, the result arrays are empty.
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // permission was denied, show alert to explain permission
                new AppSettingsDialog.Builder(this).build().show();
            }
        } else {
            requestPermissions();
        }
    }
}