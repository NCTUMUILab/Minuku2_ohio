package edu.ohio.minuku_2.controller.Ohio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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

import edu.ohio.minuku.DBHelper.DBHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/7/5.
 */

public class annotateohio extends Activity implements OnMapReadyCallback {

    private final String TAG = "annotateohio";

    public static float GOOGLE_MAP_DEFAULT_ZOOM_LEVEL = 13;
    public static LatLng GOOGLE_MAP_DEFAULT_CAMERA_CENTER = new LatLng(24, 120);

    private int sessionid;

    private String sessionkey;
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

    private ArrayList<LatLng> latLngs;
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

        latLngs = new ArrayList<LatLng>();

        error1 = false;
        error2 = false;
        error3 = false;
        error4 = false;

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            Log.d(TAG, " onKeyDown");

            annotateohio.this.finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG,"onResume");

        bundle = getIntent().getExtras();

        initannotateohio();
    }

    public void initannotateohio(){

//        sessionkey = bundle.getString("timekey_id");
        sessionkey = bundle.getString("sessionkey_id");
        Log.d(TAG,"timekey_id : " + sessionkey);
        try{

            Log.d(TAG,"ListRecordAsyncTask");

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
//                latLngs = new AnnotateRecordAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sessionkey).get();
//            else
//                latLngs = new AnnotateRecordAsyncTask().execute(sessionkey).get();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                results = new AnnotateRecordAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sessionkey).get();
            else
                results = new AnnotateRecordAsyncTask().execute(sessionkey).get();

            Log.d(TAG,"locationDataRecords = new ListRecordAsyncTask().execute().get();");

        }catch(InterruptedException e) {
            Log.d(TAG,"InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            Log.d(TAG,"ExecutionException");
            e.printStackTrace();
        }

        for(int index = 0;index < results.size(); index++){
            String[] separated = results.get(index).split(Constants.DELIMITER);
            double lat = Double.valueOf(separated[2]);
            double lng = Double.valueOf(separated[3]);

            LatLng latLng = new LatLng(lat, lng);
            latLngs.add(latLng);
        }


        Log.d(TAG,"latLngs : " + latLngs);
        showRecordingVizualization();

        time = (TextView)findViewById(R.id.Time);
        String timeketForShow = null;
//        String[] timekeys = sessionkey.split("-");
//        String start = timekeys[0];
//        String end = timekeys[1];
        start = results.get(0).split(Constants.DELIMITER)[1];
        end = results.get(results.size()-1).split(Constants.DELIMITER)[1];
        start = getmillisecondToDateWithTime(Long.valueOf(start));
        end = getmillisecondToDateWithTime(Long.valueOf(end));
        timeketForShow = start+"-"+end;
        time.setText("Time : "+timeketForShow);

        /*
        activityspinner = (Spinner)findViewById(R.id.activityspinner);
        preplanspinner = (Spinner)findViewById(R.id.preplanspinner);
        tripspinner = (Spinner)findViewById(R.id.tripspinner);

        ArrayAdapter<String> activityList = new ArrayAdapter<String>(annotateohio.this,
                android.R.layout.simple_spinner_dropdown_item,
                activityString);
        ArrayAdapter<String> preplanList = new ArrayAdapter<String>(annotateohio.this,
                android.R.layout.simple_spinner_dropdown_item,
                preplanString);
        ArrayAdapter<String> tripList = new ArrayAdapter<String>(annotateohio.this,
                android.R.layout.simple_spinner_dropdown_item,
                tripString);

        activityspinner.setAdapter(activityList);
        preplanspinner.setAdapter(preplanList);
        tripspinner.setAdapter(tripList);
        */

        initQuestionnaire();

        submit = (Button)findViewById(R.id.submit);
        submit.setOnClickListener(submitting);

        if(ongoing.equals("true")){
            submit.setClickable(false);

            Toast.makeText(getApplicationContext(), "This trip is still ongoing!", Toast.LENGTH_LONG).show();
        }

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
        String sessionid = getSessionIdByTimekey(sessionkey);
        ongoing = "";
        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor tripCursor = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ (sessionid) + "'", null); //cause sessionid start from 0.
            Log.d(TAG,"SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ (sessionid) + "'");
            int rows = tripCursor.getCount();

            if(rows!=0){
                //get the first one is that it must be set with the accurate status
                tripCursor.moveToFirst();
                ongoing = tripCursor.getString(8);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        Log.d(TAG, "ongoing : "+ ongoing);

        if(ongoing.equals("true")){
            setRG_notclickable(ques1);
            setRG_notclickable(ques2_1);
            setRG_notclickable(ques2_2);
            setRG_notclickable(ques2_3);
            setRG_notclickable(ques2_4);
            setRG_notclickable(ques2_5);
            setRG_notclickable(ques3);
            setRG_notclickable(ques4);
        }

    }

    private void setRG_notclickable(RadioGroup ques){
        for (int i = 0; i < ques.getChildCount(); i++) {
            ques.getChildAt(i).setEnabled(false);
        }
    }

    private String getSessionIdByTimekey(String timekey){
        Log.d(TAG, "getSessionIdByTimekey");

        timekey = timekey.split("-")[0];

        String sessionid = null;

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor tripCursor = db.rawQuery("SELECT "+ DBHelper.sessionid_col +" FROM " + DBHelper.trip_table +
                    " WHERE "+ DBHelper.TIME+ " ='"+ timekey + "'", null);
            Log.d(TAG,"SELECT "+ DBHelper.sessionid_col +" FROM " + DBHelper.trip_table +
                    " WHERE "+ DBHelper.TIME+ " ='"+ timekey + "'");
            int rows = tripCursor.getCount();

            tripCursor.moveToFirst();

            if(rows!=0){
                Log.d(TAG,"tripCursor.getString(0) : "+ tripCursor.getString(0));
                sessionid = tripCursor.getString(0);
                Log.d(TAG, "sessionid : "+sessionid);
            }else
                Log.d(TAG, "rows==0");

        }catch (Exception e){
            e.printStackTrace();
        }

        Log.d(TAG, "sessionid : " + sessionid);

        String sessionids[] = sessionid.split(", ");

        Log.d(TAG, "sessionids 1 : " + sessionids[1]);
        return sessionids[1];
    }

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

    private void showRecordingVizualization(){

        Log.d(TAG,"showRecordingVizualization");

//        setUpMapIfNeeded();

        Log.d(TAG,"mGoogleMap"+ mGoogleMap);

        ((MapFragment) getFragmentManager().findFragmentById(R.id.Mapfragment)).getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {

                mGoogleMap = googleMap;

                LatLng nkut = latLngs.get(0);
                LatLng end = latLngs.get(latLngs.size() - 1);
                LatLng middle = latLngs.get(latLngs.size() / 2);

                Log.d(TAG, "nkut : "+nkut+"end : "+end+"middle : "+middle);

                int i = 1;

                for (LatLng latLng:latLngs) {
                    Log.d(TAG, "latLng"+ i +" : "+latLng);
                    i++;
                }
/*
                ArrayList<LatLng> points = new ArrayList<LatLng>();
                points.add(nkut);
                points.add(middle);
                points.add(end);
*/

                mGoogleMap.addMarker(new MarkerOptions().position(nkut).title("Start"))
                        .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                mGoogleMap.addMarker(new MarkerOptions().position(end).title("End"));

//                mGoogleMap.addMarker(new MarkerOptions().position(nkut).title("Here you are."));
//                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(middle,15.0f));
                showMapWithPaths(mGoogleMap, latLngs, middle);
            }
        });


        /*if(mGoogleMap!=null) {
//            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(GOOGLE_MAP_DEFAULT_CAMERA_CENTER, GOOGLE_MAP_DEFAULT_ZOOM_LEVEL));

            LatLng nkut = latLngs.get(0);
            LatLng end = latLngs.get(latLngs.size() - 1);
            LatLng middle = latLngs.get(latLngs.size() / 2);

            Log.d(TAG, "nkut : "+nkut+"end : "+end+"middle : "+middle);

            ArrayList<LatLng> points = new ArrayList<LatLng>();
            points.add(nkut);
            points.add(middle);
            points.add(end);
            mGoogleMap.addMarker(new MarkerOptions().position(nkut).title("Here you are."));
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nkut,15.0f));
            showMapWithPaths(mGoogleMap, points, middle);

        }*/
    }

    private void setUpMapIfNeeded() {

        Log.d(TAG,"setUpMapIfNeeded");

        if (mGoogleMap==null)
            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.Mapfragment));

//        mGoogleMap = map.getMap();
//        mGoogleMap.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

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

    public void showMapWithPaths(GoogleMap map, ArrayList<LatLng> points, LatLng cameraCenter) {

        Log.d(TAG, "showMapWithPaths");
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
                try {
                    for (LatLng latLng : latLngs) {
                        Log.d(TAG, "latLng" + i + " : " + latLng);

                        latdata.put(latLng.latitude);
                        lngdata.put(latLng.longitude);
                        i++;
                    }
                }catch (JSONException e) {
                    e.printStackTrace();
                }

//                String[] timekeys = sessionkey.split("-");
//                String start = timekeys[0];
//                String end = timekeys[1];
//                start = SessionManager.getmillisecondToDateWithTime(Long.valueOf(start));
//                end = SessionManager.getmillisecondToDateWithTime(Long.valueOf(end));

                Log.d(TAG, "sessionkey : "+ sessionkey);

                addToDB(start, end,
                        ans1, ans2, ans3, ans4, latdata, lngdata);

                annotateohio.this.finish();
            }else
                Toast.makeText(annotateohio.this,"Please complete all the questions!!",Toast.LENGTH_SHORT).show();

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

    private void addToDB(String starttime, String endtime, String ans1, String ans2, String ans3
            , String ans4, JSONArray latdata, JSONArray lngdata){
        Log.d(TAG, "addToDB");

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

        Session session = new Session(Integer.valueOf(sessionkey));

        session.addAnnotation(annotation);

        DBHelper.updateSessionTable(session);

/*

        try{
//            String startimekey[] = sessionkey.split("-");
            db.delete(DBHelper.annotate_table,DBHelper.Trip_startTime+" = '"+starttime+"' ",null);
        }catch (NullPointerException e){
            Log.d(TAG,"No data yet.");
            e.printStackTrace();
        }

        try {
            values.put(DBHelper.TIME, sessionkey);
//            values.put(DBHelper.USERID, sessionid);
//            values.put(DBHelper.DEVICE, Constants.DEVICE_ID);
            values.put(DBHelper.Trip_id, sessionid);
//            values.put(DBHelper.Ques_Ans, latLngdata.toString());
            values.put(DBHelper.Trip_startTime, starttime);
            Log.d(TAG," starttime :" + starttime);
            values.put(DBHelper.Trip_endTime, endtime);
            Log.d(TAG," endtime :" + endtime);
//            values.put(DBHelper.activityType, activityType);
//            values.put(DBHelper.preplan, preplan);
            values.put(DBHelper.ans1, ans1);
            values.put(DBHelper.ans2, ans2);
            values.put(DBHelper.ans3, ans3);
            values.put(DBHelper.ans4, ans4);
            values.put(DBHelper.lat, latdata.toString());
            values.put(DBHelper.lng, lngdata.toString());
//            values.put(DBHelper.Trip_startTimeSecond, starttime);

            db.insert(DBHelper.annotate_table, null, values);
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
        finally {
            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection
        }
*/

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
    
    /*private void addToDB(JSONObject data){

        Log.d(TAG, "addToDB");

        ContentValues values = new ContentValues();

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            values.put(DBHelper.TIME, sessionkey);
//            values.put(DBHelper.USERID, sessionid);
//            values.put(DBHelper.DEVICE, Constants.DEVICE_ID);
            values.put(DBHelper.Trip_id, sessionid);
            values.put(DBHelper.Ques_Ans, data.toString());

            db.insert(DBHelper.annotate_table, null, values);
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
        finally {
            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection
        }
    }*/

    private class AnnotateRecordAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {

        private final ProgressDialog dialog = new ProgressDialog(annotateohio.this);

        @Override
        protected void onPreExecute() {
            Log.d(TAG,"onPreExecute");
            this.dialog.setMessage("Loading...");
            this.dialog.setCancelable(false);
            this.dialog.show();
        }

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            String sessionkey = params[0];

            Log.d(TAG,"sessionkey : " + sessionkey);

//            String timedata[] = sessionkey.split("-");
//            String startTime = timedata[0];

//            Log.d(TAG,"startTime : " + startTime);

            //TODO use freaking sessionkey to get data. First, get the session id by the sessionkey.

            Log.d(TAG, "listRecordAsyncTask going to list recording");

            ArrayList<LatLng> locationDataRecords = null;

            ArrayList<String> resultBySession = null;

            try {
//                sessionid = Integer.valueOf(getSessionIdByTimekey(startTime));

                sessionid = Integer.valueOf(sessionkey);

//                sessionid = Integer.valueOf(getSessionIdByTimekey(SessionManager.getSpecialTimeInMillis(startTime)));
//                locationDataRecords = LocationDataRecordDAO.getTripLocToDrawOnMap(sessionkey);
//                sessionid = bundle.getInt("position_id");

                resultBySession = DBHelper.queryRecordsInSession(DBHelper.location_table, sessionid);

                /*for(String eachrow : resultBySession){
                    String[] separated = eachrow.split(Constants.DELIMITER);
                    double lat = Double.valueOf(separated[2]);
                    double lng = Double.valueOf(separated[3]);

                    LatLng latLng = new LatLng(lat, lng);
                    locationDataRecords.add(latLng);
                }*/

//                locationDataRecords = SessionManager.getInstance().getTripLocToDrawOnMap(sessionid);

            }catch (Exception e) {
                locationDataRecords = new ArrayList<LatLng>();
                Log.d(TAG,"Exception");
                e.printStackTrace();
            }

            Log.d(TAG,"sessionid in doInBackground : " + sessionid);
            Log.d(TAG,"AnnotateRecordAsyncTask : " + locationDataRecords);

            return resultBySession;

        }

        private String getSessionIdByTimekey(String timekey){
            String sessionid = null;

            try {
                SQLiteDatabase db = DBManager.getInstance().openDatabase();
                Cursor tripCursor = db.rawQuery("SELECT "+ DBHelper.sessionid_col +" FROM " + DBHelper.trip_table +
                        " WHERE "+ DBHelper.TIME+ " ='"+ timekey + "'", null);
                Log.d(TAG,"SELECT "+ DBHelper.sessionid_col +" FROM " + DBHelper.trip_table +
                        " WHERE "+ DBHelper.TIME+ " ='"+ timekey + "'");
                int rows = tripCursor.getCount();

                tripCursor.moveToFirst();

                if(rows!=0){
                    Log.d(TAG,"tripCursor.getString(0) : "+ tripCursor.getString(0));
                    sessionid = tripCursor.getString(0);
                    Log.d(TAG, "sessionid : "+sessionid);
                }else
                    Log.d(TAG, "rows==0");

            }catch (Exception e){
                e.printStackTrace();
            }

            String sessionids[] = sessionid.split(", ");

            Log.d(TAG, "sessionids 1 : " + sessionids[1]);
            return sessionids[1];
        }


        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(ArrayList<String> result) {

            super.onPostExecute(result);

            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
        }

    }

}
