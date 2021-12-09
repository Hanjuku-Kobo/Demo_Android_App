package com.example.esp32ble.ml;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.common.MlKitException;

public interface VisionImageProcessor {

    /** Processes a bitmap image. */
    void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay);

    /** Processes a bitmap from video frame */
    Bitmap processBitmap(Bitmap bitmap);

    /** Processes ImageProxy image data, e.g. used for CameraX live preview case. */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay)
            throws MlKitException;

    /** Stops the underlying machine learning model and release resources. */
    void stop();
}