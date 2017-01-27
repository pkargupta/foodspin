package com.agnik.priyankakargupta.foodspin;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.*;
import android.location.Location;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.gson.Gson;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.List;

public class RestaurantView extends AppCompatActivity{

    public static String type;
    public static int num;
    public double latitude;
    public double longitude;
    private LocationManager service;
    TextView restaurantName;
    TextView restaurantAddress;
    TextView phoneNumber;
    ImageView rating;
    TextView website;
    GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_view);

        service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // check if enabled and if not send user to the GPS settings
        if (!enabled) {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Your location is not enabled. Would you like to enable it so that you can use this app?");
            alertDialogBuilder.setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    Intent myIntent = new Intent(RestaurantView.this, FoodSpinner.class);
                    RestaurantView.this.startActivity(myIntent);
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        // Define the criteria how to select the location provider -> use
        // default
        Criteria criteria = new Criteria();
        String provider = service.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = service.getLastKnownLocation(provider);
        // Initialize the location fields
        if (location != null) {
            onLocationChanged(location);
            new RetrieveFeedTask().execute();
        }
        else
            Toast.makeText(RestaurantView.this, "Location not found! Make sure your GPS is on!", Toast.LENGTH_SHORT).show();

        //initialize the view elements here --> variablename = (Object) findViewById(R.id.name);
        restaurantName = (TextView)findViewById(R.id.restaurantName);
        restaurantAddress = (TextView)findViewById(R.id.address);
        phoneNumber = (TextView)findViewById(R.id.phoneNumber);
        //rating = (ImageView) findViewById(R.id.rating);
       // website = (TextView) findViewById(R.id.website);
        type = FoodSpinner.type;
        num = FoodSpinner.spinnernum;

        //map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map = ((MapFragment) getFragmentManager().
                findFragmentById(R.id.map)).getMap();

    }

    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    //this is the background thread
    class RetrieveFeedTask extends AsyncTask<Void, Void, String> {

        private Exception exception;

        @Override
        protected void onPreExecute() {

        }

        protected String doInBackground(Void... urls) {
            // Do some validation here

            try {
                // these are our keys
                String CONSUMER_KEY = "K38zUUd4AVodtZOBWPLiAQ";
                String CONSUMER_SECRET = "m3sFzXCeEVgCZdTwGcFL_H05CKQ";
                String TOKEN = "XmrUqGs-HpS5FhrP4dBZlNhcBbtgx_q9";
                String TOKEN_SECRET = "GtfOksfqAImqyvu0Pl0LX8Tw0Rk";

                // we need to store the current lat and long here
                String lat = String.valueOf(latitude);
                String lng = String.valueOf(longitude);
                String category = type;

                // this executes a signed call to the yelp service
                OAuthService service = new ServiceBuilder().provider(YelpV2API.class).apiKey(CONSUMER_KEY).apiSecret(CONSUMER_SECRET).build();
                Token accessToken = new Token(TOKEN, TOKEN_SECRET);
                OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
                request.addQuerystringParameter("ll", lat + "," + lng);
                request.addQuerystringParameter("term", category);
                service.signRequest(accessToken, request);
                org.scribe.model.Response response = request.send();
                String rawData = response.getBody();
                return rawData;
            }
            catch(Exception e) {
                Toast.makeText(RestaurantView.this, "ERROR SENDING REQUEST", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if(response == null) {
                response = "THERE WAS AN ERROR";
                Toast.makeText(RestaurantView.this, response, Toast.LENGTH_SHORT).show();
            }
            else{
                YelpSearchResult places = new Gson().fromJson(response, YelpSearchResult.class);
                Toast.makeText(
                        RestaurantView.this, "Yelp returned " + places.getBusinesses().size() + " businesses in this request.",
                        Toast.LENGTH_SHORT).show();
                List<Business> r = places.getBusinesses();
                Business choice = r.get(num);

                String address = "";
                for(int i = 0; i < choice.getLocation().getAddress().size(); i++)
                    address += choice.getLocation().getAddress().get(i);

                address += ", " + choice.getLocation().getCity() + ", MD, 21043" ;
                Toast.makeText(RestaurantView.this, "Restaurant: " + choice.getName() + " Address: "
                        + address, Toast.LENGTH_SHORT).show();

                //update the UI here based on the view elements initialized in the onCreate method. its just a simple setter method.
                restaurantName.setText(choice.getName().toUpperCase());
                restaurantAddress.setText(address);
                phoneNumber.setText(choice.getPhone());
                //rating.setImageURI(Uri.parse(choice.getRatingImgUrlSmall()));
                //website.setText(choice.getUrl());

                // Get Lat + Long of restaurant location
                double lat = choice.getLocation().getCoordinate().getLatitude();
                double lng = choice.getLocation().getCoordinate().getLongitude();

                // Add Map of restaurant
                Marker m = map.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title(choice.getName())
                );

                // Move camera to the marker
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));
            }
        }
    }
}