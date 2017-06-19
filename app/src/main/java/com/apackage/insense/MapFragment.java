package com.apackage.insense;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.apackage.api.ServerConnection;
import com.apackage.api.ServerConnectionListener;
import com.apackage.db.DataBase;
import com.apackage.utils.Constants;
import com.apackage.utils.OnActivityFragmentsInteractionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Map;

import static android.content.Context.LOCATION_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnActivityFragmentsInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment implements ServerConnectionListener, OnMapReadyCallback, LocationListener {

    private OnActivityFragmentsInteractionListener mListener;
    private DataBase db;
    private ServerConnection con;
    private SupportMapFragment map;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private MarkerOptions userMarker;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, Constants.PERMISSION_RC_MAPS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container,
                false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(this.getTag(), uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnActivityFragmentsInteractionListener) {
            mListener = (OnActivityFragmentsInteractionListener) context;
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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);

        if(ContextCompat.checkSelfPermission
                (getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission
                (getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, this);


            Location location =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


            if(location != null){
                onLocationChanged(location);
            }
        }
        else{

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1000
            );
        }

    }

    @Override
    public void onLocationChanged(Location location) {

        LatLng userLocation = new LatLng(location.getLatitude()
                ,location.getLongitude());

        if(userMarker == null){
            userMarker = new MarkerOptions();
            userMarker.title("Eu");
            userMarker.position(userLocation);

            mMap.addMarker(userMarker);
        }
        else {
            userMarker.position(userLocation);
        }

        mMap.moveCamera(
                CameraUpdateFactory
                        .newLatLngZoom(userLocation,15));

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1000 &&
                grantResults.length > 0 &&
                grantResults[0]
                        == PackageManager.PERMISSION_GRANTED){

            if(ContextCompat.checkSelfPermission
                    (getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        }
    }
}
