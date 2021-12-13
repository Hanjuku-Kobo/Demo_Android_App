package com.example.esp32ble.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.fragment.PoseSettingFragment;
import com.example.esp32ble.fragment.ShortcutButtonFragment;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.mlkit.vision.common.InputImage;

import java.util.Timer;

import static com.example.esp32ble.fragment.PoseSettingFragment.useVideo;

public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

    private static final String TAG = "VisionProcessorBase";

    private final Timer fpsTimer = new Timer();
    private final ScopedExecutor executor;
    private final Context context;

    private ObjectClassifier objectClassifier;

    // Whether this processor is already shut down
    private boolean isShutdown;

    // 画像や動画で検出を行うときに必要
    private Bitmap staticImageBitmap;
    private Bitmap poseBitmap;

    protected VisionProcessorBase(Context context) {
        this.context = context;
        executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
    }

    // process image
    @Override
    public void processBitmap(Bitmap bitmap, final GraphicOverlay graphicOverlay) {
        staticImageBitmap = bitmap;

        if (objectClassifier == null) {
            // Object classification
            objectClassifier = new ObjectClassifier(context);
        }

        if (ShortcutButtonFragment.requestDetect){
            requestDetectInImage(
                    InputImage.fromBitmap(staticImageBitmap, 0),
                    graphicOverlay,
                    /* originalCameraImage= */ null);
        }

        else if(ShortcutButtonFragment.isObjectClassify) {
            Bitmap result = objectClassifier.predict(bitmap);

            graphicOverlay.clear();

            graphicOverlay.add(new ObjectGraphic(graphicOverlay, result));

            graphicOverlay.add(new InferenceInfoGraphic(graphicOverlay));

            graphicOverlay.postInvalidate();
        }
    }

    // process video
    @Override
    public Bitmap processBitmap(Bitmap bitmap) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas outCanvas = new Canvas(result);

        Bitmap objectResult = null;

        if (objectClassifier == null) {
            // Object classification
            objectClassifier = new ObjectClassifier(context);
        }

        if (ShortcutButtonFragment.requestDetect){
            poseBitmap = requestDetectInImage(
                    InputImage.fromBitmap(bitmap, 0),
                    CameraActivity.graphicOverlay);
        }

        if(ShortcutButtonFragment.isObjectClassify) {
            objectResult = objectClassifier.predict(bitmap);
        }

        outCanvas.drawBitmap(bitmap, 0, 0, null);
        if (poseBitmap != null) {
            outCanvas.drawBitmap(poseBitmap, 0, 0, null);
            poseBitmap = null;
        }
        if (objectResult != null) outCanvas.drawBitmap(objectResult, 0, 0, null);

        return result;
    }

    // real time
    @Override
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @ExperimentalGetImage
    public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) {
        if (isShutdown) {
            image.close();
            return;
        }

        if (objectClassifier == null) {
            // Object classification
            objectClassifier = new ObjectClassifier(context);
        }

        Bitmap bitmap = null;

        if (!PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())) {
            bitmap = BitmapUtils.getBitmap(image);
        }

        if (ShortcutButtonFragment.requestDetect){
            requestDetectInImage(
                    InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees()),
                    graphicOverlay,
                    /* originalCameraImage= */ bitmap)
                    // When the image is from CameraX analysis use case, must call image.close() on received
                    // images when finished using them. Otherwise, new images may not be received or the camera
                    // may stall.
                    .addOnCompleteListener(results -> image.close());
        }

        else if(ShortcutButtonFragment.isObjectClassify) {
            Bitmap result = objectClassifier.predict(bitmap);

            graphicOverlay.clear();

            graphicOverlay.add(new CameraImageGraphic(graphicOverlay, bitmap));

            graphicOverlay.add(new ObjectGraphic(graphicOverlay, result));

            graphicOverlay.add(new InferenceInfoGraphic(graphicOverlay));

            graphicOverlay.postInvalidate();

            image.close();
        }

        else {
            graphicOverlay.clear();
            graphicOverlay.postInvalidate();

            image.close();
        }
    }

    // -----------------Common processing logic-------------------------------------------------------
    private Task<T> requestDetectInImage(
            final InputImage image,
            final GraphicOverlay graphicOverlay,
            @Nullable final Bitmap originalCameraImage) {
        return setUpListener(
                detectInImage(image), graphicOverlay, originalCameraImage);
    }

    private Bitmap requestDetectInImage(
            final InputImage image,
            final GraphicOverlay graphicOverlay) {
        return setUpListener(detectInImage(image), graphicOverlay);
    }

    private Task<T> setUpListener(
            Task<T> task,
            final GraphicOverlay graphicOverlay,
            @Nullable final Bitmap originalCameraImage) {
        return task.addOnSuccessListener(
                executor,
                results -> {
                    graphicOverlay.clear();

                    if (originalCameraImage != null) {
                        graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
                    }

                    VisionProcessorBase.this.onSuccess(results, graphicOverlay, this);
                    if (useVideo != null) return;

                    if (ShortcutButtonFragment.isObjectClassify) {
                        if (originalCameraImage != null) {
                            graphicOverlay.add(new ObjectGraphic(
                                    graphicOverlay, objectClassifier.predict(originalCameraImage)
                            ));
                        } else if (staticImageBitmap != null) {
                            graphicOverlay.add(new ObjectGraphic(
                                    graphicOverlay, objectClassifier.predict(staticImageBitmap)
                            ));
                        }
                    }

                    graphicOverlay.add(new InferenceInfoGraphic(graphicOverlay));

                    graphicOverlay.postInvalidate();
                })
                .addOnFailureListener(
                        executor,
                        e -> {
                            graphicOverlay.clear();
                            graphicOverlay.postInvalidate();

                            String error = "Failed to process. Error: " + e.getLocalizedMessage();
                            Toast.makeText(
                                    graphicOverlay.getContext(),
                                    error + "\nCause: " + e.getCause(),
                                    Toast.LENGTH_SHORT)
                                    .show();
                            Log.d(TAG, error);
                            e.printStackTrace();
                            VisionProcessorBase.this.onFailure(e);
                        });
    }

    private Bitmap setUpListener(Task<T> task, final GraphicOverlay graphicOverlay) {
        task.addOnSuccessListener(
                executor,
                results -> {
                    setPoseBitmap(this.onSuccessBitmap(results));
                })
                .addOnFailureListener(
                        executor,
                        e -> {
                            String error = "Failed to process. Error: " + e.getLocalizedMessage();
                            Toast.makeText(
                                    graphicOverlay.getContext(),
                                    error + "\nCause: " + e.getCause(),
                                    Toast.LENGTH_SHORT)
                                    .show();
                            Log.d(TAG, error);
                            e.printStackTrace();
                            VisionProcessorBase.this.onFailure(e);
                        });

        Bitmap result;
        do {
            result = getPoseBitmap();
        } while (result == null);

        return result;
    }

    private Bitmap getPoseBitmap() {
        return poseBitmap;
    }

    public void setPoseBitmap(Bitmap bitmap) {
        poseBitmap = bitmap;
    }

    @Override
    public void stop() {
        executor.shutdown();
        isShutdown = true;
        fpsTimer.cancel();
        ShortcutButtonFragment.requestDetect = false;
        ShortcutButtonFragment.requestFrontCamera = false;
        ShortcutButtonFragment.isObjectClassify = false;
        ShortcutButtonFragment.isStartedSave = false;
        PoseSettingFragment.isVisualizeZ = false;
        PoseSettingFragment.isClassification = false;
        PoseSettingFragment.targetTitle = null;
        PoseSettingFragment.useImage = null;
        useVideo = null;
    }

    protected abstract Task<T> detectInImage(InputImage image);

    protected abstract void onSuccess(@NonNull T results, @NonNull GraphicOverlay graphicOverlay, VisionProcessorBase processorBase);

    protected abstract Bitmap onSuccessBitmap(@NonNull T results);

    protected abstract void onFailure(@NonNull Exception e);
}