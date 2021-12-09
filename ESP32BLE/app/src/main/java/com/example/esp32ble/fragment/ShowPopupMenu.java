package com.example.esp32ble.fragment;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.BleTestActivity;
import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.activity.FileServiceActivity;
import com.example.esp32ble.activity.BleGameActivity;
import com.example.esp32ble.activity.MainActivity;

public class ShowPopupMenu {

    private Context context;

    private String calledToClassName;

    public ShowPopupMenu(Context c) {
        context = c;

        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        calledToClassName = ste.getClassName();

        Log.i("show popup", calledToClassName);
    }

    public void createPopup(View v) {
        PopupMenu popup = new PopupMenu(context, v);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this::onMenuItemClick);    //下のメソッド
        inflater.inflate(R.menu.menuresorcefile, popup.getMenu());
        try {
            popup.show();
        } catch (Exception e) {
            Log.e("ERROR", e.toString());
        }
    }

    public void createSortPopup(View v) {
        PopupMenu popup = new PopupMenu(context, v);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this::onSortMenuItemClick);
        inflater.inflate(R.menu.sort_menu_resorce, popup.getMenu());
        try {
            popup.show();
        } catch (Exception e) {
            Log.e("ERROR", e.toString());
        }
    }

    private boolean onMenuItemClick(MenuItem item) {
        String MAIN_ACTIVITY = "com.example.esp32ble.activity.MainActivity";
        String GAME_ACTIVITY = "com.example.esp32ble.activity.GameActivity";
        String POSE_DET_ACTIVITY = "com.example.esp32ble.activity.PoseDetectorActivity";
        String FILE_SERVICE_ACTIVITY = "com.example.esp32ble.activity.FileServiceActivity";
        String BLE_TEST_ACTIVITY = "com.example.esp32ble.activity.BleTestActivity";

        switch (item.getItemId()) {
            case R.id.home_menu:
                if (!calledToClassName.equals(MAIN_ACTIVITY)) {
                    onClickMainActivity();
                    return true;
                }
                return false;

            case R.id.ble_game_menu:
                if (!calledToClassName.equals(GAME_ACTIVITY)) {
                    onClickGameActivity();
                    return true;
                }
                return false;

            case R.id.ble_test_menu:
                if (!calledToClassName.equals(BLE_TEST_ACTIVITY)) {
                    onClickBleTestActivity();
                    return true;
                }
                return false;

            case R.id.pose_det_menu:
                if (!calledToClassName.equals(POSE_DET_ACTIVITY)) {
                    onClickPoseDetectorActivity();
                    return true;
                }
                return false;

            case R.id.file_service_menu:
                if (!calledToClassName.equals(FILE_SERVICE_ACTIVITY)) {
                    onClickFileServiceActivity();
                    return true;
                }
                return false;

            default:
                return false;
        }
    }

    private boolean onSortMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sortBy:
                return true;

            default:
                return false;
        }
    }

    private void onClickMainActivity() {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    private void onClickGameActivity() {
        Intent intent = new Intent(context, BleGameActivity.class);
        context.startActivity(intent);
    }

    private void onClickPoseDetectorActivity() {
        Intent intent = new Intent(context, CameraActivity.class);
        context.startActivity(intent);
    }

    private void onClickFileServiceActivity() {
        Intent intent = new Intent(context, FileServiceActivity.class);
        context.startActivity(intent);
    }

    private void onClickBleTestActivity() {
        Intent intent = new Intent(context, BleTestActivity.class);
        context.startActivity(intent);
    }
}
