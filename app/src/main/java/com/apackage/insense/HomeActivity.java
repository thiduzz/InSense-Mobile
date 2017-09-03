package com.apackage.insense;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.telecom.ConnectionService;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.apackage.api.HotspotConnection;
import com.apackage.api.ServerConnection;
import com.apackage.api.ServerConnectionListener;
import com.apackage.db.DataBase;
import com.apackage.model.User;
import com.apackage.utils.Constants;
import com.apackage.utils.OnActivityFragmentsInteractionListener;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnActivityFragmentsInteractionListener<Object>, CommunicationService.Callbacks{
    private int userID;
    Intent serviceIntent;
    CommunicationService myService;
    DevicesFragment fragDev;
    public Handler handlerReceiverClient = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            final DataBase db = new DataBase(getApplicationContext());
            //NAO COLOCAR BREAKPOINT AQUI SE ESTIVER RODANDO COM O INSTANT RUN!!!!!!!!!!!!
            switch (message.what)
            {
                case Constants.CONNECTION_ERROR:
                    Toast.makeText(getApplicationContext(),(String)message.obj, Toast.LENGTH_LONG).show();
                    Log.i("INSENSE", "ERROR CONNECTION! "+ (String)message.obj);
                    db.deleteSetting(db.getActiveUser(),"CONNECTED_IP");
                    db.changeDeviceConnectionStatus(db.getActiveUser(),false);
                    disconnectDevice(false);
                    break;
                case Constants.CONNECTION_GENERAL_ERROR:
                    Toast.makeText(getApplicationContext(),(String)message.obj, Toast.LENGTH_LONG).show();
                    Log.i("INSENSE", "ERROR CONNECTION GENERAL! "+ (String)message.obj);
                    break;
                case Constants.HOTSPOT_DEVICE_FOUND:
                    db.saveOrUpdateSetting(db.getActiveUser(),"CONNECTED_IP", (String)message.obj);
                    Toast.makeText(getApplicationContext(),"IP:"+(String)message.obj, Toast.LENGTH_LONG).show();
                    myService.startWirelessConnection((String)message.obj,Constants.CONNECTION_PORT);
                    break;
                case Constants.HOTSPOT_DEVICE_NOTFOUND:
                    if(db.deleteSetting(db.getActiveUser(),"CONNECTED_IP") == true)
                    {
                        Log.i("INSENSE","HOTSPOT WAS CONNECTED BUT DID NOT FOUND THE DEVICE ANYMORE");

                    }else{
                        Log.i("INSENSE","HOTSPOT COULD NOT FIND ANY DEVICE");
                    }
                    db.deleteSetting(db.getActiveUser(),"CONNECTED_IP");
                    db.changeDeviceConnectionStatus(db.getActiveUser(),false);
                    disconnectDevice(true);
                    break;
                case Constants.HOTSPOT_GENERAL_ERROR:
                    Log.i("INSENSE","HOTSPOT ERROR:"+ (String) message.obj);
                    db.deleteSetting(db.getActiveUser(),"CONNECTED_IP");
                    db.changeDeviceConnectionStatus(db.getActiveUser(),false);
                    disconnectDevice(true);
                    break;
                case Constants.HOTSPOT_DISABLED:
                    Log.i("INSENSE","HOTSPOT IS NOT ENABLED ON THE PHONE");
                    db.deleteSetting(db.getActiveUser(),"CONNECTED_IP");
                    db.changeDeviceConnectionStatus(db.getActiveUser(),false);
                    disconnectDevice(true);
                    break;
                case Constants.GLASS_STARTED:
                    db.changeDeviceConnectionStatus(db.getActiveUser(),true);
                    fragDev = ((DevicesFragment)getSupportFragmentManager().
                            findFragmentByTag(Constants.FRAGMENT_DEVICES));
                    if(fragDev != null && fragDev.isFragmentUIActive())
                    {
                        fragDev.changeConnectionStatus(null);
                    }
                    Toast.makeText(getApplicationContext(),"INSENSE IS ENABLED!", Toast.LENGTH_LONG).show();
                    break;
                case Constants.GLASS_NOT_CONNECTED:
                    db.deleteSetting(db.getActiveUser(),"CONNECTED_IP");
                    db.changeDeviceConnectionStatus(db.getActiveUser(),false);
                    disconnectDevice(true);
                    break;
                case Constants.GLASS_AUDIO_RECORDING:
                    Log.i("INSENSE","GRAVANDO O AUDIO!");
                    break;
                case Constants.GLASS_AUDIO_SAVED:
                    Log.i("INSENSE","AUDIO GRAVADO, INICIANDO ENVIO PARA O GOOGLE SPEECH API!");
                    break;
                case Constants.GLASS_AUDIO_RECOGNIZED:
                    Toast.makeText(getApplicationContext(),(String)message.obj, Toast.LENGTH_LONG).show();
                    Log.i("INSENSE", "AUDIO RECOGNIZED IS: "+ (String)message.obj);
                    break;
            }
            return false;
        }
        public void disconnectDevice(boolean showToast)
        {
            fragDev = ((DevicesFragment)getSupportFragmentManager().
                    findFragmentByTag(Constants.FRAGMENT_DEVICES));
            if(fragDev != null && fragDev.isFragmentUIActive())
            {
                fragDev.changeConnectionStatus(null);
            }
            if(showToast){
                Toast.makeText(getApplicationContext(),"INSENSE IS DISCONNECTED!", Toast.LENGTH_LONG).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userID = getIntent().getExtras().getInt("userID",0);
        serviceIntent = new Intent(this, CommunicationService.class);
        startService(serviceIntent); //Starting the service
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!
        if(userID > 0)
        {
            setContentView(R.layout.activity_home);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            /**
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
            **/
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            View headerView = navigationView.inflateHeaderView(R.layout.nav_header_home);
            navigationView.setNavigationItemSelectedListener(this);

            DataBase db = new DataBase(this);
            User u = db.getActiveUser();
            TextView userName = (TextView) headerView.findViewById(R.id.userName);
            userName.setText(u.getName());
            TextView userEmail = (TextView) headerView.findViewById(R.id.userEmail);
            userEmail.setText(u.getLogin());


            Fragment mFragment = getFragmentManager().findFragmentById(R.id.main_layout);
            if(mFragment == null)
            {
                HomeFragment homeFragment = new HomeFragment();
                FragmentManager manager = getSupportFragmentManager();
                manager.beginTransaction().replace(R.id.main_layout, homeFragment, homeFragment.getTag()).commit();
                navigationView.setCheckedItem(R.id.nav_home);
            }
        }else{
            Toast.makeText(getApplicationContext(), "Problema encontrado ao buscar usuario corrente", Toast.LENGTH_LONG).show();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit().remove("isLogged").remove("userID").commit();
            finishActivity(403);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            HomeFragment homeFragment = new HomeFragment();
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(R.id.main_layout, homeFragment, Constants.FRAGMENT_HOME).commit();
        }else if (id == R.id.nav_apps) {
            AppsFragment appsFragment = new AppsFragment();
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(R.id.main_layout, appsFragment, Constants.FRAGMENT_APPS).commit();

        } else if (id == R.id.nav_devices) {
                DevicesFragment devicesFragment = new DevicesFragment();
                FragmentManager manager = getSupportFragmentManager();
                manager.beginTransaction().replace(R.id.main_layout, devicesFragment, Constants.FRAGMENT_DEVICES).commit();
        } else if (id == R.id.nav_help) {

        } else if (id == R.id.nav_map) {
            MapFragment mapFragment = new MapFragment();
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(R.id.main_layout, mapFragment, Constants.FRAGMENT_MAP).commit();

        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_view) {

        } else if (id == R.id.nav_logout)
        {

            DataBase db = new DataBase(this);
            if(db.logoutCurrentUser())
            {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                preferences.edit().remove("isLogged").remove("userID").commit();
                Intent intent = new Intent(HomeActivity.this,
                    LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(HomeActivity.this, CommunicationService.class));
    }

    @Override
    public void onFragmentInteraction(String tag, Object data) {
        Toast.makeText(getApplicationContext(), tag, Toast.LENGTH_LONG).show();
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i("INSENSE","Serviço de Background iniciado!");
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            CommunicationService.LocalBinder binder = (CommunicationService.LocalBinder) service;
            myService = binder.getServiceInstance(); //Get instance of your service!
            Log.i("INSENSE","Registrando a atividade cliente no servico Background!");
            myService.registerClient(HomeActivity.this); //Activity register in the service as client for callabcks!
            myService.startHotspotCheck(handlerReceiverClient);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i("INSENSE","Serviço de Background finalizado!");
        }
    };

    @Override
    public void startCommunication(long data) {

    }
}
