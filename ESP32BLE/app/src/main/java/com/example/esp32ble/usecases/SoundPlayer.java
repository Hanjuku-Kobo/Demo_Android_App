package com.example.esp32ble.usecases;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.View;

import com.example.esp32ble.R;

public class SoundPlayer {

    private final SoundPool soundPool;

    private final int soundGod;
    private final int soundBad;

    public SoundPlayer(Context context) {

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);

        soundGod = soundPool.load(context, R.raw.god, 1);
        soundBad = soundPool.load(context, R.raw.bad, 1);
    }

    public void godSound() {
        soundPool.play(soundGod, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void badSound() {
        soundPool.play(soundBad, 1.0f, 1.0f, 1, 0, 1.0f);
    }
}
