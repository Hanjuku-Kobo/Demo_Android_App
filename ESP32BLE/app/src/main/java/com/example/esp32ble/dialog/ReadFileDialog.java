package com.example.esp32ble.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.esp32ble.activity.BleTestActivity;
import com.example.esp32ble.usecases.CreateFileItemList;
import com.example.esp32ble.usecases.CreateFileItemList.Item;
import com.example.esp32ble.usecases.CreateFileItemList.CustomAdapter;
import com.example.esp32ble.usecases.InstructionsSave;

import java.util.ArrayList;

public class ReadFileDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private ArrayList<String> csvFiles;

    private Context context;

    private InstructionsSave is;

    public ReadFileDialog(BleTestActivity bleTest) {
        context = bleTest.getApplicationContext();

        is = new InstructionsSave(bleTest);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // ファイルを読み込み
        csvFiles = is.readCSVFiles();

        // リストを生成するクラスを呼び出す
        new CreateFileItemList();

        ArrayList<Item> itemList = new ArrayList<>();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // ファイルがある場合
        if (csvFiles != null) {
            // ファイルをItemとしてListに追加
            for (int i = 0; i < csvFiles.size(); i++) {
                Item item = new Item();
                item.setFileItem(csvFiles.get(i));
                itemList.add(item);
            }

            // ListViewにセットするアダプターの作成
            CustomAdapter adapter
                    = new CustomAdapter(context, 0, itemList);

            ListView listView = new ListView(context);
            listView.setAdapter(adapter);

            // Callbackの設定
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String name = csvFiles.get(position);

                    // 加速度を描画
                    is.drawAcceleration(name);

                    // dialogを閉じる
                    dismiss();
                }
            });

            builder.setTitle("読み込むファイルを選択");
            builder.setPositiveButton("cancel", this);

            // dialogに作成したListを追加
            builder.setView(listView);
        }

        // 既存のファイルがない場合
        else{
            builder.setTitle("保存されたファイルがありません");
            builder.setPositiveButton("閉じる",this);
        }

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
