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
import com.apackage.utils.Constants;
import com.github.petr_s.nmea.basic.BasicNMEAHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
        //destroy the connection and kill the socket
        stopWirelessConnection();
        if(hotspot != null)
        {
            hotspot.cancel(true);
        }
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

    public void startWirelessConnection(String address, int port){
        //instantiate connection
        wifi = new WifiConnection(address, port , ((HomeActivity)activity).handlerReceiverClient, getApplicationContext(), (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE));
        wifi.execute();
        handler.postDelayed(serviceRunnable, 0);
        //Toast.makeText(((HomeActivity)activity).getApplicationContext(), "Iniciando conexao com oculos!", Toast.LENGTH_SHORT).show();
    }

    public void stopWirelessConnection(){
        try {
            if(wifi != null)
            {
                if(wifi.socket.isConnected())
                {
                        wifi.socket.close();
                }
                wifi.cancel(true);
                ((HomeActivity)activity).handlerReceiverClient.obtainMessage(Constants.CONNECTION_CLOSED, "triggered by stopWirelessConnection").sendToTarget();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("INSENSE", "FAILED TO CLOSE SOCKET / TERMINATE WIFI CONNECTION");
        }
        //Toast.makeText(((HomeActivity)activity).getApplicationContext(), "Encerrando conexao com oculos!", Toast.LENGTH_SHORT).show();
    }

    //callbacks interface for communication with service clients!
    public interface Callbacks{
        public void startCommunication(long data);
    }


    public void startHotspotCheck(Handler handler)
    {
        Timer timer = new Timer();
        final Handler handlerActivity = handler;
        TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        hotspot = new HotspotConnection(handlerActivity, (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE));
                        hotspot.execute();
                    }
        };
        //a cada 30 seg. roda o check
        timer.schedule(timerTask, 0, 30000);
    }

    public void sendDeviceMessage(int code, Object what)
    {

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
}