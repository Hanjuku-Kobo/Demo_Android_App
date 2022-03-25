package com.example.esp32ble.usecases;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.os.HandlerCompat;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.example.esp32ble.R;
import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.data.Device;
import com.example.esp32ble.ml.ModelType;
import com.example.esp32ble.ml.MoveNet;
import com.example.esp32ble.ml.UseMoveNet;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class VideoProcessor {

    private final CameraActivity activity;
    private BitmapToVideoEncoder bitmapToVideoEncoder;

    private final Context context;

    public static int videoWidth;
    public static int videoHeight;

    private int resultScaleX;
    private int resultScaleY;

    private int aspectX;
    private int aspectY;

    public boolean abort = false;

    // 出力用のパスを指定
    private final String outputPath = "/storage/emulated/0/Android/data/com.example.esp32ble/files/output";
    private final String resultPath = "/storage/emulated/0/Android/data/com.example.esp32ble/files/result";

    private int usedFrames;
    private int frameCount;

    private final ExecutorService es = Executors.newWorkStealingPool();
    private final Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());

    static {
        System.loadLibrary("opencv_java4");     // 追加
    }

    public VideoProcessor(CameraActivity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }

    // opencvを使う用
    public void preparing(Uri uri, AlertDialog dialog) {
        ProgressBar progressBar = dialog.findViewById(R.id.ProgressBarHorizontal);

        handler.post(setProgressBarMax(progressBar, 3));

        // uriからPathへ変換
        String id = DocumentsContract.getDocumentId(uri);
        ContentResolver contentResolver = context.getContentResolver();
        String[] columns = { MediaStore.Video.Media.DATA };
        String selection = "_id=?";
        String[] selectionArgs = new String[]{id.split(":")[1]};
        Cursor cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, selection, selectionArgs, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndexOrThrow(columns[0]));
        cursor.close();

        handler.post(setProgressBarVal(progressBar, 1));

        // 中止
        if (abort) return;

        // 処理中は画面が固まるため別スレッドで実行する
        try {
            es.execute(() -> {
                // mp4からmjpegに変換
                // こっちだけ上書きじゃない場合の処理が必要っぽい
                int ra = FFmpeg.execute("-y -i " + path + " -vcodec mpeg4 -b:v 10000k " + outputPath + ".mjpeg");
                if (ra == RETURN_CODE_SUCCESS) {
                    Log.i("TEST", "Command execution completed successfully.");
                } else {
                    int raa = FFmpeg.execute("-i " + path + " -vcodec mpeg4 -b:v 10000k " + outputPath + ".mjpeg");
                    if (raa == RETURN_CODE_CANCEL) {
                        Log.i("TEST", String.format("FFmpeg command execution failed.", ra));
                    }
                }

                handler.post(setProgressBarVal(progressBar, 2));

                if (abort) return;

                // mjpegからmp4に変換
                int rc = FFmpeg.execute("-y -i " + outputPath + ".mjpeg" + " -vcodec mpeg4 -b:v 10000k  " + outputPath + ".mp4");
                if (rc == RETURN_CODE_SUCCESS) {
                    Log.i("TEST", "Command execution completed successfully.");
                } else {
                    Log.i("TEST", String.format("FFmpeg command execution failed.", rc));
                }

                VideoCapture videoCapture = new VideoCapture(outputPath + ".mp4");

                handler.post(setProgressBarVal(progressBar, 3));

                if (abort) return;

                if (videoCapture.isOpened()) {
                    Log.i("TEST", "true");
                    processFrame(videoCapture, dialog);
                } else {
                    Log.i("TEST", "false");
                    dialog.dismiss();
                }
            });
        } finally {
            es.shutdown();
        }
    }

    public void processFrame(VideoCapture videoCapture, AlertDialog dialog) {
        ProgressBar progressBar = dialog.findViewById(R.id.ProgressBarHorizontal);

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

        // Pathを指定
        metadataRetriever.setDataSource(outputPath+".mp4");

        // フレーム数を取得
        frameCount = Integer.parseInt(
                metadataRetriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));
        // 横の長さを取得
        videoWidth = Integer.parseInt(
                metadataRetriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        // 縦の長さを取得
        videoHeight = Integer.parseInt(
                metadataRetriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

        Log.i("TEST", videoWidth + ":" + videoHeight);

        // アスペクト比を取得
        Calculator calculator = new Calculator();
        int[] resultAspects = calculator.getAspect(videoWidth, videoHeight);
        aspectX = resultAspects[0];
        aspectY = resultAspects[1];

        bitmapToVideoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
            @Override
            public void onEncodingComplete(File outputFile) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();

                        Toast.makeText(context, "Encoding complete!", Toast.LENGTH_LONG).show();

                        activity.showSelectVideo(resultPath+".mp4");
                    }
                });
            }
        });

        bitmapToVideoEncoder.startEncoding(videoWidth, videoHeight, new File(resultPath+".mp4"));

        // rows(行): height, cols(列): width
        Mat src = new Mat(videoHeight, videoWidth, CvType.CV_8UC4);

        handler.post(setDialogTitle(dialog, "出力処理中"));
        handler.post(setProgressBarMax(progressBar, frameCount));

        try {
            es.execute(() -> detectFrameTask(handler, videoCapture, src, progressBar));
        } finally {
            es.shutdown();
        }
    }

    private void detectFrameTask(
            Handler handler,
            VideoCapture videoCapture,
            Mat src,
            ProgressBar progressBar) {

        // viewのサイズを動画のアスペクト比に変更する
        // 倍率を計算
        int multipleX = CameraActivity.videoViewWidget / aspectX;
        int multipleY = CameraActivity.videoViewHeight / aspectY;

        // 低いほうの倍率でサイズを計算
        if (multipleX < multipleY) {
            resultScaleX = aspectX * multipleX;
            resultScaleY = aspectY * multipleX;
        } else {
            resultScaleX = aspectX * multipleY;
            resultScaleY = aspectY * multipleY;
        }

        while (videoCapture.grab()) {
            if (abort) {
                bitmapToVideoEncoder.abortEncoding();
                src.release();
                return;
            }
            // フレーム読み込み
            videoCapture.retrieve(src);

            // MatからBitmapに変換
            Bitmap img = Bitmap.createBitmap(
                    src.width(), src.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src, img);

            // 検出処理
            img = activity.processBitmap(img);

            // エンコード
            bitmapToVideoEncoder.queueFrame(
                    Bitmap.createScaledBitmap(
                            img, img.getWidth(), img.getHeight(), true));

            usedFrames++;
            handler.post(setProgressBarVal(progressBar, frameCount - (frameCount - usedFrames)));
        }

        handler.post(this::postActivity);

        src.release();

        bitmapToVideoEncoder.stopEncoding();
    }

    private Runnable setDialogTitle(AlertDialog dialog, String message) {
        return () -> dialog.setTitle(message);
    }

    private Runnable setProgressBarMax(ProgressBar progressBar, int max) {
        return () -> progressBar.setMax(max);
    }

    private Runnable setProgressBarVal(ProgressBar progressBar, int val) {
        return () -> progressBar.setProgress(val);
    }

    private void postActivity() {
        activity.setVideoView(resultScaleX, resultScaleY);
    }
}

/*
参考サイト

ffmpeg error https://stackoverflow.com/questions/60370424/permission-is-denied-using-android-q-ffmpeg-error-13-permission-denied
       コマンド https://www.fixes.pub/program/468569.html

opencv isOpened == false https://answers.opencv.org/question/126732/loading-video-files-using-videocapture-in-android/
       path -> mjpeg -> mp4
 */