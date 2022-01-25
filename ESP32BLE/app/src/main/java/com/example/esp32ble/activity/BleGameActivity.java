package com.example.esp32ble.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.esp32ble.R;
import com.example.esp32ble.dialog.RequestConnectDialog;
import com.example.esp32ble.fragment.GamePageFragment;
import com.example.esp32ble.fragment.SelectExtensionFragment;
import com.example.esp32ble.fragment.SelectGameFragment;
import com.example.esp32ble.fragment.ShowPopupMenu;
import com.example.esp32ble.usecases.GetVoltageRegularly;
import com.example.esp32ble.usecases.BtProcessor;

public class BleGameActivity extends AppCompatActivity implements View.OnClickListener {

    public int pushedCount;
    private int oldPushedCount;

    private BtProcessor btProcessor;
    private GetVoltageRegularly getRegularly;

    private BluetoothAdapter mBtAdapter;

    private boolean isAlreadyConnect;

    private Handler UIHandler;

    private SelectExtensionFragment selectExtensionFragment;
    private SelectGameFragment selectGameFragment;
    private GamePageFragment gameFragment;

    public boolean onShowExtensionFragment;
    public boolean onShowSelectGameFragment;
    public boolean onShowGameFragment;

    //UI
    private Button reconnectButton;
    private TextView connectState;
    private TextView batteryLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_game);

        Toolbar toolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);

        selectGameFragment = new SelectGameFragment();
        selectExtensionFragment = new SelectExtensionFragment(selectGameFragment);
        gameFragment = new GamePageFragment();

        BluetoothManager mBtManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();
        if( null == mBtAdapter ) {
            Log.d("BtActivity","Bluetooth is not supported");
            return;
        }
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        btProcessor = new BtProcessor(this);
        getRegularly = new GetVoltageRegularly(btProcessor);

        reconnectButton = findViewById(R.id.reconnect_button);
        reconnectButton.setOnClickListener(this);

        connectState = findViewById(R.id.connect_state);

        batteryLevel = findViewById(R.id.game_battery_level);

        UIHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();

        isAlreadyConnect = false;

        RequestConnectDialog dialog = new RequestConnectDialog();
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    protected void onStop() {
        super.onStop();

        // characteristicsをクリア
        onWrite("null");

        btProcessor.clearBleGatt();

        gameFragment.callMultiUseData = false;
        gameFragment.callPostureData = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(MainActivity.uiOptions);
        }
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){

            case R.id.reconnect_button:
                onConnect();

                break;
        }
    }

    public void addGameFragment() {
        onShowGameFragment = true;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.ble_game_container2, gameFragment);
        transaction.commit();
    }

    public void closeSettingFragments() {
        selectExtensionFragment.closeFragment();
        selectGameFragment.closeFragment();
    }

    public void onConnect(){
        if(!isAlreadyConnect){
            btProcessor.discoverDevice(this, mBtAdapter);
        }
    }

    public void onWrite(String message) {
        btProcessor.onWrite(message);
    }

    public void controlBleData(float x, float y, float z) {
        if (gameFragment.callMultiUseData)
            gameFragment.setDefPosition(y);

        else if (gameFragment.callPostureData)
            gameFragment.standAndSitting(x, y);
    }

    public void updateGUI(int number, String message){
        switch (number){
            case 1:
                UIHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        getRegularly.onStart();

                        if (!onShowGameFragment) {
                            // Fragmentを追加する処理
                            onShowExtensionFragment = true;
                            onShowSelectGameFragment = true;

                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            transaction.add(R.id.ble_game_container1, selectExtensionFragment);
                            transaction.add(R.id.ble_game_container2, selectGameFragment);
                            transaction.commit();
                        } else {
                            gameFragment.ifReconnect();
                        }

                        reconnectButton.setEnabled(false);

                        connectState.setText(message);
                        connectState.setTextColor(Color.rgb(93,173,133));
                    }
                });
                if (pushedCount > 10){
                    pushedCount = oldPushedCount;
                }
                else {
                    pushedCount = 1;
                }
                isAlreadyConnect = true;  //接続が完了したときだけscanできなくするため
                break;

            case 0:
                UIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        reconnectButton.setEnabled(true);

                        connectState.setText(message);
                        connectState.setTextColor(Color.RED);

                        getRegularly.onStop();

                        if (onShowSelectGameFragment && onShowExtensionFragment) {
                            closeSettingFragments();
                        } else if(onShowGameFragment) {
                            gameFragment.ifDisconnect();
                        }
                    }
                });

                oldPushedCount= pushedCount;
                pushedCount = 404;
                isAlreadyConnect = false;
                break;
        }
    }

    public void setBatteryLevel(String level) {
        String result = level + "%";
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                batteryLevel.setText(result);
            }
        });
    }

    public void showPopup(View v) {
        ShowPopupMenu popupMenu =  new ShowPopupMenu(this);
        popupMenu.createPopup(v);
    }
}