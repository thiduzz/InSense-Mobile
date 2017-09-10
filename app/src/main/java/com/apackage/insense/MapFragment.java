package com.apackage.insense;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.Language;
import com.akexorcist.googledirection.constant.TransitMode;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.constant.Unit;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.apackage.api.ServerConnection;
import com.apackage.api.ServerConnectionListener;
import com.apackage.db.DataBase;
import com.apackage.utils.Constants;
import com.apackage.utils.PlaceAutoCompleteInterface;
import com.apackage.utils.OnActivityFragmentsInteractionListener;
import com.apackage.utils.PlaceAutocompleteAdapter;
import com.apackage.utils.SavedAddress;
import com.apackage.utils.SavedPlaceListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
public class MapFragment extends Fragment implements PlaceAutoCompleteInterface, ServerConnectionListener, OnMapReadyCallback, LocationListener {

    private OnActivityFragmentsInteractionListener mListener;
    private DataBase db;
    private ServerConnection con;
    private SupportMapFragment map;
    private Button leftButton, rightButton, upButton, downButton, downDblButton, upDblButton, rightDblButton, leftDblButton;
    private GoogleMap mMap;
    private ArrayList<Circle> stepsBounds;
    private LocationManager locationManager;
    private Marker userMarker;
    private double arrowIncrease = 0.000130;
    private LatLngBounds.Builder bounds;
    private GoogleApiClient mGoogleApiClient;
    private RecyclerView mRecyclerView;
    private LinearLayout mMapView;
    private LinearLayoutManager llm;
    private EditText mSearchEdittext;
    private PlaceAutocompleteAdapter mAdapter;
    private ImageView mClear;
    private static final LatLngBounds BOUNDS_BRAZIL = new LatLngBounds(
            new LatLng(-0, 0), new LatLng(0, 0));
    private static final LatLngBounds BOUNDS_GERMANY = new LatLngBounds(
            new LatLng(-0, 0), new LatLng(0, 0));
    List<SavedAddress> mSavedAddressList;

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
        db = new DataBase(getActivity().getApplicationContext());
        stepsBounds = new ArrayList<Circle>();
        LatLng currentLoc = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
        Direction currentDirection = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_DIRECTION"), Direction.class);
        if(currentLoc == null)
        {
            db.saveOrUpdateSetting(db.getActiveUser(),"CURRENT_LOCATION", new Gson().toJson(new LatLng(52.473683, 13.423557)));
            currentLoc = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
        }
        if(currentDirection == null)
        {
            db.saveOrUpdateSetting(db.getActiveUser(),"CURRENT_LOCATION_DESCRIPTION", "Hermannplatz, Berlin");
            getDirections(currentLoc);
        }
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                .enableAutoManage(getActivity(), 0, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(getActivity().getApplicationContext(), "Não foi possível conectar com o servidor de buscas!", Toast.LENGTH_LONG).show();
                    }
                })
                .addApi(Places.GEO_DATA_API)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container,
                false);

        mGoogleApiClient.connect();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        leftButton = (Button) v.findViewById(R.id.left_arrow);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LatLng current = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
                changeLocation(current, "west", arrowIncrease);
            }
        });
        rightButton = (Button) v.findViewById(R.id.right_arrow);
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng current = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
                changeLocation(current, "east", arrowIncrease);
            }
        });
        upButton = (Button) v.findViewById(R.id.up_arrow);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng current = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
                changeLocation(current, "north", arrowIncrease);
            }
        });
        downButton = (Button) v.findViewById(R.id.down_arrow);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng current = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
                changeLocation(current, "south", arrowIncrease);
            }
        });

        leftDblButton = (Button) v.findViewById(R.id.left_arrow_dbl);
        leftDblButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LatLng current = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
                changeLocation(current, "west", arrowIncrease* 3);
            }
        });
        rightDblButton = (Button) v.findViewById(R.id.right_arrow_dbl);
        rightDblButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng current = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
                changeLocation(current, "east", arrowIncrease * 3);
            }
        });
        upDblButton = (Button) v.findViewById(R.id.up_arrow_dbl);
        upDblButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng current = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
                changeLocation(current, "north", arrowIncrease * 3);
            }
        });
        downDblButton = (Button) v.findViewById(R.id.down_arrow_dbl);
        downDblButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng current = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
                changeLocation(current, "south", arrowIncrease * 3);
            }
        });

        v = initViews(v);
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
        mGoogleApiClient.disconnect();
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
        try {
            mMap = googleMap;
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getActivity().getApplicationContext(), R.raw.map_style));
            if (!success) {
                Log.e("INSENSE", "Style parsing failed.");
            }
            mMap.setMyLocationEnabled(false);
            setUserLocation();
            showDirections();
        } catch (Resources.NotFoundException e) {
            Log.e("INSENSE", "Can't find style. Error: ", e);
        }
    }

    private View initViews(View v){
        mRecyclerView = (RecyclerView)v.findViewById(R.id.list_search);
        mRecyclerView.setHasFixedSize(true);
        llm = new LinearLayoutManager(getActivity().getApplicationContext());
        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setVisibility(View.GONE);
        mMapView = (LinearLayout) v.findViewById(R.id.map_view);
        mMapView.setVisibility(View.VISIBLE);
        mSearchEdittext = (EditText) v.findViewById(R.id.search_et);
        mClear = (ImageView) v.findViewById(R.id.clear);
        mClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchEdittext.setText("");
                if(mAdapter!=null){
                    mAdapter.clearList();
                }
                clearDirections();
            }
        });

        mAdapter = new PlaceAutocompleteAdapter(MapFragment.this, R.layout.view_placesearch,
                mGoogleApiClient, BOUNDS_BRAZIL, null);
        mRecyclerView.setAdapter(mAdapter);

        mSearchEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    mClear.setImageResource(R.drawable.ic_multiply);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mMapView.setVisibility(View.GONE);
                    if (mAdapter != null) {
                        mRecyclerView.setAdapter(mAdapter);
                    }
                } else {
                    mClear.setImageResource(R.drawable.ic_search_thin);
                    mRecyclerView.setVisibility(View.GONE);
                    mMapView.setVisibility(View.VISIBLE);
                }
                if (!s.toString().equals("") && mGoogleApiClient.isConnected()) {
                    mAdapter.getFilter().filter(s.toString());
                } else if (!mGoogleApiClient.isConnected()) {
//                    Toast.makeText(getApplicationContext(), Constants.API_NOT_CONNECTED, Toast.LENGTH_SHORT).show();
                    Log.e("", "NOT CONNECTED");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

        });

        return v;
    }

    private void clearDirections() {
        db.deleteSetting(db.getActiveUser(),"CURRENT_DIRECTION");
        mMap.clear();
        setUserLocation();
    }

    @Override
    public void onLocationChanged(Location location) {
        //wont use this since we are using our own external GPS
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        //wont use this since we are using our own external GPS
    }

    @Override
    public void onProviderEnabled(String s) {
        //wont use this since we are using our own external GPS
    }

    @Override
    public void onProviderDisabled(String s) {
        //wont use this since we are using our own external GPS
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //wont use this since we are using our own external GPS (no permission required)
    }

    public void refreshDirections()
    {
        if(mMap != null)
        {
            showDirections();
        }
    }

    public void showDirections()
    {
        DataBase db = new DataBase(getActivity().getApplicationContext());
        Direction direction = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_DIRECTION"), Direction.class);
        if(direction != null)
        {
            generateRoute(direction);
        }
    }

    private void generateRoute(Direction direction) {
        mMap.clear();
        List<Step> stepList = direction.getRouteList().get(0).getLegList().get(0).getStepList();
        ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(getActivity().getApplicationContext(), stepList, 5, getActivity().getResources().getColor(R.color.colorPrimary), 3, Color.BLUE);
        for (PolylineOptions polylineOption : polylineOptionList) {
            mMap.addPolyline(polylineOption);
        }
        int i = 0;
        for (Step step : stepList) {
            CircleOptions opt = new CircleOptions()
                    .center(step.getEndLocation().getCoordination())
                    .radius(30)
                    .strokeWidth(2)
                    .strokeColor(R.color.colorPrimaryDark)
                    .fillColor(R.color.colorRed)
                    .clickable(false);
            Circle c = mMap.addCircle(opt);
            c.setTag(step);
            stepsBounds.add(c);
        }
        setUserLocation();
        //TODO: executar instrucao do step inicial ( index 0)
    }

    private boolean setUserLocation()
    {
        LatLng currentLoc = new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class);
        if(currentLoc != null && mMap != null)
        {
            if(userMarker != null)
            {
                userMarker.remove();
            }
            MarkerOptions markerSetup = new MarkerOptions();
            markerSetup.position(currentLoc);
            BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(R.drawable.ic_marker_insense);
            Bitmap b=bitmapdraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, 125, 125, false);

            markerSetup.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
            userMarker = mMap.addMarker(markerSetup);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc));//Moves the camera to users current longitude and latitude
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc,(float) 14.6));//Animates camera and zooms to preferred state on the user's current location.
            checkForSteps(currentLoc);
            return true;
        }
        return false;
    }

    private void checkForSteps(LatLng currentLoc) {
        if(!stepsBounds.isEmpty())
        {
            int i = 0;
            Log.i("INSENSE", "Iniciando analise de perimetro");
            for (Circle c : stepsBounds) {
                float[] distance = new float[2];
                Location.distanceBetween( currentLoc.latitude, currentLoc.longitude,
                        c.getCenter().latitude, c.getCenter().longitude, distance);
                if( distance[0] > c.getRadius()  ){
                    //Toast.makeText(getBaseContext(), "Outside", Toast.LENGTH_LONG).show();
                } else {
                    //always get the next instruction, remember to add to reproduce first instruction when starting the navigation
                    //Exception: last circle
                    if(stepsBounds.size() > i + 1)
                    {
                        Step step = (Step) (stepsBounds.get(i+1).getTag());
                        if(step != null && step.getHtmlInstruction() != null )
                        {
                            Toast.makeText(getActivity().getApplicationContext(), step.getHtmlInstruction(), Toast.LENGTH_LONG).show();
                        }
                        Log.i("INSENSE", "Entrou na area!");
                    }
                }
                i++;
            }
        }
    }

    private LatLng changeLocation(LatLng current, String type, double val)
    {
        double temp = 0;
        switch (type)
        {
            case "west":
                temp = current.longitude - val;
                current = new LatLng(current.latitude, temp);
                break;
            case "east":
                temp = current.longitude + val;
                current = new LatLng(current.latitude, temp);
                break;
            case "north":
                temp = current.latitude + val;
                current = new LatLng(temp, current.longitude);
                break;
            case "south":
                temp = current.latitude - val;
                current = new LatLng(temp, current.longitude);
                break;
        }
        db.saveOrUpdateSetting(db.getActiveUser(), "CURRENT_LOCATION", new Gson().toJson(current));

        setUserLocation();
        return current;
    }

    public void getDirections(LatLng currentLoc) {
        try {
            Geocoder geo = new Geocoder(getActivity().getApplicationContext(), Locale.US);
            List<Address> results = null;
            String destination = db.getSetting(db.getActiveUser(),"CURRENT_LOCATION_DESCRIPTION");
            results = geo.getFromLocationName((destination == null ? "" : destination), 3);
            GoogleDirection.withServerKey("AIzaSyCmWiuCgBSJKWLxRWoNeaJiP4VKRnbexQ8")
                    .from(currentLoc)
                    .to(new LatLng(results.get(0).getLatitude(), results.get(0).getLongitude()))
                    .unit(Unit.METRIC)
                    .transitMode(TransitMode.TRAIN)
                    .transitMode(TransitMode.BUS)
                    .transitMode(TransitMode.SUBWAY)
                    .transportMode(TransportMode.WALKING)
                    .language(Language.PORTUGUESE_BRAZIL)
                    .execute(new DirectionCallback() {
                        @Override
                        public void onDirectionSuccess(Direction direction, String rawBody) {
                            if(direction.isOK()) {
                                db.saveOrUpdateSetting(db.getActiveUser(),"CURRENT_DIRECTION",new Gson().toJson(direction));
                                MapFragment.this.refreshDirections();
                            }
                        }

                        @Override
                        public void onDirectionFailure(Throwable t) {
                            Log.i("INSENSE","FAILED TO GET DIRECTIONS");
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPlaceClick(ArrayList<PlaceAutocompleteAdapter.PlaceAutocomplete> mResultList, int position) {
        if(mResultList!=null){
            try {

                db.saveOrUpdateSetting(db.getActiveUser(),"CURRENT_LOCATION_DESCRIPTION", (String) mResultList.get(position).description);
                mSearchEdittext.setText((String) mResultList.get(position).description);
                toggleViewType();
                mAdapter.clearList();
                getDirections(new Gson().fromJson(db.getSetting(db.getActiveUser(),"CURRENT_LOCATION"), LatLng.class));
                /**
                 final String placeId = String.valueOf(mResultList.get(position).placeId);
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if(places.getCount()==1){
                            Intent data = new Intent();
                            data.putExtra("lat",String.valueOf(places.get(0).getLatLng().latitude));
                            data.putExtra("lng", String.valueOf(places.get(0).getLatLng().longitude));
                        }else {
                            Toast.makeText(getActivity().getApplicationContext(),"Woops! Erro ao interpretar o local selecionado",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
               **/
            }
            catch (Exception e){
                Toast.makeText(getActivity().getApplicationContext(),"Woops! Algo deu errado:"+ e.getMessage() ,Toast.LENGTH_SHORT).show();

            }

        }
    }

    public void toggleViewType()
    {
        if(mMapView != null && mRecyclerView != null)
        {
            if(mMapView.getVisibility() == View.GONE && mRecyclerView.getVisibility() == View.VISIBLE)
            {
                mMapView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            }else if(mMapView.getVisibility() == View.VISIBLE && mRecyclerView.getVisibility() == View.GONE)
            {
                mMapView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
}
