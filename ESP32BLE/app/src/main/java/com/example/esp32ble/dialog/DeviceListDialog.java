package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.usecases.BLEProcessor;
import com.example.esp32ble.usecases.CreateFileItemList;

import java.util.ArrayList;

public class DeviceListDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private BLEProcessor bleProcessor;

    private Context context;
    private ArrayList<BluetoothDevice> devices;

    public DeviceListDialog (BLEProcessor ble, Context c, ArrayList<BluetoothDevice> list){
        bleProcessor = ble;
        context = c;
        devices = list;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        new CreateFileItemList();

        ArrayList<CreateFileItemList.Item> itemList = new ArrayList<>();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        for (int i = 0; i < devices.size(); i++) {
            CreateFileItemList.Item item = new CreateFileItemList.Item();
            item.setFileItem(devices.get(i).getName());
            itemList.add(item);
        }

        CreateFileItemList.CustomAdapter adapter = new CreateFileItemList.CustomAdapter(context, 0, itemList);
        ListView listView = new ListView(context);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bleProcessor.connectDevice(devices.get(position));

                dismiss();
            }
        });

        builder.setTitle("接続するデバイスを選択");
        builder.setPositiveButton("cancel",this);
        builder.setView(listView);
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
}
