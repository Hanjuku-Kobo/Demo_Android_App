package com.example.esp32ble.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.fragment.PoseSettingFragment;

import org.checkerframework.checker.units.qual.C;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.esp32ble.fragment.PoseSettingFragment.useVideo;

public class ObjectClassifier {

    // パラメータ定数
    private static final int BATCH_SIZE = 1; // バッチサイズ
    private static final int INPUT_PIXELS = 3; // 入力ピクセル
    private final static int INPUT_SIZE = 300; // 入力サイズ
    private boolean IS_QUANTIZED = true; // 量子化
    private static final int NUM_DETECTIONS = 10; // 検出数
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    // システム
    private Context context;
    private Interpreter interpreter;
    private List<String> labels;
    private int[] imageBuffer = new int[INPUT_SIZE * INPUT_SIZE];

    // 入力
    private ByteBuffer inBuffer;
    private Bitmap inBitmap;
    private Canvas inCanvas;
    private Rect inBitmapSrc = new Rect();
    private Rect inBitmapDst = new Rect(0, 0, INPUT_SIZE, INPUT_SIZE);

    // 出力
    private float[][][] outLocations;
    private float[][] outClasses;
    private float[][] outScores;
    private float[] numDetections;
    private Bitmap outBitmap;
    private Canvas outCanvas;
    private Paint outPaint;

    public ObjectClassifier(Context context) {
        this.context = context;

        // モデルの読み込み
        MappedByteBuffer model = loadModel("objectdetect.tflite");

        // ラベルの読み込み
        this.labels = loadLabel("objectlabelmap.txt");

        // インタプリタの生成
        Interpreter.Options options = new Interpreter.Options();
        options.addDelegate(new NnApiDelegate()); //NNAPI
        //options.addDelegate(new GpuDelegate()); //GPU
        options.setNumThreads(1); // スレッド数
        this.interpreter = new Interpreter(model, options);

        // 入力の初期化
        this.inBitmap = Bitmap.createBitmap(
                INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
        this.inCanvas = new Canvas(inBitmap);
        int numBytesPerChannel = IS_QUANTIZED ? 1 : 4;
        this.inBuffer = ByteBuffer.allocateDirect(
                BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * INPUT_PIXELS * numBytesPerChannel);
        this.inBuffer.order(ByteOrder.nativeOrder());

        // 出力の初期化
        this.outLocations = new float[1][NUM_DETECTIONS][4];
        this.outClasses = new float[1][NUM_DETECTIONS];
        this.outScores = new float[1][NUM_DETECTIONS];
        this.numDetections = new float[1];
        this.outPaint = new Paint();
    }

    // tfliteモデルをassetsから読み込む
    private MappedByteBuffer loadModel(String modelPath) {
        try {
            AssetFileDescriptor fd = this.context.getAssets().openFd(modelPath);
            FileInputStream in = new FileInputStream(fd.getFileDescriptor());
            FileChannel fileChannel = in.getChannel();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY,
                    fd.getStartOffset(), fd.getDeclaredLength());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // モデルの正解ラベルデータをassetsから取得
    private List<String> loadLabel(String labelPath) {
        try {
            List<String> labels = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    this.context.getAssets().open(labelPath)));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
            reader.close();
            return labels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 推論
    public Bitmap predict(Bitmap bitmap) {
        // 先にリサイズする
        if (useVideo != null) {
            this.outBitmap = Bitmap.createBitmap(
                    CameraActivity.videoViewWidget, CameraActivity.videoViewHeight, Bitmap.Config.ARGB_8888);
        } else {
            this.outBitmap = Bitmap.createBitmap(
                    CameraActivity.cameraViewWidget, CameraActivity.cameraViewHeight, Bitmap.Config.ARGB_8888);
        }
        this.outCanvas = new Canvas(outBitmap);

        // 入力画像の生成
        int minSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int dx = (bitmap.getWidth()-minSize)/2;
        int dy = (bitmap.getHeight()-minSize)/2;
        inBitmapSrc.set(dx, dy, dx+minSize, dy+minSize);
        inCanvas.drawBitmap(bitmap, inBitmapSrc, inBitmapDst, null);

        // 入力バッファの生成
        bmpToInBuffer(inBitmap);

        // 推論
        Object[] inputArray = {inBuffer};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outLocations);
        outputMap.put(1, outClasses);
        outputMap.put(2, outScores);
        outputMap.put(3, numDetections);

        interpreter.runForMultipleInputsOutputs(inputArray, outputMap);

        // 結果の取得
        int numDetectionsOutput = Math.min(NUM_DETECTIONS, (int)numDetections[0]);
        ArrayList<Recognition> recongnitions = new ArrayList<>(numDetectionsOutput);
        if (useVideo != null) {
            for (int i = 0; i < numDetectionsOutput; ++i) {
                RectF detection = new RectF(
                        outLocations[0][i][1] * CameraActivity.videoViewWidget,
                        outLocations[0][i][0] * CameraActivity.videoViewHeight,
                        outLocations[0][i][3] * CameraActivity.videoViewWidget,
                        outLocations[0][i][2] * CameraActivity.videoViewHeight);
                int labelOffset = 1;
                recongnitions.add(new Recognition(""+i,
                        labels.get((int) outClasses[0][i]+labelOffset), outScores[0][i], detection));
            }
        } else {
            for (int i = 0; i < numDetectionsOutput; ++i) {
                RectF detection = new RectF(
                        outLocations[0][i][1] * CameraActivity.cameraViewWidget,
                        outLocations[0][i][0] * CameraActivity.cameraViewHeight,
                        outLocations[0][i][3] * CameraActivity.cameraViewWidget,
                        outLocations[0][i][2] * CameraActivity.cameraViewHeight);
                int labelOffset = 1;
                recongnitions.add(new Recognition(""+i,
                        labels.get((int) outClasses[0][i]+labelOffset), outScores[0][i], detection));
            }
        }

        // 出力画像の生成
        outCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        outPaint.setTextSize(12);
        outPaint.setAntiAlias(true);

        if (PoseSettingFragment.targetTitle == null) {
            for (Recognition recognition :  recongnitions) {
                if (recognition.confidence > 0.5f) {
                    drawBitmap(recognition);
                }
            }
        }
        else {
            for (Recognition recognition :  recongnitions) {
                if (recognition.confidence > 0.5f && recognition.title.equals(PoseSettingFragment.targetTitle)) {
                    drawBitmap(recognition);
                }
            }
        }

        return outBitmap;
    }

    private void drawBitmap(Recognition recognition) {
        RectF p = recognition.location;
        outPaint.setStyle(Paint.Style.STROKE);
        outPaint.setColor(Color.BLUE);
        outCanvas.drawRect(p, outPaint);
        outPaint.setStyle(Paint.Style.FILL);
        outCanvas.drawRect(new RectF(p.left, p.top - 16, p.right, p.top), outPaint);
        outPaint.setColor(Color.WHITE);
        float w = outPaint.measureText(recognition.title);
        outCanvas.drawText(recognition.title, p.left + (p.width() - w) / 2, p.top - 4, outPaint);
    }

    // Bitmap → 入力バッファ
    private void bmpToInBuffer(Bitmap bitmap) {
        this.inBuffer.rewind();
        bitmap.getPixels(this.imageBuffer, 0, bitmap.getWidth(),
                0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = imageBuffer[pixel++];
                if (IS_QUANTIZED) {
                    inBuffer.put((byte)((pixelValue >> 16) & 0xFF));
                    inBuffer.put((byte)((pixelValue >> 8) & 0xFF));
                    inBuffer.put((byte)(pixelValue & 0xFF));
                } else {
                    inBuffer.putFloat((((pixelValue >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                    inBuffer.putFloat((((pixelValue >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                    inBuffer.putFloat(((pixelValue & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                }
            }
        }
    }
}
