package com.example.esp32ble.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.esp32ble.R;
import com.example.esp32ble.fragment.ShowPopupMenu;
import com.example.esp32ble.usecases.CreateItemList;
import com.example.esp32ble.usecases.CreateItemList.Item;
import com.example.esp32ble.usecases.CreateItemList.CustomAdapter;
import com.example.esp32ble.usecases.CreateItemList.CustomAdapterCheckbox;
import com.example.esp32ble.usecases.InstructionsSave;

import java.util.ArrayList;

public class FileServiceActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    private Context context;

    private String inputWord;

    private boolean isAllSelect = false;

    private ArrayList<String> files;

    private InstructionsSave is;

    private ListView listView;
    private Button deleteButton;
    private Button allSelectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_ser);

        context = this;

        is = new InstructionsSave(context);

        // UI関連の初期化
        Toolbar toolbar = findViewById(R.id.file_service_toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.file_list_view);
        listView.setOnItemClickListener(this::onItemClick);

        Button sortButton = findViewById(R.id.file_sort_button);
        sortButton.setOnClickListener(this);

        deleteButton = findViewById(R.id.file_delete_button);
        deleteButton.setOnClickListener(this);

        allSelectButton = findViewById(R.id.file_all_select_button);
        allSelectButton.setOnClickListener(this);

        ImageButton searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(this);

        EditText keyWordText = findViewById(R.id.word_search_text);
        keyWordText.addTextChangedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ファイルを読み込み
        createList(is.readCSVFiles());
    }

    // 画面サイズが変更されるとき
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(MainActivity.uiOptions);
        }
    }

    private void createList(ArrayList<String> inputs) {
        // リストを生成するクラスを呼び出す
        CreateItemList createFileItemList = new CreateItemList();

        ArrayList<Item> itemList = new ArrayList<>();

        // ファイルがある場合
        if (inputs != null) {
            files = inputs;

            // ファイルをItemとしてListに追加
            for (int i = 0; i < inputs.size(); i++) {
                Item item = new Item();
                item.setItem(inputs.get(i));
                itemList.add(item);
            }

            // セットするアダプターの作成
            if (deleteButton.getText().equals("削除選択")) {
                // 通常時
                CustomAdapter adapter = new CustomAdapter(context, 0, itemList);

                listView.setAdapter(adapter);

                // 選択モードを解除
                listView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            } else {
                // ファイルを削除する場合
                CustomAdapterCheckbox adapter
                        = new CustomAdapterCheckbox(context, 0, itemList);
                adapter.setActivity(this);

                if (isAllSelect) {
                    for (int i=0; i<inputs.size(); i++) {
                        adapter.setItemChecked(i, true);
                    }
                } else {
                    for (int i=0; i<inputs.size(); i++) {
                        adapter.setItemChecked(i, false);
                    }
                }

                listView.setAdapter(adapter);

                // 選択モードを設定
                listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.file_sort_button:
                ShowPopupMenu showPopupMenu = new ShowPopupMenu(this);
                showPopupMenu.createSortPopup(v);

                break;

            case R.id.file_delete_button:
                if (deleteButton.getText().equals("削除選択")) {
                    deleteButton.setText("戻る");
                    allSelectButton.setVisibility(View.VISIBLE);

                    createList(files);
                }
                else {
                    deleteButton.setText("削除選択");
                    allSelectButton.setVisibility(View.INVISIBLE);

                    // 削除をする処理
                    for (int i=0; i<files.size(); i++) {
                        if (CustomAdapterCheckbox.checkList.get(i) != null) {
                            if (CustomAdapterCheckbox.checkList.get(i)) {
                                is.deleteCSVFiles(files.get(i));
                            }
                        }
                    }

                    // 使い終わったらリセットしましょう
                    // そして、下手にグローバル変数は使わないこと
                    // Errorの元です
                    CustomAdapterCheckbox.checkList.clear();

                    ArrayList<String> result = squeezeFile();

                    createList(result);
                }

                break;

            case R.id.file_all_select_button:
                isAllSelect = !isAllSelect;
                if (isAllSelect) allSelectButton.setText("全解除");
                else allSelectButton.setText("全選択");

                createList(files);

                break;

            case R.id.search_button:
                ArrayList<String> result = squeezeFile();

                createList(result);

                break;
        }
    }

    public void setDeleteButtonText(boolean b) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (b) {
                    deleteButton.setText("削除");
                } else {
                    deleteButton.setText("戻る");
                }
            }
        });
    }

    private void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String fileName = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + files.get(position);

        // Dialogを作成
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ファイル保存場所");
        builder.setMessage(fileName);
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private ArrayList<String> squeezeFile() {
        ArrayList<String> csvFiles = is.readCSVFiles();
        ArrayList<String> result = new ArrayList<>();
        ArrayList<Integer> matchNumber = new ArrayList<>();

        if (csvFiles == null) {
            // nullだとlistViewの更新が行われないかったため
            // 何もないListを返す
            return result;
        }

        // ファイルとEditTextの中身がある場合
        if (inputWord != null) {

            for (int i=0; i<csvFiles.size(); i++) {
                // テキストがファイル名に含まれているか
                if (csvFiles.get(i).contains(inputWord)) {
                    matchNumber.add(i);
                }
            }

            // resultに上で取得した順番にあるファイルを追加
            for (int i=0; i<matchNumber.size(); i++) {
                result.add(csvFiles.get(matchNumber.get(i)));
            }

            return result;
        }

        return csvFiles;
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override // 文字が入力され、確定された時
    public void afterTextChanged(Editable s) {
        inputWord = s.toString();
    }

    // MenuやPopupMenu関連
    public void showPopup(View v) {
        ShowPopupMenu popupMenu =  new ShowPopupMenu(this);
        popupMenu.createPopup(v);
    }
}
