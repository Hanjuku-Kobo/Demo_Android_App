package com.example.esp32ble.usecases;

import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.ACTION_PAIRING_REQUEST;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.example.esp32ble.activity.BleTestActivity;
import com.example.esp32ble.dialog.DeviceListDialog;
import com.example.esp32ble.activity.BleGameActivity;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class BLEProcessor {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic readCharacteristic;
    private BluetoothGattCharacteristic writeCharacteristic;

    private BleTestActivity bleTest;
    private BleGameActivity bleGame;
    private LineChartController chartController;

    private DeviceListDialog deviceListDialog;

    private ArrayList<BluetoothDevice> devices = new ArrayList<>();

    private Context context;

    private final UUID DEVICE_SERVICE_UUID =          UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private final UUID READ_UUID =                    UUID.fromString("b62c1ffa-bdd8-46ea-a378-d539cf405d93");
    private final UUID WRITE_UUID =                   UUID.fromString("c8f8d86f-f03a-428f-8917-39384ad98e4b");
    private final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // コンストラクタ達
    public BLEProcessor(BleTestActivity bleTest, LineChartController controller) {
        this.bleTest = bleTest;
        this.chartController = controller;
    }

    public BLEProcessor(BleGameActivity bleGame) { this.bleGame = bleGame; }

    // Bluetoothの検出&接続
    public BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        final String TAG = "TEST";

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED){
                gatt.discoverServices();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                if (bluetoothGatt != null){
                    bluetoothGatt.close();
                    bluetoothGatt = null;

                    // 切れたことをわかるようにする
                    if (bleGame != null) {
                        bleGame.updateGUI(0,"接続が途切れました");
                    } else if (bleTest != null) {
                        bleTest.needsCallDialog();
                    }
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                BluetoothGattService service = gatt.getService(DEVICE_SERVICE_UUID);

                if (service != null){
                    readCharacteristic = service.getCharacteristic(READ_UUID);
                    writeCharacteristic = service.getCharacteristic(WRITE_UUID);

                    if (readCharacteristic != null && writeCharacteristic != null){
                        if (bleGame != null) {
                            bleGame.updateGUI(1, "接続に成功しました");
                        } else if (bleTest != null) {
                            bleTest.startConnect();
                        }

                        bluetoothGatt = gatt;
                        bluetoothGatt.setCharacteristicNotification(readCharacteristic, true);

                        BluetoothGattDescriptor descriptor = readCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);

                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(descriptor);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic ){
            if (READ_UUID.equals(characteristic.getUuid())) {
                String data = characteristic.getStringValue(0);

                if (data != null && data.length() > 0) {
                    String[] listData = data.split(",");

                    // バッテリー残量の場合
                    if (listData.length == 1) {
                        float list1 = Float.parseFloat(listData[0]);

                        /*
                         * Min = 3.0vで警告 (未定)
                         * Max = 4.2vが最大値 (確定)
                         *
                         * (data - Min) * (100 / (Max - Min)
                         */
                        String percent = String.format(
                                Locale.US, "%.1f", (list1 - 3.0) * (100 / (4.2 - 3.0)));

                        if (bleTest != null){
                            bleTest.setBatteryLevel(percent);
                        } else if (bleGame != null) {
                            bleGame.setBatteryLevel(percent);
                        }
                    }

                    else if (listData[0].equals("a")) {
                        // 0.12345のデータを1000倍して送られてくるから
                        float xData = Float.parseFloat(listData[1]) / 1000;
                        float yData = Float.parseFloat(listData[2]) / 1000;
                        float zData = Float.parseFloat(listData[3]) / 1000;

                        Log.i(TAG, "x:" + xData + "y:" + yData + "z:" + zData);

                        if (bleTest != null) {
                            if (bleTest.callBleTestData) {
                                chartController.addDataAsync(xData, yData, zData);
                            }
                        } else if (bleGame != null) {
                            bleGame.controlBleData(xData, yData, zData);
                        }
                    }

                    else if (listData[0].equals("pressure")) {
                        if (bleTest != null && bleTest.callBleTestData) {

                            float xData = Float.parseFloat(listData[1]);
                            float yData = (float) getPressure(xData);
                            float zData = Float.parseFloat(listData[2]);

                            Log.i(TAG, "x:" + xData + "y:" + yData + "z:" + zData);

                            chartController.addDataAsync(xData, yData, zData);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // callback

            Log.i("TEST", "Write callback");
        }
    };

    // Bluetoothデバイス検出
    public void discoverDevice(Context context, BluetoothAdapter adapter) {
        this.context = context;
        bluetoothAdapter = adapter;

        IntentFilter filter = new IntentFilter(ACTION_FOUND);
        context.registerReceiver(receiver, filter);
        IntentFilter filter2 = new IntentFilter(ACTION_PAIRING_REQUEST);
        context.registerReceiver(receiver, filter2);

        if (bluetoothAdapter.isDiscovering()) {
            // 検索中の場合はキャンセルする
            bluetoothAdapter.cancelDiscovery();
        }

        if(bluetoothAdapter.startDiscovery()) {
            // 検出を行う
            deviceListDialog = new DeviceListDialog(this, context, devices, "デバイスを選択");
            if (bleTest != null) deviceListDialog.show(bleTest.getSupportFragmentManager(), "deviceList");
            else if (bleGame != null) deviceListDialog.show(bleGame.getSupportFragmentManager(), "deviceList");
        } else {
            Toast.makeText(context, "エラー\n権限設定を確認してください", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device;

            switch (action) {
                case ACTION_FOUND:
                    Log.d("Processor", "ACTION_FOUND");
                    // デバイスが見つかった場合、Intent から BluetoothDeviceを取り出す
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    checkDeviceDuplicate(device);

                    break;
                case ACTION_PAIRING_REQUEST:
                    Log.d("Processor", "ACTION_PAIRING_REQUEST");
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    checkDeviceDuplicate(device);

                    try {
                        byte[] pin = new byte[6];
                        for (int i = 0; i < 6; i++) {
                            pin[i] = Integer.decode("0x" + device.getAddress().split(":")[5 - i]).byteValue();
                        }
                        device.setPin(pin);
                        device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
                    } catch (Exception ignored) { }

                    break;
            }
        }
    };

    private void checkDeviceDuplicate(BluetoothDevice device) {
        // 重複しているか
        for (BluetoothDevice device2 : devices) {
            if (device.getAddress().equals(device2.getAddress())) return;
        }
        // 重複していなかったら
        devices.add(device);
        deviceListDialog.createList(devices);
        Log.d("Processor", String.valueOf(devices.size()));
    }

    // gatt serverへの接続処理
    public void connectDevice(BluetoothDevice device){
        device = bluetoothAdapter.getRemoteDevice(device.getAddress());
        bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback);
    }

    // characteristicの書き込み
    public void onWrite(String message) {
        if (bluetoothGatt == null || writeCharacteristic == null) {
            return;
        }

        try {
            writeCharacteristic = bluetoothGatt
                    .getService(DEVICE_SERVICE_UUID)
                    .getCharacteristic(WRITE_UUID);

            writeCharacteristic.setValue(message);

            bluetoothGatt.writeCharacteristic(writeCharacteristic);

            if (bleGame != null) {
                if (message.equals("null") || message.equals("battery")) return;
                Toast.makeText(bleGame, "送信されました", Toast.LENGTH_SHORT).show();
            } else if (bleTest != null) {
                if (message.equals("battery")) return;
                Toast.makeText(bleTest, "送信されました", Toast.LENGTH_SHORT).show();
            }
        } catch (NullPointerException ignored) {}
    }

    // 終了時の処理
    public void clearBleGatt() {
        if( null == bluetoothGatt ) {
            return;
        }
        // 切断
        //   mBluetoothGatt.disconnect()ではなく、mBluetoothGatt.close()しオブジェクトを解放する。
        //   理由：「ユーザーの意思による切断」と「接続範囲から外れた切断」を区別するため。
        //   ①「ユーザーの意思による切断」は、mBluetoothGattオブジェクトを解放する。再接続は、オブジェクト構築から。
        //   ②「接続可能範囲から外れた切断」は、内部処理でmBluetoothGatt.disconnect()処理が実施される。
        //     切断時のコールバックでmBluetoothGatt.connect()を呼んでおくと、接続可能範囲に入ったら自動接続する。
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    private double getPressure(float volt) {
        double resister = 10000 * volt / (3.3 - volt);

        return 2853418 * Math.pow(resister, -1.1955);
    }
}