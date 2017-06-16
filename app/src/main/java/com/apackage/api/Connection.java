package com.apackage.api;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.apackage.db.DataBase;
import com.apackage.insense.R;
import com.apackage.model.User;
import com.apackage.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Connection extends AsyncTask<String,Void,String> {

    private static final int CLIENT_ID = 1;
    private static final String CLIENT_SECRET = "bIc3jb2EmPvPRYOlCTUF8uMkYXBAHXh1kD1YIJx2";

    private ConnectionListener connectionListener;
    private Context context;
    private final DataBase db;

    public Connection(ConnectionListener connectionListener, Context context){
        this.connectionListener = connectionListener;
        this.context = context;
        this.db = new DataBase(context);
    }

    @Override
    protected String doInBackground(String... query) {
        switch(query[0])
        {
            case Constants.REQUEST_LOGIN:
                JSONObject tokenResponse = getAccessToken(query[1],query[2]);
                try {
                    JSONObject loginResponse = authenticate(tokenResponse.getString("access_token"));
                    User user = new User(loginResponse.getInt("id"), loginResponse.getString("email"), tokenResponse.getString("access_token"), loginResponse.getString("name"), tokenResponse.getString("refresh_token"));
                    int resultAuth = db.saveOrUpdate(user, true);
                    if(resultAuth == 1)
                    {
                        //TODO: salvar/atualizar os dispositivos cadastrados do usuario
                        return Integer.toString(user.getId());
                    }
                    throw new Exception("Falhou ao salvar as informacoes do usuário no dispositivo");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case Constants.REQUEST_SETTINGS:
                //return getSettings(getToken(),query[0]);
                return "";
            case Constants.REQUEST_DEVICES:
                return "";
            case Constants.REQUEST_REFRESH_TOKEN:
                return "";
        }
        return "";
    }

    private JSONObject authenticate(String access_token) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            URL url = new URL(context.getString(R.string.auth_login));
            HttpURLConnection httpURLConnection = (HttpURLConnection)
                    url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            //httpURLConnection.addRequestProperty("Authorization","Basic " +API_KEY);
            httpURLConnection.addRequestProperty("Authorization","Bearer " + access_token);
            httpURLConnection.addRequestProperty("Accept","application/json");
            httpURLConnection.connect();

            if(httpURLConnection.getResponseCode()
                    == HttpURLConnection.HTTP_OK){
                inputStream = httpURLConnection.getInputStream();
                String response = getStringFromInputStream(inputStream);
                return new JSONObject(response);
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


    @Override
    protected void onPostExecute(String string) {
        if(string == null){
            connectionListener.onConnectionError();
        }else{
            try {

                Map<String, Object> result = new HashMap<String, Object>();
                result.put("name", Thread.currentThread().getName());
                result.put("result",string);
                connectionListener.onConnectionSuccess(result);
            }
            catch (Exception e){
                Map<String, String> error = new HashMap<String, String>();
                error.put("name", Thread.currentThread().getName());
                error.put("error", e.getMessage());
                connectionListener.onConnectionError(error);
            }
        }
    }

    private JSONObject getAccessToken(String username, String password){

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
            params.put("grant_type", "password");
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
                return new JSONObject(response);
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
