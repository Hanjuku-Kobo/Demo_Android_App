package com.example.esp32ble.usecases;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.example.esp32ble.activity.BleTestActivity;
import com.example.esp32ble.ml.PoseDataProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import static com.example.esp32ble.activity.CameraActivity.poseContext;

public class InstructionsSave {

    private BleTestActivity bleTest;

    private String path;
    private FileOutputStream fos;
    private OutputStreamWriter osw;
    private final String directoryType;

    private Context context = poseContext;

    // 保存処理で使用
    public InstructionsSave() {
        directoryType = Environment.DIRECTORY_DOCUMENTS;
    }

    // 削除処理で使用
    public InstructionsSave(Context context) {
        this.context = context;
        directoryType = Environment.DIRECTORY_DOCUMENTS;
    }

    // Activityからの呼び出し
    public InstructionsSave(BleTestActivity bleTest) {
        this.bleTest = bleTest;
        context = bleTest.getApplicationContext();
        directoryType = Environment.DIRECTORY_DOCUMENTS;
    }

    private PrintWriter initSaveProcess(String name) {
        try {
            path = context.getExternalFilesDir(directoryType).toString();
            String file = path + "/" + name;

            fos = new FileOutputStream(file, true);
            osw = new OutputStreamWriter(fos, "UTF-8");

            return new PrintWriter(osw);

        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<String> readCSVFiles() {
        File[] files;
        ArrayList<String> csvFiles = new ArrayList<>();

        path = context.getExternalFilesDir(directoryType).toString();
        files = new File(path).listFiles();

        if (files == null) return null;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".csv")) {
                csvFiles.add(file.getName());
            }
        }

        if (csvFiles.isEmpty()) return null;

        return csvFiles;
    }

    public void deleteCSVFiles(String fileName) {
        File file = new File(
                context.getExternalFilesDir(directoryType), fileName);

        file.delete();
    }

    /* Acceleration */

    public boolean saveAcceleration(String name, BleTestActivity bleTest) {
        try {
            PrintWriter pw = initSaveProcess(name);

            for (int k = 0; k < bleTest.getData('s', 0); k++) {
                pw.print(bleTest.getData('x', k));
                pw.print(",");
                pw.print(bleTest.getData('y', k));
                pw.print(",");
                pw.print(bleTest.getData('z', k));
                pw.println();
            }

            pw.close();
            osw.close();
            fos.close();
            Toast.makeText(context, "csvファイルを保存しました", Toast.LENGTH_SHORT).show();

            return false;

        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public void drawAcceleration(String name){
        File file = new File(context.getExternalFilesDir(directoryType),name);

        LineChartController controller = new LineChartController(bleTest);

        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line = br.readLine();

            while (line != null) {
                // (data, data, data) こんな形式で入っているから
                StringTokenizer stringTokenizer = new StringTokenizer(line,",");

                float xData = Float.parseFloat(stringTokenizer.nextToken());
                float yData = Float.parseFloat(stringTokenizer.nextToken());
                float zData = Float.parseFloat(stringTokenizer.nextToken());

                controller.addDataAsync(xData,yData,zData);

                line = br.readLine();
            }

            bleTest.setChartName(name);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Coordinate */

    public void saveCoordinate(String fileName,
                               ArrayList<Float> elapsedTime,
                               ArrayList<Float> xList,
                               ArrayList<Float> yList,
                               ArrayList<Long> angleList) {
        try {
            PrintWriter pw = initSaveProcess(fileName);

            for (int k = 0; k < xList.size(); k++) {
                // 角度を持っていないランドマークがあるから
                if (angleList != null) {
                    pw.print(elapsedTime.get(k));
                    pw.print(",");
                    pw.print(xList.get(k));
                    pw.print(",");
                    pw.print(yList.get(k));
                    pw.print(",");
                    pw.print(angleList.get(k));
                    pw.println();
                }
                else {
                    pw.print(elapsedTime.get(k));
                    pw.print(",");
                    pw.print(xList.get(k));
                    pw.print(",");
                    pw.print(yList.get(k));
                    pw.println();
                }
            }

            pw.close();
            osw.close();
            fos.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finalCall() {
        Toast.makeText(context, "保存が完了しました", Toast.LENGTH_SHORT).show();

        PoseDataProcess.coordinateMap.clear();
        PoseDataProcess.angleMap.clear();
        PoseDataProcess.elapsedTime.clear();
        PoseDataProcess.keyCount = 0;
        PoseDataProcess.startTime = 0;
    }
}