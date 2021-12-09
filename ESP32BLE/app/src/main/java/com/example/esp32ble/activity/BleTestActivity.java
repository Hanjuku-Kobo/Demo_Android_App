package com.example.esp32ble.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.esp32ble.R;
import com.example.esp32ble.dialog.CheckPairSettingDialog;
import com.example.esp32ble.dialog.ReadFileDialog;
import com.example.esp32ble.dialog.SaveFileDialog;
import com.example.esp32ble.fragment.ShowPopupMenu;
import com.example.esp32ble.usecases.BLEProcessor;
import com.example.esp32ble.usecases.GetVoltageRegularly;
import com.example.esp32ble.usecases.LineChartController;
import com.github.mikephil.charting.charts.LineChart;

import java.util.ArrayList;

public class BleTestActivity extends AppCompatActivity {

    private LineChartController chartController;
    private BLEProcessor bleProcessor;
    private GetVoltageRegularly getRegularly;

    private LineChart chart;

    private boolean connectState = false;

    public boolean callBleTestData = false;
    private BluetoothAdapter bluetoothAdapter;

    private String useExtension;

    private ArrayList<Float> mX;
    private ArrayList<Float> mY;
    private ArrayList<Float> mZ;

    private Button changeButton;
    private Button saveButton;
    private Button writeAccel;
    private Button writePress;

    private TextView batteryLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_test);

        // UI関連の初期化
        Toolbar toolbar = findViewById(R.id.ble_test_toolbar);
        setSupportActionBar(toolbar);

        changeButton = findViewById(R.id.connect_button);
        changeButton.setOnClickListener(this::onClick);

        saveButton = findViewById(R.id.save);
        saveButton.setOnClickListener(this::onClick);

        Button readButton = findViewById(R.id.readCSVButton);
        readButton.setOnClickListener(this::onClick);

        writeAccel = findViewById(R.id.write_a_button);
        writeAccel.setOnClickListener(this::onClick);
        writeAccel.setEnabled(false);

        writePress = findViewById(R.id.write_p_button);
        writePress.setOnClickListener(this::onClick);
        writePress.setEnabled(false);

        batteryLevel = findViewById(R.id.test_battery_level);

        chartController = new LineChartController(this);
        getLineChart();

        // Bluetoothを扱うクラスのインスタンスを生成
        bleProcessor = new BLEProcessor(this, chartController);

        //Bluetoothの設定
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d("BtActivity", "Bluetooth is not supported");
            return;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (null == bluetoothAdapter) {
            // Android端末がBluetoothをサポートしていない
            Log.d("BtActivity", "Bluetooth is not supported");
            return;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // バッテリー電圧を定期的に取得するためのクラスのインスタンスを作成
        getRegularly = new GetVoltageRegularly(bleProcessor);
    }

    // onCreateの次に呼ばれる
    @Override
    protected void onResume() {
        super.onResume();

        callBleTestData = true;
        saveButton.setEnabled(false);
    }

    // 画面がバックグラウンドに行ったとき
    @Override
    public void onPause() {
        super.onPause();

        chart.clear();

        callBleTestData = false;
    }

    // Classが破棄されるとき
    @Override
    protected void onStop() {
        super.onStop();

        bleProcessor.clearBleGatt();
    }

    // 画面サイズが変更されるとき
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(MainActivity.uiOptions);
        }
    }

    /* ボタン */

    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_button:
                needsCallDialog();

                break;

            case R.id.save:
                SaveFileDialog dialogSave = new SaveFileDialog(this);
                dialogSave.show(getSupportFragmentManager(), "save");

                break;

            case R.id.readCSVButton:
                chartReset();
                if (!chart.isEnabled()) chart.clearValues();
                ReadFileDialog dialogRead = new ReadFileDialog(this);
                dialogRead.show(getSupportFragmentManager(), "read");

                break;

            case R.id.write_a_button:
                bleProcessor.onWrite("acceleration");
                useExtension = "acceleration";

                break;

            case R.id.write_p_button:
                bleProcessor.onWrite("pressure");
                useExtension = "pressure";

                break;
        }
    }

    // ここからBluetoothの接続処理
    public void needsCallDialog() {
        if (!connectState) {
            CheckPairSettingDialog dialog = new CheckPairSettingDialog();
            dialog.show(getSupportFragmentManager(), "main");
        }
        else {
            String NOW_DISCONNECT = "接続を開始";
            connectState = false;
            getRegularly.onStop();
            bleProcessor.clearBleGatt();

            // 他クラスから呼ばれるときにこれが無いとviewに対する処理ができない
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    changeButton.setText(NOW_DISCONNECT);//Connectと表示

                    writeAccel.setEnabled(false);
                    writePress.setEnabled(false);

                    Toast.makeText(BleTestActivity.this, "接続を終了しました", Toast.LENGTH_SHORT).show();

                    if (mX != null) {
                        saveButton.setEnabled(true);
                    }
                }
            });
        }
    }

    // DialogからのCallback
    public void positiveCallback() {
        bleProcessor.scanPairedDevice(this, bluetoothAdapter);
    }

    public void startConnect() {
        String NOW_CONNECT = "接続を終了";

        connectState = true;

        chartReset();

        getRegularly.onStart();

        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                changeButton.setText(NOW_CONNECT);//Disconnectと表示
                writeAccel.setEnabled(true);
                writePress.setEnabled(true);
            }
        });
    }

    /* 折れ線グラフ & LineChartControllerClassから呼ばれる */

    public LineChart getLineChart() {
        chart = findViewById(R.id.lineChert1);

        return chart;
    }

    public void chartReset() {
        if (mX != null){
            chart.clear();
        }

        chartController.reset();

        mX = new ArrayList<>();
        mY = new ArrayList<>();
        mZ = new ArrayList<>();
    }

    public void setAccelData(float x, float y, float z) {
        mX.add(x);
        mY.add(y);
        mZ.add(z);
    }

    public Float getData(char name, int count) {
        switch (name) {
            case 'x':
                return mX.get(count);

            case 'y':
                return mY.get(count);

            case 'z':
                return mZ.get(count);

            case 's':
                return chartController.getKeyCount();

            default:
                return null;
        }
    }

    public String getUseExtension() {
        return useExtension;
    }

    // MenuやPopupMenu関連
    public void showPopup(View v) {
        ShowPopupMenu popupMenu =  new ShowPopupMenu(this);
        popupMenu.createPopup(v);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bt_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bt_setting:
                startActivityForResult(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), 0);
                return true;

            case R.id.bt_scan:
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "Bluetooth通信に対応していません", Toast.LENGTH_SHORT);
                }
                else {
                    needsCallDialog();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // 外部からUIを変更する用
    public void setSaveButton(boolean enabledState) { saveButton.setEnabled(enabledState); }

    public void setChartName(String strName) { chart.getDescription().setText(strName); }

    public void setBatteryLevel(String level) {
        String result = level + "%";

        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                batteryLevel.setText(result);
            }
        });
    }
}
