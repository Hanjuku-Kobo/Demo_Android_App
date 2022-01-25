package com.example.esp32ble.usecases;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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

    private static int gcd(int xInput, int yInput) {
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
     * 相関関数を計算する
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
     * @param items
     * @return
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
     * @param items
     * @return
     */
    private Double average(final List<Double> items) {
        return sum(items) / items.size();
    }

    /**
     * 総和を計算する
     * @param items
     * @return
     */
    private Double sum(final List<Double> items) {
        Double result = 0.0;

        for (Double item : items) {
            result += item;
        }
        return result;
    }
}
