package com.example.esp32ble.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.*;
import androidx.lifecycle.LifecycleOwner;

import com.example.esp32ble.R;
import com.example.esp32ble.fragment.PoseSettingFragment;
import com.example.esp32ble.fragment.ShortcutButtonFragment;
import com.example.esp32ble.ml.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private final String[] permissions = new String[]{Manifest.permission.CAMERA};
    private final int requestPermissionCode = 101;

    private ExecutorService cameraExecutor;

    private ImageView imageView;
    private VideoView videoView;
    private PreviewView cameraView;

    private FragmentManager fragmentManager;
    private Fragment shortcutButtonFragment;

    private VisionImageProcessor imageProcessor;
    public static GraphicOverlay graphicOverlay;

    private Context context;

    private ImageButton openSetting;

    private TextView timerText;

    private boolean needUpdateGraphicOverlayImageSourceInfo = true;

    private Bitmap staticBitmap;

    private final Handler handler = new Handler();
    private long startTime = 0;

    public static int cameraViewWidget;
    public static int cameraViewHeight;

    public static int videoViewWidget;
    public static int videoViewHeight;

    // 変数化するとエラー起きる
    public static String[] getLandmarks(Context context) {
        return context.getResources().getStringArray(R.array.landmarks);
    }
    public static String[] getJointAngles(Context context) {
        return context.getResources().getStringArray(R.array.jointAngles);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_camera);

        // UIの初期化
        Toolbar toolbar = findViewById(R.id.toolbar6);
        setSupportActionBar(toolbar);

        imageView = findViewById(R.id.image_view);
        videoView = findViewById(R.id.videoView);
        cameraView = findViewById(R.id.cameraView);
        graphicOverlay = findViewById(R.id.graphic_overlay);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(this);

        openSetting = findViewById(R.id.open_setting);
        openSetting.setOnClickListener(this);

        timerText = findViewById(R.id.camera_timer_count_text);

        context = this;

        //fragmentの設定
        fragmentManager = getSupportFragmentManager();
        shortcutButtonFragment = new ShortcutButtonFragment(this);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, permissions, requestPermissionCode);
        }

        ViewTreeObserver observer = cameraView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // viewサイズを取得
                cameraViewWidget = cameraView.getWidth();
                cameraViewHeight = cameraView.getHeight();

                videoViewWidget = videoView.getWidth();
                videoViewHeight = videoView.getHeight();
            }
        });

        initCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

        imageProcessor.stop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(MainActivity.uiOptions);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                getBaseContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == requestPermissionCode && grantResults.length != 0) {

            for (int i=0; i<=permissions.length; i++) {

                if (permissions[i].equals(Manifest.permission.CAMERA)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        startCamera();
                    }

                    else {
                        Toast.makeText(context, "カメラを使用できません", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.open_setting:
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.add(R.id.contain, shortcutButtonFragment);
                transaction.commit();

                openSetting.setVisibility(View.INVISIBLE);

                break;

            case R.id.backButton:
                stopCamera();

                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);

                break;
        }
    }

    // Fragment関連
    public void addSettingFragment() {
        PoseSettingFragment settingFragment = new PoseSettingFragment(this);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.contain_full, settingFragment);
        transaction.commit();
    }

    public void changeVisibleButton() {
        openSetting.setVisibility(View.VISIBLE);
    }

    // detect & camera
    private void initPoseDetector(int MODE) {
        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build();

        if (imageView.getVisibility() == View.VISIBLE || videoView.getVisibility() == View.VISIBLE) {
            options = new PoseDetectorOptions.Builder()
                    .setDetectorMode(MODE)
                    .build();
        }

        imageProcessor = new PoseDetectorProcessor(
                context,
                options,
                false,
                PoseSettingFragment.isVisualizeZ,
                PoseSettingFragment.isVisualizeZ); // 上に同じくZ軸関係なので同期させる
    }

    public void startCamera() {
        cameraView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        videoView.setVisibility(View.INVISIBLE);

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture
                = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // Used to bind the lifecycle of cameras to the lifecycle owner
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    // Preview
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(cameraView.getSurfaceProvider());

                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    if (ShortcutButtonFragment.requestFrontCamera) {
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    }

                    // imageAnalyzer
                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
                    imageAnalysis.setAnalyzer(cameraExecutor, new PoseAnalyzer());

                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                            (LifecycleOwner)context, cameraSelector, preview, imageAnalysis);

                } catch (Exception e) {
                    Log.e("CameraXBasic", "Use case binding failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private class PoseAnalyzer implements ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {

            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {

                if (needUpdateGraphicOverlayImageSourceInfo) {
                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay.setImageSourceInfo(
                                imageProxy.getWidth(), imageProxy.getHeight(), false
                        );
                    }
                    else {
                        graphicOverlay.setImageSourceInfo(
                                imageProxy.getHeight(), imageProxy.getWidth(), false
                        );
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false;
                }
                try {
                    imageProcessor.processImageProxy(imageProxy, graphicOverlay);
                } catch (MlKitException e) {
                    Log.e("TAG", "Failed to process image. Error: " + e.getLocalizedMessage());

                    Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void initCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor();

        initPoseDetector(PoseDetectorOptions.STREAM_MODE);
    }

    public void stopCamera() {
        cameraExecutor.shutdown();
    }

    // 画像や動画を扱う
    public void initDetectorOption() {
        // 画像でやるとgraphicOverlayの情報が変わる
        // だからtrueにし、再びカメラを起動したときに情報を変えるようにする
        needUpdateGraphicOverlayImageSourceInfo = true;

        initPoseDetector(PoseDetectorOptions.SINGLE_IMAGE_MODE);
    }

    public void showSelectImage(Bitmap bitmap) {
        if (bitmap != null) {
            imageView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.INVISIBLE);
            cameraView.setVisibility(View.INVISIBLE);

            videoView.stopPlayback();

            staticBitmap = Bitmap.createScaledBitmap(
                    bitmap, 480, 640, false);

            processImage();
        }
    }

    public void processImage() {
        graphicOverlay.clear();

        // isFlippedを [ture] にするとこれ以降bitmapをさわるときにエラーが起きる
        graphicOverlay.setImageSourceInfo(
                staticBitmap.getWidth(), staticBitmap.getHeight(), false);

        // 画像を表示
        imageView.setImageBitmap(staticBitmap);

        imageProcessor.processBitmap(staticBitmap, graphicOverlay);
    }

    public void showSelectVideo(String path) {
        if (path != null) {
            videoView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);
            cameraView.setVisibility(View.INVISIBLE);

            graphicOverlay.clear();
            stopCamera();

            videoView.setVideoPath(path);

            videoView.setMediaController(new MediaController(this));

            videoView.start();
        }
    }

    public Bitmap processBitmap(Bitmap bitmap) {
        return imageProcessor.processBitmap(bitmap);
    }

    public void setVideoView(int scaleX, int scaleY) {
        // viewのサイズを変更する
        ViewGroup.LayoutParams params = videoView.getLayoutParams();

        params.width = scaleX;
        params.height = scaleY;

        videoView.setLayoutParams(params);
    }

    public void startCountUpTimer() {
        handler.postDelayed(countUpTask, 1000);

        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
        }

        timerText.setText("0s");

        timerText.setVisibility(View.VISIBLE);
    }

    public void stopCountUpTimer() {
        handler.removeCallbacks(countUpTask);

        startTime = 0;

        timerText.setVisibility(View.INVISIBLE);
    }

    Runnable countUpTask = new Runnable() {
        @Override
        public void run() {
            long time = System.currentTimeMillis();

            // long -> double -> int(result)
            int result = (int) Math.floor((double) (time - startTime) / 1000);

            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (timerText.getVisibility() == View.INVISIBLE) {
                        timerText.setVisibility(View.VISIBLE);
                    }

                    timerText.setText(result + "s");
                }
            });

            handler.postDelayed(this, 1000);
        }
    };
}