package com.apackage.insense;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.apackage.api.HotspotConnection;
import com.apackage.api.ServerConnectionListener;
import com.apackage.api.WifiConnection;
import com.apackage.model.Network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by Thiago Mello on 8/26/2017.
 */

public class CommunicationService extends Service implements ServerConnectionListener{
    Callbacks activity;
    private long startTime = 0;
    private long millis = 0;
    private final IBinder mBinder = new LocalBinder();
    Handler handler = new Handler();
    Runnable serviceRunnable = new Runnable() {
        @Override
        public void run() {
            millis = System.currentTimeMillis() - startTime;
            activity.startCommunication(millis); //Update Activity (client) by the implementd callback
            handler.postDelayed(this, 1000);
        }
    };
    private WifiConnection wifi;
    private HotspotConnection hotspot;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("SERVICO","DESTRUINDO O SERVICO!");
        super.onDestroy();
        //destroy the connection
        if(wifi != null)
        {
            wifi.cancel(true);
        }
        if(hotspot != null)
        {
            hotspot.cancel(true);
        }
    }

    @Override
    public void onConnectionError() {

    }

    @Override
    public void onConnectionSuccess() {

    }

    @Override
    public void onConnectionError(Map<String, String> result) {

    }

    @Override
    public void onConnectionSuccess(Map<String, Object> result) {

    }

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public CommunicationService getServiceInstance(){
            return CommunicationService.this;
        }
    }

    public void registerClient(Activity activity){
        this.activity = (Callbacks)activity;
    }

    public void startHotspotCheck()
    {
        hotspot = new HotspotConnection(((HomeActivity)activity).handlerReceiverClient, (WifiManager) ((HomeActivity)activity).getApplicationContext().getSystemService(Context.WIFI_SERVICE));
        hotspot.execute();
        handler.postDelayed(serviceRunnable, 0);
        Toast.makeText(((HomeActivity)activity).getApplicationContext(), "Iniciando scan do hotspot!", Toast.LENGTH_SHORT).show();
    }

    public void startWirelessConnection(String address, int port){
        //instantiate connection
        wifi = new WifiConnection(address, port , ((HomeActivity)activity).handlerReceiverClient);
        wifi.execute();
        handler.postDelayed(serviceRunnable, 0);
        Toast.makeText(((HomeActivity)activity).getApplicationContext(), "Iniciando conexao com oculos!", Toast.LENGTH_SHORT).show();
    }

    public void stopWirelessConnection(){
        wifi.cancel(true);
        Toast.makeText(((HomeActivity)activity).getApplicationContext(), "Encerrando conexao com oculos!", Toast.LENGTH_SHORT).show();
    }

    //callbacks interface for communication with service clients!
    public interface Callbacks{
        public void startCommunication(long data);
    }
}