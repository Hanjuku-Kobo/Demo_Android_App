package com.example.esp32ble.usecases;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.esp32ble.activity.BleTestActivity;
import com.example.esp32ble.activity.CameraActivity;
import com.example.esp32ble.activity.GaitAnalysisActivity;
import com.example.esp32ble.ml.PoseDataProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedDeque;

public class InstructionsSave {

    private BleTestActivity bleTest;

    private String path;
    private FileOutputStream fos;
    private OutputStreamWriter osw;
    private final String directoryType;

    private final Context context;

    private Queue<Float> forAnalysis = new ConcurrentLinkedDeque<>();

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

    public ArrayList<String> readCSVFiles(String keyWord) {
        File[] files;
        ArrayList<String> selectedFiles = new ArrayList<>();

        path = context.getExternalFilesDir(directoryType).toString();
        files = new File(path).listFiles();

        if (files == null) return null;

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(keyWord)) {
                selectedFiles.add(file.getName());
            }
        }

        if (selectedFiles.isEmpty()) return null;

        return selectedFiles;
    }

    public void deleteCSVFiles(String fileName) {
        File file = new File(
                context.getExternalFilesDir(directoryType), fileName);

        file.delete();
    }

    // Acceleration
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

                controller.addAccelDataAsync(xData,yData,zData);

                line = br.readLine();
            }

            bleTest.setChartName(name);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Coordinate
    public void saveCoordinate(String fileName) {
        try {
            PrintWriter pw = initSaveProcess(fileName);

            // ヘッダー部分を書き込み
            pw.print("時間");
            pw.print(",");
            pw.print("タイマー");
            pw.print(",");
            // 全部で33個
            for (int j = 0; j < 33; j++) {
                pw.print(CameraActivity.getLandmarks(context)[j]);
                pw.print(",");
                pw.print("");
                pw.print(",");
            }
            pw.println();

            // 縦列のfor文
            for (int k = 0; k < PoseDataProcess.keyCount; k++) {
                // 横列のfor文
                // 2(時間+タイマー)
                pw.print(PoseDataProcess.timeData.get(k*2));
                pw.print(",");
                pw.print(PoseDataProcess.timeData.get(k*2+1));
                pw.print(",");
                // 33(landmark数) * 2(x+y)
                for (int l = 0; l < 65; l++) {
                    pw.print(PoseDataProcess.coordinates.poll());
                    pw.print(",");
                }
                // 66個めの","を入れないため
                pw.print(PoseDataProcess.coordinates.poll());
                pw.println();
            }

            // 関節角度が終わったら後処理をする

            pw.close();
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveJointAngles(String fileName) {
        try {
            PrintWriter pw = initSaveProcess(fileName);

            // ヘッダー部分を書き込み
            pw.print("時間");
            pw.print(",");
            pw.print("タイマー");
            pw.print(",");
            // 全部で12個
            for (int j = 0; j < 12; j++) {
                pw.print(CameraActivity.getJointAngles(context)[j]);
                pw.print(",");
            }
            pw.println();

            // 縦列のfor文
            for (int k = 0; k < PoseDataProcess.keyCount; k++) {
                // 横列のfor文
                // 2(時間+タイマー)
                pw.print(PoseDataProcess.timeData.get(k*2));
                pw.print(",");
                pw.print(PoseDataProcess.timeData.get(k*2+1));
                pw.print(",");
                // 12(landmark数)
                for (int l = 0; l < 11; l++) {
                    // データ分析用
                    if (l==7 || l==8 || l==9 || l==10) {
                        float data = PoseDataProcess.jointAngles.poll();
                        forAnalysis.add(data);
                        pw.print(data);
                    } else {
                        pw.print(PoseDataProcess.jointAngles.poll());
                    }
                    pw.print(",");
                }
                // 12個めの","を入れないため
                pw.print(PoseDataProcess.jointAngles.poll());
                pw.println();
            }

            // 分析用が終わったら後処理をする

            pw.close();
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveForAnalysis(String fileName) {
        try {
            PrintWriter pw = initSaveProcess(fileName);

            // ヘッダー部分を書き込み
            pw.print("時間");
            pw.print(",");
            pw.print("タイマー");
            pw.print(",");
            // 全部で4個
            for (int j = 0; j < 4; j++) {
                pw.print(GaitAnalysisActivity.getForAnalysis(context)[j]);
                pw.print(",");
            }
            pw.println();

            // 縦列のfor文
            for (int k = 0; k < PoseDataProcess.keyCount; k++) {
                // 横列のfor文
                // 2(時間+タイマー)
                pw.print(PoseDataProcess.timeData.get(k*2));
                pw.print(",");
                pw.print(PoseDataProcess.timeData.get(k*2+1));
                pw.print(",");
                // 4(landmark数)
                for (int l = 0; l < 3; l++) {
                    pw.print(forAnalysis.poll());
                    pw.print(",");
                }
                // 12個めの","を入れないため
                pw.print(forAnalysis.poll());
                pw.println();
            }

            // 終了処理
            finalCall();

            pw.close();
            osw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finalCall() {
        Toast.makeText(context, "保存が完了しました", Toast.LENGTH_SHORT).show();

        PoseDataProcess.coordinates.clear();
        PoseDataProcess.jointAngles.clear();
        PoseDataProcess.timeData.clear();
        PoseDataProcess.keyCount = 0;
        PoseDataProcess.startTime = 0;
    }

    // ファイルをコピーして指定の場所に移動
    public void moveFiles(String outFile) {
        boolean result = false;

        File inputFile = new File("/storage/emulated/0/Android/data/com.example.esp32ble/files/result.mp4");
        File outputFile = new File(outFile);

        FileInputStream inStream;
        FileOutputStream outStream;

        try {
            inStream = new FileInputStream(inputFile);
            outStream = new FileOutputStream(outputFile);

            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();

            long pos = 0;
            while (pos < inChannel.size()) {
                pos += inChannel.transferTo(pos, inChannel.size(), outChannel);
            }

            inStream.close();
            outStream.close();

            result = true;
            Toast.makeText(context, "保存できました", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!result) {
            Toast.makeText(context, "保存できませんでした", Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<Float> choseTimerData(String fileName) {
        File file = new File(context.getExternalFilesDir(directoryType), fileName);
        ArrayList<Float> timers = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            Log.d("Header", br.readLine());
            String line = br.readLine();

            while (line != null) {
                // (date, timer, data, data, data, data) こんな形式で入っているから
                StringTokenizer stringTokenizer = new StringTokenizer(line,",");

                stringTokenizer.nextToken(); //時間をスキップ
                timers.add(Float.parseFloat(stringTokenizer.nextToken()));

                // dataをスキップ
                for (int i=0; i<4; i++) stringTokenizer.nextToken();

                line = br.readLine();
            }

            return timers;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Integer> choseForAnalysis(String fileName, int xPoint) {
        File file = new File(context.getExternalFilesDir(directoryType), fileName);

        ArrayList<Integer> data = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            Log.d("Header", br.readLine());
            String line = br.readLine();

            while (line != null) {
                // (date, timer, data, data, data, data) こんな形式で入っているから
                StringTokenizer stringTokenizer = new StringTokenizer(line,",");

                stringTokenizer.nextToken(); // 時間をスキップ
                stringTokenizer.nextToken(); // タイマーをスキップ

                int tokenCount = stringTokenizer.countTokens();
                for (int i=0; i<tokenCount; i++) {
                    if(i == xPoint) data.add((int)Float.parseFloat(stringTokenizer.nextToken()));
                    else stringTokenizer.nextToken();
                }

                line = br.readLine();
            }

            return data;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}