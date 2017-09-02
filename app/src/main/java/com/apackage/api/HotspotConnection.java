package com.apackage.api;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.apackage.utils.Constants;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.Socket;

/**
 * Created by tschannerl on 20/07/17.
 */

public class HotspotConnection extends AsyncTask<Void, Void, Void> {
    Message messageResponse = null;
    private Handler handlerReceiverClient;
    WifiManager wifiManager;


    public HotspotConnection(Handler handlerReceiverClient, WifiManager wifiManager) {
        this.handlerReceiverClient = handlerReceiverClient;
        this.wifiManager = wifiManager;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
                if(this.isWifiApEnabled())
                {
                    this.trackConnectedClients();
                }
        } catch (Exception ex){
            ex.printStackTrace();
            messageResponse = Message.obtain( handlerReceiverClient, Constants.HOTSPOT_GENERAL_ERROR, ex.getMessage() );
        }


        return null;
    }


    public void trackConnectedClients(){
        int macCount = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null ) {
                    String mac = splitted[3];
                    //Log.i("HOTSPOT","Mac : Outside If " + mac);
                    if (mac.matches("..:..:..:..:..:..")) {
                        macCount++;
                        Log.i("HOTSPOT","Mac : " + mac + " IP Address : " + splitted[0]);
                        handlerReceiverClient.obtainMessage(Constants.HOTSPOT_DEVICE_FOUND, splitted[0]).sendToTarget();
                        //Log.i("HOTSPOT","Mac_Count  " + macCount + " MAC_ADDRESS  " + mac);

                    }
                }
            }
        } catch(Exception e) {
            Log.e("INSENSE","ERRO NA CLASSE HOTSPOT CONNECTION: "+ e.getMessage());
        }
    }

    public boolean isWifiApEnabled() {
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    return (boolean) method.invoke(wifiManager);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
