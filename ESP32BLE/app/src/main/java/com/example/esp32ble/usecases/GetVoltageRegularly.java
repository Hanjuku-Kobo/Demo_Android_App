package com.example.esp32ble.usecases;

import android.os.Handler;

public class GetVoltageRegularly {

    private final BtProcessor bluetoothProcessor;

    private final Handler handler;

    private final String GET_VOLTAGE = "battery";

    public GetVoltageRegularly(BtProcessor processor) {
        bluetoothProcessor = processor;

        handler = new Handler();
    }

    public void onStart() {
        // writeのスパンが短すぎたら二つ目が呼ばれない
        bluetoothProcessor.onWrite(GET_VOLTAGE);

        handler.postDelayed(task, 30000);
    }

    Runnable task  = new Runnable() {
        @Override
        public void run() {
            bluetoothProcessor.onWrite(GET_VOLTAGE);

            handler.postDelayed(task, 30000);
        }
    };

    public void onStop() {
        handler.removeCallbacks(task);
    }
}
