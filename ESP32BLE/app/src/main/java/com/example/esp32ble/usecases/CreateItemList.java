package com.example.esp32ble.usecases;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.FileServiceActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateItemList {

    private static LayoutInflater layoutInflater;

    // 利用するItemのsetterやgetter
    public static class Item{
        private String item;
        private Boolean checkData = false;

        public void setItem(String strItem) { this.item = strItem; }

        public String getItem() { return this.item; }

        public void setChecked(boolean bool){ checkData = bool; }

        public boolean isChecked(){ return checkData; }
    }

    // ListViewにセットするAdapterを作成するクラス
    public static class CustomAdapter extends ArrayAdapter<Item> {

        public CustomAdapter(Context con, int resource, List<Item> objects) {
            super(con, resource, objects);
            layoutInflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            Item item = getItem(position);
            if (null == v) v = layoutInflater.inflate(R.layout.abs_list, null);

            TextView intTextView = v.findViewById(R.id.item_name);
            intTextView.setText(item.getItem());
            if (item.getItem().contains("Nintendo RVL-") || item.getItem().contains("ESP32")) {
                intTextView.setTextColor(Color.rgb(0, 191, 0));
                intTextView.setTypeface(null, Typeface.BOLD);
            } else {
                intTextView.setTextColor(Color.BLACK);
                intTextView.setTypeface(null, Typeface.NORMAL);
            }
            return v;
        }
    }

    // checkboxを追加したviewを利用するAdapter
    public static class CustomAdapterCheckbox extends ArrayAdapter<Item> {

        public static Map<Integer, Boolean> checkList = new HashMap<>();
        private FileServiceActivity activity;

        public CustomAdapterCheckbox(Context con, int resource, List<Item> objects) {
            super(con, resource, objects);
            layoutInflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setActivity(FileServiceActivity activity) {
            this.activity = activity;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            Item item = getItem(position);
            if (null == v) v = layoutInflater.inflate(R.layout.abs_list_checkbox, null);

            TextView intTextView = v.findViewById(R.id.item_name);
            intTextView.setText(item.getItem());

            CheckBox checkBox = v.findViewById(R.id.checkBox);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.isChecked());
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    checkList.put(position, isChecked);

                    Item data = getItem(position);
                    data.setChecked(isChecked);

                    checkBoxEvaluation();
                }
            });

            return v;
        }

        public void setItemChecked(int position, boolean isChecked) {
            checkList.put(position, isChecked);

            Item data = getItem(position);
            data.setChecked(isChecked);

            checkBoxEvaluation();
        }

        private void checkBoxEvaluation() {
            for (int i=0; i<checkList.size(); i++) {
                if (checkList.get(i)) {
                    activity.setDeleteButtonText(true);
                    return;
                }
            }
            activity.setDeleteButtonText(false);
        }
    }
}