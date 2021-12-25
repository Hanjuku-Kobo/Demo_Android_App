package com.example.esp32ble.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.dialog.GetPermissionDialog;
import com.example.esp32ble.dialog.VideoSaveDialog;
import com.example.esp32ble.ml.PoseDataProcess;
import com.example.esp32ble.usecases.InstructionsSave;

import static com.example.esp32ble.fragment.PoseSettingFragment.useVideo;

public class ShortcutButtonFragment extends Fragment implements View.OnClickListener {

    private final CameraActivity activity;

    private ImageButton saveButton;

    public static boolean requestFrontCamera = false;
    public static boolean requestDetect = false;
    public static boolean isObjectClassify = false;
    public static boolean isStartedSave = false;

    public ShortcutButtonFragment(CameraActivity activity) {
        this.activity = activity;
    }

    // viewを作成
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_pose_shortcut, container, false);
    }

    // viewが作成された後
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton cameraChangeButton = view.findViewById(R.id.camera_button);
        cameraChangeButton.setOnClickListener(this);

        ImageButton detectButton = view.findViewById(R.id.detection_button);
        detectButton.setOnClickListener(this);

        ImageButton objectButton = view.findViewById(R.id.object_button);
        objectButton.setOnClickListener(this);

        saveButton = view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(this);
        if (!requestDetect && useVideo == null) {
            saveButton.setEnabled(false);
        } else if (isStartedSave) {
            saveButton.setImageResource(R.drawable.ic_baseline_stop_24);
        }

        ImageButton detailButton = view.findViewById(R.id.detail_button);
        detailButton.setOnClickListener(this);

        ImageButton closeButton = view.findViewById(R.id.close_fragment_button);
        closeButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_button:
                requestFrontCamera = !requestFrontCamera;

                activity.startCamera();

                break;

            case R.id.detection_button:
                if (!requestDetect) {
                    // 開始
                    requestDetect = true;

                    clearUsedSaveMaps();

                    if (!saveButton.isEnabled()) {
                        saveButton.setEnabled(true);
                    }
                }
                else {
                    // 停止
                    requestDetect = false;

                    // Pose detectionの終わるタイミングで強制終了
                    saveButton.setEnabled(false);
                    if (isStartedSave) {
                        saveStop();
                    }
                }

                if (PoseSettingFragment.useImage != null) {
                    activity.processImage();
                }

                break;

            case R.id.object_button:
                isObjectClassify = !isObjectClassify;

                if (PoseSettingFragment.useImage != null) {
                    activity.processImage();
                }

                break;

            case R.id.save_button:
                // リアルタイム検出
                if (useVideo == null) {
                    if (!isStartedSave) {
                        // 開始
                        isStartedSave = true;

                        clearUsedSaveMaps();

                        saveButton.setImageResource(R.drawable.ic_baseline_stop_24);

                        new PoseDataProcess().setStartTime();

                        activity.startCountUpTimer();
                    }
                    else {
                        // 停止
                        saveStop();
                    }
                }
                // 動画使用時
                else  {
                    VideoSaveDialog videoSaveDialog = new VideoSaveDialog();
                    videoSaveDialog.show(getFragmentManager(), "save");
                }

                break;

            case R.id.detail_button:
                // shortcutのfragmentを閉じる
                closeFragment();

                // cameraを止める
                activity.stopCamera();

                // 詳細設定のfragmentを表示
                activity.addSettingFragment();

                break;

            case R.id.close_fragment_button:
                closeFragment();

                activity.changeVisibleButton();

                break;
        }
    }

    private void saveStop() {
        activity.stopCountUpTimer();

        isStartedSave = false;

        saveButton.setImageResource(R.drawable.ic_baseline_save_alt_24);

        if (PoseDataProcess.coordinates.size() != 0) {
            GetPermissionDialog getPermissionDialog = new GetPermissionDialog();
            getPermissionDialog.show(activity.getSupportFragmentManager(), "save");
        }
    }

    private void clearUsedSaveMaps() {
        if (PoseDataProcess.coordinates.size() != 0) {
            PoseDataProcess.coordinates.clear();
            PoseDataProcess.jointAngles.clear();
            PoseDataProcess.timeData.clear();
            PoseDataProcess.keyCount = 0;
            PoseDataProcess.startTime = 0;
        }
    }

    private void closeFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.remove(this);
        transaction.commit();
    }
}
