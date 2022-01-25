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
import com.example.esp32ble.tab.TabFragment1;
import com.example.esp32ble.usecases.CreateItemList;
import com.example.esp32ble.usecases.CreateItemList.Item;
import com.example.esp32ble.usecases.CreateItemList.CustomAdapter;
import com.example.esp32ble.usecases.InstructionsSave;

import java.util.ArrayList;

public class ReadFileDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private ArrayList<String> csvFiles;
    private Context context;
    private String keyWord;

    private InstructionsSave is;
    private BleTestActivity bleTest;
    private TabFragment1 fragment;

    public ReadFileDialog(BleTestActivity bleTest, String keyWord) {
        context = bleTest.getApplicationContext();
        this.bleTest = bleTest;
        this.keyWord = keyWord;

        is = new InstructionsSave(bleTest);
    }

    public ReadFileDialog(TabFragment1 fragment, String keyWord) {
        this.context = fragment.getContext();
        this.fragment = fragment;
        this.keyWord = keyWord;

        is = new InstructionsSave(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // ファイルを読み込み
        csvFiles = is.readCSVFiles(keyWord);

        // リストを生成するクラスを呼び出す
        new CreateItemList();

        ArrayList<Item> itemList = new ArrayList<>();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // ファイルがある場合
        if (csvFiles != null) {
            // ファイルをItemとしてListに追加
            for (int i = 0; i < csvFiles.size(); i++) {
                Item item = new Item();
                String itemName = csvFiles.get(i);
                if (bleTest == null) {
                    int endIndex = itemName.indexOf("_");         // 指定の文字位置の取得
                    item.setItem(itemName.substring(0, endIndex));// 開始と終端を指定して切り出し
                } else {
                    item.setItem(itemName);
                }
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

                    if (bleTest != null) {
                        // 加速度を描画
                        is.drawAcceleration(name);
                    } else {
                        fragment.drawLineChar(name);
                    }

                    // dialogを閉じる
                    dismiss();
                }
            });

            builder.setTitle("ファイルを選択");
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
