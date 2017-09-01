package com.apackage.api;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.apackage.model.Network;
import com.apackage.utils.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by tschannerl on 20/07/17.
 */

public class WifiConnection extends AsyncTask<Void, Void, Void> {
    private String address;
    private int port;
    Message messageResponse = null;
    private Socket socket;
    private boolean connectSocket = false;
    private Handler handlerReceiverClient;
    private int bufferSize = 255;
    private boolean initAudio = false;
    private ByteArrayOutputStream bufAudio;
    private Activity activity;


    public WifiConnection(String address, int port, Handler handler) {
        this.activity = activity;
        this.address = address;
        this.port = port;
        this.handlerReceiverClient = handler;
    }

    public boolean isConnectSocket() {
        return connectSocket;
    }

    public void setConnectSocket(boolean connectSocket) {
        this.connectSocket = connectSocket;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            socket = new Socket(address, port);
            connectSocket = true;
            handlerReceiverClient.obtainMessage(1,"Conectado!!!").sendToTarget();
            InputStream inputStream = null;
            while(!isCancelled() && connectSocket){
                inputStream = socket.getInputStream();
                if (inputStream.available() > 0){
                    byte[] bData = new byte[bufferSize];
                    int bytes = inputStream.read(bData);

                    // 0 = dados gerais
                    // 1 = INI (inicio de transmissão de dados de audio)
                    // 2 = FIM (fim de transmissão de dados de audio)
                    // 3 = GPS (dados do GPS)
                    // 4 = DIR (dados de direção)
                    switch (findTypeData(bData)){
                        case 0 : {
                            if (initAudio){
                                bufAudio.write(bData);
                            }
                            break;
                        }
                        case 1 : {
                            handlerReceiverClient.obtainMessage(1,"Iniciando a gravação do arquivo").sendToTarget();
                            initAudio = true;
                            bufAudio = new ByteArrayOutputStream();
                            break;
                        }
                        case 2 : {
                            initAudio = false;
                            saveFileAudio();

                            break;
                        }
                        case 3 : {

                            break;
                        }
                        case 4 : {

                            break;
                        }
                    }
                    String readMessage = new String(bData);
                    Log.i("Recebendo", readMessage);
                }
                /*DataInputStream data = new DataInputStream(socket.getInputStream());
                data.read(bData);
                Log.i("Socket", bData.toString());*/
            }
            
        } catch (ConnectException ex){
            ex.printStackTrace();
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, ex.getMessage() );
        } catch (Exception ex){
            ex.printStackTrace();
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_GENERAL_ERROR, ex.getMessage() );
        }


        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        handlerReceiverClient.sendMessage( messageResponse);
        super.onPostExecute(aVoid);
    }


    // 0 = dados gerais
    // 1 = INI (inicio de transmissão de dados de audio)
    // 2 = FIM (fim de transmissão de dados de audio)
    // 3 = GPS (dados do GPS)
    // 4 = DIR (dados de direção)
    protected int findTypeData(byte[] bData){
        int ret = 0;

        if (bData[0] == 73 && bData[1] == 78 && bData[2] == 73){
            ret = 1;
        }else if (bData[0] == 70 && bData[1] == 73 && bData[2] == 77){
            ret = 2;
        }else if (bData[0] == 71 && bData[1] == 80 && bData[2] == 83){
            ret = 3;
        }else if (bData[0] == 68 && bData[1] == 73 && bData[2] == 82){
            ret = 4;
        }

        return ret;
    }

    protected void saveFileAudio(){
        try{
            if (bufAudio != null) {
                FileOutputStream fos = new FileOutputStream(new File("/sdcard/tmp1.wav"));
                //FileOutputStream fos = new FileOutputStream(new File("/sdcard/teste.txt"));
                bufAudio.writeTo(fos);
                bufAudio.flush();
                fos.flush();
                bufAudio.close();
                fos.close();
                handlerReceiverClient.obtainMessage(1,"Arquivo Salvo !!!").sendToTarget();
            }


        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public void disconnectSocket(){
        if(socket != null){
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
