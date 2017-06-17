package com.apackage.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.apackage.db.DataBase;
import com.apackage.insense.R;
import com.apackage.model.User;
import com.apackage.utils.Constants;
import com.google.gson.Gson;

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

public class Connection extends AsyncTask<String,Void,Object> {

    private static final int CLIENT_ID = 1;
    private static final String CLIENT_SECRET = "bIc3jb2EmPvPRYOlCTUF8uMkYXBAHXh1kD1YIJx2";

    private ConnectionListener connectionListener;
    private Context context;
    private String type;
    private final DataBase db;

    public Connection(ConnectionListener connectionListener, Context context){
        this.connectionListener = connectionListener;
        this.context = context;
        this.db = new DataBase(context);
    }

    @Override
    protected Object doInBackground(String... query) {
        this.type = query[0];
        switch(type)
        {
            case Constants.REQUEST_LOGIN:
                try {
                    JSONObject tokenResponse = getAccessToken(query[1],query[2]);
                    return attemptAuthentication(tokenResponse.getString("access_token"), tokenResponse.getString("refresh_token"));
                } catch (JSONException e) {
                    return e;
                } catch (Exception e) {
                    return e;
                }
            case Constants.REQUEST_SETTINGS:
                //return getSettings(getToken(),query[0]);
                return null;
            case Constants.REQUEST_DEVICES:
                return null;
            case Constants.REQUEST_REFRESH_TOKEN:
                return null;
            case Constants.REQUEST_VALIDATE_TOKEN:
                try {
                    JSONObject refreshTokenResponse = refreshAccessToken(query[1]);
                    return attemptAuthentication(refreshTokenResponse.getString("access_token"), refreshTokenResponse.getString("refresh_token"));
                } catch (JSONException e) {
                    return e;
                } catch (Exception e) {
                    return e;
                }
        }
        return null;
    }

    private User attemptAuthentication(String access_token, String refresh_token) throws Exception {
        JSONObject loginResponse = user(access_token);
        if(loginResponse != null)
        {
            User user = new User(loginResponse.getInt("id"), loginResponse.getString("email"), access_token, loginResponse.getString("name"), refresh_token);
            int resultAuth = db.saveOrUpdate(user, true);
            if(resultAuth == 1)
            {
                //TODO: salvar/atualizar os dispositivos cadastrados do usuario
                return user;
            }
            throw new Exception("Falhou ao salvar as informações do usuário no dispositivo");
        }
        throw new Exception("Falha na autenticação do usuário!");
    }

    private JSONObject user(String access_token) {
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
    protected void onPostExecute(Object connection_result) {
        Map<String, Object> result = new HashMap<String, Object>();
        if(connection_result == null){
            connectionListener.onConnectionError();
        }else if(connection_result instanceof Exception){
            Map<String, String> error = new HashMap<String, String>();
            error.put("name", this.type);
            error.put("error", ((Exception)connection_result).getMessage());
            connectionListener.onConnectionError(error);
        }else{
            try {
                switch(this.type)
                {
                    case Constants.REQUEST_LOGIN:
                        if(connection_result instanceof User)
                        {
                            Gson gson = new Gson();
                            result.put("name", this.type);
                            result.put("result",(User)connection_result);
                            connectionListener.onConnectionSuccess(result);
                        }
                        break;
                    case Constants.REQUEST_SETTINGS:
                        break;
                    case Constants.REQUEST_DEVICES:
                        break;
                    case Constants.REQUEST_REFRESH_TOKEN:
                        break;
                    case Constants.REQUEST_VALIDATE_TOKEN:
                        if(connection_result instanceof User)
                        {
                            Gson gson = new Gson();
                            result.put("name", this.type);
                            result.put("result",(User)connection_result);
                            connectionListener.onConnectionSuccess(result);
                        }
                        break;
                    default:
                        Map<String, String> error = new HashMap<String, String>();
                        error.put("name", this.type);
                        error.put("error", "Resultado inesperado da request");
                        connectionListener.onConnectionError(error);
                        break;
                }
            }
            catch (Exception e){
                Map<String, String> error = new HashMap<String, String>();
                error.put("name", this.type);
                error.put("error", e.getMessage());
                connectionListener.onConnectionError(error);
            }
        }
    }

    private JSONObject getAccessToken(String username, String password) throws Exception {

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
                throw new Exception("Falha na autenticação!");
            }
        }
        catch (Exception e){
            throw e;
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

    private JSONObject refreshAccessToken(String refresh_token) throws Exception {
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
            params.put("grant_type", "refresh_token");
            params.put("client_id", CLIENT_ID);
            params.put("client_secret", CLIENT_SECRET);
            params.put("refresh_token", refresh_token);
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
                throw new Exception("Falha na atualizacao do token!");
            }
        }
        catch (Exception e){
            throw e;
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
