package edu.ohio.minuku_2.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Data.DataHandler;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/7/5.
 */

public class AnnotateSessionActivity extends Activity implements OnMapReadyCallback {

    private final String TAG = "AnnotateSessionActivity";

    private int mSessionId;
    private Session mSession;
    private TextView time;
    private RadioGroup ques1, ques2, ques3, ques4;

    private EditText optionalNote;

    private String ans1, ans2, ans3, ans4;

    private boolean combineOrDelete, IsSplitLocationChosen;

    private HashMap<Integer, Marker> addedSplitMarker;
    private int currentMarkerKey;

    private GoogleMap mGoogleMap;

    private LatLng splittingLatlng = new LatLng(-999, -999);
    private long splittingTime = -9999;

    private Button submit, combine, delete, split;
    private ArrayList<RadioGroup> mRadioGroups;
    private Bundle bundle;

    private ArrayList<Long> mESMOpenTimes;
    private long mESMSubmitTime = 0;

    private SharedPreferences sharedPrefs;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.annotate_activity_ohio);

        combineOrDelete = false;
        IsSplitLocationChosen = false;
        addedSplitMarker = new HashMap<>();
        currentMarkerKey = -1;

        mESMOpenTimes  = new ArrayList<Long>();
        mRadioGroups = new ArrayList<RadioGroup>();

        bundle = getIntent().getExtras();

        sharedPrefs = getSharedPreferences(Constants.sharedPrefString, MODE_PRIVATE);

        mSessionId = Integer.parseInt(bundle.getString("sessionkey_id"));

        Log.d(TAG,"[test show trip] on create session id: : " + mSessionId);

        sharedPrefs.edit().putInt("annotationSessionid", mSessionId).apply();

        mSession = SessionManager.getSession(mSessionId);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            //Log.d(TAG, " onKeyDown");

            AnnotateSessionActivity.this.finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy");

        //TODO reinitialized the sessionid due to leaving the page
        sharedPrefs.edit().putInt("annotationSessionid", Constants.INVALID_IN_INT).apply();

    }

    @Override
    public void onResume() {
        super.onResume();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.Mapfragment);
        mapFragment.getMapAsync(this);

        //open the ESM. record the time
        mESMOpenTimes.add(ScheduleAndSampleManager.getCurrentTimeInMillis()) ;

        mSessionId = sharedPrefs.getInt("annotationSessionid",  Constants.INVALID_IN_INT);

        mSession = SessionManager.getSession(mSessionId);
        //Log.d(TAG, "[test show trip] loading session " + mSession.getId() + " the annotations are " + mSession.getAnnotationsSet().toJSONObject().toString());

        initAnnotationView();
    }

    private void triggerAlertDialog(){

        final LayoutInflater inflater = LayoutInflater.from(AnnotateSessionActivity.this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(AnnotateSessionActivity.this);
        final View layout = inflater.inflate(R.layout.splitedmap_dialog,null);

        builder.setView(layout)
                .setPositiveButton(getResources().getString(R.string.confirm), null)
                .setNegativeButton(getResources().getString(R.string.cancel), null);

        final AlertDialog mAlertDialog = builder.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface dialogInterface) {

                showMapInDialog(mAlertDialog, layout);

                Log.d(TAG, "splittingLatlng : " + splittingLatlng);

                Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Confirm split - sessionid : "+ mSessionId);

                        if(IsSplitLocationChosen) {

                            try {

                                //TODO add the information to show they are from the same trip.
                                //keep the original one hided
                                DBHelper.hideSessionTable(mSessionId, Constants.SESSION_TYPE_CHANGED);

                                //update the session into two different sessions
                                Session lastSession = SessionManager.getLastSession();
                                int sessionCount = lastSession.getId();

                                long originStartTime = mSession.getStartTime();
                                long originEndTime = mSession.getEndTime();

                                Log.d(TAG, "originStartTime : "+ScheduleAndSampleManager.getTimeString(originStartTime));
                                Log.d(TAG, "originEndTime : "+ScheduleAndSampleManager.getTimeString(originEndTime));
                                Log.d(TAG, "splittingTime : "+ScheduleAndSampleManager.getTimeString(splittingTime));

                                //1st session
                                Session firstSession = mSession;
                                firstSession.setCreatedTime(ScheduleAndSampleManager.getCurrentTimeInMillis() / Constants.MILLISECONDS_PER_SECOND);
                                firstSession.setStartTime(originStartTime);
                                firstSession.setEndTime(splittingTime);
                                firstSession.setLongEnough(true);
                                firstSession.setId(sessionCount + 1);
                                firstSession.setToShow(true);
                                firstSession.setReferenceId(String.valueOf(mSessionId));
                                firstSession.setType(Constants.SESSION_TYPE_SPLIT);

//                                DBHelper.updateSessionTable(mSessionId, mSession.getStartTime(), splittingTime);
                                DBHelper.insertSessionTable(firstSession);
                                DBHelper.updateRecordsInSessionBeforeSplit(DBHelper.STREAM_TYPE_LOCATION, splittingTime, mSessionId, firstSession.getId());

                                //2nd session
                                Session secondSession = mSession;
                                secondSession.setCreatedTime(ScheduleAndSampleManager.getCurrentTimeInMillis() / Constants.MILLISECONDS_PER_SECOND);
                                secondSession.setStartTime(splittingTime);
                                secondSession.setEndTime(originEndTime);
                                secondSession.setLongEnough(true);
                                secondSession.setId(sessionCount + 2);
                                secondSession.setToShow(true);
                                secondSession.setReferenceId(String.valueOf(mSessionId));
                                secondSession.setType(Constants.SESSION_TYPE_SPLIT);

                                DBHelper.insertSessionTable(secondSession);

                                //update session locations' session id,
                                //set the time after splittingTime to the old ids concatenating with the new id
                                DBHelper.updateRecordsInSession(DBHelper.STREAM_TYPE_LOCATION, splittingTime, mSessionId, secondSession.getId());
                            } catch (ArrayIndexOutOfBoundsException e) {

                            }

                            Toast.makeText(AnnotateSessionActivity.this, getResources().getString(R.string.reminder_trip_split_successfully), Toast.LENGTH_SHORT).show();

                            dialogInterface.dismiss();

                            AnnotateSessionActivity.this.finish();
                        }else {

                            Toast.makeText(AnnotateSessionActivity.this, getResources().getString(R.string.reminder_to_split_trip_by_clicking_map), Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        });

        mAlertDialog.show();
    }

    private void showMapInDialog(Dialog dialog, final View view){

        MapView mapView = (MapView) view.findViewById(R.id.mapView);
        MapsInitializer.initialize(this);

        mapView.onCreate(dialog.onSaveInstanceState());
        mapView.onResume();
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {

                if (mSession!=null) {

                    showRecordingVizualization((int) mSession.getId(), googleMap);

                    ArrayList<LatLng> points = new ArrayList<>();

                    try{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            points = new LoadDataAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSession.getId()).get();
                        else
                            points = new LoadDataAsyncTask().execute(mSession.getId()).get();

                    } catch(InterruptedException e) {

                    } catch (ExecutionException e) {

                    }

                    Log.d(TAG, "[test show trip] in onPostExecute, the poitns obtained are : " + points.size());
                    if (points.size()>0){

                        LatLng startLatLng  = points.get(0);
                        LatLng endLatLng = points.get(points.size()-1);
                        LatLng middleLagLng = points.get((points.size()/2));

                        Log.d(TAG, "[test show trips] the session is not in the currently recording session");

                        //we first need to know what visualization we want to use, then we get data for that visualization
                        //show maps with path (draw polylines)
                        showMapWithPaths(googleMap, points, middleLagLng, startLatLng, endLatLng);
                    }

                    googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {

                            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Annotation Map - sessionid : "+ mSessionId);

                            //remove the current marker
                            if(currentMarkerKey != -1){

                                try{

                                    Marker currentMarker = addedSplitMarker.get(currentMarkerKey);
                                    currentMarker.remove();
                                    addedSplitMarker.remove(currentMarkerKey);
                                }catch (Exception e){

                                }
                            }

                            Pair<Long, LatLng> closestLocationAndTime = getSplitTimeAndClosestLocation(latLng);

                            splittingTime = closestLocationAndTime.first;
                            splittingLatlng = closestLocationAndTime.second;

                            Marker marker = googleMap.addMarker(new MarkerOptions()
                                    .position(splittingLatlng)
                                    .draggable(true).visible(true));;

                            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

                            currentMarkerKey++;
                            addedSplitMarker.put(currentMarkerKey, marker);

                            IsSplitLocationChosen = true;
                        }
                    });

                }
            }
        });

    }

    private Pair<Long, LatLng> getSplitTimeAndClosestLocation(final LatLng latLng){

        ArrayList<String> data = DataHandler.getDataBySession(mSession.getId(), DBHelper.STREAM_TYPE_LOCATION);
        Log.d(TAG, "[test show trip] get data id: " + mSession.getId());

        Log.d(TAG, "[test show trip] get data: " + data.size() + " rows");

        long chosenTime = -999;

        double cutpointLat = latLng.latitude;
        double cutpointLng = latLng.longitude;
        double shortestDist = -1;

        double chosenLat = cutpointLat, chosenLng = cutpointLng;

        for (int i=0; i < data.size(); i++){

            String[] record = data.get(i).split(Constants.DELIMITER);

            double lat = Double.parseDouble(record[2]);
            double lng = Double.parseDouble(record[3]);

            float[] results = new float[1];
            Location.distanceBetween(cutpointLat, cutpointLng, lat, lng, results);

            if(shortestDist < 0){

                shortestDist = results[0];
                chosenLat = lat;
                chosenLng = lng;
                chosenTime = Long.valueOf(record[1]);
            }else{

                Location.distanceBetween(cutpointLat, cutpointLng, lat, lng, results);
                double currDist = results[0];

                if(shortestDist > currDist){

                    shortestDist = currDist;
                    chosenLat = lat;
                    chosenLng = lng;
                    chosenTime = Long.valueOf(record[1]);
                }
            }
        }

        final LatLng chosenLatlng = new LatLng(chosenLat, chosenLng);

        Pair<Long, LatLng> pair = new Pair<>(chosenTime, chosenLatlng);


        return pair;
    }

    public void initAnnotationView(){

        initQuestionnaire();

    }

    private void showRecordingVizualization(final int sessionId, GoogleMap mGoogleMap){

        //draw map
        if (mGoogleMap!=null){

            //if we're reviewing a previous session ( the trip is not ongoing), get session from the database (note that we have to use session id to check instead of a session instance)
            if (!SessionManager.isSessionOngoing(sessionId)) {

                //because there could be many points for already ended trace, so we use asynch to download the annotations

                try{

                    ArrayList<LatLng> points;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        points = new LoadDataAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sessionId).get();
                    else
                        points = new LoadDataAsyncTask().execute(sessionId).get();

                    if (points.size()>0){

                        LatLng startLatLng  = points.get(0);
                        LatLng endLatLng = points.get(points.size()-1);
                        LatLng middleLagLng = points.get((points.size()/2));

                        Log.d(TAG, "[test show trips] the session is not in the currently recording session");
                        //we first need to know what visualization we want to use, then we get data for that visualization

                        //show maps with path (draw polylines)
                        showMapWithPaths(mGoogleMap, points, middleLagLng, startLatLng, endLatLng);
                    }

                } catch(InterruptedException e) {

                } catch (ExecutionException e) {

                }
            }
            //the recording is ongoing, so we periodically query the database to show the latest path
            else {

                try{

                    //get location points to draw on the map..
                    ArrayList<LatLng> points = getLocationPointsToDrawOnMap(sessionId);

                    //we use endLatLng, which is the user's current location as the center of the camera
                    LatLng startLatLng, endLatLng;

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

                }

            }

        }

    }

    public void showMapWithPaths(GoogleMap map, ArrayList<LatLng> points, LatLng cameraCenter, LatLng startLatLng, LatLng endLatLng) {

        //map option
        GoogleMapOptions options = new GoogleMapOptions();
        options.tiltGesturesEnabled(false);
        options.rotateGesturesEnabled(false);

        //center the map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraCenter, 13));

        //draw linges between points and add end and start points
        PolylineOptions pathPolyLineOption = new PolylineOptions().color(Color.RED).geodesic(true);
        pathPolyLineOption.addAll(points);

        //draw lines
        Polyline path = map.addPolyline(pathPolyLineOption);

        //after getting the start and ened point of location trace, we put a marker
        map.addMarker(new MarkerOptions().position(startLatLng).title("Start"))
                .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        map.addMarker(new MarkerOptions().position(endLatLng).title("End"));

    }

    public void showMapWithPathsAndCurLocation(GoogleMap map, ArrayList<LatLng> points, LatLng curLoc) {

        map.clear();

        //map option
        GoogleMapOptions options = new GoogleMapOptions();
        options.tiltGesturesEnabled(false);
        options.rotateGesturesEnabled(false);

        //get current zoom level
        float zoomlevel = map.getCameraPosition().zoom;

        //center the map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(curLoc, 13));

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

    private void initQuestionnaire()  {

        //tell if the session has been labeled. It is in the annotaiton with ESM tag
        ArrayList<Annotation> annotations = mSession.getAnnotationsSet().getAnnotationByTag("ESM");
        String labelStr = "No Label";

        JSONObject ESMJSON=null;

        //{"Entire_session":true,"Tag":["ESM"],"Content":"{\"ans1\":\"Walking outdoors.\",\"ans2\":\"Food\",\"ans3\":\"No\",\"ans4\":\"Right before\"}"}
        if (annotations.size()>0){

            try {

                String content = annotations.get(0).getContent();

                ESMJSON = new JSONObject(content);

                labelStr = ESMJSON.getString("ans1");
            } catch (JSONException e) {

            }
        }

        combine = (Button)findViewById(R.id.combine);
        combine.setOnClickListener(combining);

        delete = (Button)findViewById(R.id.delete);
        delete.setOnClickListener(deleting);

        split = (Button)findViewById(R.id.split);
        split.setOnClickListener(splitting);

        combine.setEnabled(true);
        split.setEnabled(true);
        delete.setEnabled(true);

        submit = (Button)findViewById(R.id.submit);
        submit.setOnClickListener(submitting);

        ques1 = (RadioGroup)findViewById(R.id.ques1);
        ques2 = (RadioGroup)findViewById(R.id.ques2);
        ques3 = (RadioGroup)findViewById(R.id.ques3);
        ques4 = (RadioGroup)findViewById(R.id.ques4);

        mRadioGroups.add(ques1);
        mRadioGroups.add(ques2);
        mRadioGroups.add(ques3);
        mRadioGroups.add(ques4);

        optionalNote = (EditText)findViewById(R.id.optionalNote);
        optionalNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "EditText - click - Optional Note - sessionid : "+ mSessionId);
            }
        });

        optionalNote.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "EditText - onTextChanged - Optional Note - sessionid : "+ mSessionId);
            }
        });


        ques1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Ques1 - Ans"+(checkedId+1)+" - sessionid : "+ mSessionId);

                if(checkedId==-1){

                    combineOrDelete = false;
                }else{

                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans1 = radioButton.getText().toString();
                }
            }
        });

        ques2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Ques2 - Ans"+(checkedId+1)+" - sessionid : "+ mSessionId);

                if(checkedId==-1){

                }else{

                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans2 = radioButton.getText().toString();
                }
            }
        });

        ques3.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Ques3 - Ans"+(checkedId+1)+" - sessionid : "+ mSessionId);

                if(checkedId==-1){

                }else{

                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans3 = radioButton.getText().toString();
                }

            }
        });

        ques4.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Ques4 - Ans"+(checkedId+1)+" - sessionid : "+ mSessionId);

                if(checkedId==-1){

                }else{

                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans4 = radioButton.getText().toString();
                }
            }
        });


        //if the session is ongoing or has been labeled.  we show the answer, and disable the buttons
        if (SessionManager.isSessionOngoing(mSession.getId()) || !labelStr.equals("No Label")){

            ArrayList<String > answers = new ArrayList<String>();
            //show the answer
            if (ESMJSON!=null){

                String showOptionalText = "";

                try {

                    answers.add(ESMJSON.getString("ans1"));
                    answers.add(ESMJSON.getString("ans2"));
                    answers.add(ESMJSON.getString("ans3"));
                    answers.add(ESMJSON.getString("ans4"));

                    showOptionalText = ESMJSON.getString("optionalNote");
                } catch (JSONException e) {

                }

                //show answer
                showAnswersInRadioGroup(answers);

                optionalNote.setText(showOptionalText);
            }

            //disable button
            setRadioGroupNotclickable();

            combine.setEnabled(false);
            delete.setEnabled(false);
            split.setEnabled(false);
            optionalNote.setEnabled(false);
        }
    }


    private void setRadioGroupNotclickable() {

        for (RadioGroup radioGroup: mRadioGroups){

            setRadioGroupNotclickable(radioGroup);
        }
    }



    private void showAnswersInRadioGroup(ArrayList<String > answers){

        for (String answer: answers){

            for (RadioGroup radioGroup: mRadioGroups){

                showAnswerInRadioGroup(radioGroup,answer);
            }
        }
    }

    private void showAnswerInRadioGroup(RadioGroup ques, String ans){

        //make them disabled
        for (int i = 0; i < ques.getChildCount(); i++) {

            RadioButton button = (RadioButton) ques.getChildAt(i);

            if (button.getText().toString().equals(ans)){

                button.setChecked(true);
            }
        }
    }


    private void setRadioGroupNotclickable(RadioGroup ques){

        //make them disabled
        for (int i = 0; i < ques.getChildCount(); i++) {

            ques.getChildAt(i).setEnabled(false);
        }
        submit.setEnabled(false);
    }

    private void setRadioGroupClickable(RadioGroup ques){

        //make them disabled
        for (int i = 0; i < ques.getChildCount(); i++) {

            ques.getChildAt(i).setEnabled(true);
        }
        submit.setEnabled(true);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        //Log.d(TAG,"test show trip onMapReady");

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

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_DATE_TEXT_HOUR_MIN_AMPM);

        if (endTime==0){
            time.setText( getResources().getString(R.string.page_annotate_session_duration_time) + ScheduleAndSampleManager.getTimeString(startTime, sdf) + getResources().getString(R.string.page_annotate_session_duration_time_till_unknown) );
        } else {

            if (SessionManager.isSessionOngoing(mSessionId))
                time.setText( getResources().getString(R.string.page_annotate_session_duration_time) + ScheduleAndSampleManager.getTimeString(startTime, sdf) + getResources().getString(R.string.page_annotate_session_duration_time_till_now)  );
            else
                time.setText( getResources().getString(R.string.page_annotate_session_duration_time) + ScheduleAndSampleManager.getTimeString(startTime, sdf) + " - " + ScheduleAndSampleManager.getTimeString(endTime, sdf)  );
        }

        //Log.d(TAG,"[test show trip] setting time label" + time.getText().toString());

        /** After we got a new session id, we retrieve and visualize data for the session**/
        if (session!=null) {

            showRecordingVizualization((int) session.getId(), mGoogleMap);
        }
    }


    private Button.OnClickListener submitting = new Button.OnClickListener() {
        public void onClick(View v) {

            //Log.d(TAG, "ans1 : "+ans1+" ans2 : "+ans2+" ans3 : "+ans3+" ans4 : "+ ans4 +" ans4_2 : "+ans4_2);

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Submit - sessionid : "+ mSessionId);

            if(isAllSelected() || combineOrDelete){

                updateSessionWithAnnotation(ans1, ans2, ans3, ans4, optionalNote.getText().toString());

                AnnotateSessionActivity.this.finish();
            }else
                Toast.makeText(AnnotateSessionActivity.this, getResources().getString(R.string.reminder_must_answer_all_question), Toast.LENGTH_SHORT).show();

        }
    };

    private Button.OnClickListener combining = new Button.OnClickListener() {
        public void onClick(View v) {

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Combine - sessionid : "+ mSessionId);

            Intent intent = new Intent(AnnotateSessionActivity.this, CombinationActivity.class);

            Bundle mBundle = new Bundle();
            mBundle.putInt("Sessionid", mSessionId);
            intent.putExtras(mBundle);

            startActivity(intent);

            AnnotateSessionActivity.this.finish();
        }
    };

    private Button.OnClickListener deleting = new Button.OnClickListener() {
        public void onClick(View v) {

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Delete - sessionid : "+ mSessionId);

            new AlertDialog.Builder(AnnotateSessionActivity.this)
                    .setTitle(getResources().getString(R.string.dialog_delete_title))
                    .setMessage(getResources().getString(R.string.dialog_delete_message))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Delete - Confirm - sessionid : "+ mSessionId);

                            //TODO still keep them and add a field to not show them
                            DBHelper.hideSessionTable(mSessionId, Constants.SESSION_TYPE_DELETED);
//                            DBHelper.deleteSessionTable(mSessionId);

                            //update the background data corresponding, -1 indicated that it was gone
//                            DBHelper.updateRecordsInSession(DBHelper.STREAM_TYPE_LOCATION, mSessionId, -1);

                            Toast.makeText(AnnotateSessionActivity.this, getResources().getString(R.string.reminder_trip_deleted), Toast.LENGTH_SHORT).show();

                            AnnotateSessionActivity.this.finish();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Delete - Cancel - sessionid : "+ mSessionId);

                        }
                    }).show();

        }
    };

    private Button.OnClickListener splitting = new Button.OnClickListener(){
        public void onClick(View v) {

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Split - sessionid : "+ mSessionId);

            triggerAlertDialog();
        }
    };

    private boolean isAllSelected(){

        return (ques1.getCheckedRadioButtonId() != -1) &&
                (ques2.getCheckedRadioButtonId() != -1) &&
                (ques3.getCheckedRadioButtonId() != -1) &&
                (ques4.getCheckedRadioButtonId() != -1);
    }


    private void updateSessionWithAnnotation(String ans1, String ans2, String ans3
            , String ans4, String optionalNote){
        //Log.d(TAG, "updateSessionWithAnnotation");

        //create annoation from the ESM
        Annotation annotation = new Annotation();

        JSONObject esmContentJSON = new JSONObject();

        mESMSubmitTime = ScheduleAndSampleManager.getCurrentTimeInMillis();

        try {

            esmContentJSON.put("openTimes", mESMOpenTimes.toString());
            esmContentJSON.put("submitTime", mESMSubmitTime);
            esmContentJSON.put("ans1", ans1);
            esmContentJSON.put("ans2", ans2);
            esmContentJSON.put("ans3", ans3);
            esmContentJSON.put("ans4", ans4);
            esmContentJSON.put("optionalNote", optionalNote);
        }catch (JSONException e){
            //e.printStackTrace();
        }
        annotation.setContent(esmContentJSON.toString());
        annotation.addTag("ESM");


        //Log.d(TAG,"[test show trip] session " + mSession.getId() +  " before add annotation the annotaitons are " + mSession.getAnnotationsSet());

        //add ESM response to annotation
        mSession.addAnnotation(annotation);

        //Log.d(TAG,"[test show trip] session " + mSession.getId() +  "after add annotation now the annotaitons are " + mSession.getAnnotationsSet());

        //update session with its annotation to the session table
        DBHelper.updateSessionTable(mSession.getId(), mSession.getEndTime(), mSession.getAnnotationsSet());
    }

    public ArrayList<LatLng> getLocationPointsToDrawOnMap(int sessionId) {

        ArrayList<LatLng> points = new ArrayList<>();

        //get data from the database
        ArrayList<String> data = DataHandler.getDataBySession(sessionId, DBHelper.STREAM_TYPE_LOCATION);
        Log.d(TAG, "[test show trip] getLocationPointsToDrawOnMap get data:" + data.size() + "rows");

        for (int i=0; i<data.size(); i++){

            String[] record = data.get(i).split(Constants.DELIMITER);

            double lat = Double.parseDouble(record[2]);
            double lng = Double.parseDouble(record[3]);

            points.add(new LatLng(lat, lng));
        }

        return points;
    }

    //use Asynk task to load sessions
    private class LoadDataAsyncTask extends AsyncTask<Integer, Void, ArrayList<LatLng>> {

        @Override
        protected ArrayList<LatLng> doInBackground(Integer... params) {

            int sessionId = params[0];
            ArrayList<LatLng> points = getLocationPointsToDrawOnMap(sessionId);

            return points;
        }

        // can use UI thread here
        @Override
        protected void onPreExecute() {
            //Log.d(TAG, "[test show trip] onPreExecute ");
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(ArrayList<LatLng> points) {
            super.onPostExecute(points);

            Log.d(TAG, "[test show trip] in onPostExecute, the poitns obtained are : " + points.size());
        }

    }

}
