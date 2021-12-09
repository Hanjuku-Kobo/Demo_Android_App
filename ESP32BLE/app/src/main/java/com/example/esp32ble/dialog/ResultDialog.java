package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.BleGameActivity;
import com.example.esp32ble.fragment.GamePageFragment;

public class ResultDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private LayoutInflater mLayoutInflater;

    private View view;
    private TextView sucText;
    private TextView failText;

    private int[] getInt;

    private GamePageFragment fragment;

    public ResultDialog (GamePageFragment fragment, int success, int failure) {
        this.fragment = fragment;

        getInt = new int[2];
        getInt[0] = failure;
        getInt[1] = success;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mLayoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        view = mLayoutInflater.inflate(R.layout.dialog_result, null);

        failText = view.findViewById(R.id.failure_val);
        sucText = view.findViewById(R.id.success_val);

        failText.setText(String.valueOf(getInt[0]));
        sucText.setText(String.valueOf(getInt[1]));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton("OK",this::onClick);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        Log.d("dialog", "表示完了");
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                fragment.closeFragment();
                break;
        }
    }
}
