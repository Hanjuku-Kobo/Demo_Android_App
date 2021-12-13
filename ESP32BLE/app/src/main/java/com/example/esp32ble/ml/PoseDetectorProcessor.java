package com.example.esp32ble.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PoseDetectorProcessor
    extends VisionProcessorBase<PoseDetectorProcessor.PoseWithClassification> {

        private static final String TAG = "PoseDetectorProcessor";

        private final PoseDetector detector;
        private final Context context;

        private final boolean showInFrameLikelihood;
        private final boolean visualizeZ;
        private final boolean rescaleZForVisualization;
        private final boolean runClassification;
        private final boolean isStreamMode;
        private final Executor classificationExecutor;

        private PoseClassifierProcessor poseClassifierProcessor;

        /** Internal class to hold Pose and classification results. */
        protected static class PoseWithClassification {
            private final Pose pose;
            private final List<String> classificationResult;

            public PoseWithClassification(Pose pose, List<String> classificationResult) {
                this.pose = pose;
                this.classificationResult = classificationResult;
            }
        }

    public PoseDetectorProcessor(
          Context context,
          PoseDetectorOptionsBase options,
          boolean showInFrameLikelihood,
          boolean visualizeZ,
          boolean rescaleZForVisualization,
          boolean runClassification,
          boolean isStreamMode) {
            super(context);
            this.context = context;
            this.showInFrameLikelihood = showInFrameLikelihood;
            this.visualizeZ = visualizeZ;
            this.rescaleZForVisualization = rescaleZForVisualization;
            this.runClassification = runClassification;
            this.isStreamMode = isStreamMode;
            detector = PoseDetection.getClient(options);
            classificationExecutor = Executors.newSingleThreadExecutor();
        }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    @Override
    protected Task<PoseWithClassification> detectInImage(InputImage image) {
            return detector
                    .process(image)
                    .continueWith(
                            classificationExecutor,
                            task -> {
                                Pose pose = task.getResult();
                                List<String> classificationResult = new ArrayList<>();

                                if (runClassification) {
                                    if (poseClassifierProcessor == null) {
                                    poseClassifierProcessor = new PoseClassifierProcessor(context, isStreamMode);
                                    }
                                    classificationResult = poseClassifierProcessor.getPoseResult(pose);
                                 }

                                return new PoseWithClassification(pose, classificationResult);
                            });
    }

    @Override
    protected void onSuccess(
            @NonNull PoseWithClassification poseWithClassification,
            @NonNull GraphicOverlay graphicOverlay,
            VisionProcessorBase processorBase) {

            graphicOverlay.add(new PoseGraphic(
                    graphicOverlay,
                    processorBase,
                    poseWithClassification.pose,
                    showInFrameLikelihood,
                    visualizeZ,
                    rescaleZForVisualization,
                    poseWithClassification.classificationResult)
            );
    }

    @Override
    protected Bitmap onSuccessBitmap(
            @NonNull PoseWithClassification poseWithClassification) {

            PoseGraphicBitmap poseGraphicBitmap = new PoseGraphicBitmap(
                    poseWithClassification.pose,
                    showInFrameLikelihood,
                    visualizeZ,
                    rescaleZForVisualization,
                    poseWithClassification.classificationResult);

            return poseGraphicBitmap.onDraw();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Pose detection failed!", e);
    }
}
