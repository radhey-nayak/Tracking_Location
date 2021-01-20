package com.example.trackinglocation;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class Map extends AppCompatActivity {

    TextView timer, goFinishText;
    CardView goFinishCV;
    SupportMapFragment gMap;

    Long startTime, sec, min, hour, timeInMilliseconds;
    String totalTime, hours, minutes, seconds, milliSeconds, name,
            extraIntentType, value, storageType;
    Boolean serviceStatus = false, doubleBackToExitPressedOnce = true;
    int REQUEST_CHECK_SETTINGS = 102;

    FusedLocationProviderClient fusedLocationClient;
    LocationRequest mLocationRequest;
    LocationCallback mLocationCallback;
    LocationSettingsRequest.Builder builder;
    LatLng latLng, lastLatLng;
    Handler handler;
    SharedPreferences sharedPreferences;
    ArrayList<String> stringPaths = new ArrayList<>();
    ArrayList<LatLng> latLngPaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();
        extraIntentType = intent.getStringExtra("ExtraIntentType");
        storageType = intent.getStringExtra("StorageType");

        timer = findViewById(R.id.timmer);
        goFinishText = findViewById(R.id.go_finish_text);
        goFinishCV = findViewById(R.id.go_finish_cv);
        gMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.gMap);

        // if user call this activity from TrackingHistory
        if (extraIntentType.equals("History")) {
            goFinishText.setText(R.string.close); // changing text of goFinishText to close
            value = intent.getStringExtra("value"); // getting values
            assert value != null;
            value = value.replaceAll("[\\[\\]\"]", ""); // removing squire braces & double quotes
            String[] valuesData = Objects.requireNonNull(value).split(","); // splitting by comma

            // extracting total time and set into timer TextView
            totalTime = valuesData[valuesData.length - 1];
            timer.setText(totalTime);

            // converting location coordinate string to LatLng type
            for (int i = 0; i < valuesData.length - 1; i++) {
                String[] points = valuesData[i].split("/");
                double lat = Double.parseDouble(points[0]);
                double lng = Double.parseDouble(points[1]);
                latLngPaths.add(new LatLng(lat, lng));
            }
            // setting latLng to last index of array
            latLng = latLngPaths.get(latLngPaths.size() - 1);
            // calling map function and sending paths coordinates
            map(latLngPaths);
        }

        // goFinishCV click Listener for start and finish tracking
        goFinishCV.setOnClickListener(v -> {
            if (extraIntentType.equals("History")) {
                finish();

            } else {
                if (serviceStatus) {
                    complete();
                } else {
                    goFinishText.setText(R.string.finish);

                    // stopwatch assign
                    handler = new Handler();
                    startTime = SystemClock.uptimeMillis();
                    handler.postDelayed(runnable, 0);

                    // start getting Location
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                    fetchLastLocation(); // by this we can test our permission request before process start
                    mLocationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            if (locationResult == null) {
                                return;
                            }
                            for (Location location : locationResult.getLocations()) {
                                // Update UI with location data
                                latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                Log.e("CONTINIOUSLOC: ", location.toString());

                                if (!latLng.equals(lastLatLng)) {
                                    latLngPaths.add(latLng);
                                    stringPaths.add(latLng.latitude + "/" + latLng.longitude);
                                }
                                // assigning current location to lastLatLng for checking
                                lastLatLng = latLng;
                                // calling map and sending latLngPaths
                                map(latLngPaths);
                            }
                        }
                    };

                    // requesting location continuously by some interval
                    mLocationRequest = createLocationRequest();
                    builder = new LocationSettingsRequest.Builder()
                            .addLocationRequest(mLocationRequest);
                    checkLocationSetting(builder);

                    serviceStatus = true; // setting service status to true
                    doubleBackToExitPressedOnce = false; // enabling doubleTap for back button
                }
            }
        });
    }

    private void map(ArrayList<LatLng> pathPoints) {
        // starting google map operations
        gMap.getMapAsync(googleMap -> {

            // clearing all marker and line for rewriting
            googleMap.clear();
            // marking starting point
            googleMap.addMarker(new MarkerOptions().position(pathPoints.get(0))
                    .title("Start Point"))
                    .setIcon(BitmapDescriptorFactory.defaultMarker
                            (BitmapDescriptorFactory.HUE_VIOLET));

            PolylineOptions polylineOptions = new PolylineOptions();
            // Adding all the points in the route to LineOptions
            polylineOptions.addAll(pathPoints)
                    .width(8f)
                    .color(Color.RED);

            // drawing line by points
            googleMap.addPolyline(polylineOptions);
            // setting camera
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            // marking current/ending point
            googleMap.addMarker(new MarkerOptions().position(latLng)
                    .title("End Point"))
                    .setIcon(BitmapDescriptorFactory.defaultMarker
                            (BitmapDescriptorFactory.HUE_ROSE));
        });
    }

    private void complete() {
        //stopping handler
        handler.removeCallbacks(runnable);

        // getting date
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd MMM yyyy",
                Locale.getDefault());
        name = sdf.format(new Date());
        // adding total time of run into storage
        stringPaths.add(totalTime);

        if (storageType.equals("Local")) {

            sharedPreferences = getSharedPreferences("MAP_ROUTS", MODE_PRIVATE);
            Gson gson = new Gson();
            String json = gson.toJson(stringPaths); // converting string array to single string
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(name, json); // putting string to storage
            editor.apply();
        } else if (storageType.equals("Firebase")) {

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                    .child("MAP_ROUTS").child(name); // connecting to firebase database
            Gson gson = new Gson();
            String json = gson.toJson(String.valueOf(stringPaths)); // converting string array to single string
            ref.setValue(json); // putting string to storage
        }

        Toast.makeText(this, "Data saved successfully: " + name,
                Toast.LENGTH_SHORT).show();
        stopLocationUpdates(); //stopping watch
        finish();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            if (latLngPaths.size() != 0) {
                if (extraIntentType.equals("NewRout")) {
                    stopLocationUpdates(); // stopping location update
                }
            }
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit(You'll LOSE data!)",
                Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                    this,
                    "You need to accept location permissions to use this app.",
                    REQUEST_CHECK_SETTINGS,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            );
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "You need to accept location permissions to use this app.",
                    REQUEST_CHECK_SETTINGS,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CHECK_SETTINGS) {// If request is cancelled, the result arrays are empty.
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // permission was denied, show alert to explain permission
                new AppSettingsDialog.Builder(this).build().show();
            }
        } else {
            requestPermissions();
        }
    }

    private void fetchLastLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions();
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object for another way
                        Log.e("LAST LOCATION: ", location.toString()); // You will get your last location here
                    }
                });
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(5000L); // 05 sec
        mLocationRequest.setFastestInterval(2000L); // 02 sec
        //mLocationRequest.setSmallestDisplacement(5); // not needed; this for setting minimum displacement only
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private void checkLocationSetting(LocationSettingsRequest.Builder builder) {

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            startLocationUpdates();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                AlertDialog.Builder builder1 = new AlertDialog.Builder(Map.this);
                builder1.setTitle("Continuous Location Request");
                builder1.setMessage("This request is essential to get location update continuously");
                builder1.create();
                builder1.setPositiveButton("OK", (dialog, which) -> {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    try {
                        resolvable.startResolutionForResult(Map.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e1) {
                        e1.printStackTrace();
                    }
                });
                builder1.setNegativeButton("Cancel", (dialog, which) -> Toast.makeText(Map.this,
                        "Location update permission not granted", Toast.LENGTH_LONG).show());
                builder1.show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                startLocationUpdates();
            } else {
                checkLocationSetting(builder);
            }
        }
    }

    public void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions();

            return;
        }
        fusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    //Stop Watch logic
    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            hour = TimeUnit.MILLISECONDS.toHours(timeInMilliseconds);
            timeInMilliseconds -= TimeUnit.HOURS.toMillis(hour);
            min = TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds);
            timeInMilliseconds -= TimeUnit.MINUTES.toMillis(min);
            sec = TimeUnit.MILLISECONDS.toSeconds(timeInMilliseconds);
            timeInMilliseconds -= TimeUnit.SECONDS.toMillis(sec);
            timeInMilliseconds /= 10;

            if (hour < 10) hours = "0" + hour;
            if (min < 10) minutes = ":0" + min;
            else minutes = ":" + min;
            if (sec < 10) seconds = ":0" + sec;
            else seconds = ":" + sec;
            if (timeInMilliseconds < 10) milliSeconds = ":0" + timeInMilliseconds;
            else milliSeconds = ":" + timeInMilliseconds;

            totalTime = hours + minutes + seconds + milliSeconds;
            //System.out.println("\ntime: "+ totalTime);
            timer.setText(totalTime);

            handler.postDelayed(this, 0);
        }

    };
}