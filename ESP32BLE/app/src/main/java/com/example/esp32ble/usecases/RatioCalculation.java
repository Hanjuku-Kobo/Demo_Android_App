package com.example.esp32ble.usecases;

import android.util.Log;

public class RatioCalculation {

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
        int remVal = 0;

        do {
            remVal = largeVal % smallVal;
            largeVal = smallVal;
            smallVal = remVal;
        } while(remVal > 0);

        return largeVal;
    }
}
