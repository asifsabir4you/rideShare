package com.example.asifsabir.rideshareapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public int flag = 0;
    private GoogleMap mMap;
    private static final int LOCATION_REQUEST = 500;
    ArrayList<LatLng> listPoints;
    Button btnReqRides;
    TextView tvFare, tvDistance,tvNotDistance;
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    String riderName, riderNid, riderRating, riderPhone;
    int fare;
    double distance;
    public static DecimalFormat df1 = new DecimalFormat(".##");
    public static DecimalFormat df2 = new DecimalFormat(".##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        btnReqRides = (Button) findViewById(R.id.button_req_riders);
        tvDistance = (TextView) findViewById(R.id.tv_distance);
        tvFare = (TextView) findViewById(R.id.tv_fare);
        tvNotDistance = (TextView)findViewById(R.id.tv_not_distance);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        listPoints = new ArrayList<>();


        riderPhone = getIntent().getExtras().getString("riderPhone", null);

        //rendering  driver data

        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Rider").child(riderPhone);
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                RiderReg riderReg = snapshot.getValue(RiderReg.class);
                riderName = riderReg.fullName;
                riderNid = riderReg.nid;
                riderRating = riderReg.rating;

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MapsActivity.this, "Error happened in fetching rider data!", Toast.LENGTH_SHORT).show();
            }

        });


        btnReqRides.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                DatabaseReference reqRef = database.getReference("rideRequest").push();
                String reqKey = reqRef.getKey();

                RideRequest rideRequest = new RideRequest(riderName, riderPhone, riderNid, riderRating,
                        "", "", "", "",
                        String.valueOf(distance), String.valueOf(fare), "0", reqKey);
                reqRef.setValue(rideRequest);

                Intent i = new Intent(MapsActivity.this, ShowRiderRequest.class);
                i.putExtra("reqKey", reqKey);
                startActivity(i);
                finish();

            }
        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //Reset marker when already 2
                if (listPoints.size() == 2) {
                    listPoints.clear();
                    mMap.clear();
                }
                //Save first point select
                listPoints.add(latLng);
                //Create marker
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);

                if (listPoints.size() == 1) {
                    //Add first marker to the map
//                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_start))
                            .title("starting position");
                    Toast.makeText(MapsActivity.this, "Select Ending point", Toast.LENGTH_SHORT).show();
                    flag = 0;

                } else {
                    //Add second marker to the map
//                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_stop))
                            .title("finishing position");
                    Toast.makeText(MapsActivity.this, "Destination Set!", Toast.LENGTH_SHORT).show();
                    btnReqRides.setVisibility(View.VISIBLE);

                    //show distance
                    distance = showDistance(listPoints.get(0), listPoints.get(1));
                    tvDistance.setText(String.format( "%.2f", distance ) + " KM");
                    //show bill
                    fare = showBikeFare((int) distance);
                    int carFare = showCarFare((int) distance);

                    tvFare.setText("Bike:" + fare + " tk\n" + "Car:" + carFare + " tk");
                }
                mMap.addMarker(markerOptions);

                if (listPoints.size() == 2) {
                    //Create the URL to get request from first marker to second marker
                    String url = getRequestUrl(listPoints.get(0), listPoints.get(1));
                    TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                    taskRequestDirections.execute(url);

                    //making second marker

                    double midLat = (listPoints.get(0).latitude + listPoints.get(1).latitude) / 1.999701646;
                    double midLon = (listPoints.get(0).longitude + listPoints.get(1).longitude) / 1.999921833;


                    String url1 = getRequestUrl(listPoints.get(0), new LatLng(midLat, midLon));
                    TaskRequestDirections taskRequestDirections1 = new TaskRequestDirections();
                    taskRequestDirections1.execute(url1);
                    //making third marker//nope 2nd is half

                    String url2 = getRequestUrl(new LatLng(midLat, midLon), listPoints.get(1));
                    TaskRequestDirections taskRequestDirections2 = new TaskRequestDirections();
                    taskRequestDirections2.execute(url2);


                    //making third marker

                    double midLat2 = (listPoints.get(0).latitude + listPoints.get(1).latitude) / 1.999828888;
                    double midLon2 = (listPoints.get(0).longitude + listPoints.get(1).longitude) / 2.000169271;


                    String url3 = getRequestUrl(listPoints.get(0), new LatLng(midLat2, midLon2));
                    TaskRequestDirections taskRequestDirections3 = new TaskRequestDirections();
                    taskRequestDirections3.execute(url3);
                    //making third marker//nope 2nd is half

                    String url4 = getRequestUrl(new LatLng(midLat2, midLon2), listPoints.get(1));
                    TaskRequestDirections taskRequestDirections4 = new TaskRequestDirections();
                    taskRequestDirections4.execute(url4);

                    //rendering not taken ppaths

                    double notDist1 = showDistance(listPoints.get(0),new LatLng(midLat,midLon))
                            +showDistance(new LatLng(midLat,midLon),listPoints.get(1))+1;

                    double notDist2 = showDistance(listPoints.get(0),new LatLng(midLat2,midLon2))
                            +showDistance(new LatLng(midLat,midLon),listPoints.get(1))+1;

                    tvNotDistance.setText(String.format( "%.2f", notDist1 )+"KM\n"+String.format( "%.2f", notDist2 )+"KM");
                }
            }
        });

    }

    private String getRequestUrl(LatLng origin, LatLng dest) {
        //Value of origin
        String str_org = "origin=" + origin.latitude + "," + origin.longitude;
        //Value of destination
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
//        Toast.makeText(this, str_org + "\n" + str_dest, Toast.LENGTH_SHORT).show();
        //Set value enable the sensor
        String sensor = "sensor=false";
        //Mode for find direction
        String mode = "mode=driving";
        //Build the full param
        String param = str_org + "&" + str_dest + "&" + sensor + "&" + mode;
        //Output format
        String output = "json";
        //Create url to request
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param;
        return url;
    }

    private String getMapsApiDirectionsUrl1(LatLng origin, LatLng dest) {

        String waypoints = "waypoints=optimize:true|"
                + origin.latitude + "," + origin.longitude
                + "|" + "|" + origin.latitude + ","
                + dest.longitude + "|" + dest.latitude + ","
                + dest.longitude;

        String sensor = "sensor=false";
        String params = waypoints + "&" + sensor;
        String output = "json";
        String url1 = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        Log.d("url", url1);
        return url1;
    }

    private String getMapsApiDirectionsUrl2(LatLng origin, LatLng dest) {
        String waypoints = "waypoints=optimize:true|"
                + origin.latitude + "," + origin.longitude
                + "|" + "|" + dest.latitude + ","
                + origin.longitude + "|" + dest.latitude + ","
                + dest.longitude;
        ;

        String sensor = "sensor=false";
        String params = waypoints + "&" + sensor;
        String output = "json";
        String url2 = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        Log.d("url", url2);
        return url2;
    }


    private String requestDirection(String reqUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            //Get the response result
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
                break;
        }
    }


    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Parse json here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            //Get list route and display it into the map

            ArrayList points = null;

            PolylineOptions polylineOptions = null;

            for (List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat, lon));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(7);
                if (flag == 1) {
                    polylineOptions.color(Color.RED);
                    polylineOptions.width(2);
                } else if (flag == 2) {
                    polylineOptions.color(Color.RED);
                    polylineOptions.width(2);
                } else if (flag == 3) {
                    polylineOptions.color(Color.RED);
                    polylineOptions.width(2);
                } else if (flag == 4) {
                    polylineOptions.color(Color.RED);
                    polylineOptions.width(2);
                } else if (flag == 5) {
                    polylineOptions.color(Color.RED);
                    polylineOptions.width(2);
                } else {
                    polylineOptions.color(Color.GREEN);
                    polylineOptions.width(7);
                }
                flag++;

                polylineOptions.geodesic(true);

            }

            if (polylineOptions != null) {
                mMap.addPolyline(polylineOptions);

                //req second marker
                //making second marker
//
//                String url1 = getMapsApiDirectionsUrl1(listPoints.get(0), listPoints.get(1));
//                TaskRequestDirections taskRequestDirections1 = new TaskRequestDirections();
//                taskRequestDirections1.execute(url1);

            } else {
                Toast.makeText(getApplicationContext(), "Direction not found!", Toast.LENGTH_SHORT).show();
                tvDistance.setText("Error");
                tvFare.setText("Error");

            }
        }
    }


    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(MapsActivity.this, RiderMainAcitivity.class)
                .putExtra("riderName", getIntent().getExtras().getString("riderName", null))
                .putExtra("riderPhone", getIntent().getExtras().getString("riderPhone", null)));
        finish();
    }


    /**
     * This is the implementation Haversine Distance Algorithm between two places
     * R = earth’s radius (mean radius = 6,371km)
     * Δlat = lat2− lat1
     * Δlong = long2− long1
     * a = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
     * c = 2.atan2(√a, √(1−a))
     * d = R.c
     */

    double showDistance(LatLng from, LatLng to) {

        int Radius = 6371;// radius of earth in Km
        double lat1 = from.latitude;
        double lat2 = to.latitude;
        double lon1 = from.longitude;
        double lon2 = to.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;

        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return (Radius * c);
    }

    int showBikeFare(int distance) {

        int baseFare = 20;
        int fareRate = 10;

        int fare = distance * fareRate;
        if (fare < baseFare) return baseFare;
        else return fare;
    }

    int showCarFare(int distance) {

        int baseFare = 60;
        int fareRate = 70;

        int fare = distance * fareRate;
        if (fare < baseFare) return baseFare;
        else return fare;
    }
}
