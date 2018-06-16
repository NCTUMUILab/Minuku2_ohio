package edu.ohio.minuku_2.controller;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Data.DataHandler;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
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

    private int mSessionId;
    private Session mSession;
    private TextView time, question3, question4;
    private RadioButton ans3_1, ans3_2, ans3_3;
    private RadioButton ans4_1, ans4_2, ans4_3;
    private RadioGroup ques1,
            ques2,
            ques2_1, ques2_2, ques2_3, ques2_4, ques2_5,
            ques3, ques4;

    private String ans1, ans2, ans3, ans4, activityType, tripType; //, preplan;
    private boolean error1, error2, error3, error4, error4_2;

    private GoogleMap mGoogleMap;

    private Button submit;
    private ArrayList<RadioGroup> mRadioGroups;
    private Bundle bundle;

    private ArrayList<Long> mESMOpenTimes;
    private long mESMSubmitTime = 0;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.annotate_activity_ohio);

        error1 = false;
        error2 = false;
        error3 = false;
        error4 = false;

        mESMOpenTimes  = new ArrayList<Long>();
        mRadioGroups = new ArrayList<RadioGroup>();

        //Log.d(TAG, "[test show trip] onCreate AnntoateSessionaACtivity");

        bundle = getIntent().getExtras();
        mSessionId = Integer.parseInt(bundle.getString("sessionkey_id"));

        //Log.d(TAG,"[test show trip] on create session id: : " + mSessionId);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.Mapfragment);

        mapFragment.getMapAsync(this);


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

    @Override
    public void onResume() {
        super.onResume();

        //open the ESM. record the time
        mESMOpenTimes.add(ScheduleAndSampleManager.getCurrentTimeInMillis()) ;

        mSession=null;

        mSession = SessionManager.getSession(mSessionId);
        //Log.d(TAG, "[test show trip] loading session " + mSession.getId() + " the annotaitons are " + mSession.getAnnotationsSet().toJSONObject().toString());

        initAnnotationView();

    }

    /**
     * this function initialize the view of the annotation acitvity, including quesitonnaire and map
     */
    public void initAnnotationView(){

        initQuestionnaire();

    }

    private void showRecordingVizualization(final int sessionId){

        //draw map
        if (mGoogleMap!=null){

            //if we're reviewing a previous session ( the trip is not ongoing), get session from the database (note that we have to use session id to check instead of a session instance)
            if (!SessionManager.isSessionOngoing(sessionId)) {

                //because there could be many points for already ended trace, so we use asynch to download the annotations

                try{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        new LoadDataAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sessionId).get();
                    else
                        new LoadDataAsyncTask().execute(sessionId).get();

                }catch(InterruptedException e) {

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
                    Log.e(TAG, "Could not unregister receiver " + e.getMessage()+"");
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


        ques2.clearCheck();


        question4 = (TextView)findViewById(R.id.question4);
        ans3_1 = (RadioButton)findViewById(R.id.ans3_1);
        ans3_2 = (RadioButton)findViewById(R.id.ans3_2);

        ans4_1 = (RadioButton)findViewById(R.id.ans4_1);
        ans4_2 = (RadioButton)findViewById(R.id.ans4_2);
        ans4_3 = (RadioButton)findViewById(R.id.ans4_3);


        ques1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if(checkedId==-1){

                    error1=false;
                }else{

                    error1=true;

                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans1 = radioButton.getText().toString();

                    RadioButton combineOption = (RadioButton) findViewById(R.id.ans1_9);
                    final String combine = combineOption.getText().toString();

                    RadioButton deleteOption = (RadioButton) findViewById(R.id.ans1_10);
                    final String delete = deleteOption.getText().toString();

                    if(ans1.equals(combine) || ans1.equals(delete)){

                        error2 = error3 = error4 = true;

                        //set the question below unavailable
                        setRadioGroupNotclickable(ques2);
                        setRadioGroupNotclickable(ques3);
                        setRadioGroupNotclickable(ques4);
                        submit.setEnabled(true);
                    }else {

                        error2 = error3 = error4 = false;

                        setRadioGroupClickable(ques2);
                        setRadioGroupClickable(ques3);
                        setRadioGroupClickable(ques4);
                    }
                }
            }
        });

        ques2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if(checkedId==-1){

                    error2=false;
                }else{

                    error2=true;
//                    ans3=String.valueOf(group.indexOfChild((RadioButton) findViewById(checkedId))) ;
                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans2 = radioButton.getText().toString();
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

                    RadioButton radioButton = (RadioButton) findViewById(checkedId);
                    ans3 = radioButton.getText().toString();
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

                try {

                    answers.add(ESMJSON.getString("ans1"));
                    answers.add(ESMJSON.getString("ans2"));
                    answers.add(ESMJSON.getString("ans3"));
                    answers.add(ESMJSON.getString("ans4"));
                } catch (JSONException e) {

                }

                //show answer
                showAnswersInRadioGroup(answers);
            }

            //disable button
            setRadioGroupNotclickable();
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

            RadioButton button =(RadioButton) ques.getChildAt(i);

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

            if(checkedId==-1){

                error2=false;
            }else{

                error2=true;

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

            if(checkedId==-1){

                error2=false;
            }else{

                error2=true;

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

            if(checkedId==-1){

                error2=false;
            }else{

                error2=true;

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

            if(checkedId==-1){

                error2=false;
            }else{
                error2=true;

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

    @Override
    public void onMapReady(GoogleMap googleMap) {

        //Log.d(TAG,"test show trip onMapReady");

        mGoogleMap = googleMap;

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        //setup map
        Session session=null;


        //Log.d(TAG,"[test show trip] get session using id " + mSessionId);

        //get session
        session = SessionManager.getSession(mSessionId);

        //Log.d(TAG,"[test show trip] get session using id " + session.getId() + " starting at  " + session.getStartTime() + " and end at " + session.getEndTime());

        //show trip time
        time = (TextView)findViewById(R.id.Time);

        long startTime = session.getStartTime();
        long endTime = session.getEndTime();

        //TODO changed the date into "today" or "yesterday".
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_DATE_TEXT_HOUR_MIN_AMPM);
        String timelabel = null;

        if (endTime==0){
            time.setText( "Time: " + ScheduleAndSampleManager.getTimeString(startTime, sdf) + " - Unknown" );
        }
        else {

            if (SessionManager.isSessionOngoing(mSessionId))
                time.setText( "Time: " + ScheduleAndSampleManager.getTimeString(startTime, sdf) + " - Now"  );
            else
                time.setText( "Time: " + ScheduleAndSampleManager.getTimeString(startTime, sdf) + " - " + ScheduleAndSampleManager.getTimeString(endTime, sdf)  );
        }

        //Log.d(TAG,"[test show trip] setting time label" + time.getText().toString());

        /** After we got a new session id, we retrieve and visualize data for the session**/
        if (session!=null) {
            showRecordingVizualization((int) session.getId());
        }

    }


    private Button.OnClickListener submitting = new Button.OnClickListener() {
        public void onClick(View v) {


            //Log.e(TAG, "submit clicked");

            //Log.d(TAG, "ans1 : "+ans1+" ans2 : "+ans2+" ans3 : "+ans3+" ans4 : "+ ans4 +" ans4_2 : "+ans4_2);

            if(error1 && error2 && error3 && error4){

                /* TODO storing data */
                JSONObject data = new JSONObject();

                JSONArray latdata = new JSONArray();
                JSONArray lngdata = new JSONArray();

                int i = 0;

                updateSessionWithAnnotation(ans1, ans2, ans3, ans4, latdata, lngdata);

                AnnotateSessionActivity.this.finish();
            }else
                Toast.makeText(AnnotateSessionActivity.this,"Please complete all the questions!!",Toast.LENGTH_SHORT).show();

        }
    };

    private void updateSessionWithAnnotation(String ans1, String ans2, String ans3
            , String ans4, JSONArray latdata, JSONArray lngdata){
        //Log.d(TAG, "updateSessionWithAnnotation");

        ContentValues values = new ContentValues();
        SQLiteDatabase db = DBManager.getInstance().openDatabase();

        //create annoation from the ESM
        Annotation annotation = new Annotation();

        JSONObject esmContentJSON = new JSONObject();

        try {
            esmContentJSON.put("openTimes", mESMOpenTimes.toString());
            esmContentJSON.put("submitTime", mESMSubmitTime);
            esmContentJSON.put("ans1", ans1);
            esmContentJSON.put("ans2", ans2);
            esmContentJSON.put("ans3", ans3);
            esmContentJSON.put("ans4", ans4);

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

        //Log.d(TAG, "[test show trip] getLocationPointsToDrawOnMap ");

        ArrayList<LatLng> points = new ArrayList<LatLng>();

        //get data from the database
        ArrayList<String> data = DataHandler.getDataBySession(sessionId, DBHelper.STREAM_TYPE_LOCATION);
        //Log.d(TAG, "[test show trip] getLocationPointsToDrawOnMap  get data:" + data.size() + "rows");

        for (int i=0; i<data.size(); i++){

            String[] record = data.get(i).split(Constants.DELIMITER);

            //TODO: need to get lat and lng from the Data JSON Object from LocationRecord

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

            //Log.d(TAG, "[test show trip] in onPostExecute, the poitns obtained are : " + points.size());
            if (points.size()>0){
                LatLng startLatLng  = points.get(0);
                LatLng endLatLng = points.get(points.size()-1);
                LatLng middleLagLng = points.get((points.size()/2));

                //Log.d(TAG, "[test show trips] the session is not in the currently recording session");
                //we first need to know what visualization we want to use, then we get data for that visualization

                //show maps with path (draw polylines)
                showMapWithPaths(mGoogleMap, points, middleLagLng, startLatLng, endLatLng);
            }

        }

    }

}
