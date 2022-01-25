package com.example.esp32ble.tab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.GaitAnalysisActivity;
import com.example.esp32ble.dialog.ReadFileDialog;
import com.example.esp32ble.usecases.InstructionsSave;
import com.example.esp32ble.usecases.LineChartController;
import com.github.mikephil.charting.charts.LineChart;

public class TabFragment1 extends Fragment {

    private LineChart chart1;
    private LineChart chart2;
    private LineChart chart3;
    private LineChart chart4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab1, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Button selectionButton = view.findViewById(R.id.selection_button);
        selectionButton.setOnClickListener(this::onClick);

        chart1 = view.findViewById(R.id.tLineChert1);
        chart2 = view.findViewById(R.id.tLineChert2);
        chart3 = view.findViewById(R.id.tLineChert3);
        chart4 = view.findViewById(R.id.tLineChert4);
    }

    private void onClick(View view) {
        if (view.getId() == R.id.selection_button) {
            ReadFileDialog dialogRead = new ReadFileDialog(this, "_analysis.csv");
            dialogRead.show(getFragmentManager(), "read");
        }
    }

    public void drawLineChar(String fileName) {
        // instructionSave class でdraw methodを作成
        InstructionsSave is = new InstructionsSave(getContext());


        String[] forAnalysis = GaitAnalysisActivity.getForAnalysis(getContext());

        for (int number=0; number<4; number++) {
            LineChartController chartController = new LineChartController();
            if (number == 0) chartController.initChart(chart1, forAnalysis[number]);
            else if (number == 1) chartController.initChart(chart2, forAnalysis[number]);
            else if (number == 2) chartController.initChart(chart3, forAnalysis[number]);
            else chartController.initChart(chart4, forAnalysis[number]);

            chartController.addChartForAnalysis(
                    is.choseTimerData(fileName), is.choseForAnalysis(fileName, number));
        }
    }
}