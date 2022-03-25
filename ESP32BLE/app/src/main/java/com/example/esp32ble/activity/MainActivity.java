package com.example.esp32ble.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.esp32ble.R;
import com.example.esp32ble.fragment.ShowPopupMenu;
import com.example.esp32ble.usecases.Calculator;

import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String[] PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int REQUEST_PERMISSION_CODE = 101;

    public static int uiOptions =
            View.SYSTEM_UI_FLAG_IMMERSIVE
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // パーミッションが許可されているか
        if (!isGranted()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_CODE);
        }

        // UI関連の初期化
        Toolbar toolbar = findViewById(R.id.toolbar3);
        setSupportActionBar(toolbar);

        Button bleGame = findViewById(R.id.button1);
        bleGame.setOnClickListener(this::onClick);

        Button camera = findViewById(R.id.button2);
        camera.setOnClickListener(this::onClick);

        Button file = findViewById(R.id.button3);
        file.setOnClickListener(this::onClick);

        Button bleTest = findViewById(R.id.button4);
        bleTest.setOnClickListener(this::onClick);

        Button gaitAnalysis = findViewById(R.id.button5);
        gaitAnalysis.setOnClickListener(this::onClick);
    }

    // 画面にlayoutが表示される前に呼ばれる
    @Override
    protected void onResume() {
        super.onResume();
    }

    // 画面がバックグラウンドに行ったとき
    @Override
    public void onPause() {
        super.onPause();
    }

    // Classが破棄されるとき
    @Override
    protected void onStop() {
        super.onStop();
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

    // パーミッション許可
    private boolean isGranted() {
        // 許可されていないパーミッションがある場合はfalseを返す

        for (String permission : PERMISSIONS) {
            if (!(ContextCompat.checkSelfPermission(getBaseContext(), permission)
                    == PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.length > 0) {

            for (int i = 0; i < permissions.length; i++) {

                if (permissions[i].equals(Manifest.permission.BLUETOOTH)) {
                    if (!(grantResults[i] == PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Bluetooth is not working", Toast.LENGTH_SHORT).show();
                    }
                } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (!(grantResults[i] == PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "ファイルへアクセスできません", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.button1:
                Intent intent1 = new Intent(this, BleGameActivity.class);
                startActivity(intent1);
                break;

            case R.id.button2:
                Intent intent2 = new Intent(this, CameraActivity.class);
                startActivity(intent2);
                break;

            case R.id.button3:
                Intent intent3 = new Intent(this, FileServiceActivity.class);
                startActivity(intent3);
                break;

            case R.id.button4:
                Intent intent4 = new Intent(this, BleTestActivity.class);
                startActivity(intent4);
                break;

            case R.id.button5:
                Intent intent5 = new Intent(this, GaitAnalysisActivity.class);
                startActivity(intent5);
                break;
        }
    }

    // MenuやPopupMenu関連
    public void showPopup(View v) {
        ShowPopupMenu popupMenu =  new ShowPopupMenu(this);
        popupMenu.createPopup(v);
    }
}