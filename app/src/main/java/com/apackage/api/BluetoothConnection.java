package com.apackage.api;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import java.util.UUID;

/**
 * Created by tschannerl on 15/06/17.
 */

public class BluetoothConnection {
    private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_HM_RX_TX = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HM_DESCRIPTION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //public final static UUID UUID_HM_TX = UUID.fromString("713d0003-503e-4c75-ba94-3148f18d941e");
    //public final static UUID UUID_HM_RX = UUID.fromString("713d0002-503e-4c75-ba94-3148f18d941e");

    public static String BLE_SHIELD_TX = "713d0003-503e-4c75-ba94-3148f18d941e";
    public static String BLE_SHIELD_RX = "713d0002-503e-4c75-ba94-3148f18d941e";


    //private Context context;
    private Activity activity;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ProgressDialog progressDialog;
    private Handler handlerReceiver;
    private BluetoothGatt mGatt;
    private List<BluetoothGattService> services;
    private BluetoothGattService serviceRXTX;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicRX;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicTX;

    private Boolean isConnected = false;

    private static final  int connected = 1;
    private static final  int errorOnConnection = 2;


    public Boolean getConnected() {
        return isConnected;
    }

    private Handler handler = new Handler(Looper.getMainLooper()){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            progressDialog.dismiss();

            String message = null;
            if(msg.what == connected){
                message = "Conectado";
                isConnected = true;
            }else if(msg.what == errorOnConnection){
                message = "Erro na conexão";
                isConnected = false;
            }
            Toast.makeText(activity,message,Toast.LENGTH_SHORT).show();
        }
    } ;


    public BluetoothConnection(Activity activity, Handler handlerReceiver){

        this.activity = activity;
        this.handlerReceiver = handlerReceiver;
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.v("Discovery", device.getName());
                Toast.makeText(activity, "Encontrou: " + device.getName(), Toast.LENGTH_SHORT).show();

                if (device.getName().equals("INSENSE")) {
                    bluetoothAdapter.cancelDiscovery();

                    connectToDevice(device);
                }
            }
        }
    };

    public void showBluetoothDevices(){

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothAdapter.startDiscovery();
        activity.registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        /*
        Set<BluetoothDevice> devicesBonded = bluetoothAdapter.getBondedDevices();

        ArrayList<String> devicesName = new ArrayList<>();

        for(BluetoothDevice device : devicesBonded){
            devicesName.add(device.getName());
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,devicesName);

        builder.setTitle("Selecione o dispositivo");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                connectToDevice((BluetoothDevice)bluetoothAdapter.getBondedDevices().toArray()[i]);
            }
        });


        builder.create().show();
        */

    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    handler.sendEmptyMessage(connected);

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            //gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));

            for (BluetoothGattService service : services){
                bluetoothGattCharacteristicRX = service.getCharacteristic(UUID_HM_RX_TX);
                bluetoothGattCharacteristicTX = service.getCharacteristic(UUID_HM_RX_TX);

                if (bluetoothGattCharacteristicRX != null) {
                    for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattCharacteristicRX.getDescriptors()) {
                        if (bluetoothGattDescriptor.getUuid().equals(UUID_HM_DESCRIPTION)) {
                            bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mGatt.writeDescriptor(bluetoothGattDescriptor);

                            readDataGatt();
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            //read the characteristic data
            byte[] data = characteristic.getValue();
            String str = new String(data);

            Log.v("Dados: ", str);
        }
    };

    private void connectToDevice(final BluetoothDevice bluetoothDevice){


        progressDialog = ProgressDialog.show(activity,"","Conectando no dispositivo " + bluetoothDevice.getName());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    if(mGatt == null){
                        mGatt = bluetoothDevice.connectGatt(activity,false,gattCallback);
                    }

                }
                catch (Exception e){
                    handler.sendEmptyMessage(errorOnConnection);
                    bluetoothSocket = null;
                }
            }
        }).start();
    }


    public void sendDataGatt(String message){
        final byte[] tx = message.getBytes();
        if(isConnected){
            bluetoothGattCharacteristicTX.setValue(tx);

            mGatt.writeCharacteristic(bluetoothGattCharacteristicTX);

        }
    }

    public void readDataGatt(){
        if(isConnected){
            mGatt.setCharacteristicNotification(bluetoothGattCharacteristicRX,true);
            mGatt.readCharacteristic(bluetoothGattCharacteristicRX);
        }
    }


}
