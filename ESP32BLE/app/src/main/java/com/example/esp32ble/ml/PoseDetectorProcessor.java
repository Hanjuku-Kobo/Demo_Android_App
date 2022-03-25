package com.example.esp32ble.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PoseDetectorProcessor
    extends VisionProcessorBase<PoseDetectorProcessor.PoseWithClassification> {

        private static final String TAG = "PoseDetectorProcessor";

        private final PoseDetector detector;

        private final boolean showInFrameLikelihood;
        private final boolean visualizeZ;
        private final boolean rescaleZForVisualization;
        private final Executor classificationExecutor;

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
          boolean rescaleZForVisualization) {
            super(context);
            this.showInFrameLikelihood = showInFrameLikelihood;
            this.visualizeZ = visualizeZ;
            this.rescaleZForVisualization = rescaleZForVisualization;
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

                                return new PoseWithClassification(pose, classificationResult);
                            });
    }

    @Override
    protected void onSuccess(
            @NonNull PoseWithClassification poseWithClassification,
            @NonNull GraphicOverlay graphicOverlay) {

            graphicOverlay.add(new PoseGraphic(
                    graphicOverlay,
                    poseWithClassification.pose,
                    showInFrameLikelihood,
                    visualizeZ,
                    rescaleZForVisualization,
                    poseWithClassification.classificationResult)
            );
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Pose detection failed!", e);
    }
}
