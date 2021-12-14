package com.example.esp32ble.ml;

import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.fragment.ShortcutButtonFragment;
import com.google.common.primitives.Ints;
import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;
import java.util.Locale;

import static com.example.esp32ble.usecases.VideoProcessor.videoHeight;
import static com.example.esp32ble.usecases.VideoProcessor.videoWidth;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class PoseGraphicBitmap {
    private static final float DOT_RADIUS = 8.0f;
    private static final float IN_FRAME_LIKELIHOOD_TEXT_SIZE = 30.0f;
    private static final float STROKE_WIDTH = 6.0f;
    private static final float POSE_CLASSIFICATION_TEXT_SIZE = 60.0f;

    private final Pose pose;
    private final boolean showInFrameLikelihood;
    private final boolean visualizeZ;
    private final boolean rescaleZForVisualization;
    private float zMin = Float.MAX_VALUE;
    private float zMax = Float.MIN_VALUE;

    private final List<String> poseClassification;
    private final Paint classificationTextPaint;
    private final Paint leftPaint;
    private final Paint rightPaint;
    private final Paint whitePaint;

    private final PoseDataProcess dataProcess;
    private final String[] landmarkList;

    public static boolean isViewUpperDegree = false;
    public static boolean isViewLowerDegree = false;

    private float scaleFactor = 1.0f;

    private int imageWidth;
    private int imageHeight;

    private boolean needUpdateTransformation;

    PoseGraphicBitmap(
            Pose pose,
            boolean showInFrameLikelihood,
            boolean visualizeZ,
            boolean rescaleZForVisualization,
            List<String> poseClassification) {
        this.pose = pose;
        this.showInFrameLikelihood = showInFrameLikelihood;
        this.visualizeZ = visualizeZ;
        this.rescaleZForVisualization = rescaleZForVisualization;

        this.poseClassification = poseClassification;
        classificationTextPaint = new Paint();
        classificationTextPaint.setColor(Color.WHITE);
        classificationTextPaint.setTextSize(POSE_CLASSIFICATION_TEXT_SIZE);
        classificationTextPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);

        whitePaint = new Paint();
        whitePaint.setStrokeWidth(STROKE_WIDTH);
        whitePaint.setColor(Color.WHITE);
        whitePaint.setTextSize(IN_FRAME_LIKELIHOOD_TEXT_SIZE);
        leftPaint = new Paint();
        leftPaint.setStrokeWidth(STROKE_WIDTH);
        leftPaint.setColor(Color.GREEN);
        rightPaint = new Paint();
        rightPaint.setStrokeWidth(STROKE_WIDTH);
        rightPaint.setColor(Color.YELLOW);

        dataProcess = new PoseDataProcess();
        landmarkList = CameraActivity.getLandmarks();

        this.imageWidth = CameraActivity.videoViewWidget;
        this.imageHeight = CameraActivity.videoViewHeight;
        needUpdateTransformation = true;

        updateTransformationIfNeeded();
    }

    public Bitmap onDraw() {
        Bitmap outBitmap = Bitmap.createBitmap(
                CameraActivity.videoViewWidget, CameraActivity.videoViewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outBitmap);

        List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
        if (landmarks.isEmpty()) {
            return null;
        }

        // Draw pose classification text.
        float classificationX = POSE_CLASSIFICATION_TEXT_SIZE * 0.5f;
        for (int i = 0; i < poseClassification.size(); i++) {
            float classificationY = (canvas.getHeight() - POSE_CLASSIFICATION_TEXT_SIZE * 1.5f
                    * (poseClassification.size() - i));
            canvas.drawText(
                    poseClassification.get(i),
                    classificationX,
                    classificationY,
                    classificationTextPaint);
        }

        // Draw all the points
        for (PoseLandmark landmark : landmarks) {
            drawPoint(canvas, landmark, whitePaint);
            if (visualizeZ && rescaleZForVisualization) {
                zMin = min(zMin, landmark.getPosition3D().getZ());
                zMax = max(zMax, landmark.getPosition3D().getZ());
            }
        }

        PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
        PoseLandmark leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER);
        PoseLandmark leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE);
        PoseLandmark leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER);
        PoseLandmark rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER);
        PoseLandmark rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE);
        PoseLandmark rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER);
        PoseLandmark leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR);
        PoseLandmark rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR);
        PoseLandmark leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH);
        PoseLandmark rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH);

        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
        PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
        PoseLandmark leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        PoseLandmark rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

        PoseLandmark leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);
        PoseLandmark rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
        PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
        PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
        PoseLandmark leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB);
        PoseLandmark rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
        PoseLandmark leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL);
        PoseLandmark rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL);
        PoseLandmark leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX);
        PoseLandmark rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX);

        // Face
        drawLine(canvas, nose, leftEyeInner, whitePaint);
        drawLine(canvas, leftEyeInner, leftEye, whitePaint);
        drawLine(canvas, leftEye, leftEyeOuter, whitePaint);
        drawLine(canvas, leftEyeOuter, leftEar, whitePaint);
        drawLine(canvas, nose, rightEyeInner, whitePaint);
        drawLine(canvas, rightEyeInner, rightEye, whitePaint);
        drawLine(canvas, rightEye, rightEyeOuter, whitePaint);
        drawLine(canvas, rightEyeOuter, rightEar, whitePaint);
        drawLine(canvas, leftMouth, rightMouth, whitePaint);

        drawLine(canvas, leftShoulder, rightShoulder, whitePaint);
        drawLine(canvas, leftHip, rightHip, whitePaint);

        // Left body
        drawLine(canvas, leftShoulder, leftElbow, leftPaint);
        drawLine(canvas, leftElbow, leftWrist, leftPaint);
        drawLine(canvas, leftShoulder, leftHip, leftPaint);
        drawLine(canvas, leftHip, leftKnee, leftPaint);
        drawLine(canvas, leftKnee, leftAnkle, leftPaint);
        drawLine(canvas, leftWrist, leftThumb, leftPaint);
        drawLine(canvas, leftWrist, leftPinky, leftPaint);
        drawLine(canvas, leftWrist, leftIndex, leftPaint);
        drawLine(canvas, leftIndex, leftPinky, leftPaint);
        drawLine(canvas, leftAnkle, leftHeel, leftPaint);
        drawLine(canvas, leftAnkle, leftFootIndex, leftPaint);
        drawLine(canvas, leftHeel, leftFootIndex, leftPaint);

        // Right body
        drawLine(canvas, rightShoulder, rightElbow, rightPaint);
        drawLine(canvas, rightElbow, rightWrist, rightPaint);
        drawLine(canvas, rightShoulder, rightHip, rightPaint);
        drawLine(canvas, rightHip, rightKnee, rightPaint);
        drawLine(canvas, rightKnee, rightAnkle, rightPaint);
        drawLine(canvas, rightWrist, rightThumb, rightPaint);
        drawLine(canvas, rightWrist, rightPinky, rightPaint);
        drawLine(canvas, rightWrist, rightIndex, rightPaint);
        drawLine(canvas, rightIndex, rightPinky, rightPaint);
        drawLine(canvas, rightAnkle, rightHeel, rightPaint);
        drawLine(canvas, rightAnkle, rightFootIndex, rightPaint);
        drawLine(canvas, rightHeel, rightFootIndex, rightPaint);

        // Draw inFrameLikelihood for all points
        if (showInFrameLikelihood) {
            for (PoseLandmark landmark : landmarks) {
                drawText(canvas, landmark, landmark.getInFrameLikelihood());
            }
        }

        // Angle calculation
        long angleOfLeftShoulder = dataProcess.getAngle(leftElbow, leftShoulder, leftHip);
        long angleOfRightShoulder = dataProcess.getAngle(rightElbow, rightShoulder, rightHip);
        long angleOfLeftElbow = dataProcess.getAngle(leftWrist, leftElbow, leftShoulder);
        long angleOfRightElbow = dataProcess.getAngle(rightWrist, rightElbow, rightShoulder);
        long angleOfLeftWrist = dataProcess.getAngle(leftIndex, leftWrist, leftElbow);
        long angleOfRightWrist = dataProcess.getAngle(rightIndex, rightWrist, rightElbow);

        long angleOfLeftHip = dataProcess.getAngle(leftShoulder, leftHip, leftKnee);
        long angleOfRightHip = dataProcess.getAngle(rightShoulder, rightHip, rightKnee);
        long angleOfLeftKnee = dataProcess.getAngle(leftHip, leftKnee, leftAnkle);
        long angleOfRightKnee = dataProcess.getAngle(rightHip, rightKnee, rightAnkle);
        long angleOfLeftAnkle = dataProcess.getAngle(leftKnee, leftAnkle, leftFootIndex);
        long angleOfRightAnkle = dataProcess.getAngle(rightKnee, rightAnkle, rightFootIndex);

        if (isViewUpperDegree) {
            // Draw upper degree text
            drawAngleText(canvas, leftShoulder, angleOfLeftShoulder);
            drawAngleText(canvas, rightShoulder, angleOfRightShoulder);
            drawAngleText(canvas, leftElbow, angleOfLeftElbow);
            drawAngleText(canvas, rightElbow, angleOfRightElbow);
            drawAngleText(canvas, leftWrist, angleOfLeftWrist);
            drawAngleText(canvas, rightWrist, angleOfRightWrist);
        }

        if (isViewLowerDegree) {
            // Draw lower degree text
            drawAngleText(canvas, leftHip, angleOfLeftHip);
            drawAngleText(canvas, rightHip, angleOfRightHip);
            drawAngleText(canvas, leftKnee, angleOfLeftKnee);
            drawAngleText(canvas, rightKnee, angleOfRightKnee);
            drawAngleText(canvas, leftAnkle, angleOfLeftAnkle);
            drawAngleText(canvas, rightAnkle, angleOfRightAnkle);
        }

        if (ShortcutButtonFragment.isStartedSave) {
            // Save coordinates
            dataProcess.addCoordinate(nose, null);
            dataProcess.addCoordinate(leftEyeInner, null);
            dataProcess.addCoordinate(leftEye, null);
            dataProcess.addCoordinate(leftEyeOuter, null);
            dataProcess.addCoordinate(rightEyeInner, null);
            dataProcess.addCoordinate(rightEye, null);
            dataProcess.addCoordinate(rightEyeOuter, null);
            dataProcess.addCoordinate(leftEar, null);
            dataProcess.addCoordinate(rightEar, null);
            dataProcess.addCoordinate(leftMouth, null);
            dataProcess.addCoordinate(rightMouth, null);

            dataProcess.addCoordinate(leftShoulder, angleOfLeftShoulder);
            dataProcess.addCoordinate(rightShoulder, angleOfRightShoulder);
            dataProcess.addCoordinate(leftElbow, angleOfLeftElbow);
            dataProcess.addCoordinate(rightElbow, angleOfRightElbow);
            dataProcess.addCoordinate(leftWrist, angleOfLeftWrist);
            dataProcess.addCoordinate(rightWrist, angleOfRightWrist);
            dataProcess.addCoordinate(leftPinky, null);
            dataProcess.addCoordinate(rightPinky, null);
            dataProcess.addCoordinate(leftIndex, null);
            dataProcess.addCoordinate(rightIndex, null);
            dataProcess.addCoordinate(leftThumb, null);
            dataProcess.addCoordinate(rightThumb, null);
            dataProcess.addCoordinate(leftHip, angleOfLeftHip);
            dataProcess.addCoordinate(rightHip, angleOfRightHip);

            dataProcess.addCoordinate(leftKnee, angleOfLeftKnee);
            dataProcess.addCoordinate(rightKnee, angleOfRightKnee);
            dataProcess.addCoordinate(leftAnkle, angleOfLeftAnkle);
            dataProcess.addCoordinate(rightAnkle, angleOfRightAnkle);
            dataProcess.addCoordinate(leftHeel, null);
            dataProcess.addCoordinate(rightHeel, null);
            dataProcess.addCoordinate(leftFootIndex, null);
            dataProcess.addCoordinate(rightFootIndex, null);

            PoseDataProcess.keyCount++;
        }

        return outBitmap;
    }

    void drawPoint(Canvas canvas, PoseLandmark landmark, Paint paint) {
        PointF3D point = landmark.getPosition3D();
        maybeUpdatePaintColor(paint, canvas, point.getZ());

        if (ShortcutButtonFragment.requestFrontCamera) {
            canvas.drawCircle(
                    dataProcess.getReciprocalX(point.getX()),
                    point.getY(),
                    DOT_RADIUS, paint);
        } else {
            canvas.drawCircle(point.getX(), point.getY(), DOT_RADIUS, paint);
        }
    }

    void drawLine(Canvas canvas, PoseLandmark startLandmark, PoseLandmark endLandmark, Paint paint) {
        PointF3D start = startLandmark.getPosition3D();
        PointF3D end = endLandmark.getPosition3D();

        // Gets average z for the current body line
        float avgZInImagePixel = (start.getZ() + end.getZ()) / 2;
        maybeUpdatePaintColor(paint, canvas, avgZInImagePixel);

        if (ShortcutButtonFragment.requestFrontCamera) {
            canvas.drawLine(
                    dataProcess.getReciprocalX(start.getX()),
                    start.getY(),
                    dataProcess.getReciprocalX(end.getX()),
                    end.getY(),
                    paint);
        } else {
            canvas.drawLine(
                    start.getX(),
                    start.getY(),
                    end.getX(),
                    end.getY(),
                    paint);
        }
    }

    void drawText(Canvas canvas, PoseLandmark midPoint, double text) {
        canvas.drawText(
                String.format(Locale.US, "%.2f", text),
                midPoint.getPosition().x,
                midPoint.getPosition().y,
                whitePaint
        );
    }

    void drawAngleText(Canvas canvas, PoseLandmark midPoint, long text) {
        canvas.drawText(
                String.valueOf(text),
                midPoint.getPosition().x + 10,
                midPoint.getPosition().y,
                whitePaint
        );
    }

    // GraphicOverlayの中身 よくわからん
    public float scale(float imagePixel) {
        return imagePixel * scaleFactor;
    }

    private void updateTransformationIfNeeded() {
        int viewWidth = CameraActivity.videoViewWidget;
        int viewHeight = CameraActivity.videoViewHeight;

        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return;
        }
        float viewAspectRatio = (float) viewWidth / viewHeight;
        float imageAspectRatio = (float) imageWidth / imageHeight;
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = (float) viewWidth / imageWidth;
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = (float) viewHeight / imageHeight;
        }

        needUpdateTransformation = false;
    }

    private void maybeUpdatePaintColor(Paint paint, Canvas canvas, float zInImagePixel) {
        if (!visualizeZ) {
            return;
        }

        // When visualizeZ is true, sets up the paint to different colors based on z values.
        // Gets the range of z value.
        float zLowerBoundInScreenPixel;
        float zUpperBoundInScreenPixel;

        if (rescaleZForVisualization) {
            zLowerBoundInScreenPixel = min(-0.001f, scale(zMin));
            zUpperBoundInScreenPixel = max(0.001f, scale(zMax));
        } else {
            // By default, assume the range of z value in screen pixel is [-canvasWidth, canvasWidth].
            float defaultRangeFactor = 1f;
            zLowerBoundInScreenPixel = -defaultRangeFactor * canvas.getWidth();
            zUpperBoundInScreenPixel = defaultRangeFactor * canvas.getWidth();
        }

        float zInScreenPixel = scale(zInImagePixel);

        if (zInScreenPixel < 0) {
            // Sets up the paint to draw the body line in red if it is in front of the z origin.
            // Maps values within [zLowerBoundInScreenPixel, 0) to [255, 0) and use it to control the
            // color. The larger the value is, the more red it will be.
            int v = (int) (zInScreenPixel / zLowerBoundInScreenPixel * 255);
            v = Ints.constrainToRange(v, 0, 255);
            paint.setARGB(255, 255, 255 - v, 255 - v);
        } else {
            // Sets up the paint to draw the body line in blue if it is behind the z origin.
            // Maps values within [0, zUpperBoundInScreenPixel] to [0, 255] and use it to control the
            // color. The larger the value is, the more blue it will be.
            int v = (int) (zInScreenPixel / zUpperBoundInScreenPixel * 255);
            v = Ints.constrainToRange(v, 0, 255);
            paint.setARGB(255, 255 - v, 255 - v, 255);
        }
    }
}
