package com.apackage.insense;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.apackage.model.Network;

import com.apackage.api.ServerConnection;
import com.apackage.api.ServerConnectionListener;
import com.apackage.db.DataBase;
import com.apackage.model.User;
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
    public Handler commHandler;

    private NetworkListAdapter adapterNetworks;
    private ArrayList<Network> networks;

    public static final int WPA = 1;
    public static final int WEP = 2;

    private DataBase db;
    private ServerConnection con;

    private OnActivityFragmentsInteractionListener mListener;

    StringBuilder sb = new StringBuilder();

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



        /**
        String ip = Formatter.formatIpAddress(WifiManager.getConnectionInfo().getIpAddress());
        String[] split = ip.split(".");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            if (i == split.length - 1) {
                sb.append("1");
            }else{
                sb.append(split[i] + ".");
            }
        }
        Network n = new Network();
        n.setAddress("192.168.4.1");
        n.setPort(Constants.CONNECTION_PORT);
        ((HomeActivity)getActivity()).myService.startWirelessConnection(n);
         **/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Do something with granted permission

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_devices, container, false);

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        User user = db.getActiveUser();
        ImageView imgConex = (ImageView) v.findViewById(R.id.connectionImg);
        TextView textConex = (TextView) v.findViewById(R.id.connectionText);
        if(user.getId() > 0)
        {
            if(db.isActiveUserConnected(user.getId()))
            {
                //connected
                imgConex.setBackground(getResources().getDrawable(R.drawable.connected_icon));
                textConex.setText("Conectado");

            }else{
                //not connected
                imgConex.setBackground(getResources().getDrawable(R.drawable.disconnected_icon));
                textConex.setText("Desconectado");
            }
        }else{
            //not connected
            imgConex.setBackground(getResources().getDrawable(R.drawable.disconnected_icon));
            textConex.setText("Desconectado");
        }

        /**
        Button btn_change_credentials = (Button)v.findViewById(R.id.btnChangeCredentials);
        btn_change_credentials.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                DevicesFragment.this.constructDialog();
            }
        });
         **/

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

    public void constructDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Altere as Credenciais");
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_credentials, (ViewGroup) getView(), false);
        // Set up the input
        final EditText name = (EditText) viewInflated.findViewById(R.id.newNetworkName);
        final EditText password = (EditText) viewInflated.findViewById(R.id.newNetworkPassword);
        builder.setView(viewInflated);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity().getApplicationContext(),"Enviando novas credenciais: "+name.getText().toString(),Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

}
