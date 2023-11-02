package com.example.bluetoothterminal;

import static com.android.volley.VolleyLog.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.RunnableFuture;

public class MainActivity extends AppCompatActivity implements Serializable{

    private Context context;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothUtils bluetoothUtils;

    private ListView listViewTerminal;
    private EditText createTextSend;
    private Button btnSendMessage, gaugeButton;

    private ArrayAdapter<String> adapterMainTerminal;

    private final int LOCATION_PERMISSION_GRANTED = 101;
    private final int SELECT_DEVICE = 102;

    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";

    public String dataReceived;
    private String connectedDevice;


    private int throttlePosition, coolantTemp, massAirFlow, timeRun, vehicleSpeed,engineLoad;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (message.arg1) {
                        case BluetoothUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case BluetoothUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case BluetoothUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case BluetoothUtils.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) message.obj;
                    String inputBuffer = new String(buffer, 0, message.arg1);
                    if (!inputBuffer.isEmpty()) {
                        adapterMainTerminal.add(connectedDevice + ":" + inputBuffer);
                        dataReceived = inputBuffer;
                        setDataReceivedData(dataReceived);
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) message.obj;
                    String outputBuffer = new String(buffer1);
                    adapterMainTerminal.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = this;

        BluetoothConnection.getApplication().setAPI_KEY("R16UQ9G0K9HHTOTZ");

        init();
        initBluetooth();


        bluetoothUtils = new BluetoothUtils(context, handler);
    }

    private void init() {
        listViewTerminal = findViewById(R.id.list_terminal);
        btnSendMessage = findViewById(R.id.btn_send);
        createTextSend = findViewById(R.id.terminalMessage);

        adapterMainTerminal = new ArrayAdapter(context, R.layout.message_layout);
        listViewTerminal.setAdapter(adapterMainTerminal);

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String message = createTextSend.getText().toString() + '\r';
                if (!message.isEmpty()) {
                    createTextSend.setText("");
                    bluetoothUtils.write(message.getBytes());
                }
            }
        });


        gaugeButton = findViewById(R.id.gaugeButton);
        gaugeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                BluetoothConnection.getApplication().setupBluetoothConnection(bluetoothUtils);
                BluetoothConnection.getApplication().setContext(context);

                Intent intent = new Intent(context, GaugeView.class);
                startActivityForResult(intent, SELECT_DEVICE);
            }
        });
    }



    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "No Bluetooth Found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_devices:
                checkPermissions();
                return true;
            case R.id.menu_enable_bluetooth:
                enableBluetooth();
                return true;
            case R.id.menu_set_API_KEY:
                setAPIkey();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setAPIkey() {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose ThingSpeak Channel");

        // add a radio button list
        String[] animals = {"Channel1", "Channel2", "Channel3"};
        int checkedItem = 0; // Channel1
        builder.setSingleChoiceItems(animals, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user checked an item
                switch (which){
                    case 0:
                        BluetoothConnection.getApplication().setAPI_KEY("R16UQ9G0K9HHTOTZ");
                        break;
                    case 1:
                        BluetoothConnection.getApplication().setAPI_KEY("7H7QYNUGYZSOPNY2");
                        break;
                    case 2:
                        BluetoothConnection.getApplication().setAPI_KEY("UWY05TFROBAZ7HZY");
                        break;

                }

            }
        });
        // add OK and Cancel buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "API Key set: " + BluetoothConnection.getApplication().getAPI_KEY(), Toast.LENGTH_SHORT).show();
                // user clicked OK
            }
        });
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_GRANTED);
        } else {
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            String address = data.getStringExtra("deviceAddress");
            bluetoothUtils.connect(bluetoothAdapter.getRemoteDevice(address));
            Toast.makeText(context, "Address:" + address, Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_GRANTED) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(context, DeviceListActivity.class);
                startActivityForResult(intent, SELECT_DEVICE);
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission required, please grant")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                checkPermissions();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MainActivity.this.finish();
                            }
                        })
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(context, "Enabling Bluetooth", Toast.LENGTH_SHORT).show();
            bluetoothAdapter.enable();
        }
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            startActivity(discoveryIntent);
        }
        else {
            Toast.makeText(context, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothUtils != null){
            bluetoothUtils.stop();
        }
    }

    private void setDataReceivedData(String dataReceived){
        if (dataReceived.matches(".*[0-9A-Fa-f]{2}.*")){
            dataReceived = dataReceived.trim();
            Log.v("message recevied", dataReceived);
            String data[] = dataReceived.split(" ");

            Integer i = 1;
            if (data[0].matches("7E8")){
                while (i<data.length){
                    Integer PID = Integer.parseInt(data[i].trim(), 16);
                    Log.v("PID ", PID.toString());
                    switch (PID){
                        case 16:
                            break;
                        case 15:
                            break;
                        case 65:
                            break;
                        case 5:
                            this.coolantTemp = Integer.parseInt(data[i+1], 16)- 40;
                            Log.v("Coolant data int", String.valueOf(coolantTemp));
                            BluetoothConnection.getApplication().setCoolantTemp(coolantTemp);
                            adapterMainTerminal.add("Coolant temp" + ": " + coolantTemp + " °C");
                            i++;
                            break;
                        case 13:
                            i++;
                            this.vehicleSpeed = Integer.parseInt(data[i].trim(), 16);
                            BluetoothConnection.getApplication().setVehicleSpeed(vehicleSpeed);
                            Log.v("speed data int", String.valueOf(vehicleSpeed));
                            adapterMainTerminal.add("Speed" + ": " + vehicleSpeed+ " km/h");
                            i++;
                            break;
                        case 33:
                            i++;
                            Integer value2 = Integer.parseInt(data[i].trim(), 16);
                            Integer value3 = Integer.parseInt(data[i+1].trim(), 16);
                            massAirFlow = (value3 + value2*256)/100;
                            Log.v("MAF data int", String.valueOf(massAirFlow));
                            BluetoothConnection.getApplication().setMassAirFlow(massAirFlow);
                            adapterMainTerminal.add("MAF" + ": " + massAirFlow + " grams/sec");
                            i++;
                            break;
                        case 17:
                            i++;
                            throttlePosition = Integer.parseInt(data[i].trim(), 16) * 100/255;
                            BluetoothConnection.getApplication().setThrottlePosition(throttlePosition);
                            Log.v("Throttle data int", String.valueOf(throttlePosition));
                            adapterMainTerminal.add("Throttle position" + ": " + throttlePosition + " %");
                            break;
                        case 31:
                            i++;
                            Integer value21 = Integer.parseInt(data[i].trim(), 16);
                            Integer value31 = Integer.parseInt(data[i+1].trim(), 16);
                            this.timeRun = 256 *value21 + value31;
                            Log.v("Time run", String.valueOf(timeRun));
                            adapterMainTerminal.add("Time run" + ": " + timeRun + " Seconds");
                            i++;
                            i++;
                            break;
                        case 34:
                            i++;
                            i++;
                            double eng =  Integer.parseInt(data[i].trim(), 16)/ 2.55;
                            this.engineLoad = (int) eng;
                            BluetoothConnection.getApplication().setEngineLoad(engineLoad);
                            Log.v("Engine Load", String.valueOf(engineLoad));
                            adapterMainTerminal.add("Engine Load" + ": " + engineLoad + " %");
                            i++;
                            break;
                }
                i++;

                }
            }
            }

        }



}



