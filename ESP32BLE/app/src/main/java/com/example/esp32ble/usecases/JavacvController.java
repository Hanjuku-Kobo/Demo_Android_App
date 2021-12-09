package com.example.esp32ble.usecases;

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
import android.widget.Toast;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.dialog.ProgressDialog;
import com.example.esp32ble.fragment.PoseSettingFragment;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.osgi.OpenCVInterface;
import org.opencv.videoio.VideoCapture;

import java.io.File;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class JavacvController {

    private CameraActivity activity;
    private PoseSettingFragment fragment;

    private Context context;

    private int videoWidth;
    private int videoHeight;

    // 出力用のパスを指定
    private String outputPath = "/storage/emulated/0/Android/data/com.example.esp32ble/output";
    private String resultPath = "/storage/emulated/0/Android/data/com.example.esp32ble/result";

    static {
        System.loadLibrary("opencv_java4");     // 追加
    }

    public JavacvController(PoseSettingFragment fragment, CameraActivity activity) {
        this.fragment = fragment;
        this.activity = activity;

        this.context = activity.getApplicationContext();
    }

    // opencvを使う用
    public void getDivideFrames(Context context, Uri uri) {
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

        // 処理中は画面が固まるため別スレッドで実行する
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // mp4からmjpegに変換
                int ra = FFmpeg.execute("-y -i " + path + " -c:v mpeg4 " + outputPath + ".mjpeg");
                if (ra == RETURN_CODE_SUCCESS) {
                    Log.i("TEST", "Command execution completed successfully.");
                } else {
                    Log.i("TEST", String.format("FFmpeg command execution failed.", ra));
                }

                // mjpegからmp4に変換
                int rc = FFmpeg.execute("-y -i " + outputPath + ".mjpeg" + " -c:v mpeg4 " + outputPath + ".mp4");
                if (rc == RETURN_CODE_SUCCESS) {
                    Log.i("TEST", "Command execution completed successfully.");
                } else {
                    Log.i("TEST", String.format("FFmpeg command execution failed.", rc));
                }

                VideoCapture videoCapture = new VideoCapture(outputPath + ".mp4");

                if (videoCapture.isOpened()) {
                    Log.i("TEST", "true");
                    fragment.deleteDialog(videoCapture);
                } else {
                    Log.i("TEST", "false");
                    fragment.deleteDialog(null);
                }
            }
        });
    }

    public void getVideoFrames(VideoCapture videoCapture, ProgressDialog dialog) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

        // Pathを指定
        metadataRetriever.setDataSource(outputPath+".mp4");

        // フレーム数を取得
        int frameCount = Integer.parseInt(
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

        Log.i("TEST", String.valueOf(frameCount));

        BitmapToVideoEncoder bitmapToVideoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
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

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                while (videoCapture.grab()) {
                    videoCapture.retrieve(src);

                    // MatからBitmapに変換
                    Bitmap img = Bitmap.createBitmap(src.width(), src.height(),
                            Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(src, img);

                    // 検出処理
                    img = activity.processImage(
                            Bitmap.createScaledBitmap(img, 480, 640, false));

                    bitmapToVideoEncoder.queueFrame(
                            Bitmap.createScaledBitmap(img, videoWidth, videoHeight, true));
                }

                src.release();

                bitmapToVideoEncoder.stopEncoding();
            }
        });
    }
}

/*
参考サイト

ffmpeg error https://stackoverflow.com/questions/60370424/permission-is-denied-using-android-q-ffmpeg-error-13-permission-denied
       コマンド https://www.fixes.pub/program/468569.html

opencv isOpened == false https://answers.opencv.org/question/126732/loading-video-files-using-videocapture-in-android/
       path -> mjpeg -> mp4
 */