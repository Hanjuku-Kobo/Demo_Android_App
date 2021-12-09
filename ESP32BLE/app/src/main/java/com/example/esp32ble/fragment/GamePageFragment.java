package com.example.esp32ble.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.BleGameActivity;
import com.example.esp32ble.dialog.ConfirmDialog;
import com.example.esp32ble.dialog.ProgressDialog;
import com.example.esp32ble.dialog.ResultDialog;
import com.example.esp32ble.usecases.SoundPlayer;

import java.util.Timer;
import java.util.TimerTask;

public class GamePageFragment extends Fragment {

    private BleGameActivity activity;

    public boolean callMultiUseData = false;
    public boolean callPostureData = false;

    private Timer countDownTimer;
    private int successCounter = 0;

    private int stackSuccess = 0;
    private int gameCount = 1;
    private boolean bootThread;
    private int timeLeft;

    private String oldIscText;

    private SoundPlayer soundPlayer;
    private ProgressDialog progressDialog;

    private Handler UIHandler;
    private Handler dHandler;

    private TextView iscText;
    private Button next;

    private TextView remVal;
    private TextView okVal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_ble_game, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = (BleGameActivity) getActivity();

        soundPlayer = new SoundPlayer(activity);
        progressDialog = new ProgressDialog();

        UIHandler = new Handler(Looper.getMainLooper());
        dHandler = new Handler();

        iscText = view.findViewById(R.id.instructions_text);

        next = view.findViewById(R.id.next_button);
        next.setOnClickListener(this::onClick);

        remVal = view.findViewById(R.id.rem_val);
        okVal = view.findViewById(R.id.ok_val);
    }

    public void closeFragment() {
        activity.onShowGameFragment = false;

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.remove(this);
        transaction.commit();
    }

    public void onClick(View v){
        switch (v.getId()) {
            case R.id.next_button:
                moveNextPage();

                break;
        }
    }

    public void moveNextPage(){
        if (activity.pushedCount == 1) {
            ConfirmDialog dialog = new ConfirmDialog(this);
            dialog.show(getFragmentManager(), "confirm");
        }
        else if (activity.pushedCount == 0) {
            activity.onWrite("null");

            callPostureData = false;

            dHandler.removeCallbacks(runnable);

            try { // タイマーがまだ生成されてないときに発生するエラーのため
                countDownTimer.cancel();
            } catch (NullPointerException ignored){}

            int num = gameCount-successCounter-1;
            if (num < 0) num = 0;

            ResultDialog resultDialog
                    = new ResultDialog(this, successCounter, num);
            resultDialog.show(getFragmentManager(),"result");
        }
        else if (activity.pushedCount == 404) {
            activity.onConnect();
        }
        else if (activity.pushedCount == 2) {
            iscText.setText("立って");
            next.setText("終了");

            activity.pushedCount = 0;
            callPostureData = true;
        }
    }

    public void inPreparation(){
        iscText.setText("いすに座ってゲームをスタート");
        callMultiUseData = true;

        next.setEnabled(false);
        activity.pushedCount++;

        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                Bundle args = new Bundle();
                args.putString("title", "情報を取得中");
                progressDialog.setArguments(args);
                progressDialog.show(getFragmentManager(), "progress");
            }
        });
    }

    public void ifReconnect() {
        next.setEnabled(true);
        iscText.setText(oldIscText);
    }

    public void ifDisconnect() {
        oldIscText = (String) iscText.getText();

        next.setEnabled(false);
        iscText.setText("接続が途切れました");
    }

    /* 姿勢測定 */

    public void standAndSitting(float x, float y){
        if (!bootThread) {
            bootThread = true;
            timeLeft = 5;

            dHandler.postDelayed(runnable, 5000);
            countDownTimer = new Timer();
            countDownTimer.scheduleAtFixedRate(posTimer, 0, 1000);
        }

        if (gameCount%2 != 0) { // countが奇数の時 「立って」
            if (x <= -8) postureJudgment();
        }
        else { // countが偶数の時 「座って」
            if (y >= 8) postureJudgment();
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            callPostureData = false;
            countDownTimer.cancel();

            iscText.setText("失敗");
            soundPlayer.badSound();
            changePos();
        }
    };

    TimerTask posTimer = new TimerTask() {
        @Override
        public void run() {
            posSetUi(String.valueOf(timeLeft));
            timeLeft--;
        }
    };

    private void postureJudgment() {
        stackSuccess++;

        if (stackSuccess >= 10) {
            callPostureData = false;

            dHandler.removeCallbacks(runnable);
            countDownTimer.cancel();

            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    iscText.setText("OK");
                    okVal.setText(String.valueOf(successCounter += 1));

                    soundPlayer.godSound();
                    changePos();
                }
            });
        }
    }

    private void changePos(){
        dHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (gameCount%2 != 0) iscText.setText("座って");
                else iscText.setText("立って");

                gameCount++;
                stackSuccess = 0;
                bootThread = false;
                callPostureData = true;
            }
        }, 1000);
    }

    private void posSetUi(String message){
        UIHandler.post(new Runnable() {
            @Override
            public void run() { remVal.setText(message); }
        });
    }

    /* 複数使用 */

    private void isSitting(float y){
        if (activity.pushedCount == 2) {
            if (y >= 8) {
                stackSuccess++;
                if (stackSuccess >= 10) {
                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            soundPlayer.godSound();
                            iscText.setText("ボタンを押して始める");
                            next.setText("スタート");
                            next.setEnabled(true);
                        }
                    });
                    callMultiUseData = false;
                    stackSuccess = 0;
                    progressDialog.dismiss();
                }
            }
        } else if (activity.pushedCount == 0){
            if (y >= 8) {
                stackSuccess++;
                if (stackSuccess >= 10) {
                    callMultiUseData = false;
                    stackSuccess = 0;
                    progressDialog.dismiss();

                    UIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            iscText.setText("OK");
                            next.setText("終了");
                            next.setEnabled(true);
                        }
                    });
                }
            }
        }
    }

    public void setDefPosition(float y){
        if (activity.pushedCount == 2) {
            isSitting(y);
        }
    }
}
