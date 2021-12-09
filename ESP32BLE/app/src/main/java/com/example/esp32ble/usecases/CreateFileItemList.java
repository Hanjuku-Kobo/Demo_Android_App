package com.example.esp32ble.usecases;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.esp32ble.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateFileItemList {

    private static LayoutInflater layoutInflater;

    // 利用するItemのsetterやgetter
    public static class Item{
        private String fileItem;
        private Boolean checkData = false;

        public void setFileItem(String strItem) { this.fileItem = strItem; }

        public String getFileItem() { return this.fileItem; }

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
            if (null == v) v = layoutInflater.inflate(R.layout.abs_file_list, null);

            TextView intTextView = v.findViewById(R.id.file_name);
            intTextView.setText(item.getFileItem());
            return v;
        }
    }

    // checkboxを追加したviewを利用するAdapter
    public static class CustomAdapterCheckbox extends ArrayAdapter<Item> {

        public static Map<Integer, Boolean> checkList = new HashMap<>();

        public CustomAdapterCheckbox(Context con, int resource, List<Item> objects) {
            super(con, resource, objects);
            layoutInflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            Item item = getItem(position);
            if (null == v) v = layoutInflater.inflate(R.layout.abs_file_list_checkbox, null);

            TextView intTextView = v.findViewById(R.id.file_name);
            intTextView.setText(item.getFileItem());

            CheckBox checkBox = v.findViewById(R.id.checkBox);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.isChecked());
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    checkList.put(position, isChecked);

                    Item data = getItem(position);
                    data.setChecked(isChecked);
                }
            });

            return v;
        }

        public void setItemChecked(int position, boolean isChecked) {
            checkList.put(position, isChecked);

            Item data = getItem(position);
            data.setChecked(isChecked);
        }
    }
}
