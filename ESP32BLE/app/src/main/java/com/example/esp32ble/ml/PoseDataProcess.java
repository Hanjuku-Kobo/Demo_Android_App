package com.example.esp32ble.ml;

import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.usecases.InstructionsSave;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.atan2;

public class PoseDataProcess {

    public static Map<String, Float> coordinateMap = new HashMap<>();
    public static Map<String, Long> angleMap = new HashMap<>();
    public static Map<String, Float> elapsedTime = new HashMap<>();

    public static int keyCount = 0;

    public static long startTime = 0;

    private float xCriteria;

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

    public float getXCriteria() {
        return xCriteria;
    }

    public float getReciprocalX(Float defPoint) {
        // フロントカメラ用に数値を逆転させる
        // 240は2倍した数値がx軸のサイズ
        // 画面が一番左が0で一番右が480
        return 240 + (240 - defPoint);
    }

    public void addCoordinate(PoseLandmark landmark, String poseName, Long angle) {
        float valX = landmark.getPosition().x;
        float valY = landmark.getPosition().y;

        String keyX = poseName + "X" + keyCount;
        String keyY = poseName + "Y" + keyCount;

        coordinateMap.put(keyX, valX);
        coordinateMap.put(keyY, valY);

        if (angle != null) {
            String keyAngle = poseName + keyCount;
            angleMap.put(keyAngle, angle);
        }

        String keyTime = poseName + keyCount;
        elapsedTime.put(keyTime, getElapsedTime());
    }

    public void getFileName(String fileName) {
        String[] landmarkList = CameraActivity.getLandmarks();

        keyCount--;

        for (String s : landmarkList) {
            dataSelection(s, fileName);
        }
    }

    public void dataSelection(String landmarkName, String fileName) {
        ArrayList<Float> time = selectionTime(landmarkName);
        ArrayList<Float> xList = selectionX(landmarkName);
        ArrayList<Float> yList = selectionY(landmarkName);
        ArrayList<Long> angleList = selectionAngle(landmarkName);

        InstructionsSave instruct = new InstructionsSave();
        instruct.saveCoordinate(
                fileName + landmarkName + ".csv",
                time,
                xList,
                yList,
                angleList);

        String[] list = CameraActivity.getLandmarks();

        if (landmarkName.equals(list[32])) {
            instruct.finalCall();
        }
    }

    private ArrayList<Float> selectionTime(String name) {
        ArrayList<Float> dataList = new ArrayList<>();

        for (int i=0; i<=keyCount; i++) {
            String keyName = name + i;

            dataList.add(elapsedTime.get(keyName));
        }

        return dataList;
    }

    private ArrayList<Float> selectionX(String name) {
        ArrayList<Float> dataList = new ArrayList<>();

        for (int i=0; i<=keyCount; i++) {
            String keyName = name + "X" + i;

            dataList.add(coordinateMap.get(keyName));
        }

        return dataList;
    }

    private ArrayList<Float> selectionY(String name) {
        ArrayList<Float> dataList = new ArrayList<>();

        for (int i=0; i<=keyCount; i++) {
            String keyName = name + "Y" + i;

            dataList.add(coordinateMap.get(keyName));
        }

        return dataList;
    }

    private ArrayList<Long> selectionAngle(String name) {
        ArrayList<Long> dataList = new ArrayList<>();

        if (angleMap.get(name + "0") != null) {
            for (int i=0; i<=keyCount; i++) {
                String keyName = name + i;

                dataList.add(angleMap.get(keyName));
            }

            return dataList;
        }

        return null;
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
}