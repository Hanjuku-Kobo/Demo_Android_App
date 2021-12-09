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

public class SelectGameFragment extends Fragment {

    // ゲーム選択
    private Button upDownGame;

    private String selectedGameTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_select_game, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        upDownGame = view.findViewById(R.id.game_button1);
        upDownGame.setOnClickListener(this::onClick);

        Button decideButton = view.findViewById(R.id.decide_button);
        decideButton.setOnClickListener(this::onClick);
    }

    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.game_button1:
                selectedGameTitle = (String) upDownGame.getText();

                break;

            case R.id.decide_button:
                if (selectedGameTitle != null) {
                    BleGameActivity activity = (BleGameActivity) getActivity();
                    assert activity != null;

                    activity.closeSettingFragments();

                    activity.addGameFragment();
                }

                break;
        }
    }

    public void ifUseAcceleration() {
        upDownGame.setEnabled(true);
    }

    public void ifUsePressure() {
        upDownGame.setEnabled(false);
    }

    public void closeFragment() {
        BleGameActivity activity = (BleGameActivity) getActivity();
        activity.onShowSelectGameFragment = false;

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.remove(this);
        transaction.commit();
    }
}
