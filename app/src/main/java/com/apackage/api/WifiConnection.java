package com.apackage.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.apackage.model.Network;
import com.apackage.utils.Constants;
import com.github.petr_s.nmea.basic.BasicNMEAHandler;
import com.github.petr_s.nmea.basic.BasicNMEAParser;
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private int bufferSize = 200;
    private boolean initAudio = false;
    private boolean initGPS = false;
    private ByteArrayOutputStream bufAudio;
    private ByteArrayOutputStream bufData;
    private Context context;
    private WifiManager wifiManager;


    public WifiConnection() {
    }

    public WifiConnection(String address, int port, Handler handler, Context context, WifiManager wifiManager) {
        this.address = address;
        this.port = port;
        this.handlerReceiverClient = handler;
        this.context = context;
        this.wifiManager = wifiManager;
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
            //handlerReceiverClient.obtainMessage(Constants.GLASS_AUDIO_RECOGNIZED, "Hermannplatz, Berlin").sendToTarget();
            while(!isCancelled() && socket.isConnected() && !socket.isClosed()){
                inputStream = socket.getInputStream();
                checkStatus();
                if (inputStream.available() > 0)
                {
                    byte[] bData = new byte[bufferSize];
                    int bytes = inputStream.read(bData);

                    //String readMessage = new String(bData);
                    //Log.i("Recebendo", readMessage);

                    // 0 = dados gerais
                    // 1 = INI (inicio de transmissão de dados de audio)
                    // 2 = FIM (fim de transmissão de dados de audio)
                    // 3 = GPS (dados do GPS)
                    // 4 = DIR (dados de direção)
                    switch (findTypeData(bData)){
                        case 0 : {
                            if (initAudio){
                                bufAudio.write(bData);
                            }else if (initGPS){
                                initGPS = false;
                                decodeGPS(bData);
                            }
                            break;
                        }
                        case 1 : {
                            //handlerReceiverClient.obtainMessage(Constants.GLASS_AUDIO_RECOGNIZED, "Wiener Strasse 35, Berlin").sendToTarget();
                            handlerReceiverClient.obtainMessage(Constants.GLASS_AUDIO_RECORDING,null).sendToTarget();
                            initAudio = true;
                            bufAudio = new ByteArrayOutputStream();
                            break;
                        }
                        case 2 : {
                            initAudio = false;

                            //saveFileAudio();

                            if (bufAudio.size() > 0){
                                byte[] b = bufAudio.toByteArray();

                                recognitionAudio(b);
                            }

                            break;
                        }
                        case 3 : {
                            initGPS = true;
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

                //Log.i("INSENSE", "Rodando WifiConnection");
            }
            Log.i("INSENSE", "WifiConnection PAROU!");
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, "Socket was closed or stopped answering");
            return null;
        }  catch (GoogleJsonResponseException ex){
            ex.getStatusCode();
            ex.printStackTrace();
            messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, ex.getMessage() );
            return null;
        } catch (ConnectException ex){
            ex.printStackTrace();
            try {
                socket.close();
                messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, ex.getMessage() );
            } catch (IOException e) {
                e.printStackTrace();
                messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, ex.getMessage() );
            }
            return null;
        } catch (Exception ex){
            ex.printStackTrace();
            try {
                socket.close();
                messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_GENERAL_ERROR, ex.getMessage() );
            } catch (IOException e) {
                e.printStackTrace();
                messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_ERROR, ex.getMessage() );
            }
            return null;
        }
    }

    public void recognitionAudio(byte[] b){
        try {
            byte[] b16 = new byte[b.length + b.length-44];

            int Subchunk2Size = b.length-44 + b.length-44;
            byte[] Subchunk2SizeByte = new byte[4];
            Subchunk2SizeByte[0] = (byte) (Subchunk2Size >> 0);
            Subchunk2SizeByte[1] = (byte) (Subchunk2Size >> 8);
            Subchunk2SizeByte[2] = (byte) (Subchunk2Size >> 16);
            Subchunk2SizeByte[3] = (byte) (Subchunk2Size >> 24);

            int ChunkSize = Subchunk2Size + 36;
            byte[] ChunkSizeByte = new byte[4];
            ChunkSizeByte[0] = (byte) (ChunkSize >> 0);
            ChunkSizeByte[1] = (byte) (ChunkSize >> 8);
            ChunkSizeByte[2] = (byte) (ChunkSize >> 16);
            ChunkSizeByte[3] = (byte) (ChunkSize >> 24);

            // ChunkSize        36 + SubChunk2Size, or more precisely: 4 + (8 + SubChunk1Size) + (8 + SubChunk2Size)
            b16[4] = ChunkSizeByte[0];
            b16[5] = ChunkSizeByte[1];
            b16[6] = ChunkSizeByte[2];
            b16[7] = ChunkSizeByte[3];

            // valor 32000 ByteRate (== SampleRate * NumChannels * BitsPerSample/8)
            b16[28] = 0;
            b16[29] = 125;
            b16[30] = 0;
            b16[31] = 0;

            // valor 1*16/8 = 2 (BlockAlign == NumChannels * BitsPerSample/8)
            b16[32] = 2;
            b16[33] = 0;

            // BitsPerSample    8 bits = 8, 16 bits = 16, etc.
            b16[34] = 16;
            b16[35] = 0;

            // Subchunk2Size    == NumSamples * NumChannels * BitsPerSample/8
            b16[40] = Subchunk2SizeByte[0];
            b16[41] = Subchunk2SizeByte[1];
            b16[42] = Subchunk2SizeByte[2];
            b16[43] = Subchunk2SizeByte[3];

            int index16 = 1;
            for (int i = 0; i < b.length; i++){
                if ((i < 4) || (i >= 8 && i < 28) || (i >= 36 && i < 40)){
                    b16[i] = b[i];
                }else if (i > 43){
                    int bit8 = b[i];
                    int bit16;

                    if (bit8 <= 0){
                        bit16 = 128 + bit8;
                    }else{
                        bit16 = bit8 - 128;
                    }
                    b16[i+index16-1] = 0;
                    b16[i+index16] = (byte)bit16;

                    index16++;
                }
            }

            sendData("AUD:112");


            Log.i("INSENSE", "INICIANDO INTERPRETACAO 1");
            String audioEncoded = Base64.encodeBase64String(b16);
            Log.i("INSENSE", "INICIANDO INTERPRETACAO 2");
            Speech speechService = new Speech.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(),
                    null
            ).setSpeechRequestInitializer(
                    new SpeechRequestInitializer(CLOUD_API_KEY))
                    .build();
            Log.i("INSENSE", "INICIANDO INTERPRETACAO 3");
            RecognitionConfig recognitionConfig = new RecognitionConfig();
            recognitionConfig.setLanguageCode("pt-BR");
            recognitionConfig.setEncoding("LINEAR16");
            recognitionConfig.setSampleRateHertz(16000);
            RecognitionAudio recognitionAudio = new RecognitionAudio();
            recognitionAudio.setContent(audioEncoded);
            Log.i("INSENSE", "INICIANDO INTERPRETACAO 4");
            // Create request
            RecognizeRequest request = new RecognizeRequest();
            request.setConfig(recognitionConfig);
            request.setAudio(recognitionAudio);
            Log.i("INSENSE", "INICIANDO INTERPRETACAO 5");
            RecognizeResponse response = speechService.speech().recognize(request).execute();
            List<SpeechRecognitionResult> result = response.getResults();
            Log.i("INSENSE", "INICIANDO INTERPRETACAO 6");
            if (result != null && result.size() > 0) {
                final String transcript = result.get(0).toPrettyString();
                //messageResponse = Message.obtain(handlerReceiverClient, Constants.GLASS_AUDIO_RECOGNIZED, transcript);
                handlerReceiverClient.obtainMessage(Constants.GLASS_AUDIO_RECOGNIZED,transcript).sendToTarget();
            } else {
                Log.i("INSENSE", "NÃO RECONHECEU NADA");
                sendData("AUD:114");
            }
        }catch (IOException e){
            e.printStackTrace();
            messageResponse = Message.obtain( handlerReceiverClient, Constants.GLASS_ERROR_CODE, e.getMessage() );
        }
    }

    public void sendData(String data){
        try{
            Log.i("Enviando: ", data);
            data = data + '\n';
            socket.getOutputStream().write(data.getBytes());
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private void checkStatus() throws IOException {
        try {
            //run every 5sec
            if(Calendar.getInstance().getTimeInMillis() % 5000 == 0)
            {
                if(!this.isWifiApEnabled() || !this.hasConnectedClient() || !this.isConnectionPinging())
                {
                    Log.i("INSENSE", "Abortando WifiConnection: fechando socket!");
                    socket.close();
                    messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_GENERAL_ERROR, "The socket was disconnected, the device seem to have lost connection!");
                }
            }
        }catch (SocketException e) {
            if(e.getMessage() != "already connected"){
                socket.close();
                messageResponse = Message.obtain( handlerReceiverClient, Constants.CONNECTION_GENERAL_ERROR, e.getMessage() );
            }
        }catch (Exception e) {
            Log.i("INSENSE", "Abortando WifiConnection: fechando socket!");
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

    private void decodeGPS(byte[] dataGPS){
        String dataGPSS = "";

        int index = 0;
        char gpsC = (char)dataGPS[index];
        while (gpsC != '\n'){
            dataGPSS += gpsC;
            index++;
            gpsC = (char)dataGPS[index];
        }

        String gps = dataGPSS.substring(0, index-3);
        String dir = dataGPSS.substring(index-3, index);
        //Log.i("INSENSE", gps);
        //Log.i("INSENSE", dir);

        ArrayList<String> gpsDir = new ArrayList<>();
        gpsDir.add(gps);
        gpsDir.add(dir);

        handlerReceiverClient.obtainMessage(Constants.GLASS_GPS_COORDINATE_RECEIVED,gpsDir).sendToTarget();


        /*
        for (int i = 0; i < 70; i++){
            gps += (char)dataGPS[i];
        }

        handlerReceiverClient.obtainMessage(Constants.GLASS_GPS_COORDINATE_RECEIVED,gps).sendToTarget();

        Log.i("INSENSE", gps);

        String dir = "";
        for (int j=70; j < 73; j++){
            dir += (char) dataGPS[j];
        }

        Log.i("INSENSE", dir);
        */
    }

    private boolean saveFileAudio(){
        try{
            if (bufAudio != null) {
                File file = new File(Constants.RECORDED_AUDIO_FILE_PATH);
                if(file.exists() && file.canWrite())
                {
                    file.delete();
                }
                FileOutputStream fos = new FileOutputStream(new File(Constants.RECORDED_AUDIO_FILE_PATH),false);
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


    public boolean hasConnectedClient(){
        int macCount = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            boolean found = false;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null ) {
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        macCount++;
                        if(Constants.REGISTERED_IMACS.contains(mac.toString()))
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch(Exception e) {
            Log.e("INSENSE","ERRO NA CLASSE WIFI CONNECTION: "+ e.getMessage());
            return false;
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
    public boolean isConnectionPinging(){
        /**
        socket = new Socket();
        socket.connect(new InetSocketAddress(address, port),3000);
        if(socket.isConnected())
        {
            socket.close();
            return true;
        }
        return false;
        **/
        return true;
    }
}
