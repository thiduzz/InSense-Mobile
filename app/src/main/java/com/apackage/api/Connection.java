package com.apackage.api;

import android.content.Context;
import android.os.AsyncTask;

import com.apackage.insense.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class Connection extends AsyncTask<String,Void,String> {

    private static final int CLIENT_ID = 1;
    private static final String CLIENT_SECRET = "GdXU1Z7I7woXVzgUsh0FfenNBdJo270XdpWzk2wC";

    private ConnectionListener connectionListener;
    private Context context;

    public Connection(ConnectionListener connectionListener, Context context){
        this.connectionListener = connectionListener;
        this.context = context;
    }

    @Override
    protected String doInBackground(String... query) {
        switch(query[0])
        {
            case "login":
                return getToken(query[1],query[2]);
            case "settings":
                //return getSettings(getToken(),query[0]);
                return "";
            case "devices":
                return "";
        }
        return "";
    }


    @Override
    protected void onPostExecute(String string) {
        if(string == null){
            connectionListener.onConnectionError();
        }else{
            try {
                JSONObject jsonObject = new JSONObject(string);
                JSONArray jsonArray = jsonObject.getJSONArray("statuses");
                connectionListener.onConnectionSuccess(jsonArray);
            }
            catch (Exception e){
                connectionListener.onConnectionError();
            }
        }
    }


    private String getSettings(String apiToken, String query){

        InputStream inputStream = null;
        try {
            URL url = new URL( context.getString(R.string.auth_login) + query);
            HttpURLConnection httpURLConnection = (HttpURLConnection)
                    url.openConnection();

            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.addRequestProperty("Authorization","Bearer " + apiToken);

            httpURLConnection.connect();

            if(httpURLConnection.getResponseCode()
                    == HttpURLConnection.HTTP_OK){
                inputStream = httpURLConnection.getInputStream();
                return getStringFromInputStream(inputStream);
            }else{
                return null;
            }

        }catch (Exception e){
            return null;
        }finally {
            if (inputStream != null){
                try {
                    inputStream.close();
                }catch (Exception e){}
            }
        }


    }

    private String getToken(String username, String password){

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {

            URL url = new URL(context.getString(R.string.auth_token));
            HttpURLConnection httpURLConnection = (HttpURLConnection)
                    url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            //httpURLConnection.addRequestProperty("Authorization","Basic " +API_KEY);
            httpURLConnection.addRequestProperty("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");
            Map<String,Object> params = new LinkedHashMap<>();
            params.put("grant_type", "client_credentials");
            params.put("client_id", CLIENT_ID);
            params.put("client_secret", CLIENT_SECRET);
            params.put("username", username);
            params.put("password", password);
            params.put("scope", "*");

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String,Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            outputStream = httpURLConnection.getOutputStream();
            outputStream.write(postDataBytes);

            httpURLConnection.connect();

            if(httpURLConnection.getResponseCode()
                    == HttpURLConnection.HTTP_OK){
                inputStream = httpURLConnection.getInputStream();
                String response = getStringFromInputStream(inputStream);
                JSONObject jsonObject = new JSONObject(response);
                return jsonObject.getString("access_token");
            }
            else{
                return  null;
            }
        }
        catch (Exception e){
            return  null;
        }
        finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (Exception ex){}
            }
            if(outputStream != null){
                try {
                    outputStream.close();
                }catch (Exception ex){}
            }
        }

    }

    private String getStringFromInputStream(InputStream inputStream)throws Exception {

        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        String response = "";
        while (true){
            char[] charReader = new char[1024];
            int len = reader.read(charReader);
            if(len == -1){
                break;
            }else{
                response = response + new String(charReader,0,len);
            }
        }
        return response;
    }

}
