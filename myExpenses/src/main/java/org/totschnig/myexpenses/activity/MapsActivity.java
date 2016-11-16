package org.totschnig.myexpenses.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.places.PlacesResponse;
import org.totschnig.myexpenses.places.Result;

import java.util.List;


public class MapsActivity extends ProtectedFragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private RequestQueue mRequestQueue;

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    private final int ACCESS_FINE_LOCATION_PERMISSION = 1;
    boolean mGotMarkers = false;
    LatLng latLng;
    GoogleMap mGoogleMap;
    SupportMapFragment mFragment;
    Marker currLocationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyApplication.getThemeId());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setupToolbar(true);
        mGotMarkers = false;
        getSupportActionBar().setTitle("Find banks");
        mRequestQueue = Volley.newRequestQueue(this);

        mFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        mGoogleMap = gMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION);
        } else {
            mGoogleMap.setMyLocationEnabled(true);
            buildGoogleApiClient();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mGoogleMap.setMyLocationEnabled(true);
                        buildGoogleApiClient();
                        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                        mGoogleApiClient.connect();
                    }
                } else {
                    Toast.makeText(this, "No permission granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Toast.makeText(this, "Finding your location", Toast.LENGTH_SHORT).show();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Your Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            currLocationMarker = mGoogleMap.addMarker(markerOptions);
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Toast.makeText(this, "Connection is suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection is failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        //place marker at current position
        //mGoogleMap.clear();
        if (currLocationMarker != null) {
            currLocationMarker.remove();
        }
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (!mGotMarkers) {
            getBanks(latLng);
            mGotMarkers = true;
        }
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        currLocationMarker = mGoogleMap.addMarker(markerOptions);

        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(14).build();

        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void getBanks(LatLng latLng) {
        int radius = 1000;
        String placeType = "bank";
        String placeAPIKey = "AIzaSyCr9ORdA_Pt-d0tB_Q3HqM1hsHOofo4hvs";
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latLng.latitude + "," + latLng.longitude + "&radius=" + radius + "&types=" + placeType + "&key=" + placeAPIKey;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Gson gson = new Gson();
                List<Result> places = gson.fromJson(response.toString(), PlacesResponse.class).getResults();
                for (Result place : places) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    org.totschnig.myexpenses.places.Location location = place.getGeometry().getLocation();
                    markerOptions.position(new LatLng(location.getLat(), location.getLng()));

                    String title = place.getName();

                    if (place.getOpeningHours() != null) {
                        title += "\n";
                        title += ". Working now: " + (place.getOpeningHours().getOpenNow() ? "yes" : "no");

                    }
                    if (place.getOpeningHours() != null && place.getOpeningHours().getOpenNow()) {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    } else {
                        if (place.getOpeningHours() == null)
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        else
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }

                    markerOptions.title(title);
                    mGoogleMap.addMarker(markerOptions);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Cannot find banks near you", Toast.LENGTH_SHORT).show();
            }
        });
        mRequestQueue.add(request);
    }
}