package com.example.bluetoothterminal;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;


public class BluetoothUtils {
    private Context context;
    private Handler handler;

    private BluetoothAdapter bluetoothAdapter;

    private final UUID MY_UUID = UUID.fromString("ADD_UUID HERE");


    private final String APP_NAME = "BluetoothTerminal";

    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private  int state;

    public BluetoothUtils(Context context, Handler handler ){
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    public int getState(){

        return state;
    }

    public synchronized void setState(int state){
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state ,-1).sendToTarget();
    }

    private synchronized void start(){
        if (connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread == null){
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_LISTEN);

    }

    public synchronized void stop(){
        if (connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
        }
        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    public synchronized void connect(BluetoothDevice bluetoothDevice){
        if (state == STATE_CONNECTING){
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();

        if (connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);

    }

    public void write(byte[] buffer){
        ConnectedThread connThread;
        synchronized (this){
            if (state != STATE_CONNECTED){
                return;
            }
            connThread = connectedThread;
        }
        connThread.write(buffer);

    }


    private class AcceptThread extends Thread{
        private BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME,MY_UUID);
            }catch (IOException e){
                Log.e("Accept->Constructor",e.toString());
            }
            bluetoothServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket bluetoothSocket = null;
            try {
                bluetoothSocket = bluetoothServerSocket.accept();
            }catch (IOException e ){
                Log.e("Accept->RUN",e.toString());
                try {
                    bluetoothServerSocket.close();
                }catch (IOException e1){
                    Log.e("Accept->Close",e.toString());
                }
            }
            if (bluetoothSocket != null){
                switch (state){
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connect(bluetoothSocket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            bluetoothSocket.close();
                        }catch (IOException e){
                            Log.e("Accept->CloseSocket",e.toString());
                        }
                        break;

                }
            }
        }

        public void cancel(){
            try {
                bluetoothServerSocket.close();
            }catch (IOException e ){
                Log.e("Accept->CloseServer",e.toString());

            }

        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice bluetoothDevice){
            this.bluetoothDevice = bluetoothDevice;
            BluetoothSocket tmp = null;
            try {
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            }catch (IOException e){
                Log.e("Connect->Constructor",e.toString());
            }
            bluetoothSocket = tmp;
        }

        @Override
        public void run() {
            try {
                bluetoothSocket.connect();
            }catch (IOException e ){
                Log.e("Connect->RUN",e.toString());

                try {
                    bluetoothSocket.close();
                }catch (IOException e1) {
                    Log.e("Connect->CLOSE SOCKET", e.toString());
                }
                connectionFailed();
                return;
            }
            synchronized (BluetoothUtils.this){
                connectThread = null;
            }
            connected(bluetoothDevice);
        }

        private synchronized void connected(BluetoothDevice bluetoothDevice) {
            if (connectThread != null){
                connectThread.cancel();
                connectThread = null;
            }

            if (connectedThread != null){
                connectedThread.cancel();
                connectedThread = null;
            }

            connectedThread = new ConnectedThread(bluetoothSocket);
            connectedThread.start();

            Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.DEVICE_NAME, bluetoothDevice.getName());
            message.setData(bundle);
            handler.sendMessage(message);

            setState(STATE_CONNECTED);
        }

        private void cancel() {
            try {
                bluetoothSocket.close();
            }catch (IOException e){
                Log.e("Connect->Cancel", e.toString());

            }
        }

    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket bluetoothSocket){
            this.bluetoothSocket = bluetoothSocket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();
            }catch (IOException e){

            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer;
            ArrayList<Integer> arr_byte = new ArrayList<Integer>();
            int bytes;

            while (true){
                try {
                    bytes = inputStream.read();
                    if (bytes == 0x0A){
                    }else if (bytes == 0x0D){
                        buffer = new byte[arr_byte.size()];
                        for (int i = 0 ; i < arr_byte.size() ; i++){
                            buffer[i] = arr_byte.get(i).byteValue();
                        }
                        handler.obtainMessage(MainActivity.MESSAGE_READ, buffer.length, -1, buffer).sendToTarget();
                        arr_byte = new ArrayList<Integer>();
                    }else {
                        arr_byte.add(bytes);
                    }
                }catch (IOException e){
                    connectionLost();
                    BluetoothUtils.this.start();
                }
            }
        }


        private void connectionLost() {
            Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            //bundle.putString(MainActivity.TOAST, "Connection Lost");
            message.setData(bundle);

            handler.sendMessage(message);
            //BluetoothUtils.this.start();
        }

        public void write(byte[] buffer ){
            try {
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.MESSAGE_WRITE,-1,-1,buffer).sendToTarget();
            }catch (IOException e){
                connectionLost();

            }
        }
        public void cancel(){
            try {
                bluetoothSocket.close();
            }catch (IOException e){

            }
        }
    }
    private synchronized void connectionFailed() {
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Can't connect to device");
        message.setData(bundle);
        handler.sendMessage(message);

        BluetoothUtils.this.start();
    }
}
