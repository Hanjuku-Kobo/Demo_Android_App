package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.activity.BleTestActivity;
import com.example.esp32ble.activity.MainActivity;

public class CheckPairSettingDialog extends DialogFragment implements DialogInterface.OnClickListener{

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("ペアリング設定は終えていますか？");
        builder.setMessage("端末の設定によるBluetoothデバイスとのペアリング設定");
        builder.setPositiveButton("はい", this::onClick);
        builder.setNegativeButton("いいえ", this::onClick);
        builder.setNeutralButton("あとで",this::onClick);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                //「はい」
                // 説明は RequestConnectDialog.java を参照
                BleTestActivity activity = (BleTestActivity) getActivity();
                assert activity != null;
                activity.positiveCallback();

                break;

            case DialogInterface.BUTTON_NEGATIVE:
                //「いいえ」
                startActivityForResult(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), 0);
                break;
        }
    }
}