package edu.ohio.minuku_2.controller.Ohio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Data.DataHandler;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/7/5.
 */

public class AnnotateSessionActivity extends Activity implements OnMapReadyCallback {

    private final String TAG = "AnnotateSessionActivity";

    public static float GOOGLE_MAP_DEFAULT_ZOOM_LEVEL = 13;
    public static LatLng GOOGLE_MAP_DEFAULT_CAMERA_CENTER = new LatLng(24, 120);

    private int mSessionId=-1;
    private TextView time, question3, question4;
    private RadioButton ans3_1, ans3_2, ans3_3;
    private RadioButton ans4_1, ans4_2, ans4_3;
    private RadioGroup ques1,
            ques2_1, ques2_2, ques2_3, ques2_4, ques2_5,
            ques3, ques4;

    private String ans1, ans2, ans3, ans4, activityType, tripType; //, preplan;
    private boolean error1, error2, error3, error4, error4_2;

    private Context mContext;
    private MapFragment map;
    private MapView mMapView;
    private Barcode.GeoPoint geoPoint;
    private GoogleMap mGoogleMap;

    private Button submit;
    private ArrayList<String> results;

    private Bundle bundle;

    private String ongoing;

    private String start;
    private String end;

    private Spinner activityspinner, preplanspinner, tripspinner;
    final String[] activityString = {"Choose an activity", "Static", "Biking", "In a car. (I'm the driver)",
            "In a car. (I'm NOT the driver)", "Taking a bus", "Walking outdoors", "Walking indoors"};
    final String[] preplanString = {"Did you have preplaned this trip?","Yes", "No"};
    final String[] tripString = {"This is a clear trip", "This is not a clear trip", "This is same with the previous trip"};

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.annotate_activity_ohio);
        error1 = false;
        error2 = false;
        error3 = false;
        error4 = false;
        Log.d(TAG, "[test show trip] onCreate AnntoateSessionaACtivity");

        bundle = getIntent().getExtras();
        mSessionId = Integer.parseInt(bundle.getString("sessionkey_id"));
        Log.d(TAG,"[test show trip]  on create session id: : " + mSessionId);


        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.Mapfragment);

        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            Log.d(TAG, " onKeyDown");

            AnnotateSessionActivity.this.finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();

        initAnnotationView();

    }

    /**
     * this function initialize the view of the annotation acitvity, including quesitonnaire and map
     */
    public void initAnnotationView(){

//        ArrayList<LatLng> latLngs = new ArrayList<LatLng>();
//
//        /** storing location records after we obain from the database*/
//        for(int index = 0;index < results.size(); index++){
//            String[] separated = results.get(index).split(Constants.DELIMITER);
//            double lat = Double.valueOf(separated[2]);
//            double lng = Double.valueOf(separated[3]);
//
//            LatLng latLng = new LatLng(lat, lng);
//            latLngs.add(latLng);
//        }
//
//
//        Log.d(TAG,"latLngs : " + latLngs);


        /** showing location records on the map **/
//        showRecordingVizualization(latLngs);



        /*
        activityspinner = (Spinner)findViewById(R.id.activityspinner);
        preplanspinner = (Spinner)findViewById(R.id.preplanspinner);
        tripspinner = (Spinner)findViewById(R.id.tripspinner);

        ArrayAdapter<String> activityList = new ArrayAdapter<String>(AnnotateSessionActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                activityString);
        ArrayAdapter<String> preplanList = new ArrayAdapter<String>(AnnotateSessionActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                preplanString);
        ArrayAdapter<String> tripList = new ArrayAdapter<String>(AnnotateSessionActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                tripString);

        activityspinner.setAdapter(activityList);
        preplanspinner.setAdapter(preplanList);
        tripspinner.setAdapter(tripList);
        */

        initQuestionnaire();

        submit = (Button)findViewById(R.id.submit);
        submit.setOnClickListener(submitting);

//        if(ongoing.equals("true")){
//            submit.setClickable(false);
//
//            Toast.makeText(getApplicationContext(), "This trip is still ongoing!", Toast.LENGTH_LONG).show();
//        }

    }


    private void showRecordingVizualization(final int sessionId){

        Log.d(TAG,"[test show trip] enter showRecordingVizualization ");
        //validate map
        setUpMapIfNeeded();

        Log.d(TAG, "[test show trip] aftr setUpMapIfNeeded");

        //draw map
        if (mGoogleMap!=null){

            Log.d(TAG, "[test show trip] Google Map is not null");
//                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(VisualizationManager. GOOGLE_MAP_DEFAULT_CAMERA_CENTER, 13));

            //if we're reviewing a previous session ( the trip is not ongoing), get session from the database (note that we have to use session id to check instead of a session instance)
            if (!SessionManager.isSessionOngoing(sessionId)) {

                Log.d(TAG, "[test show trip] the session is NOT in the currently recording session, we should query database");
                //because there could be many points for already ended trace, so we use asynch to download the annotations

                try{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        new LoadDataAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sessionId).get();
                    else
                        new LoadDataAsyncTask().execute(sessionId).get();

                }catch(InterruptedException e) {
                    Log.d(TAG,"InterruptedException");
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    Log.d(TAG,"ExecutionException");
                    e.printStackTrace();
                }


            }
            //the recording is ongoing, so we periodically query the database to show the latest path
            //TODO: draw my current location from the starting point.
            else {

                Log.d(TAG, "[test show trip] the session is in the currently recording session");

                try{

                    Log.d(TAG, "[test show trip] the session is in the currently recording session, update map!!! Get new points!");

                    //get location points to draw on the map..
                    ArrayList<LatLng> points = getLocationPointsToDrawOnMap(sessionId);

                    LatLng startLatLng;
                    //we use endLatLng, which is the user's current location as the center of the camera
                    LatLng endLatLng;

                    //only has one point
                    if (points.size()==1){

                        startLatLng  = points.get(0);
                        endLatLng = points.get(0);

                        showMapWithPathsAndCurLocation(mGoogleMap, points, endLatLng);
                    }
                    //when have multiple locaiton points
                    else if (points.size()>1) {

                        startLatLng  = points.get(0);
                        endLatLng = points.get(points.size()-1);

                        showMapWithPathsAndCurLocation(mGoogleMap, points, endLatLng);
                    }


                }catch (IllegalArgumentException e){
                    //Log.e(LOG_TAG, "Could not unregister receiver " + e.getMessage()+"");
                }


//                final Handler updateMapHandler = new Handler();
//
//                Runnable runnable = new Runnable() {
//                    @Override
//                    public void run() {
//
//                        try{
//
//                            Log.d(TAG, "[test show trip] the session is in the currently recording session, update map!!! Get new points!");
//
//                            //get location points to draw on the map..
//                            ArrayList<LatLng> points = getLocationPointsToDrawOnMap(sessionId);
//
//                            LatLng startLatLng;
//                            //we use endLatLng, which is the user's current location as the center of the camera
//                            LatLng endLatLng;
//
//                            //only has one point
//                            if (points.size()==1){
//
//                                startLatLng  = points.get(0);
//                                endLatLng = points.get(0);
//
//                                showMapWithPathsAndCurLocation(mGoogleMap, points, endLatLng);
//                            }
//                            //when have multiple locaiton points
//                            else if (points.size()>1) {
//
//                                startLatLng  = points.get(0);
//                                endLatLng = points.get(points.size()-1);
//
//                                showMapWithPathsAndCurLocation(mGoogleMap, points, endLatLng);
//                            }
//
//
//                        }catch (IllegalArgumentException e){
//                            //Log.e(LOG_TAG, "Could not unregister receiver " + e.getMessage()+"");
//                        }
//                        updateMapHandler.postDelayed(this, 5*1000);
//                    }
//                };

                /**start repeatedly store the extracted contextual information into Record objects**/
//                updateMapHandler.post(runnable);

            }

        }

    }

    public void showMapWithPaths(GoogleMap map, ArrayList<LatLng> points, LatLng cameraCenter, LatLng startLatLng, LatLng endLatLng) {

        Log.d(TAG, "[test show trips] in showMapWithPaths");

        //map option
        GoogleMapOptions options = new GoogleMapOptions();
        options.tiltGesturesEnabled(false);
        options.rotateGesturesEnabled(false);

        //center the map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraCenter, 13));

        // Marker start = map.addMarker(new MarkerOptions().position(startLatLng));

        //draw linges between points and add end and start points
        PolylineOptions pathPolyLineOption = new PolylineOptions().color(Color.RED).geodesic(true);
        pathPolyLineOption.addAll(points);

        //draw lines
        Polyline path = map.addPolyline(pathPolyLineOption);

        /**after getting the start and ened point of location trace, we put a marker**/
        map.addMarker(new MarkerOptions().position(startLatLng).title("Start"))
                .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        map.addMarker(new MarkerOptions().position(endLatLng).title("End"));

    }

    public void showMapWithPathsAndCurLocation(GoogleMap map, ArrayList<LatLng> points, LatLng curLoc) {

        Log.d(TAG, "[test show trips] in showMapWithPathssAndCurLocation");

        map.clear();

        //map option
        GoogleMapOptions options = new GoogleMapOptions();
        options.tiltGesturesEnabled(false);
        options.rotateGesturesEnabled(false);

        //get current zoom level
        float zoomlevel = map.getCameraPosition().zoom;

        //center the map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(curLoc, 13));

        // Marker start = map.addMarker(new MarkerOptions().position(startLatLng));
        Marker me =  map.addMarker(new MarkerOptions()
                .position(curLoc)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.mylocation))

        );

        //draw linges between points and add end and start points
        PolylineOptions pathPolyLineOption = new PolylineOptions().color(Color.RED).geodesic(true);
        pathPolyLineOption.addAll(points);

        //draw lines
        Polyline path = map.addPolyline(pathPolyLineOption);


    }

    private void initQuestionnaire(){

        ques1 = (RadioGroup)findViewById(R.id.ques1);
        ques2_1 = (RadioGroup)findViewById(R.id.ques2_1);
        ques2_2 = (RadioGroup)findViewById(R.id.ques2_2);
        ques2_3 = (RadioGroup)findViewById(R.id.ques2_3);
        ques2_4 = (RadioGroup)findViewById(R.id.ques2_4);
        ques2_5 = (RadioGroup)findViewById(R.id.ques2_5);
        ques3 = (RadioGroup)findViewById(R.id.ques3);
        ques4 = (RadioGroup)findViewById(R.id.ques4);

        ques4.setVisibility(View.GONE);

        ques2_1.clearCheck();
        ques2_2.clearCheck();
        ques2_3.clearCheck();
        ques2_4.clearCheck();
        ques2_5.clearCheck();

        question4 = (TextView)findViewById(R.id.question4);
        ans3_1 = (RadioButton)findViewById(R.id.ans3_1);
        ans3_2 = (RadioButton)findViewById(R.id.ans3_2);

        ans4_1 = (RadioButton)findViewById(R.id.ans4_1);
        ans4_2 = (RadioButton)findViewById(R.id.ans4_2);
        ans4_3 = (RadioButton)findViewById(R.id.ans4_3);

        ques1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if(checkedId==-1){
                    error1=false;
                }else{
                    error1=true;
//                    ans1=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans1 = radioButton.getText().toString();
                }
            }
        });

        ques2_1.setOnCheckedChangeListener(ques2_1Listener);
        ques2_2.setOnCheckedChangeListener(ques2_2Listener);
        ques2_3.setOnCheckedChangeListener(ques2_3Listener);
        ques2_4.setOnCheckedChangeListener(ques2_4Listener);
        ques2_5.setOnCheckedChangeListener(ques2_5Listener);

        ques3.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if(checkedId==-1){
                    error3=false;
                }else{
                    error3=true;
//                    ans3=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans3 = radioButton.getText().toString();
                }

                //different question on question3 based on question2
                if(checkedId==R.id.ans3_1){ //Yes
                    ques4.setVisibility(View.VISIBLE);
                    question4.setText("When did you leave for this routine trip?");
                    ans4_1.setText("Ahead of schedule"); //"Ahead of schedule", "On time", "Behind schedule"
                    ans4_2.setText("On time");
                    ans4_3.setText("Behind schedule");

                }else if(checkedId==R.id.ans3_2){ //No
                    ques4.setVisibility(View.VISIBLE);
                    question4.setText("When did you decide to take this trip?");
                    ans4_1.setText("Right before");
                    ans4_2.setText("Today");
                    ans4_3.setText("Before Today");

                }
            }
        });

        ques4.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if(checkedId==-1){
                    error4 =false;
                }else{
                    error4 =true;
//                    ans4=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans4 = radioButton.getText().toString();
                }
            }
        });

        //checking its ongoing or not
//        String sessionid = getSessionIdByTimekey(sessionkey);
//        ongoing = "";
//        try {
//            SQLiteDatabase db = DBManager.getInstance().openDatabase();
//            Cursor tripCursor = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ (sessionid) + "'", null); //cause sessionid start from 0.
//            Log.d(TAG,"SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ (sessionid) + "'");
//            int rows = tripCursor.getCount();
//
//            if(rows!=0){
//                //get the first one is that it must be set with the accurate status
//                tripCursor.moveToFirst();
//                ongoing = tripCursor.getString(8);
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        Log.d(TAG, "ongoing : "+ ongoing);
//
//        if(ongoing.equals("true")){
//            setRG_notclickable(ques1);
//            setRG_notclickable(ques2_1);
//            setRG_notclickable(ques2_2);
//            setRG_notclickable(ques2_3);
//            setRG_notclickable(ques2_4);
//            setRG_notclickable(ques2_5);
//            setRG_notclickable(ques3);
//            setRG_notclickable(ques4);
//        }

    }

    private void setRG_notclickable(RadioGroup ques){
        for (int i = 0; i < ques.getChildCount(); i++) {
            ques.getChildAt(i).setEnabled(false);
        }
    }

//    private String getSessionIdByTimekey(String timekey){
//        Log.d(TAG, "getSessionIdByTimekey");
//
//        timekey = timekey.split("-")[0];
//
//        String sessionid = null;
//
//        try {
//            SQLiteDatabase db = DBManager.getInstance().openDatabase();
//            Cursor tripCursor = db.rawQuery("SELECT "+ DBHelper.sessionid_col +" FROM " + DBHelper.trip_table +
//                    " WHERE "+ DBHelper.TIME+ " ='"+ timekey + "'", null);
//            Log.d(TAG,"SELECT "+ DBHelper.sessionid_col +" FROM " + DBHelper.trip_table +
//                    " WHERE "+ DBHelper.TIME+ " ='"+ timekey + "'");
//            int rows = tripCursor.getCount();
//
//            tripCursor.moveToFirst();
//
//            if(rows!=0){
//                Log.d(TAG,"tripCursor.getString(0) : "+ tripCursor.getString(0));
//                sessionid = tripCursor.getString(0);
//                Log.d(TAG, "sessionid : "+sessionid);
//            }else
//                Log.d(TAG, "rows==0");
//
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        Log.d(TAG, "sessionid : " + sessionid);
//
//        String sessionids[] = sessionid.split(", ");
//
//        Log.d(TAG, "sessionids 1 : " + sessionids[1]);
//        return sessionids[1];
//    }

    private RadioGroup.OnCheckedChangeListener ques2_1Listener = new RadioGroup.OnCheckedChangeListener() {
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // TODO Auto-generated method stub

            if(checkedId==-1){
                error2=false;

            }else{
                error2=true;
//                    ans2=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                ques2_2.setOnCheckedChangeListener(null);
                ques2_2.clearCheck();
                ques2_2.setOnCheckedChangeListener(ques2_2Listener);

                ques2_3.setOnCheckedChangeListener(null);
                ques2_3.clearCheck();
                ques2_3.setOnCheckedChangeListener(ques2_3Listener);

                ques2_4.setOnCheckedChangeListener(null);
                ques2_4.clearCheck();
                ques2_4.setOnCheckedChangeListener(ques2_4Listener);

                ques2_5.setOnCheckedChangeListener(null);
                ques2_5.clearCheck();
                ques2_5.setOnCheckedChangeListener(ques2_5Listener);

                RadioButton radioButton = (RadioButton) findViewById(checkedId);
                ans2 = radioButton.getText().toString();
            }

        }
    };

    private RadioGroup.OnCheckedChangeListener ques2_2Listener = new RadioGroup.OnCheckedChangeListener() {
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // TODO Auto-generated method stub

            if(checkedId==-1){
                error2=false;

            }else{
                error2=true;
//                    ans2=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                ques2_1.setOnCheckedChangeListener(null);
                ques2_1.clearCheck();
                ques2_1.setOnCheckedChangeListener(ques2_1Listener);

                ques2_3.setOnCheckedChangeListener(null);
                ques2_3.clearCheck();
                ques2_3.setOnCheckedChangeListener(ques2_3Listener);

                ques2_4.setOnCheckedChangeListener(null);
                ques2_4.clearCheck();
                ques2_4.setOnCheckedChangeListener(ques2_4Listener);

                ques2_5.setOnCheckedChangeListener(null);
                ques2_5.clearCheck();
                ques2_5.setOnCheckedChangeListener(ques2_5Listener);

                RadioButton radioButton = (RadioButton) findViewById(checkedId);
                ans2 = radioButton.getText().toString();
            }

        }
    };

    private RadioGroup.OnCheckedChangeListener ques2_3Listener = new RadioGroup.OnCheckedChangeListener() {
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // TODO Auto-generated method stub

            if(checkedId==-1){
                error2=false;

            }else{
                error2=true;
//                    ans2=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                ques2_1.setOnCheckedChangeListener(null);
                ques2_1.clearCheck();
                ques2_1.setOnCheckedChangeListener(ques2_1Listener);

                ques2_2.setOnCheckedChangeListener(null);
                ques2_2.clearCheck();
                ques2_2.setOnCheckedChangeListener(ques2_2Listener);

                ques2_4.setOnCheckedChangeListener(null);
                ques2_4.clearCheck();
                ques2_4.setOnCheckedChangeListener(ques2_4Listener);

                ques2_5.setOnCheckedChangeListener(null);
                ques2_5.clearCheck();
                ques2_5.setOnCheckedChangeListener(ques2_5Listener);
                RadioButton radioButton = (RadioButton) findViewById(checkedId);
                ans2 = radioButton.getText().toString();
            }

        }
    };

    private RadioGroup.OnCheckedChangeListener ques2_4Listener = new RadioGroup.OnCheckedChangeListener() {
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // TODO Auto-generated method stub

            if(checkedId==-1){
                error2=false;

            }else{
                error2=true;
//                    ans2=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                ques2_1.setOnCheckedChangeListener(null);
                ques2_1.clearCheck();
                ques2_1.setOnCheckedChangeListener(ques2_1Listener);

                ques2_2.setOnCheckedChangeListener(null);
                ques2_2.clearCheck();
                ques2_2.setOnCheckedChangeListener(ques2_2Listener);

                ques2_3.setOnCheckedChangeListener(null);
                ques2_3.clearCheck();
                ques2_3.setOnCheckedChangeListener(ques2_3Listener);

                ques2_5.setOnCheckedChangeListener(null);
                ques2_5.clearCheck();
                ques2_5.setOnCheckedChangeListener(ques2_5Listener);
                RadioButton radioButton = (RadioButton) findViewById(checkedId);
                ans2 = radioButton.getText().toString();
            }

        }
    };

    private RadioGroup.OnCheckedChangeListener ques2_5Listener = new RadioGroup.OnCheckedChangeListener() {
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // TODO Auto-generated method stub

            if(checkedId==-1){
                error2=false;

            }else{
                error2=true;
//                    ans2=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                ques2_1.setOnCheckedChangeListener(null);
                ques2_1.clearCheck();
                ques2_1.setOnCheckedChangeListener(ques2_1Listener);

                ques2_2.setOnCheckedChangeListener(null);
                ques2_2.clearCheck();
                ques2_2.setOnCheckedChangeListener(ques2_2Listener);

                ques2_3.setOnCheckedChangeListener(null);
                ques2_3.clearCheck();
                ques2_3.setOnCheckedChangeListener(ques2_3Listener);

                ques2_4.setOnCheckedChangeListener(null);
                ques2_4.clearCheck();
                ques2_4.setOnCheckedChangeListener(ques2_4Listener);

                RadioButton radioButton = (RadioButton) findViewById(checkedId);
                ans2 = radioButton.getText().toString();
            }

        }
    };
/*
    private void initQuestionnaire(){
        final RadioGroup ques1, ques2, ques3, ques4_1, ques4_2;

        ques1 = (RadioGroup)findViewById(R.id.ques1);
        ques2 = (RadioGroup)findViewById(R.id.ques2);
        ques3 = (RadioGroup)findViewById(R.id.ques3);
        ques4_1 = (RadioGroup)findViewById(R.id.ques4_1);
        ques4_2 = (RadioGroup)findViewById(R.id.ques4_2);

        ques3.setVisibility(View.GONE);

        question3 = (TextView)findViewById(R.id.question3);
        ans3_1 = (RadioButton)findViewById(R.id.ans3_1);
        ans3_2 = (RadioButton)findViewById(R.id.ans3_2);
        ans3_3 = (RadioButton)findViewById(R.id.ans3_3);

        ques1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if(checkedId==-1){
                    error1=false;
                }else{
                    error1=true;
//                    ans1=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans1 = radioButton.getText().toString();
                }
            }
        });

        ques2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if(checkedId==-1){
                    error2=false;
                }else{
                    error2=true;
//                    ans2=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans2 = radioButton.getText().toString();
                }

                //different question on question3 based on question2
                if(checkedId==R.id.ans2_1){ //Yes
                    ques3.setVisibility(View.VISIBLE);
                    question3.setText("Did you leave at your normal time?");
                    ans3_1.setText("left early");
                    ans3_2.setText("left at normal time");
                    ans3_3.setText("left late");

                }else if(checkedId==R.id.ans2_2){ //No
                    ques3.setVisibility(View.VISIBLE);
                    question3.setText("When did you decide to take this non-routine trip?");
                    ans3_1.setText("right before");
                    ans3_2.setText("today");
                    ans3_3.setText("before today");

                }
            }
        });

        ques3.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if(checkedId==-1){
                    error3=false;
                }else{
                    error3=true;
//                    ans3=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans3 = radioButton.getText().toString();
                }
            }
        });

        ques4_1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if(checkedId==-1){
                    error4=false;
                }else{
                    error4=true;
//                    ans4=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans4 = radioButton.getText().toString();
                }
            }
        });

        ques4_2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if(checkedId==-1){
                    error4_2=false;
                }else{
                    error4_2=true;
//                    ans4_2=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans4_2 = radioButton.getText().toString();
                }
            }
        });

    }*/

//    private void showRecordingVizualization(final ArrayList<LatLng> latLngs){
//
//        Log.d(TAG,"trip data showRecordingVizualization");
//
//
//        ((MapFragment) getFragmentManager().findFragmentById(R.id.Mapfragment)).getMapAsync(new OnMapReadyCallback() {
//            @Override
//            public void onMapReady(GoogleMap googleMap) {
//
//                Log.d(TAG,"trip data showRecordingVizualization for latlngs " + latLngs.toString());
//                mGoogleMap = googleMap;
//                Log.d(TAG,"trip data safter google mao ");
//
//                LatLng startLatLng = latLngs.get(0);
//                LatLng endLatLng = latLngs.get(latLngs.size() - 1);
//                LatLng middleLatLng = latLngs.get(latLngs.size() / 2);
//
//                Log.d(TAG, "trip data startLatLng : "+startLatLng+"end : "+endLatLng+"middle : "+middleLatLng);
//
//                int i = 1;
//
//                for (LatLng latLng:latLngs) {
//                    Log.d(TAG, "trip data latLng"+ i +" : "+latLng);
//                    i++;
//                }
//
//                /**after getting the start and ened point of location trace, we put a marker**/
//                mGoogleMap.addMarker(new MarkerOptions().position(startLatLng).title("Start"))
//                        .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//                mGoogleMap.addMarker(new MarkerOptions().position(endLatLng).title("End"));
//
////                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(middle,15.0f));
//                showMapWithPaths(mGoogleMap, latLngs, middleLatLng,startLatLng, middleLatLng );
//            }
//        });
//
//
//        /*if(mGoogleMap!=null) {
////            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(GOOGLE_MAP_DEFAULT_CAMERA_CENTER, GOOGLE_MAP_DEFAULT_ZOOM_LEVEL));
//
//            LatLng nkut = latLngs.get(0);
//            LatLng end = latLngs.get(latLngs.size() - 1);
//            LatLng middle = latLngs.get(latLngs.size() / 2);
//
//            Log.d(TAG, "nkut : "+nkut+"end : "+end+"middle : "+middle);
//
//            ArrayList<LatLng> points = new ArrayList<LatLng>();
//            points.add(nkut);
//            points.add(middle);
//            points.add(end);
//            mGoogleMap.addMarker(new MarkerOptions().position(nkut).title("Here you are."));
//            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nkut,15.0f));
//            showMapWithPaths(mGoogleMap, points, middle);
//
//        }*/
//    }

    private void setUpMapIfNeeded() {

        Log.d(TAG,"test show trip setUpMapIfNeeded");


//        mGoogleMap = map.getMap();
//        mGoogleMap.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG,"test show trip onMapReady");

        mGoogleMap = googleMap;

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        //setup map
        Session session=null;


        Log.d(TAG,"[test show trip] get session using id " + mSessionId);

        //get session
        session = SessionManager.getSession(mSessionId);

        Log.d(TAG,"[test show trip] get session using id " + session.getId() + " starting at  " + session.getStartTime() + " and end at " + session.getEndTime());

        //show trip time
        time = (TextView)findViewById(R.id.Time);

        long startTime = session.getStartTime();
        long endTime = session.getEndTime();
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_DATE_TEXT_HOUR_MIN);
        String timelabel = null;

        if (endTime==0){
            time.setText( "Time: " + ScheduleAndSampleManager.getTimeString(startTime, sdf) + " - " + ScheduleAndSampleManager.getTimeString(endTime, sdf) );
        }
        else {

            if (SessionManager.isSessionOngoing(mSessionId))
                time.setText( "Time: " + ScheduleAndSampleManager.getTimeString(startTime, sdf) + " - Now"  );
            else
                time.setText( "Time: " + ScheduleAndSampleManager.getTimeString(startTime, sdf) + " - Unkown"  );

        }

        Log.d(TAG,"[test show trip] setting time label" + time.getText().toString());

        /** After we got a new session id, we retrieve and visualize data for the session**/
        if (session!=null) {
            showRecordingVizualization((int) session.getId());
        }

//        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);

       /* LatLng nkut = new LatLng(24.78680, 120.99722);
        LatLng end = new LatLng(24.79283, 120.99284);
        LatLng middle = new LatLng(24.78965, 120.99535);

        ArrayList<LatLng> points = new ArrayList<LatLng>();
        points.add(nkut);
        points.add(middle);
        points.add(end);
        mGoogleMap.addMarker(new MarkerOptions().position(nkut).title("Here you are."));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nkut,15.0f));
        showMapWithPaths(mGoogleMap, points, middle);*/

    }


    private Button.OnClickListener submitting = new Button.OnClickListener() {
        public void onClick(View v) {
            Log.e(TAG, "submit clicked");

            Log.d(TAG, "ans1 : "+ans1+" ans2 : "+ans2+" ans3 : "+ans3+" ans4 : "+ ans4 +" ans4_2 : "+ans4_2);

            if(error1 && error2 && error3 && error4){ // && PreplanError  && error4_2 && activityError) {

                /* TODO storing data */
                JSONObject data = new JSONObject();

                JSONArray latdata = new JSONArray();
                JSONArray lngdata = new JSONArray();

                int i = 0;

                updateSessionWithAnnotation(start, end,
                        ans1, ans2, ans3, ans4, latdata, lngdata);

                AnnotateSessionActivity.this.finish();
            }else
                Toast.makeText(AnnotateSessionActivity.this,"Please complete all the questions!!",Toast.LENGTH_SHORT).show();

        }
    };

    private long getSpecialTimeInMillis(String givenDateFormat){
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW);
        long timeInMilliseconds = 0;
        try {
            Date mDate = sdf.parse(givenDateFormat);
            timeInMilliseconds = mDate.getTime();
            Log.d(TAG,"Date in milli :: " + timeInMilliseconds);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timeInMilliseconds;
    }

    private void updateSessionWithAnnotation(String starttime, String endtime, String ans1, String ans2, String ans3
            , String ans4, JSONArray latdata, JSONArray lngdata){
        Log.d(TAG, "updateSessionWithAnnotation");

        ContentValues values = new ContentValues();
        SQLiteDatabase db = DBManager.getInstance().openDatabase();

        Annotation annotation = new Annotation();

        JSONObject toContent = new JSONObject();

        try {
            toContent.put("starttime", starttime);
            toContent.put("endtime", endtime);
            toContent.put("ans1", ans1);
            toContent.put("ans2", ans2);
            toContent.put("ans3", ans3);
            toContent.put("ans4", ans4);

        }catch (JSONException e){
            e.printStackTrace();
        }
        annotation.setContent(toContent.toString());
        annotation.addTag("ESM");

        Session session = new Session(mSessionId);

        //add answer as annotation
        session.addAnnotation(annotation);

        Log.d(TAG,"[test show trip] session " + session.getId() +  " add annotation" + session.getAnnotationsSet());

        //update session with its annotation to the session table
        DBHelper.updateSessionTable(session.getId(), session.getEndTime(), session.getAnnotationsSet());


    }

    private String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }

    public String getmillisecondToDateWithTime(long timeStamp){

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH)+1;
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int mhour = calendar.get(Calendar.HOUR);
        int mMin = calendar.get(Calendar.MINUTE);
        int mSec = calendar.get(Calendar.SECOND);
        int ampm = calendar.get(Calendar.AM_PM);

        String AMPM = "AM";

        /*if(mhour>12)
            mhour -= 12;*/

        if(ampm == Calendar.AM){
            AMPM = "AM";
        }else{
            AMPM = "PM";
            if(mhour==0)
                mhour = 12;
        }

        return addZero(mhour)+":"+addZero(mMin)+" "+AMPM;
//        return addZero(mYear)+"/"+addZero(mMonth)+"/"+addZero(mDay)+" "+addZero(mhour)+":"+addZero(mMin)+":"+addZero(mSec);

    }

    public ArrayList<LatLng> getLocationPointsToDrawOnMap(int sessionId) {

        Log.d(TAG, "[test show trip] getLocationPointsToDrawOnMap ");

        ArrayList<LatLng> points = new ArrayList<LatLng>();

        //get data from the database
        ArrayList<String> data = DataHandler.getDataBySession(sessionId, DBHelper.STREAM_TYPE_LOCATION);
        Log.d(TAG, "[test show trip] getLocationPointsToDrawOnMap  get data:" + data.size() + "rows");

        for (int i=0; i<data.size(); i++){

            String[] record = data.get(i).split(Constants.DELIMITER);

            //TODO: need to get lat and lng from the Data JSON Object from LocationRecord

//            get location parameters
            double lat = Double.parseDouble(record[2] );
            double lng = Double.parseDouble(record[3] );

            points.add(new LatLng(lat, lng));
            // Log.d(LOG_TAG, "[showRecordingVizualization] lat ( " + lat + ", " + lng + " )"  );

        }

        return points;
    }

    //use Asynk task to load sessions
    private class LoadDataAsyncTask extends AsyncTask<Integer, Void, ArrayList<LatLng>> {
        private final ProgressDialog dialog = new ProgressDialog(AnnotateSessionActivity.this);

        @Override
        protected ArrayList<LatLng> doInBackground(Integer... params) {

            int sessionId = params[0];
            ArrayList<LatLng> points = getLocationPointsToDrawOnMap(sessionId);

//            Log.d(TAG, "[test show trip] in doInBackground, after query data using session id "+ sessionId);

            return points;
        }

        // can use UI thread here
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "[test show trip] onPreExecute ");
//
//            this.dialog.setMessage("Loading...");
//            this.dialog.show();
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(ArrayList<LatLng> points) {
            super.onPostExecute(points);

            Log.d(TAG, "[test show trip] in onPostExecute, the poitns obtained are : " + points.size());
            if (points.size()>0){
                LatLng startLatLng  = points.get(0);
                LatLng endLatLng = points.get(points.size()-1);
                LatLng middleLagLng = points.get((points.size()/2));

                Log.d(TAG, "[test show trips] the session is not in the currently recording session");
                //we first need to know what visualization we want to use, then we get data for that visualization

                //show maps with path (draw polylines)
                showMapWithPaths(mGoogleMap, points, middleLagLng, startLatLng, endLatLng);
            }

//            if (this.dialog.isShowing()) {
//                this.dialog.dismiss();
//            }
        }




    }



//    private class LoadRecordInSessionAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {
//
//        private final ProgressDialog dialog = new ProgressDialog(AnnotateSessionActivity.this);
//
//        @Override
//        protected void onPreExecute() {
//            Log.d(TAG,"onPreExecute");
//            this.dialog.setMessage("Loading...");
//            this.dialog.setCancelable(false);
//            this.dialog.show();
//        }
//
//        @Override
//        protected ArrayList<String> doInBackground(String... params) {
//            String sessionkey = params[0];
//
//            Log.d(TAG,"sessionkey : " + sessionkey);
//
////            String timedata[] = sessionkey.split("-");
////            String startTime = timedata[0];
//
////            Log.d(TAG,"startTime : " + startTime);
//
//            //TODO use freaking sessionkey to get data. First, get the session id by the sessionkey.
//
//            ArrayList<LatLng> locationDataRecords = null;
//
//            ArrayList<String> resultBySession = null;
//
//            try {
//                sessionid = Integer.valueOf(sessionkey);
//                resultBySession = DBHelper.queryRecordsInSession(DBHelper.STREAM_TYPE_LOCATION, sessionid);
//
////                Log.d(TAG,"result after query in doInBagrkound" + resultBySession.toString());
//
//            }catch (Exception e) {
//                locationDataRecords = new ArrayList<LatLng>();
//                Log.d(TAG,"Exception");
//                e.printStackTrace();
//            }
//
//
//            return resultBySession;
//
//        }
//
//
//
//        private String getSessionIdByTimekey(String timekey){
//            String sessionid = null;
//
//            try {
//                SQLiteDatabase db = DBManager.getInstance().openDatabase();
//                Cursor tripCursor = db.rawQuery("SELECT "+ DBHelper.sessionid_col +" FROM " + DBHelper.trip_table +
//                        " WHERE "+ DBHelper.TIME+ " ='"+ timekey + "'", null);
//                Log.d(TAG,"SELECT "+ DBHelper.sessionid_col +" FROM " + DBHelper.trip_table +
//                        " WHERE "+ DBHelper.TIME+ " ='"+ timekey + "'");
//                int rows = tripCursor.getCount();
//
//                tripCursor.moveToFirst();
//
//                if(rows!=0){
//                    Log.d(TAG,"tripCursor.getString(0) : "+ tripCursor.getString(0));
//                    sessionid = tripCursor.getString(0);
//                    Log.d(TAG, "sessionid : "+sessionid);
//                }else
//                    Log.d(TAG, "rows==0");
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//
//            String sessionids[] = sessionid.split(", ");
//
//            Log.d(TAG, "sessionids 1 : " + sessionids[1]);
//            return sessionids[1];
//        }
//
//
//        // onPostExecute displays the results of the AsyncTask.
//        @Override
//        protected void onPostExecute(ArrayList<String> result) {
//
//            super.onPostExecute(result);
//
//            if (this.dialog.isShowing()) {
//                this.dialog.dismiss();
//            }
//        }
//
//    }

}
