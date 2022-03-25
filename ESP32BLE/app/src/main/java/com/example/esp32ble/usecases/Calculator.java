package com.example.esp32ble.usecases;

import android.util.Log;

import org.checkerframework.checker.units.qual.A;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Calculator {

    /**
     * 最大公約数を計算する
     * @param x width
     * @param y height
     * @return {width結果, height結果}
     */
    public int[] getAspect(int x, int y) {
        final int gcdResult = gcd(x, y);
        final int xRatio = x / gcdResult;
        final int yRatio = y / gcdResult;

        Log.i("TEST", "最大公約数: " + gcdResult + " => " + xRatio + " : " + yRatio);

        return new int[]{xRatio, yRatio};
    }

    private int gcd(int xInput, int yInput) {
        int largeVal = Math.max(xInput, yInput);
        int smallVal = Math.min(xInput, yInput);
        int remVal;

        do {
            remVal = largeVal % smallVal;
            largeVal = smallVal;
            smallVal = remVal;
        } while(remVal > 0);

        return largeVal;
    }

    /**
     * 相関係数を計算する
     * @param itemsXi 項目リスト(Xi)
     * @param itemsYi 項目リスト(Yi)
     * @return 結果
     *
     * 入力データ例
     * int[] xi = {
     * 		28, 30, 25, 27, 32, 36, 31, 29, 30, 35, 33, 37, 36, 33, 28,
     * 		34, 27, 35, 33, 31, 35, 28, 31, 39, 34, 31, 38, 37, 32, 31};
     * 	int[] yi = {
     * 		73, 67, 62, 71, 70, 73, 72, 71, 73, 74, 76, 78, 77, 71, 66,
     * 		70, 64, 75, 68, 66, 69, 65, 66, 71, 77, 70, 81, 73, 75, 78};
     */
    public Double correlationCoefficient(final List<Double> itemsXi, final List<Double> itemsYi) {
        Double sxy = deviationSumOfProduct(itemsXi, itemsYi);
        Double sxx = sumOfSquares(itemsXi);
        Double syy = sumOfSquares(itemsYi);
        return  sxy / Math.sqrt(sxx * syy);
    }

    /**
     * 偏差積和を計算する
     * @param itemsXi 項目リスト（Xi）
     * @param itemsYi 項目リスト（Yi）
     * @return 結果
     */
    private Double deviationSumOfProduct(final List<Double> itemsXi, final List<Double> itemsYi) {
        List<Double> itemsXiYi = new ArrayList<>();
        int n = itemsXi.size();

        for (int i = 0; i < n; i++) {
            itemsXiYi.add(itemsXi.get(i) * itemsYi.get(i));
        }
        Double xiyiSum = sum(itemsXiYi);
        Double xiSum = sum(itemsXi);
        Double yiSum = sum(itemsYi);
        return xiyiSum - ((xiSum * yiSum) / n);
    }

    /**
     * 平方和を計算する
     */
    private Double sumOfSquares(final List<Double> items) {
        Double xbar = average(items);
        List<Double> squares = new ArrayList<>();

        for (Double item : items) {
            Double sqare = (item - xbar) * (item - xbar);
            squares.add(sqare);
        }
        return sum(squares);
    }

    /**
     * 平均値を計算する
     */
    private Double average(final List<Double> items) {
        return sum(items) / items.size();
    }

    /**
     * 総和を計算する
     */
    private Double sum(final List<Double> items) {
        Double result = 0.0;

        for (Double item : items) {
            result += item;
        }
        return result;
    }

    /**
     * ユークリッド距離を計算する
     */
    public Double euclideanDistance(final List<Double> itemsXi, final List<Double> itemsYi) {
        double sum = 0.0d;

        for (int i=0; i<itemsXi.size(); i++) {
            sum += Math.pow(itemsXi.get(i) - itemsYi.get(i), 2);
        }

        double sumOfSqrt = Math.sqrt(sum);

        return 1 / (1 + sumOfSqrt);
    }

    /**
     * グラフの正規化（Min-Max法）
     */
    public Float normalization(float position, float dataSize) {
       return position / dataSize * 100;
    }

    /**
     * データ量を統一する
     */
    public ArrayList<Integer> unification(final Map<Float, Integer> changedMap, final Map<Float, Integer> constData) {
        ArrayList<Integer> result = new ArrayList<>();

        // データ量の多いほうの正規化したx軸を取得
        for (Float constKey : constData.keySet()) {

            Float veryNearDif = null;
            Float oldChangedKey = null;
            // 少ないほう
            for (Float changedKey : changedMap.keySet()) {

                // 差を絶対値で取得
                float dif = Math.abs(changedKey - constKey);

                // 一回目か差が縮まった時
                if (veryNearDif == null || dif - veryNearDif < 0 ) {
                    veryNearDif = dif;
                    oldChangedKey = changedKey;
                } else {
                    // 差が広がった = 一番近いx軸が確定した
                    result.add(changedMap.get(oldChangedKey));
                    oldChangedKey = null;
                    break;
                }
            }

            // 一番最後を追加するため
            if (oldChangedKey != null) {
                result.add(changedMap.get(oldChangedKey));
            }
        }

        return  result;
    }

    /**
     * ArrayList<Integer> から List<Double> へ型変換
     */
    public List<Double> convertIntToDouble(List<Integer> inList) {
        List<Double> outList = new ArrayList<>();
        for (int i=0; i<inList.size(); i++) {
            outList.add(Double.valueOf(inList.get(i)));
        }

        return outList;
    }

    /**
     * List<Integer> から Map型に変換 + データの正規化
     */
    public Map<Float, Integer> convertListToMap(List<Integer> inList) {
        int listSize = inList.size();
        // TreeMap : データを自動的にソートしてくれる
        Map<Float, Integer> outMap = new TreeMap<>();
        for (int j=0; j<listSize; j++) {
            // データ列の正規化
            outMap.put(normalization(j, listSize), inList.get(j));
        }

        return outMap;
    }
}
