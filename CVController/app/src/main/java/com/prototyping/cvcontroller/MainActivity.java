package com.prototyping.cvcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private BluetoothController bluetoothController;
    private final int REQUEST_CODE_BT = 1001;

    private final JSONObject sensorData = new JSONObject();

    private TextView statusText;
    private TextView tempText;
    private TextView humidityText;
    private TextView distanceText;
    private TextView speedText;
    private TextView tYawText;
    private TextView tPitchText;
    private TextView launchText;
    private ImageView shipView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        statusText = findViewById(R.id.permissionText);

        ImageButton tDownButton = findViewById(R.id.tDown);
        ImageButton tUpButton = findViewById(R.id.tUp);
        ImageButton tLeftButton = findViewById(R.id.tLeft);
        ImageButton tRightButton = findViewById(R.id.tRight);
        ImageButton tLaunchButton = findViewById(R.id.tLaunchButton);

        ImageButton mLeftButton = findViewById(R.id.mLeft);
        ImageButton mRightButton = findViewById(R.id.mRight);
        ImageButton mUpButton = findViewById(R.id.mUp);
        ImageButton mDownButton = findViewById(R.id.mDown);

        shipView = findViewById(R.id.imageViewShip);
        tempText = findViewById(R.id.tTemp);
        humidityText = findViewById(R.id.tHumidity);
        distanceText = findViewById(R.id.tDistance);
        speedText = findViewById(R.id.tSpeed);
        tYawText = findViewById(R.id.tTYaw);
        tPitchText = findViewById(R.id.tTPitch);
        launchText = findViewById(R.id.tTLaunch);

        mLeftButton.setOnClickListener(v -> adjustMotor(0, -1));
        mRightButton.setOnClickListener(v -> adjustMotor(0, 1));
        mUpButton.setOnClickListener(v -> adjustMotor(1, 0));
        mDownButton.setOnClickListener(v -> adjustMotor(-1, 0));

        tLeftButton.setOnClickListener(v -> adjustTurret(-30, 0));
        tRightButton.setOnClickListener(v -> adjustTurret(30, 0));
        tUpButton.setOnClickListener(v -> adjustTurret(0, -30));
        tDownButton.setOnClickListener(v -> adjustTurret(0, 30));

        tLaunchButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 手指按下时触发
                        sendJsonAndRender("A", 1);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // 手指抬起时触发
                        sendJsonAndRender("A", 2);
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        // 手指滑出按钮范围时触发
                        sendJsonAndRender("A", 2);
                        return true;
                }
                return false;
            }
        });

        Button sendBtn = findViewById(R.id.sendButton);

        sendBtn.setOnClickListener(v -> {
            try {
                bluetoothController.sendJson(new JSONObject().put("msg", "Hello from Android"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        btStart();
    }

    private void adjustMotor(int speedDelta, int turnDelta) {
        int motorM1Level = sensorData.optInt("M1", 0);
        int motorM2Level = sensorData.optInt("M2", 0);
        int turn = sensorData.optInt("t", 0);
        try {
            motorM1Level = Math.max(0, Math.min(3, motorM1Level + speedDelta));
            motorM2Level = Math.max(0, Math.min(3, motorM2Level + speedDelta));
            turn = Math.max(-2, Math.min(2, turn + turnDelta));

            JSONObject json = new JSONObject();
            json.put("M1", motorM1Level);
            json.put("M2", motorM2Level);
            json.put("t", turn);
            sendJsonAndRender(json);
            onRecieve(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void adjustTurret(int yDelta, int pDelta) {
        int s1 = sensorData.optInt("S1", 0);
        int s2 = sensorData.optInt("S2", 0);
        try {
            JSONObject json = new JSONObject();

            if (yDelta != 0) {
                s1 = Math.max(0, Math.min(180, s1 + yDelta));
                json.put("S1", s1);
            }
            if (pDelta != 0) {
                s2 = Math.max(0, Math.min(180, s2 + pDelta));
                json.put("S2", s2);
            }
            sendJsonAndRender(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void sendJsonAndRender(String key, int value) {
        try {
            JSONObject json = new JSONObject();
            json.put(key, value);
            bluetoothController.sendJson(json);
            onRecieve(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void sendJsonAndRender(JSONObject json) {
        bluetoothController.sendJson(json);
        onRecieve(json);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            fullScreen();
//        }
    }

    @Override
    protected void onDestroy() {
        bluetoothController.disconnect();
        super.onDestroy();
    }

    void fullScreen() {
        // 隐藏状态栏和导航栏，并使内容可以延伸到这些区域后面
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    // 隐藏状态栏和导航栏
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    // 设置当用户从屏幕边缘滑动时，系统栏（状态栏和导航栏）的行为
                    // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE 表示它们会短暂显示然后再次隐藏
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } finally {

            }
        } else {
            // 对于旧版本 (API < 30)
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // 使用沉浸式粘性模式
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(uiOptions);
        }
        // 对于 API 28 (Pie) 及更高版本，处理刘海屏区域
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
    }

    void onRecieve(JSONObject json) {
        statusText.setText("Received: " + json.toString());

        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String key = it.next();
            try {
                sensorData.put(key, json.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        json = sensorData;

        double temp = json.optDouble("T", 0);
        double humi = json.optDouble("H", 0);
        double dist = json.optDouble("D", -999);

        int turn = json.optInt("t", 0);
        int speed = Math.max(json.optInt("M1", 0), json.optInt("M2", 0));
        int s1 = json.optInt("S1", 0);
        int s2 = json.optInt("S2", 0);
        int launch = json.optInt("A", 2);

        if (launch == 1) {
            launchText.setText("1");
        } else {
            launchText.setText("0");
        }

        switch (speed) {
            case 0:
                speedText.setText("stop");
                break;
            case 1:
                speedText.setText("low");
                break;
            case 2:
                speedText.setText("normal");
                break;
            case 3:
                speedText.setText("high");
                break;
        }

        int deg = 30 * turn;
        shipView.animate()
                .rotation(deg)
                .setDuration(500)
                .start();

        tempText.setText(String.format("%.1f°C", temp));
        humidityText.setText(String.format("%.1f%%", humi));
        distanceText.setText(String.format("%.1fcm", dist));
        tYawText.setText(String.format("%d°", s1));
        tPitchText.setText(String.format("%d°", s2));
        View view = getWindow().getDecorView();
        if (dist < 10 && dist >= 0) {
            WarningAnimation.startBlinking(view);
        } else {
            WarningAnimation.stopBlinking(view);
        }
    }

    private ActivityResultLauncher<String[]> multiPermissionLauncher;
    void btStart() {
        if (bluetoothController == null) {
            bluetoothController = new BluetoothController(this);
        }

        multiPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean bluetooth = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                    Boolean location = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean scan = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);

                    if (bluetooth != true || location != true || scan != true) {
                        statusText.setText("Bluetooth permissions denied");
                        return;
                    }

                    try {
                        boolean connected = bluetoothController.connectToHC05();
                        if (connected) {
                            statusText.setText("Connected to HC-05");
                            bluetoothController.setOnDataReceivedListener(json -> runOnUiThread(() ->
                                    this.onRecieve(json)
                            ));
                            bluetoothController.startReceivingJson();
                        } else {
                            statusText.setText("Connection failed");
                        }
                    } catch (SecurityException e) {
                        statusText.setText("Connection error");
                    }

                    Log.d("Permission", "蓝牙权限：" + bluetooth + "，位置权限：" + location);
                });

        statusText.setText("Requesting permissions.....");
        multiPermissionLauncher.launch(new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN
        });
    }
}