package com.example.esp32ble.ml;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.example.esp32ble.ml.GraphicOverlay.Graphic;

public class ObjectGraphic extends Graphic {

    private Bitmap bitmap;

    public ObjectGraphic(GraphicOverlay overlay, Bitmap result ) {
        super(overlay);

        bitmap = result;
    }

    @Override
    public void draw(Canvas canvas) {
        // 出力画像の生成
        canvas.drawBitmap(bitmap, 0f, 0f, null);
    }
}
