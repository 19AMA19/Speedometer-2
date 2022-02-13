package com.dtsoftware.speedometer;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {


    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    startLocationUpdates();
                } else {
                    // Permission is denied
//                    Snackbar.make(findViewById(R.id.root), getString(R.string.location_permission_msg),
//                            Snackbar.LENGTH_LONG)
//                            .show();
                }
            });


    private ImageView ivAguja;
    private TextView tvCurrentSpeed, tvMaxSpeed, tvAvgSpeed, tvDistance;
    private Chronometer chronometer;
    private int maxSpeed = 0;
    private float distance = 0, currentRotation = 0;
    private Location prevLocation = null;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        ivAguja = findViewById(R.id.ivAguja);
        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);
        tvMaxSpeed = findViewById(R.id.tvMaxSpeed);
        tvAvgSpeed = findViewById(R.id.tvAvgSpeed);
        tvDistance = findViewById(R.id.tvDistance);
        chronometer = findViewById(R.id.chronometer);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                setSpeed(locationResult.getLastLocation());
            }
        };

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }


    private void setSpeed(Location location) {

        float speedKmh = (float) (location.getSpeed() * 3.6);
        float newRotation = 0;

        if (speedKmh > maxSpeed)
            maxSpeed = (int) speedKmh;

        if (prevLocation != null)
            distance += location.distanceTo(prevLocation);

        if (speedKmh >= 4 && speedKmh <= 260) {
            newRotation = speedKmh;
        } else if (speedKmh > 260) {
            newRotation = 260;
        } else if (speedKmh < 4) {
            newRotation = 0;
        }
        Log.d("SPEED", (int) speedKmh + "kmh");

        RotateAnimation rotateAnimation1 = new RotateAnimation(currentRotation, newRotation,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation1.setDuration(1100);
        rotateAnimation1.setFillAfter(true);
        ivAguja.startAnimation(rotateAnimation1);
        Log.d("ROTATION", "from " + (int) currentRotation + "    to " + newRotation);

        tvCurrentSpeed.setText(String.valueOf((int) speedKmh));
        tvMaxSpeed.setText(String.valueOf(maxSpeed));
        tvDistance.setText(String.format(Locale.US, "%.1f", distance / 1000));

        long seconds = TimeUnit.MILLISECONDS.toSeconds(SystemClock.elapsedRealtime() - chronometer.getBase());
        tvAvgSpeed.setText(String.valueOf((int) ((distance / seconds) * 3.6)));

        Log.d("TIME HRS", String.valueOf(TimeUnit.MILLISECONDS.toHours(SystemClock.elapsedRealtime() - chronometer.getBase())));

        currentRotation = newRotation;
        prevLocation = location;
    }


    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

}