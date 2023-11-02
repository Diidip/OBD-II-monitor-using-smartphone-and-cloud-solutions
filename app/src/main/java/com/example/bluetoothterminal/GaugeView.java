package com.example.bluetoothterminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ekn.gruzer.gaugelibrary.ArcGauge;
import com.ekn.gruzer.gaugelibrary.Range;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import java.util.ArrayList;

public class GaugeView extends AppCompatActivity {

    private static final long UPLOAD_INTERVAL = 16;
    private static final int PERMISSION_FINE_LOCATION = 2;
    private static final int PERMISSION_COARSE_LOCATION = 3;
    private static final long UPDATE_TIME = 2;
    private static final long FASTEST_UPDATE_INTERVAL = 3;
    private double UPDATE_INTERVAL = 0.5;

    private String API_KEY;

    private static final String SEND_MESSAGE = "01 05 0D 10 11 1F 04" + '\r';

    ArcGauge engineCoolantGauge,speedGauge,massAirFlowGauge, engineLoadGauge;
    private int throttlePosition, coolantTemp, massAirFlow, timeRun, vehicleSpeed,engineLoad;
    private double fuelCons;

    private Range range = new Range();

    Button btn_return;
    TextView distanceTraveled;
    BluetoothUtils bluetoothUtils;
    Context context;

    FusedLocationProviderClient fusedLocationProviderClient;

    //Location request
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    private String latitude;
    private String longitude;
    float distance = 0;

    Location lastLocation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gauge_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        engineCoolantGauge = findViewById(R.id.engineCoolantGauge);
        engineLoadGauge = findViewById(R.id.engineLoadGauge);
        speedGauge = findViewById(R.id.speedGauge);
        massAirFlowGauge = findViewById(R.id.massAirGauge);
        btn_return = findViewById(R.id.btn_return);
        distanceTraveled = findViewById(R.id.distanceTraveledTxt);

        this.bluetoothUtils = BluetoothConnection.getApplication().getCurrentBluetoothConnection();
        this.context = BluetoothConnection.getApplication().getContext();
        this.API_KEY = BluetoothConnection.getApplication().getAPI_KEY();

        range.setColor(Color.RED);

        createLocationRequest();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //save location
                updateLocationData(locationResult.getLastLocation());
            }
        };
        startLocationUpdates();
        updateGPS();


        CountDownTimer countDownTimer = new GaugeView.MyCountDownTimer(UPLOAD_INTERVAL *1000,1*1000);
        updateData(countDownTimer);


        CountDownTimer btCountDownTimer = new GaugeView.BTCountDownTimer((long) (UPDATE_INTERVAL *1000), 1*1000);

        btCountDownTimer.start();

        btn_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countDownTimer.cancel();
                btCountDownTimer.cancel();
                setZero();
                sendHTTPMessage();
                stopLocationUpdates();
                finish();
            }
        });
    }

    private void setZero() {
        distance = 0;
        massAirFlow = 0;
        engineLoad = 0;
        vehicleSpeed = 0;
    }

    private void createLocationRequest() {
        this.locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_TIME * 1000);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //permission needs to be granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_COARSE_LOCATION);
            }
        } else {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        // logic to handle location object
                        updateLocationData(location);
                    } else {
                        Toast.makeText(GaugeView.this, "Error: Location object null", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void updateLocationData(Location location) {
        this.latitude = String.valueOf(location.getLatitude());
        this.longitude = String.valueOf(location.getLongitude());

        if (lastLocation != null){
            Log.v("Location", "true");
            if (lastLocation.distanceTo(location) > 10){
                distance = distance + lastLocation.distanceTo(location);
                distance = Math.round(distance);
                //distance = distance*10;
            }
            distanceTraveled.setText(String.valueOf(distance));
        }
        lastLocation = location;
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_COARSE_LOCATION);
                startLocationUpdates();
            }
        } else {
            Toast.makeText(context, "Permissions granted", Toast.LENGTH_SHORT).show();
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }
    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Toast.makeText(context, "No longer tracking location", Toast.LENGTH_SHORT).show();
    }

    private void updateData(CountDownTimer countDownTimer) {
        countDownTimer.start();
    }

    private void updateGaugeUI() {
        updateEngineCoolantGauge();
        updateEngineLoadGauge();
        updateSpeedGauge();
        updateMassAirFlowGauge();
        throttlePosition = BluetoothConnection.getApplication().getThrottlePosition();
        timeRun = BluetoothConnection.getApplication().getTimeRun();
        Log.v("Gaugeupdate", "true");
    }

    private void updateMassAirFlowGauge() {
        massAirFlow = BluetoothConnection.getApplication().getMassAirFlow();
        vehicleSpeed = BluetoothConnection.getApplication().getVehicleSpeed();
        massAirFlowGauge.addRange(range);
        double var1 = 14.7*820*vehicleSpeed;
        //if (var1 == 0) var1 = 1;
        double var2 = (massAirFlow)/(var1);
        fuelCons = var2 *(360000) *10;
        fuelCons = Math.round(fuelCons);
        fuelCons = fuelCons /10;
        if (fuelCons > 20) fuelCons = 0;
        massAirFlowGauge.setMinValue(0);
        massAirFlowGauge.setMaxValue(20);
        massAirFlowGauge.setValue(fuelCons);
    }

    private void updateSpeedGauge() {
        vehicleSpeed = BluetoothConnection.getApplication().getVehicleSpeed();
        speedGauge.addRange(range);
        speedGauge.setMinValue(0);
        speedGauge.setMaxValue(255);
        speedGauge.setValue(vehicleSpeed);
    }

    private void updateEngineLoadGauge() {
        engineLoad = BluetoothConnection.getApplication().getEngineLoad();
        engineLoadGauge.addRange(range);
        engineLoadGauge.setMinValue(0);
        engineLoadGauge.setMaxValue(100);
        engineLoadGauge.setValue(engineLoad);
    }

    private void updateEngineCoolantGauge() {
        coolantTemp = BluetoothConnection.getApplication().getCoolantTemp();
        engineCoolantGauge.addRange(range);
        engineCoolantGauge.setMinValue(-40);
        engineCoolantGauge.setMaxValue(215);
        engineCoolantGauge.setValue(coolantTemp);
    }


    private void sendHTTPMessage() {
        String payload = "&field1=" + coolantTemp + "&field2=" + throttlePosition + "&field3=" + vehicleSpeed +
                "&field4=" + massAirFlow + "&field5=" + engineLoad + "&field6=" + distance + "&field7=" + latitude
                + "&field8=" + longitude;

        HTTPMessages httpMessages = new HTTPMessages(payload,context);
        httpMessages.setAPIKey(API_KEY);
        httpMessages.run();
    }


    public class MyCountDownTimer extends CountDownTimer {
        CountDownTimer countDownTimer = this;
        public MyCountDownTimer(long startTime, long interval){
            super(startTime,interval);
        }
        @Override
        public void onTick(long millisUntillFinnished) {
            long time = millisUntillFinnished /1000;
            Log.v("timer", "" + time);
        }
        @Override
        public void onFinish() {
            //Send message
            Log.v("sent:message", " HHTPMessage sent");
            sendHTTPMessage();
            countDownTimer.start();
        }
    }
    public class BTCountDownTimer extends CountDownTimer{
        CountDownTimer countDownTimer = this;
        public BTCountDownTimer(long startTime, long interval){
            super(startTime,interval);
        }
        @Override
        public void onTick(long millisUntillFinnished) {
            long time = millisUntillFinnished /1000;
            Log.v("timer", "" + time);
        }
        @Override
        public void onFinish() {
            //Send message
            BluetoothConnection.getApplication().setMessageloop(1);
            bluetoothUtils.write(SEND_MESSAGE.getBytes());
            Log.v("sent:message", SEND_MESSAGE);
            updateGaugeUI();
            countDownTimer.start();
        }


    }

}