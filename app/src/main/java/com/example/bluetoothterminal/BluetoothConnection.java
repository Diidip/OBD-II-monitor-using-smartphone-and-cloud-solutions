package com.example.bluetoothterminal;

import android.app.Application;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

public class BluetoothConnection extends Application {

    private static BluetoothConnection sInstance;

    private int throttlePosition, coolantTemp, massAirFlow, timeRun, vehicleSpeed,engineLoad;

    private String latitude;
    private String longitude;

    private String API_KEY;


    public static BluetoothConnection getApplication() {
        return sInstance;
    }

    BluetoothUtils bluetoothUtils = null;
    Context context;
    public int messageloop = 1;

    public static BluetoothConnection getsInstance() {
        return sInstance;
    }

    public static void setsInstance(BluetoothConnection sInstance) {
        BluetoothConnection.sInstance = sInstance;
    }

    public int getThrottlePosition() {
        return throttlePosition;
    }

    public void setThrottlePosition(int throttlePosition) {
        this.throttlePosition = throttlePosition;
    }

    public int getCoolantTemp() {
        return coolantTemp;
    }

    public void setCoolantTemp(int coolantTemp) {
        this.coolantTemp = coolantTemp;
    }

    public int getMassAirFlow() {
        return massAirFlow;
    }

    public void setMassAirFlow(int massAirFlow) {
        this.massAirFlow = massAirFlow;
    }

    public int getTimeRun() {
        return timeRun;
    }

    public void setTimeRun(int timeRun) {
        this.timeRun = timeRun;
    }

    public int getVehicleSpeed() {
        return vehicleSpeed;
    }

    public void setVehicleSpeed(int vehicleSpeed) {
        this.vehicleSpeed = vehicleSpeed;
    }

    public int getEngineLoad() {
        return engineLoad;
    }

    public void setEngineLoad(int engineLoad) {
        this.engineLoad = engineLoad;
    }

    public void onCreate() {
        super.onCreate();

        sInstance = this;
    }

    public void setupBluetoothConnection(BluetoothUtils bluetoothUtils)
    {
        this.bluetoothUtils = bluetoothUtils;
        // Either setup your connection here, or pass it in
    }

    public BluetoothUtils getCurrentBluetoothConnection()
    {
        return bluetoothUtils;
    }

    public void setContext(Context context)
    {
        this.context = context;
        // Either setup your connection here, or pass it in
    }

    public Context getContext()
    {
        return context;
    }

    public int getMessageloop() {
        return messageloop;
    }

    public void setMessageloop(int messageloop) {
        this.messageloop = messageloop;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getAPI_KEY() {
        return API_KEY;
    }

    public void setAPI_KEY(String API_KEY) {
        this.API_KEY = API_KEY;
    }
}
