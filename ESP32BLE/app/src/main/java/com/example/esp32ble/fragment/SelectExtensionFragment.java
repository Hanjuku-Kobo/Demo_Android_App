package com.example.esp32ble.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.BleGameActivity;

public class SelectExtensionFragment extends Fragment {

    private BleGameActivity activity;
    private SelectGameFragment gameFragment;

    // 拡張機能選択
    private Button accelerationButton;
    private Button pressureButton;

    public SelectExtensionFragment(SelectGameFragment gameFragment) {
        this.gameFragment = gameFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_select_extension, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = (BleGameActivity) getActivity();
        assert activity != null;

        accelerationButton = view.findViewById(R.id.extension_acceleration);
        accelerationButton.setOnClickListener(this::onClick);

        pressureButton = view.findViewById(R.id.extension_pressure);
        pressureButton.setOnClickListener(this::onClick);
    }

    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.extension_acceleration:
                activity.onWrite("acceleration");

                accelerationButton.setEnabled(false);
                pressureButton.setEnabled(true);

                gameFragment.ifUseAcceleration();

                break;

            case R.id.extension_pressure:
                activity.onWrite("pressure");

                accelerationButton.setEnabled(true);
                pressureButton.setEnabled(false);

                gameFragment.ifUsePressure();

                break;
        }
    }

    public void closeFragment() {
        activity.onShowExtensionFragment = false;

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.remove(this);
        transaction.commit();
    }
}
