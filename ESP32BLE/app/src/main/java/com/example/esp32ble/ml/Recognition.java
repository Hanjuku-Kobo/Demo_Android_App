package com.example.esp32ble.ml;

import android.graphics.RectF;

// 認識
public class Recognition {
    public String id; // ID
    public String title; // タイトル
    public Float confidence; // 信頼度
    public RectF location; // 位置

    // コンストラクタ
    public Recognition(String id, String title, Float confidence, RectF location) {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }
}
