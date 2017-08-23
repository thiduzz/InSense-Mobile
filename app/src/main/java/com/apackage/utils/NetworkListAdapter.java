package com.apackage.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by thidu on 8/22/2017.
 */
import com.apackage.insense.R;
import com.apackage.model.Network;

import java.util.ArrayList;

public class NetworkListAdapter extends BaseAdapter {

    private ArrayList<Network> networks;
    private Context context;

    public NetworkListAdapter(ArrayList<Network> networks, Context context){
        this.networks = networks;
        this.context = context;
    }

    @Override
    public int getCount() {
        return networks.size();
    }

    @Override
    public Object getItem(int i) {
        return networks.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view,
                        ViewGroup viewGroup) {

        Network network = networks.get(position);

        if(view == null){

            LayoutInflater inf = LayoutInflater.from(context);
            view = inf.inflate(R.layout.network_list_item, null);
        }



        TextView textView = (TextView) view.findViewById(R.id.ssid);
        textView.setText(network.getSsid());
        TextView macView = (TextView) view.findViewById(R.id.mac);
        macView.setText(network.getMac());

        ImageView connectionImageView = (ImageView) view.findViewById(R.id.connectionImageView);
        if (network.isConnected()){
            connectionImageView.setVisibility(View.VISIBLE);
        }else {
            connectionImageView.setVisibility(View.INVISIBLE);
        }


        return view;
    }
}

