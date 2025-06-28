package com.prototyping.cvcontroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothController {

    private static final String TAG = "BluetoothController";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Activity activity;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread receiveThread;
    private boolean receiving = false;
    private OnDataReceivedListener dataListener;

    public BluetoothController(Activity activity) {
        this.activity = activity;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @RequiresPermission(allOf = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
    })
    public boolean connectToHC05() {
        if (!isBluetoothAvailable()) return false;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName() != null && device.getName().contains("HC-05")) {
                try {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                    socket.connect();
                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();
                    Log.d(TAG, "Connected to HC-05");
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Connection failed", e);
                }
            }
        }
        return false;
    }

    public void sendJson(JSONObject json) {
        try {
            if (outputStream != null) {
                outputStream.write((json.toString() + "\n").getBytes()); // \n 分隔
            }
        } catch (IOException e) {
            Log.e(TAG, "Send failed", e);
        }
    }

    public String receiveData() {
        try {
            if (inputStream != null && inputStream.available() > 0) {
                byte[] buffer = new byte[1024];
                int bytes = inputStream.read(buffer);
                return new String(buffer, 0, bytes);
            }
        } catch (IOException e) {
            Log.e(TAG, "Receive failed", e);
        }
        return null;
    }

    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        this.dataListener = listener;
    }

    public void startReceivingJson() {
        receiving = true;
        receiveThread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while (receiving) {
                try {
                    line = reader.readLine(); // 一行一个数据包
                    if (line != null && dataListener != null) {
                        try {
                            JSONObject json = new JSONObject(line);
                            dataListener.onJsonReceived(json);
                        } catch (JSONException e) {
                            Log.w(TAG, "Received non-JSON data: " + line);
                            // 可选：通知 UI 或忽略
                            // runOnUiThread(() -> statusText.setText("非JSON数据: " + line));
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Input stream closed", e);
                    break;
                }
            }
        });
        receiveThread.start();
    }


    public void stopReceiving() {
        receiving = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
    }


    public void disconnect() {
        stopReceiving(); // 停止接收
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Disconnect failed", e);
        }
    }

    public void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    }, 1001);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, 1001);
        }
    }

    public boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }
}
