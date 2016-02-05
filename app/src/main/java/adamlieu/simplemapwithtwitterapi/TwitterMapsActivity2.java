package adamlieu.simplemapwithtwitterapi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TwitterMapsActivity2 extends FragmentActivity {


    private GoogleMap mMap;
    LocationManager locManager;// = (LocationManager)getSystemService(LOCATION_SERVICE);
    String provider;
    Location location;
    Criteria criteria = new Criteria();
    List<LatLng> cachePos = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        String gpsProvider = LocationManager.GPS_PROVIDER;

        //Prompts user to enable location services if it is not already enabled
        if (!locManager.isProviderEnabled(gpsProvider)) {
            /*Toast toast =  Toast.makeText(context, "Location GPS must be enabled!", Toast.LENGTH_LONG);
            toast.show();*/

            //Alert Dialog
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Notice");
            alertDialog.setMessage("Location GPS must be enabled!");
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String locConfig = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
                    Intent enableGPS = new Intent(locConfig);
                    startActivity(enableGPS);
                }
            });
            alertDialog.show();
        }

        //mMap = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_maps2);
        setUpMapIfNeeded();

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        /*
        try {
            //int limit = 8000;
            //loadJSON(limit);
            //circleToMap(cachePos, limit);
        } catch (JSONException ex){
            ex.printStackTrace();
        }*/
        Integer limit = 10000;
        new RetrieveTweets().execute(limit);
    }

    public List<LatLng> loadJSON(int limit) throws JSONException{
        //String json = null;
        List<LatLng> list = new ArrayList<>();
        try{
            InputStream is = getResources().openRawResource(R.raw.toronto);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            //Read just coordinates for now
            int counter = 0;
            while(((line = reader.readLine()) != null) && counter < limit){
                JSONObject obj = new JSONObject(line);
                if(!obj.isNull("coordinates")) {
                    JSONObject coords = new JSONObject(obj.get("coordinates").toString());
                    JSONArray latlng = coords.getJSONArray("coordinates");
                    //Log.v("Coordinates:", latlng.get(0).toString() + " : " + latlng.get(1).toString());
                    //************************
                    //TWITTER USES LONGITUDE THEN LATITUDE
                    //************************
                    Double lat = Double.parseDouble(latlng.get(1).toString());
                    double lng = Double.parseDouble(latlng.get(0).toString());
                    LatLng pos = new LatLng(lat, lng);
                    //cachePos.add(pos);
                    list.add(pos);
                    counter++;
                    Log.v("Coordinates:", "" + lat + " : " + lng);
                    Log.v("Coordinates Counter:", "" + counter);
                }
            }
            is.close();
            Log.v("Read JSON:", "Success");
        } catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
        return list;
    }

    private class RetrieveTweets extends AsyncTask<Integer, Void, Integer> {
        Context context = getApplicationContext();
        protected Integer doInBackground(Integer... test){
            try{
                cachePos = loadJSON(test[0]);
            } catch (JSONException ex){
                ex.printStackTrace();
            }
            return test[0];
        }

        protected void onPostExecute(Integer test){
            Toast.makeText(context, "Retrieval complete, displaying " + test + " Tweets." , Toast.LENGTH_SHORT).show();
            circleToMap(cachePos);

        }
    }

    private void circleToMap(List<LatLng> pos){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        int d = 100;
        Bitmap bitmap = Bitmap.createBitmap(d,d,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(0x7F96B0FF);
        canvas.drawCircle(d/2, d/2, d/2, paint);

        BitmapDescriptor desc = BitmapDescriptorFactory.fromBitmap(bitmap);

        int counter = 0;

        //TODO: Separate this to allow for continuous adding of things after the fact
        for(LatLng i : pos) {
            mMap.addGroundOverlay(new GroundOverlayOptions()
                            .image(desc)
                            .position(i, 150)
            );
            Log.v("Adding point:", "" + i);
            counter++;

            //if(counter > limit) break;
        }
    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        provider = locManager.getBestProvider(criteria, true); // Name for best provider
        //Check for permissions if they are granted
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)){
            return;
        }
        location = locManager.getLastKnownLocation(provider); // Get last known location, basically current location
        if(location != null){
            //Get current long and lat positions
            //LatLng currentPos = new LatLng(location.getLatitude(), location.getLongitude());
            LatLng currentPos = new LatLng(43.945791, -78.894689);
            //Add a marker on the map with the current position
            //mMap.addMarker(new MarkerOptions().position(currentPos).title("UOIT"));

            //Controls the camera so it would zoom into current position
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentPos, 13);
            mMap.animateCamera(cameraUpdate);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_twitter_maps_activity2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
