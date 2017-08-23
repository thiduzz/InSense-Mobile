package com.apackage.insense;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.apackage.model.Network;

import com.apackage.api.ServerConnection;
import com.apackage.api.ServerConnectionListener;
import com.apackage.db.DataBase;
import com.apackage.utils.Constants;
import com.apackage.utils.NetworkListAdapter;
import com.apackage.utils.OnActivityFragmentsInteractionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnActivityFragmentsInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DevicesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DevicesFragment extends Fragment implements ServerConnectionListener {

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 10010;
    private Button btnBluetooth;
    private EditText txtSend;
    private Button btnSend;
    ListView listView;
    ArrayAdapter<String> adapter;
    static WifiManager WifiManager;
    List<ScanResult> Wlan_list;

    private NetworkListAdapter adapterNetworks;
    private ArrayList<Network> networks;

    public static final int WPA = 1;
    public static final int WEP = 2;

    private DataBase db;
    private ServerConnection con;

    private OnActivityFragmentsInteractionListener mListener;

    StringBuilder sb = new StringBuilder();

    private final Handler handler = new Handler();

    public DevicesFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static DevicesFragment newInstance(String param1, String param2) {
        DevicesFragment fragment = new DevicesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WifiManager = (WifiManager)getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);


            IntentFilter filter = new IntentFilter(android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            getActivity().registerReceiver(Wifi, filter);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Do something with granted permission
            Discovery();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_devices, container, false);
        listView = (ListView) v.findViewById(R.id.networkListView);
        networks = new ArrayList<Network>();
        adapterNetworks = new NetworkListAdapter(networks, getActivity().getApplicationContext());
        listView.setAdapter(adapterNetworks);
        listView.setOnItemClickListener(Net);
        //Scan
        Button btn_scan = (Button)v.findViewById(R.id.btnBluetooth);
        btn_scan.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
                }else{
                    Discovery();
                }
            }
        });
        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(this.getTag(), uri);
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnActivityFragmentsInteractionListener) {
            mListener = (OnActivityFragmentsInteractionListener) context;
            db = new DataBase(getActivity());
            con = new ServerConnection(this, getContext());
        } else {
            throw new RuntimeException(context.toString()
                    + " deve implementar OnActivityFragmentsInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

    protected void Discovery() {
        WifiManager.startScan();
    }

    private AdapterView.OnItemClickListener Net = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long arg3) {

            Network n = ((Network)av.getAdapter().getItem(position));
            int Key = 0;
            if(n != null)
            {
                String PW = Constants.DEFAULT_PASSWORD_INSENSE_GLASS;
                conectarDispositivo(n.getMac(), Key, PW, n.getSsid());
            }
        }
    };

    private final BroadcastReceiver Wifi = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            networks.clear();
            Wlan_list = WifiManager.getScanResults();
            if (WifiManager.isWifiEnabled() == false){
                Toast.makeText(getActivity().getApplicationContext(),"The wifi is disabled!", Toast.LENGTH_LONG).show();
            } else {
                for(int i = 0; i < Wlan_list.size(); i++){
                    if(Wlan_list.get(i).SSID.toLowerCase().contains("insense glass"))
                    {
                        networks.add(new Network(Wlan_list.get(i).SSID, Wlan_list.get(i).BSSID));
                        adapterNetworks.notifyDataSetChanged();
                    }
                }
                if (networks.toArray().length == 0){
                    Toast.makeText(getActivity().getApplicationContext(),"No network available!", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    //Verbindung mit gewählten AP aufbauen
    public static boolean conectarDispositivo(String sBSSID, int iSecurityType, String sSecurityKey, String sSSID){

        WifiConfiguration tmpConfig;

        List <WifiConfiguration> listConfig = WifiManager.getConfiguredNetworks();

        for (int i = 0; i<listConfig.size(); i++){
            tmpConfig = listConfig.get(i);
            if (tmpConfig.BSSID != null && tmpConfig.BSSID.equalsIgnoreCase(sBSSID)){
                return WifiManager.enableNetwork(tmpConfig.networkId, true);
            }
        }

        tmpConfig = new WifiConfiguration();
        tmpConfig.BSSID = sBSSID;
        tmpConfig.SSID = sSSID;
        tmpConfig.priority = 1;

        switch(iSecurityType){
            //WPA
            case WPA:
                tmpConfig.preSharedKey = sSecurityKey;
                break;
            //WEP
            case WEP:
                tmpConfig.wepKeys[0] = sSecurityKey;
                tmpConfig.wepTxKeyIndex = 0;
                break;
            default:
                break;
        }
        tmpConfig.status = WifiConfiguration.Status.ENABLED;
        tmpConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        int netId = WifiManager.addNetwork(tmpConfig);

        boolean result =  WifiManager.enableNetwork(netId, true);
        WifiManager.saveConfiguration();
        return result;
    }

}
