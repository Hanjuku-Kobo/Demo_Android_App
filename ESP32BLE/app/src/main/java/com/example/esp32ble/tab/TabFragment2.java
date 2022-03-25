package com.example.esp32ble.tab;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.esp32ble.R;
import com.example.esp32ble.activity.GaitAnalysisActivity;
import com.example.esp32ble.usecases.Calculator;
import com.example.esp32ble.usecases.FileOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TabFragment2 extends Fragment {

    public static String fileName;

    private Calculator calculator;

    private TextView fileNameText;

    private TextView[] correlationText;
    private TextView val1;
    private TextView val2;

    private TextView[] euclideanText;
    private TextView val3;
    private TextView val4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab2, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        fileNameText = view.findViewById(R.id.analysis_file_name);

        correlationText = new TextView[]{
                view.findViewById(R.id.dataName1),
                view.findViewById(R.id.dataName2)};

        euclideanText = new TextView[]{
                view.findViewById(R.id.dataName5),
                view.findViewById(R.id.dataName6)};

        val1 = view.findViewById(R.id.correlation_value1);
        val2 = view.findViewById(R.id.correlation_value2);
        val3 = view.findViewById(R.id.euclidean_value1);
        val4 = view.findViewById(R.id.euclidean_value2);
    }

    // Fragmentが表示されるたびに呼ばれる
    @Override
    public void onResume() {
        super.onResume();

        // グラフがセットされてる場合
        if (fileName != null) {
            fileNameText.setText(fileName);

            String[] forAnalysis = GaitAnalysisActivity.getForAnalysis(getContext());

            // データをセット
            ArrayList<Double> calcResult = new ArrayList<>();
            for (int i=0; i<correlationText.length; i++) {
                correlationText[i].setText(forAnalysis[i]);
                euclideanText[i].setText(forAnalysis[i]);

                FileOperation fileOperation = new FileOperation(getContext());
                calculator = new Calculator();

                List<Integer> nSampleColumn = fileOperation.choseForAnalysis("sample_analysis.csv", i);
                List<Integer> nTargetColumn = fileOperation.choseForAnalysis(fileName, i);

                // 2のほうが大きい場合
                if (nSampleColumn.size() - nTargetColumn.size() < 0) {
                    // Map型に変換 + データの正規化
                    Map<Float, Integer> changedMap = calculator.convertListToMap(nSampleColumn);
                    Map<Float, Integer> constMap = calculator.convertListToMap(nTargetColumn);

                    // データ量の統一
                    nSampleColumn = calculator.unification(changedMap, constMap);
                } else {
                    Map<Float, Integer> changedMap = calculator.convertListToMap(nTargetColumn);
                    Map<Float, Integer> constMap = calculator.convertListToMap(nSampleColumn);

                    nTargetColumn = calculator.unification(changedMap, constMap);
                }

                List<Double> dSampleColumn = calculator.convertIntToDouble(nSampleColumn);
                List<Double> dTargetColumn = calculator.convertIntToDouble(nTargetColumn);

                calcResult.add(calculator.correlationCoefficient(dSampleColumn, dTargetColumn));
                calcResult.add(calculator.euclideanDistance(dSampleColumn, dTargetColumn));
            }

            for (int i=0; i<2; i++) {
                double percent1 = Math.round(calcResult.get(i)*10000.0)/100.0;
                double percent2 = Math.round(calcResult.get(i+2)*10000.0)/100.0;

                if (i==0) {
                    val1.setText(String.valueOf(percent1));
                    val2.setText(String.valueOf(percent2));
                } else {
                    val3.setText(String.valueOf(percent1));
                    val4.setText(String.valueOf(percent2));
                }
            }
        }
    }
}
