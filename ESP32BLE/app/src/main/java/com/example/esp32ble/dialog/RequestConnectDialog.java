package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.activity.BleGameActivity;

public class RequestConnectDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("まずは、デバイスと接続しましょう");
        builder.setPositiveButton("OK", this::onClick);
        builder.setNegativeButton("あとで", this::onClick);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                // activityを取得 (ひとつのActivityから呼ばれる場合のみ使用可能)
                BleGameActivity activity = (BleGameActivity) getActivity();

                // 条件を満たさなければthrowが投げられる
                assert activity != null;
                activity.onConnect();

                break;

            case DialogInterface.BUTTON_NEGATIVE:
                break;
        }
    }
}
