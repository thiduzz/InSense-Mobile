package com.apackage.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.apackage.model.Network;
import com.apackage.utils.Constants;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.Base64;
import com.google.api.services.speech.v1.Speech;
import com.google.api.services.speech.v1.SpeechRequestInitializer;
import com.google.api.services.speech.v1.model.RecognitionAudio;
import com.google.api.services.speech.v1.model.RecognitionConfig;
import com.google.api.services.speech.v1.model.RecognizeRequest;
import com.google.api.services.speech.v1.model.RecognizeResponse;
import com.google.api.services.speech.v1.model.SpeechRecognitionResult;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Locale;

/**
 * Created by tschannerl on 20/07/17.
 */

public class WifiConnection extends AsyncTask<Void, Void, Void> {
    private String address;
    private final String CLOUD_API_KEY = "AIzaSyCmWiuCgBSJKWLxRWoNeaJiP4VKRnbexQ8";
    private int port;
    Message messageResponse = null;
    public Socket socket;
    private boolean connectSocket = false;
    private Handler handlerReceiverClient;
    private int bufferSize = 255;
    private boolean initAudio = false;
    private ByteArrayOutputStream bufAudio;
    private ByteArrayOutputStream bufData;
    private Context context;
    private SpeechRecognizer speechRecognizer;


    public WifiConnection(String address, int port, Handler handler, Context context) {
        this.address = address;
        this.port = port;
        this.handlerReceiverClient = handler;
        this.context = context;
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
            if(socket != null && socket.isConnected())
            {
                socket.close();
            }
            socket = new Socket();
            socket.connect(new InetSocketAddress(address, port),5000);
            InputStream inputStream = null;
            if(!isCancelled())
            {
                handlerReceiverClient.obtainMessage(Constants.GLASS_STARTED,null).sendToTarget();
            }
            handlerReceiverClient.obtainMessage(Constants.GLASS_AUDIO_RECOGNIZED, "Hermannplatz, Berlin").sendToTarget();
            while(!isCancelled() && socket.isConnected() && !socket.isClosed()){
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
                            if (initAudio == false){
                                bufData = new ByteArrayOutputStream();
                                bufData.write(bData);
                            }
                            break;
                        }
                        case 1 : {
                            handlerReceiverClient.obtainMessage(Constants.GLASS_AUDIO_RECORDING,null).sendToTarget();
                            initAudio = true;
                            bufAudio = new ByteArrayOutputStream();
                            break;
                        }
                        case 2 : {
                            initAudio = false;
                            if(saveFileAudio())
                            {
                                byte[] audioByte = readFileAudio();
                                if(bData != null)
                                {

                                    Log.i("INSENSE","INICIANDO INTERPRETACAO 1");
                                    String audioEncoded = Base64.encodeBase64String(bData);
                                    Log.i("INSENSE","INICIANDO INTERPRETACAO 2");
                                    Speech speechService = new Speech.Builder(
                                            AndroidHttp.newCompatibleTransport(),
                                            new AndroidJsonFactory(),
                                            null
                                    ).setSpeechRequestInitializer(
                                            new SpeechRequestInitializer(CLOUD_API_KEY))
                                            .build();
                                    Log.i("INSENSE","INICIANDO INTERPRETACAO 3");
                                    RecognitionConfig recognitionConfig = new RecognitionConfig();
                                    recognitionConfig.setLanguageCode("pt-BR");
                                    recognitionConfig.setEncoding("LINEAR16");
                                    recognitionConfig.setSampleRateHertz(16000);
                                    RecognitionAudio recognitionAudio = new RecognitionAudio();
                                    recognitionAudio.setContent(audioEncoded);
                                    Log.i("INSENSE","INICIANDO INTERPRETACAO 4");
                                    // Create request
                                    RecognizeRequest request = new RecognizeRequest();
                                    request.setConfig(recognitionConfig);
                                    request.setAudio(recognitionAudio);
                                    Log.i("INSENSE","INICIANDO INTERPRETACAO 5");
                                    RecognizeResponse response = speechService.speech()
                                            .recognize(request)
                                            .execute();
                                     List<SpeechRecognitionResult> result = response.getResults();
                                    Log.i("INSENSE","INICIANDO INTERPRETACAO 6");
                                    final String transcript = result.get(0).toPrettyString();
                                    /**
                                    final String transcript = result.getAlternatives().get(0)
                                            .getTranscript();
                                     **/
                                    messageResponse = Message.obtain( handlerReceiverClient, Constants.GLASS_AUDIO_RECOGNIZED, transcript);
                                }
                            }
                            break;
                        }
                        case 3 : {

                            break;
                        }
                        case 4 : {

                            break;
                        }
                    }
                    //String readMessage = new String(bData);
                }else{
                    checkStatus();
                }
            }
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, "Socket was closed or stopped answering");
        }  catch (GoogleJsonResponseException ex){
            ex.getStatusCode();
            ex.printStackTrace();
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, ex.getMessage() );
        } catch (ConnectException ex){
            ex.printStackTrace();
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, ex.getMessage() );
        } catch (Exception ex){
            ex.printStackTrace();
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_GENERAL_ERROR, ex.getMessage() );
        }

        return null;
    }

    private void checkStatus() throws IOException {
        try {
            socket.connect(new InetSocketAddress(address, port),5000);
        }catch (SocketException e) {
            if(e.getMessage() != "already connected"){
                socket.close();
                messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_GENERAL_ERROR, e.getMessage() );
            }
        }catch (Exception e) {
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_GENERAL_ERROR, e.getMessage() );
        }
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

    private boolean saveFileAudio(){
        try{
            if (bufAudio != null) {
                File file = new File(context.getFilesDir(), Constants.RECORDED_AUDIO_FILE_PATH);
                if(file.exists() && file.canWrite())
                {
                    file.delete();
                }
                FileOutputStream fos = new FileOutputStream(new File(context.getFilesDir(), Constants.RECORDED_AUDIO_FILE_PATH),false);
                //FileOutputStream fos = new FileOutputStream(new File("/sdcard/teste.txt"));
                bufAudio.writeTo(fos);
                bufAudio.flush();
                fos.flush();
                bufAudio.close();
                fos.close();
                Log.i("INSENSE","AUDIO SALVO!");
                //handlerReceiverClient.obtainMessage(Constants.GLASS_AUDIO_SAVED,null).sendToTarget();
                return  true;
            }else{
                return false;
            }


        }catch (IOException ex){
            ex.printStackTrace();
            Log.i("INSENSE","ERROR WHILE SAVING THE AUDIO FILE!");
            return false;
        }
    }


    private byte[] readFileAudio(){

        Log.i("INSENSE","INICIANDO LEITURA 1");
        File file = new File(context.getFilesDir(), Constants.RECORDED_AUDIO_FILE_PATH);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            byte[] arr = IOUtils.toByteArray(buf);
            buf.close();
            return arr;
        } catch (FileNotFoundException e) {
            Log.i("INSENSE","ERROR WHILE READING THE AUDIO FILE!");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            Log.i("INSENSE","ERROR WHILE READING THE AUDIO FILE!");
            e.printStackTrace();
            return null;
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
