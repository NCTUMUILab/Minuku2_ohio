package edu.ohio.minuku.streamgenerator;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;

import com.google.common.util.concurrent.AtomicDouble;
import com.opencsv.CSVWriter;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.dao.LocationNoGoogleDataRecordDAO;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.MinukuDAOManager;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.model.DataRecord.LocationNoGoogleDataRecord;
import edu.ohio.minuku.stream.LocationNoGoogleStream;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.exception.StreamAlreadyExistsException;
import edu.ohio.minukucore.exception.StreamNotFoundException;
import edu.ohio.minukucore.stream.Stream;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by Lawrence on 2017/11/20.
 */

public class LocationNoGoogleStreamGenerator extends AndroidStreamGenerator<LocationNoGoogleDataRecord> {

    private LocationNoGoogleStream mStream;
    private String TAG = "LocationNoGoogleStreamGenerator";
    private AtomicDouble latitude;
    private AtomicDouble longitude;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;

    private float accuracy;

    Location loc;

    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";

    private CSVWriter csv_writer = null;

    private LocationManager lms;
    private String bestProvider = LocationManager.GPS_PROVIDER;

    private Context context;

    LocationNoGoogleDataRecordDAO mDAO;

//    LocationListener locationListener;
    LocationManager locationManager;
    String mprovider;

    boolean isGPS = false;
    boolean isNetwork = false;

    private boolean getService;

    public LocationNoGoogleStreamGenerator(Context applicationContext){
        super(applicationContext);

        this.mStream = new LocationNoGoogleStream(Constants.LOCATION_QUEUE_SIZE);
        this.mDAO = MinukuDAOManager.getInstance().getDaoFor(LocationNoGoogleDataRecord.class);
        this.latitude = new AtomicDouble();
        this.longitude = new AtomicDouble();

        this.context = applicationContext;

       /*while(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            //如果GPS或網路定位開啟，呼叫locationServiceInitial()更新位置
            //如果GPS或網路定位沒開啟，繼續等

        }*/

        this.register();

    }

    /*private void waitingForPermission(){

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPS && !isNetwork) {
            Log.d(TAG, "Connection off");
            getLastLocation();
        } else {
            Log.d(TAG, "Connection on");
            String provider = locationManager.getBestProvider(new Criteria(), true);
            locationManager.requestLocationUpdates(provider, 0, 0, this);
            // get location
//            getLocation();
        }

       *//* Criteria criteria = new Criteria();

        mprovider = locationManager.getBestProvider(criteria, false);

        Log.d(TAG, "Provider : "+ mprovider);

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            try {
                if (mprovider != null && !mprovider.equals("")) {
                    Location location = locationManager.getLastKnownLocation(mprovider);
                    locationManager.requestLocationUpdates(mprovider, 10000, 1, this);

                    if (location != null)
                        onLocationChanged(location);
                    else
                        Log.d(TAG, "No Location Provider Found Check Your Code");

                }
            }catch(SecurityException e){
                e.printStackTrace();
            }
        }else{

            waitingForPermission();

        }*//*

    }*/

    /*private Location getLastKnownLocation() {
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            try{
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return bestLocation;
    }

    private void getLocation() {
        try {
            Log.d(TAG, "Can get location");
            if (isGPS) {
                // from GPS
                Log.d(TAG, "GPS on");
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                Log.d(TAG, "requestLocationUpdates");

                if (locationManager != null) {
//                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    loc = getLastKnownLocation();
                    if (loc != null) {
                        Log.d(TAG, "lat : "+loc.getLatitude()+"long : "+loc.getLongitude());

                        this.latitude.set(loc.getLatitude());
                        this.longitude.set(loc.getLongitude());
                        accuracy = loc.getAccuracy();
                    }else {
                        Log.d(TAG, "loc == null");
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

                    }
                }else {
                    Log.d(TAG, "location is null");
                }
            } else if (isNetwork) {
                // from Network Provider
                Log.d(TAG, "NETWORK_PROVIDER on");
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                if (locationManager != null) {
//                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    loc = getLastKnownLocation();
                    if (loc != null){
                        Log.d(TAG, "lat : "+loc.getLatitude()+"long : "+loc.getLongitude());

                        this.latitude.set(loc.getLatitude());
                        this.longitude.set(loc.getLongitude());
                        accuracy = loc.getAccuracy();
                    }else {
                        Log.d(TAG, "loc == null");
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

                    }
                }else {
                    Log.d(TAG, "location is null");
                }
            } else {
                Log.d(TAG, "lat : "+loc.getLatitude()+"long : "+loc.getLongitude());

                loc.setLatitude(-999.0);
                loc.setLongitude(-999.0);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            try{
                // delay 5 second, wait for user confirmed.
                Thread.sleep(5000);

            } catch(InterruptedException e2){
                e2.printStackTrace();
            }
            getLocation();
        }
    }*/

    private void getLastLocation() {
        try {
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(provider);
            Log.d(TAG, provider);
            Log.d(TAG, location == null ? "NO LastLocation" : location.toString());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void locationServiceInitial() {
        lms = (LocationManager) context.getSystemService(LOCATION_SERVICE);	//取得系統定位服務
//        Criteria criteria = new Criteria();	//資訊提供者選取標準
//        bestProvider = lms.getBestProvider(criteria, true);
        try {
            //TODO
            Location location = lms.getLastKnownLocation(LocationManager.GPS_PROVIDER);    //使用GPS定位座標

            Log.d(TAG, "latitude : "+ location.getLatitude()+"longitude : "+ location.getLongitude());

            this.latitude.set(location.getLatitude());
            this.longitude.set(location.getLongitude());
            accuracy = location.getAccuracy();

        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean updateStream() {
        Log.d(TAG, "Update stream called.");
        try {
//            lms.requestLocationUpdates(bestProvider, 1000, 1, this);

//            getLocation();

            LocationNoGoogleDataRecord locationNoGoogleDataRecord = new LocationNoGoogleDataRecord(
                    (float)latitude.get(),
                    (float)longitude.get(),
                    accuracy);
            Log.e(TAG,"locationDataRecord latitude : "+latitude.get()+" longitude : "+longitude.get());


            mStream.add(locationNoGoogleDataRecord);
            Log.d(TAG, "Location to be sent to event bus" + locationNoGoogleDataRecord);

            // also post an event.
            EventBus.getDefault().post(locationNoGoogleDataRecord);
            try {
                mDAO.add(locationNoGoogleDataRecord);

            } catch (DAOException e) {
                e.printStackTrace();
                return false;
            }

        }catch(SecurityException e){
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void register() {
        Log.d(TAG, "Registering with StreamManager.");
        try {
            MinukuStreamManager.getInstance().register(mStream, LocationNoGoogleDataRecord.class, this);
        } catch (StreamNotFoundException streamNotFoundException) {
            Log.e(TAG, "One of the streams on which LocationDataRecord depends in not found.");
        } catch (StreamAlreadyExistsException streamAlreadyExistsException) {
            Log.e(TAG, "Another stream which provides LocationDataRecord is already registered.");
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged");

            Log.d(TAG, "latitude" + location.getLatitude());

            latitude.set(location.getLatitude());
            longitude.set(location.getLongitude());
            accuracy = location.getAccuracy();

            long lastposupdate = new Date().getTime();

            StoreToCSV(lastposupdate,location.getLatitude(),location.getLongitude(),location.getAccuracy());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.d(TAG, "onStatusChanged");
        }

        @Override
        public void onProviderEnabled(String s) {
            Log.d(TAG, "onProviderEnabled");

        }

        @Override
        public void onProviderDisabled(String s) {
            Log.d(TAG, "onProviderDisabled");

        }
    };

    @Override
    public void onStreamRegistration() {

        this.latitude.set(-999.0);
        this.longitude.set(-999.0);

        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
        }catch (SecurityException e){
            e.printStackTrace();
            try{
                // delay 5 second, wait for user confirmed.
                Thread.sleep(5000);

            } catch(InterruptedException e2){
                e2.printStackTrace();
            }

            onStreamRegistration();
        }
//        waitingForPermission();



        Log.d(TAG, "Stream " + TAG + " registered successfully");

    }

    @Override
    public Stream<LocationNoGoogleDataRecord> generateNewStream() {
        return mStream;
    }

    /*@Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.d(TAG, "GPS: "
                    + location.getLatitude() + ", "
                    + location.getLongitude() + ", "
                    + "accuracy: " + location.getAccuracy()
                    +"Extras : " + location.getExtras());

            // If the location is accurate to 30 meters, it's good enough for us.
            // Post an update event and exit. //TODO maybe be
            float dist = 0;
            float[] results = new float[1];

            Log.d(TAG, "last time GPS : "
                    + latitude.get() + ", "
                    + longitude.get() + ", "
                    + "accuracy: " + location.getAccuracy());

//            Location.distanceBetween(location.getLatitude(),location.getLongitude(),latitude.get(),longitude.get(),results);
//
//            if(!(latitude.get() == -999.0 && longitude.get() == -999.0))
//                dist = results[0];
//            else
//                dist = 1000;

            Log.d(TAG, "dist : " + dist);
            //if the newest
            //TODO cancel the dist restriction
//            if(dist < 100 || (latitude.get() == -999.0 && longitude.get() == -999.0)){
            // Log.d(TAG, "Location is accurate upto 50 meters");
            this.latitude.set(location.getLatitude());
            this.longitude.set(location.getLongitude());
            accuracy = location.getAccuracy();

            //the lastposition update value timestamp
            long lastposupdate = new Date().getTime();

            StoreToCSV(lastposupdate,location.getLatitude(),location.getLongitude(),location.getAccuracy());

            LocationNoGoogleDataRecord locationNoGoogleDataRecord = new LocationNoGoogleDataRecord(
                    (float) latitude.get(),
                    (float) longitude.get(),
                    accuracy);
//            TripManager.getInstance().setTrip(locationNoGoogleDataRecord);


            Log.d(TAG,"onLocationChanged latitude : "+latitude+" longitude : "+ longitude);
//                    this.location = location;

//            }

        }

    }*/

    public void StoreToCSV(long timestamp, double latitude, double longitude, float accuracy){

        Log.d(TAG,"StoreToCSV");

        String sFileName = "LocationNoGoogle.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"timestamp","timeString","Latitude","Longitude","Accuracy"});
            String timeString = getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp),timeString,String.valueOf(latitude),String.valueOf(longitude),String.valueOf(accuracy)});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private String getTimeString(long time){

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW);
        String currentTimeString = sdf_now.format(time);

        return currentTimeString;
    }

    @Override
    public long getUpdateFrequency() {
        return 1;
    }

    @Override
    public void sendStateChangeEvent() {
        Log.d(TAG, "sendStateChangeEvent");

    }

    @Override
    public void offer(LocationNoGoogleDataRecord dataRecord) {
        Log.d(TAG, "offer");

    }
/*
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d(TAG, "onStatusChanged");

    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d(TAG, "onProviderEnabled");
        getLocation();
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d(TAG, "onProviderDisabled");
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }*/
}
