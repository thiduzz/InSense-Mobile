package com.apackage.insense;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.apackage.api.Connection;
import com.apackage.api.ConnectionListener;

import java.util.Map;

public class HomeActivity extends AppCompatActivity implements ConnectionListener {
    private Connection con;
    private int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userID = getIntent().getExtras().getInt("userID",0);
        if(userID > 0)
        {
            con = new Connection(this, getApplicationContext());
            //TODO: check if the current active user has a valid token
            // - if not, try using the refresh token,
            // - if the refresh token doesnt work - close the activity and set the active user to false

            setContentView(R.layout.activity_home);
        }else{
            Toast.makeText(getApplicationContext(), "Problema encontrado ao buscar usuario corrente", Toast.LENGTH_LONG).show();
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
