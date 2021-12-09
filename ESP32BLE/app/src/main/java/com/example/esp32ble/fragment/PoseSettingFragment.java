package com.example.esp32ble.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.dialog.ProgressDialog;
import com.example.esp32ble.ml.PoseGraphic;
import com.example.esp32ble.usecases.JavacvController;

import org.opencv.videoio.VideoCapture;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressLint("UseSwitchCompatOrMaterialCode")
public class PoseSettingFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private final CameraActivity activity;
    private final Context context;

    private ProgressDialog dialog;

    private JavacvController cvController;

    public static boolean isVisualizeZ = false;
    public static boolean isClassification = false;
    public static String targetTitle = null;

    private Switch backCamera;
    private Switch frontCamera;

    private Switch poseDetect;
    private Switch visualize3d;
    private Switch poseClassify;
    private Switch viewUpperDegree;
    private Switch viewLowerDegree;

    private Switch objectDetect;

    private ImageButton imageSelectButton;
    private ImageButton videoSelectButton;

    public static Bitmap useImage;
    public static Uri useVideo;
    private VideoCapture videoCapture;

    private TextView imageStateText;
    private TextView videoStateText;

    private final String IS_THERE_STATE = "選択されています";
    private final String IS_NOT_THERE_STATE = "選択されていません";

    public PoseSettingFragment (CameraActivity activity) {
        this.activity = activity;
        context = activity.getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_pose_setting, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 背景色を追加
        view.setBackgroundColor(Color.WHITE);

        // UI
        // camera
        backCamera = view.findViewById(R.id.back_camera_switch);
        backCamera.setOnCheckedChangeListener(this::onCheckedChanged);

        frontCamera = view.findViewById(R.id.front_camera_switch);
        frontCamera.setOnCheckedChangeListener(this::onCheckedChanged);

        // pose detection
        poseDetect = view.findViewById(R.id.pose_detect_switch);
        poseDetect.setOnCheckedChangeListener(this::onCheckedChanged);

        visualize3d = view.findViewById(R.id._3d_detect_switch);
        visualize3d.setOnCheckedChangeListener(this::onCheckedChanged);

        poseClassify = view.findViewById(R.id.classify_switch);
        poseClassify.setOnCheckedChangeListener(this::onCheckedChanged);

        viewUpperDegree = view.findViewById(R.id.d0_switch);
        viewUpperDegree.setOnCheckedChangeListener(this::onCheckedChanged);

        viewLowerDegree = view.findViewById(R.id.d1_switch);
        viewLowerDegree.setOnCheckedChangeListener(this::onCheckedChanged);

        // object detection
        objectDetect = view.findViewById(R.id.object_detect_switch);
        objectDetect.setOnCheckedChangeListener(this::onCheckedChanged);

        Spinner squeezeSpinner = view.findViewById(R.id.squeeze_spinner);

        // image or video
        imageSelectButton = view.findViewById(R.id.image_select_button);
        imageSelectButton.setOnClickListener(this::onClick);
        if (useImage != null) imageSelectButton.setImageResource(R.drawable.ic_baseline_close_24);

        imageStateText = view.findViewById(R.id.input_image_state);
        changeTextView(imageStateText, useImage != null);

        videoSelectButton = view.findViewById(R.id.video_select_button);
        videoSelectButton.setOnClickListener(this::onClick);
        if (useVideo != null) videoSelectButton.setImageResource(R.drawable.ic_baseline_close_24);

        videoStateText = view.findViewById(R.id.input_video_state);
        changeTextView(videoStateText, useVideo != null);

        // fragment close
        ImageButton closeButton = view.findViewById(R.id.close_fragment_button);
        closeButton.setOnClickListener(this::onClick);

        // dialog
        dialog = new ProgressDialog();

        // useCase
        cvController = new JavacvController(this, activity);

        // 既存の情報からcheckを設定
        backCamera.setChecked(!ShortcutButtonFragment.requestFrontCamera);
        frontCamera.setChecked(ShortcutButtonFragment.requestFrontCamera);
        poseDetect.setChecked(ShortcutButtonFragment.requestDetect);
        poseClassify.setChecked(isClassification);
        visualize3d.setChecked(isVisualizeZ);
        viewUpperDegree.setChecked(PoseGraphic.isViewUpperDegree);
        viewLowerDegree.setChecked(PoseGraphic.isViewLowerDegree);
        objectDetect.setChecked(ShortcutButtonFragment.isObjectClassify);

        // Spinner用
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.squeeze_object, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        squeezeSpinner.setAdapter(adapter);
        squeezeSpinner.setOnItemSelectedListener(this);

        ArrayList<String> targetItems = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.squeeze_object)));

        if (targetTitle != null) {
            for (int i = 0; i< targetItems.size(); i++) {
                if (targetTitle.equals(targetItems.get(i))) {
                    squeezeSpinner.setSelection(i);
                }
            }
        }
    }

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.back_camera_switch:
                frontCamera.setChecked(!isChecked);
                break;

            case R.id.front_camera_switch:
                backCamera.setChecked(!isChecked);
                break;

            case R.id._3d_detect_switch:
            case R.id.classify_switch:
                if (visualize3d.isChecked() || poseClassify.isChecked()) {
                    objectDetect.setEnabled(false);
                } else if (!visualize3d.isChecked() && !poseClassify.isChecked()) {
                    objectDetect.setEnabled(true);
                }

                break;

            case R.id.object_detect_switch:
                visualize3d.setEnabled(!isChecked);
                poseClassify.setEnabled(!isChecked);

                break;
        }
    }

    private void onClick(View v) {
        switch (v.getId()) {

            case R.id.image_select_button:
                if (useImage == null) {
                    Intent imageIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    imageIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    imageIntent.setType("image/*");
                    startActivityForResult(imageIntent, 100);
                } else {
                    imageSelectButton.setImageResource(R.drawable.ic_baseline_search_24);

                    imageStateText.setText(IS_NOT_THERE_STATE);
                    imageStateText.setTextColor(Color.LTGRAY);

                    useImage = null;
                }

                break;

            case R.id.video_select_button:
                if (useVideo == null) {
                    Intent videoIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    videoIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    videoIntent.setType("video/*");
                    startActivityForResult(videoIntent, 1000);
                } else {
                    videoSelectButton.setImageResource(R.drawable.ic_baseline_search_24);

                    videoStateText.setText(IS_NOT_THERE_STATE);
                    videoStateText.setTextColor(Color.LTGRAY);

                    useVideo = null;
                }

                break;

            case R.id.close_fragment_button:
                // switchの結果を代入
                ShortcutButtonFragment.requestFrontCamera = frontCamera.isChecked();

                ShortcutButtonFragment.requestDetect = poseDetect.isChecked();
                isVisualizeZ = visualize3d.isChecked();
                isClassification = poseClassify.isChecked();

                PoseGraphic.isViewUpperDegree = viewUpperDegree.isChecked();
                PoseGraphic.isViewLowerDegree = viewLowerDegree.isChecked();

                ShortcutButtonFragment.isObjectClassify = objectDetect.isChecked();

                // fragmentを閉じる
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.remove(this);
                transaction.commit();

                // 設定を開くButtonを表示
                activity.changeVisibleButton();

                if (useImage == null && useVideo == null) {
                    Log.i("TEST", "camera");

                    // cameraを再始動
                    activity.initCamera();
                    activity.startCamera();
                } else {
                    if (useImage != null) {
                        Log.i("TEST", "image");

                        // cameraを起動させずに画像を表示させる
                        activity.showSelectImage(useImage);
                    } else {
                        Log.i("TEST", "video");

                        activity.initImageView();

                        Bundle args = new Bundle();
                        args.putString("title", "動画を読み込み中");
                        dialog.setArguments(args);
                        dialog.show(activity.getSupportFragmentManager(), "progress");

                        // frameを切り出す
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cvController.getVideoFrames(videoCapture, dialog);
                            }
                        }, 500);
                    }
                }

                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultData != null) {
            Uri uri = resultData.getData();

            // 画像用
            if (requestCode == 100) {
                try {
                    useImage = getBitmapFromUri(uri);

                    useVideo = null;

                    changeTextView(imageStateText, true);
                    changeTextView(videoStateText, false);

                    imageSelectButton.setImageResource(R.drawable.ic_baseline_close_24);
                    videoSelectButton.setImageResource(R.drawable.ic_baseline_search_24);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 動画用
            else if (requestCode == 1000) {
                useVideo = uri;

                useImage = null;

                changeTextView(videoStateText, true);
                changeTextView(imageStateText, false);

                videoSelectButton.setImageResource(R.drawable.ic_baseline_close_24);
                imageSelectButton.setImageResource(R.drawable.ic_baseline_search_24);

                Bundle args = new Bundle();
                args.putString("title", "動画を読み込み中");
                dialog.setArguments(args);
                dialog.show(getFragmentManager(), "progress");

                // 1秒後に動画を読み込ませる
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cvController.getDivideFrames(context, uri);
                        //cvController.setFilePath(context, useVideo);
                    }
                }, 300);
            }
        }
    }

    public void deleteDialog(VideoCapture videoCapture) {
        if (videoCapture != null) {
            this.videoCapture = videoCapture;
        }

        dialog.dismiss();
    }

    private void changeTextView(TextView view, boolean bool) {
        if (bool) {
            view.setText(IS_THERE_STATE);
            view.setTextColor(Color.rgb(93,173,133));
        } else {
            view.setText(IS_NOT_THERE_STATE);
            view.setTextColor(Color.LTGRAY);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner)parent;
        String item = (String)spinner.getSelectedItem();

        if (item.equals("all")) {
            targetTitle = null;
        } else {
            targetTitle = item;
        }
    }

    // 何も選択されなかったとき
    @Override
    public void onNothingSelected(AdapterView<?> parent) { }
}