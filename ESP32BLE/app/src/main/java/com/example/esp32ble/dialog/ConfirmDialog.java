package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.activity.BleGameActivity;
import com.example.esp32ble.fragment.GamePageFragment;

public class ConfirmDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private GamePageFragment fragment;

    public ConfirmDialog(GamePageFragment fragment){ this.fragment = fragment; }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("デバイスをセットしましたか");
        builder.setPositiveButton("OK",this);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE){

            fragment.inPreparation();
        }
    }
}
