package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.usecases.BLEProcessor;
import com.example.esp32ble.usecases.CreateItemList;

import java.util.ArrayList;

public class DeviceListDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private ListView listView;

    private BLEProcessor bleProcessor;

    private final Context context;
    private ArrayList<BluetoothDevice> devices;

    private final String title;

    public DeviceListDialog (
            BLEProcessor processor, Context context, ArrayList<BluetoothDevice> list, String title){
        bleProcessor = processor;
        this.context = context;
        devices = list;
        this.title = title;

        listView = new ListView(this.context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        createList(devices);

        builder.setTitle(title);
        builder.setMessage("数秒時間がかかります");
        builder.setPositiveButton("cancel",this);
        builder.setView(listView);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public void createList(ArrayList<BluetoothDevice> list) {
        ArrayList<CreateItemList.Item> itemList = new ArrayList<>();
        this.devices = list;

        for (BluetoothDevice device : devices) {
            CreateItemList.Item item = new CreateItemList.Item();
            item.setItem(device.getName()+"  <"+device.getAddress()+">");
            itemList.add(item);
        }

        CreateItemList.CustomAdapter adapter = new CreateItemList.CustomAdapter(context, 0, itemList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bleProcessor.connectDevice(devices.get(position));
                devices.clear();

                dismiss();
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE){
            devices.clear();
            dismiss();
        }
    }
}
