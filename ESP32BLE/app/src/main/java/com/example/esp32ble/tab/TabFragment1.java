package com.example.esp32ble.tab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.GaitAnalysisActivity;
import com.example.esp32ble.dialog.ReadFileDialog;
import com.example.esp32ble.usecases.Calculator;
import com.example.esp32ble.usecases.FileOperation;
import com.example.esp32ble.usecases.LineChartController;
import com.github.mikephil.charting.charts.LineChart;

import java.util.List;
import java.util.Map;

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
        // FileOperation class でdraw methodを作成
        FileOperation fileOperation = new FileOperation(getContext());
        Calculator calculator = new Calculator();

        String[] forAnalysis = GaitAnalysisActivity.getForAnalysis(getContext());

        for (int number=0; number<2; number++) {
            LineChartController chartController = new LineChartController();
            if (number == 0) chartController.initChart(chart1, forAnalysis[number]);
            else chartController.initChart(chart2, forAnalysis[number]);

            List<Integer> nSampleColumn = fileOperation.choseForAnalysis("sample_analysis.csv", number);
            List<Integer> nTargetColumn = fileOperation.choseForAnalysis(fileName, number);

            // Map型に変換 + データの正規化
            Map<Float, Integer> mSampleColumn = calculator.convertListToMap(nSampleColumn);
            Map<Float, Integer> mTargetColumn = calculator.convertListToMap(nTargetColumn);

            chartController.addChartForAnalysis(mSampleColumn, mTargetColumn);
        }

        Toast.makeText(getContext(), "グラフが表示されない場合はタップしてください", Toast.LENGTH_LONG).show();
        TabFragment2.fileName = fileName;
    }
}