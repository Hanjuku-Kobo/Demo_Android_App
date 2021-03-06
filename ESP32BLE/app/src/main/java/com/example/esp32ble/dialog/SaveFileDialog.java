package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.activity.BleTestActivity;
import com.example.esp32ble.usecases.FileOperation;
import com.example.esp32ble.R;

import java.util.ArrayList;

public class SaveFileDialog extends DialogFragment implements DialogInterface.OnClickListener, View.OnClickListener, TextWatcher {

    private ArrayList<String> csvFiles = new ArrayList<>();

    private BleTestActivity bleTest;
    private final FileOperation is;

    private Context context;
    private String name;

    private TextView text;
    private Button submit;
    private View view;

    public SaveFileDialog(Context context) {
        this.context = context;
        is = new FileOperation(context);
    }

    public SaveFileDialog(BleTestActivity bleTest) {
        this.bleTest = bleTest;
        is = new FileOperation(bleTest);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater mLayoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        view = mLayoutInflater.inflate(R.layout.dialog_save, null);

        EditText fileName = view.findViewById(R.id.edit_file_name);
        fileName.addTextChangedListener(this);

        submit = view.findViewById(R.id.dialog_submit);
        submit.setOnClickListener(this);
        submit.setEnabled(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("保存するファイル名を入力");
        builder.setPositiveButton("cancel",this);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE){

            dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        if (bleTest != null) {
            boolean state = is.saveAcceleration(name + ".csv", bleTest);

            bleTest.setSaveButton(state);
        }
        else {
            is.saveCoordinate(name + "_point.csv");
            is.saveJointAngles(name + "_angle.csv");
        }

        dismiss();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override // 文字が入力され、確定された時
    public void afterTextChanged(Editable s) {
        String inputText = s.toString();

        csvFiles = is.readCSVFiles(".csv");
        int i = 0;

        if (csvFiles != null) {
            boolean duplication = false;

            if (context != null) duplication = checkDuplicate(inputText);

            while (i < csvFiles.size()) {
                if (csvFiles.get(i).equals(inputText + ".csv") || duplication){
                    text = view.findViewById(R.id.subTitle);
                    text.setText("ファイル名が重複しています。変更してください。");
                    text.setTextColor(Color.RED);

                    submit.setEnabled(false);

                    return;
                }
                i++;
            }
            if (text != null) {
                text.setText("ファイル名");
                text.setTextColor(Color.BLACK);
            }
        }

        name = inputText;

        submit.setEnabled(true);
    }

    private boolean checkDuplicate(String fileName) {
        for (int i=0; i<csvFiles.size(); i++) {
            if (csvFiles.get(i).equals(fileName + "_point.csv") && csvFiles.get(i).equals(fileName + "_angle.csv")) {
                return true;
            }
        }

        return false;
    }
}
