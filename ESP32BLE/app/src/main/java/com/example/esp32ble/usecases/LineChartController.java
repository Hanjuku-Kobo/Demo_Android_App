package com.example.esp32ble.usecases;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.esp32ble.activity.BleTestActivity;
import com.example.esp32ble.activity.MainActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class LineChartController {

    private final BleTestActivity bleTest;

    private LineChart chart;

    private ArrayList<Entry> xLine;
    private ArrayList<Entry> yLine;
    private ArrayList<Entry> zLine;
    private int keyCount;

    public LineChartController(BleTestActivity bleTest) {
        this.bleTest = bleTest;
    }

    // HandlerはViewに対する処理をするために必要

    public LineChart initChart(LineChart chart) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                chart.getDescription().setEnabled(true);         //グラフ説明テキストを表示するか
                chart.getDescription().setTextColor(Color.BLACK);//グラフ説明テキストの文字色指定
                chart.setBackgroundColor(Color.WHITE);           //グラフの背景色
                chart.getAxisLeft().setEnabled(true);           //左側のメモリ
                chart.getAxisRight().setEnabled(true);           //右側のメモリ

                if (bleTest.getUseExtension().equals("acceleration")) {
                    chart.getDescription().setText("加速度センサー");   //グラフ説明テキスト

                    chart.getAxisLeft().setAxisMinimum(-35);
                    chart.getAxisLeft().setAxisMaximum(35);
                    chart.getAxisRight().setAxisMinimum(-35);
                    chart.getAxisRight().setAxisMaximum(35);
                } else if (bleTest.getUseExtension().equals("pressure")) {
                    chart.getDescription().setText("圧力センサー");

                    chart.getAxisLeft().setAxisMinimum(0);
                    chart.getAxisRight().setAxisMinimum(0);
                }

                chart.setTouchEnabled(true);
                chart.setDragEnabled(true);
                chart.setScaleEnabled(true);
                chart.setDrawGridBackground(false);
                chart.setPinchZoom(true);

                //グラフのX軸の設定
                XAxis xAxis = chart.getXAxis();                 //XAxisをインスタンス化
                xAxis.setAxisMinimum(0);                        //X軸最小値
                xAxis.setDrawGridLines(false);

                //グラフのY軸の設定
                YAxis yAxis = chart.getAxisLeft();              //YAxisをインスタンス化
                yAxis.setDrawGridLines(true);                   //グリッドを表示

                LineData data = new LineData();
                data.setValueTextColor(Color.BLACK);
                chart.setData(data);


            }
        });

        this.chart = chart;

        return chart;
    }


    public void addDataAsync(float xData, float yData, float zData) {
        if (chart == null) {
            chart = initChart(bleTest.getLineChart());
        }

        if (xLine == null) { reset(); }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //LineDataにLineDataSet格納用
                ArrayList<ILineDataSet> dataSets = new ArrayList<>();

                //Entry型でListを作成し(x,y)=(i,data)で座標を格納
                xLine.add(new Entry(keyCount, xData));

                //LineDataSet
                LineDataSet xSet = new LineDataSet(xLine, "X軸");
                if (bleTest.getUseExtension().equals("pressure")) {
                    xSet = new LineDataSet(xLine, "電圧");
                }
                xSet.setColor(Color.RED);
                xSet.setDrawCircles(false);
                xSet.setLineWidth(1.5f);              //線の太さ
                xSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);// 折れ線グラフの表示方法
                xSet.setDrawValues(false);            // 折れ線グラフの値を非表示

                dataSets.add(xSet);

                yLine.add(new Entry(keyCount, yData));

                LineDataSet ySet = new LineDataSet(yLine, "Y軸");
                if (bleTest.getUseExtension().equals("pressure")) {
                    ySet = new LineDataSet(yLine, "圧力");
                }
                ySet.setColor(Color.BLUE);
                ySet.setDrawCircles(false);
                ySet.setLineWidth(1.5f);
                ySet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                ySet.setDrawValues(false);

                dataSets.add(ySet);


                zLine.add(new Entry(keyCount, zData));

                LineDataSet zSet = new LineDataSet(zLine, "Z軸");
                if (bleTest.getUseExtension().equals("pressure")) {
                    zSet = new LineDataSet(zLine, "バッテリー電圧");
                }

                zSet.setColor(Color.GREEN);
                zSet.setDrawCircles(false);
                zSet.setLineWidth(1.5f);
                zSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                zSet.setDrawValues(false);

                dataSets.add(zSet);


                LineData data = new LineData(dataSets);

                //LineChartにLineData格納
                chart.setData(data);

                //LineChartを更新
                chart.notifyDataSetChanged();

                //X軸に表示する最大のEntryの数を指定
                chart.setVisibleXRangeMaximum(100);

                //最大数を超えたら動かす
                chart.moveViewToX(getKeyCount());

                //x軸を+1する
                increaseKeyCount();

                //main.drawLineChart(data);
                bleTest.setAccelData(xData,yData,zData);
            }
        });
    }

    public Float getKeyCount() {
        return Float.parseFloat(String.valueOf(keyCount));
    }

    public void increaseKeyCount() {
        keyCount++;
    }

    public void reset() {
        //接続が開始した時点でリストを初期化
        xLine = new ArrayList<>();
        yLine = new ArrayList<>();
        zLine = new ArrayList<>();

        keyCount = 0;
    }
}
