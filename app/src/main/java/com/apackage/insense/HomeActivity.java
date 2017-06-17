package com.apackage.insense;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.apackage.api.Connection;
import com.apackage.api.ConnectionListener;
import com.apackage.db.DataBase;

import java.util.Map;

public class HomeActivity extends AppCompatActivity implements ConnectionListener {
    private Connection con;
    private int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final DataBase db = new DataBase(this);
        userID = getIntent().getExtras().getInt("userID",0);
        if(userID > 0)
        {
            con = new Connection(this, getApplicationContext());
            setContentView(R.layout.activity_home);
        }else{
            Toast.makeText(getApplicationContext(), "Problema encontrado ao buscar usuario corrente", Toast.LENGTH_LONG).show();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().remove("isLogged").remove("userID").commit();
            finishActivity(403);
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
}
