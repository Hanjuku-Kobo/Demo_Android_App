package com.example.esp32ble.ml;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class InferenceInfoGraphic extends GraphicOverlay.Graphic {

    private static final int TEXT_COLOR = Color.WHITE;
    private static final float TEXT_SIZE = 60.0f;

    private final Paint textPaint;
    private final GraphicOverlay overlay;

    public InferenceInfoGraphic(
            GraphicOverlay overlay) {
        super(overlay);
        this.overlay = overlay;
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(TEXT_SIZE);
        postInvalidate();
    }

    @Override
    public synchronized void draw(Canvas canvas) {
        float x = TEXT_SIZE * 0.5f;
        float y = TEXT_SIZE * 1.5f;

        // 文字枠を表示
        textPaint.setStrokeWidth(4f);
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.STROKE);
        canvas.drawText(
                "InputImage size: " + overlay.getImageHeight() + "x" + overlay.getImageWidth(),
                x,
                y,
                textPaint
                );

        // 文字を表示
        textPaint.setStrokeWidth(0f);
        textPaint.setColor(TEXT_COLOR);
        textPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(
                "InputImage size: " + overlay.getImageHeight() + "x" + overlay.getImageWidth(),
                x,
                y,
                textPaint);
    }
}
