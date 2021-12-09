package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.R;

public class ProgressDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_progress);
        this.setCancelable(false);
        dialog.setTitle(getArguments().getString("title", ""));

        if (!getArguments().getString("title", "").equals("動画を読み込み中")) {
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.gravity = Gravity.BOTTOM;

            dialog.getWindow().setAttributes(lp);
        }

        return dialog;
    }
}
