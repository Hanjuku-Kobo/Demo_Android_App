package com.example.esp32ble.ml;

import android.annotation.SuppressLint;

import com.google.mlkit.vision.pose.PoseLandmark;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.lang.Math.atan2;

public class PoseDataProcess {

    public static Queue<Float> coordinates = new ConcurrentLinkedDeque<>();
    public static Queue<Float> jointAngles = new ConcurrentLinkedDeque<>();
    public static ArrayList<String> timeData = new ArrayList<>();

    public static int keyCount = 0;

    public static long startTime = 0;

    // SAMPLE (firstPoint = rightHip, midPoint = rightKnee, lastPoint = rightAnkle)
    public long getAngle(PoseLandmark firstPoint, PoseLandmark midPoint, PoseLandmark lastPoint){
        double result =
                Math.toDegrees(
                        atan2(lastPoint.getPosition().y - midPoint.getPosition().y,
                                lastPoint.getPosition().x - midPoint.getPosition().x)
                                - atan2(firstPoint.getPosition().y - midPoint.getPosition().y,
                                firstPoint.getPosition().x - midPoint.getPosition().x));

        // Angle should never be negative
        result = Math.abs(result);

        // Always get the acute representation of the angle
        if (result > 180) {
            result = (360.0 - result);
        }

        // 小数点四捨五入
        return Math.round(result);
    }

    public float getReciprocalX(Float defPoint) {
        // フロントカメラ用に数値を逆転させる
        // 240は2倍した数値がx軸のサイズ
        // 画面が一番左が0で一番右が480
        return 240 + (240 - defPoint);
    }

    public void setTime() {
        timeData.add(getDate());       // 日付
        timeData.add(String.valueOf(getElapsedTime()));// タイマー
    }

    public void addCoordinate(PoseLandmark landmark, Long angle) {
        float valX = landmark.getPosition().x;
        float valY = landmark.getPosition().y;

        if (angle != null) {
            jointAngles.add(Float.valueOf(angle));
        }

        coordinates.add(valX);
        coordinates.add(valY);
    }

    public void setStartTime() {
        startTime = System.currentTimeMillis();
    }

    public Float getElapsedTime() {
        if (startTime == 0) {
            setStartTime();
        }

        long date = System.currentTimeMillis();

        return (float) (date - startTime) / 1000;
    }

    private String getDate() {
        // 現在日時を取得
        Date nowDate = new Date();
        // 表示形式を指定
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(nowDate);
    }
}