package edu.ohio.minuku_2.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Data.DataHandler;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2018/6/26.
 */

public class CombinationActivity extends Activity {

    private final static String TAG = "CombinationActivity";

    ArrayList<Session> mSessions;

    private int sessionToCombineId;

    private Button confirm;

    private ListView listview;

    ArrayList<Integer> sessionPosList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.combined_list_activity);

        mSessions = new ArrayList<>();

        sessionToCombineId = getIntent().getExtras().getInt("Sessionid");
    }

    @Override
    public void onResume() {
        super.onResume();

        initSessionList();
    }

    private void initSessionList(){

        listview = (ListView)findViewById(R.id.otherTripsList);

        listview.setEmptyView(findViewById(R.id.emptyView));

        try{

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                new ListSessionAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            else
                new ListSessionAsyncTask(this).execute().get();

        } catch(InterruptedException e) {

        } catch (ExecutionException e) {

        }

        //clickListener on the session
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                CheckedTextView chkItem = (CheckedTextView) view.findViewById(R.id.check1);
                chkItem.setChecked(!chkItem.isChecked());

                Session sessionChosen = mSessions.get(position);

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "List - trip to combine - Chosen to be combined sessionid : "+sessionChosen.getId() + " - sessionid : " +sessionToCombineId);

                if(chkItem.isChecked())
                    sessionPosList.add(position);
                else{

                    try{

                        //the position is not reliable for the index of list
                        //int != Integer
                        sessionPosList.remove(Integer.valueOf(position));
                    }catch (Exception e){
                    }
                }

                updateConfirmButtonClickable();
            }
        });

        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {

                final LayoutInflater inflater = LayoutInflater.from(CombinationActivity.this);
                final AlertDialog.Builder builder = new AlertDialog.Builder(CombinationActivity.this);
                final View layout = inflater.inflate(R.layout.map_dialog,null);

                final Session sessionChosen = mSessions.get(position);

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "List - trip to combine - long click to be show the map sessionid : "+sessionChosen.getId() + " - sessionid : " +sessionToCombineId);

                builder.setView(layout)
                        .setPositiveButton(getResources().getString(R.string.closemap), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });

                final AlertDialog mAlertDialog = builder.create();
                mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(DialogInterface dialogInterface) {

                        showMapInDialog(sessionChosen, mAlertDialog, layout);
                    }
                });
                mAlertDialog.show();

                final Button positiveButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) positiveButton.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                positiveButton.setLayoutParams(layoutParams);

                return true;
            }
        });

        confirm = (Button)findViewById(R.id.confirm);
        confirm.setOnClickListener(confirming);

        if(mSessions.size()==0)
            confirm.setVisibility(View.GONE);
        else
            confirm.setVisibility(View.VISIBLE);

        updateConfirmButtonClickable();
    }

    //TODO test
    private void updateConfirmButtonClickable(){

        if(sessionPosList.size() == 0){

            confirm.setClickable(false);
        }else{

            confirm.setClickable(true);
        }
    }

    private void showMapInDialog(final Session sessionChosen, Dialog dialog, final View view){

        MapView mapView = (MapView) view.findViewById(R.id.mapView);
        MapsInitializer.initialize(this);

        mapView.onCreate(dialog.onSaveInstanceState());
        mapView.onResume();
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {

                showRecordingVizualization((int) sessionChosen.getId(), googleMap);
            }
        });
    }

    private Button.OnClickListener confirming = new Button.OnClickListener() {
        public void onClick(View v) {

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Confirm the combination - sessionid : "+ sessionToCombineId);

            Session currentSession = SessionManager.getSession(sessionToCombineId);

            long currentStartTime = currentSession.getStartTime();
            long currentEndTime = currentSession.getEndTime();

            Session lastSession = SessionManager.getLastSession();
            int sessionCount = lastSession.getId();
            int newSessionId = sessionCount + 1;

            String referenceId = "";

            //combine the time and update to the current session
            for(int index = 0; index < sessionPosList.size(); index++) {

                int pos = sessionPosList.get(index);
                Session session = mSessions.get(pos);
                int sessionid = session.getId();

                long startTime = session.getStartTime();
                long endTime = session.getEndTime();

                if(startTime < currentStartTime)
                    currentStartTime = startTime;

                if(endTime > currentEndTime)
                    currentEndTime = endTime;

                //update the combining one into isCombined and ShouldBeSent
                DBHelper.updateSessionTable(sessionid, Constants.SESSION_SHOULD_BE_SENT_FLAG, Constants.SESSION_IS_COMBINED_FLAG);

                //update the background data with the corresponding sessionids
                DBHelper.updateRecordsInSession(DBHelper.STREAM_TYPE_LOCATION, sessionid, newSessionId);

                if(index == 0){
                    referenceId += sessionid;
                }else{
                    referenceId += "," + sessionid;
                }
            }

            referenceId += ","+sessionToCombineId;

            DBHelper.hideSessionTable(sessionToCombineId, Constants.SESSION_TYPE_ORIGINAL_COMBINED);
            DBHelper.updateRecordsInSession(DBHelper.STREAM_TYPE_LOCATION, sessionToCombineId, newSessionId);

            Session session = new Session(sessionToCombineId);
            session.setId(newSessionId);
            session.setCreatedTime(ScheduleAndSampleManager.getCurrentTimeInMillis() / Constants.MILLISECONDS_PER_SECOND);
            session.setStartTime(currentStartTime);
            session.setEndTime(currentEndTime);
            session.setIsSent(Constants.SESSION_SHOULDNT_BEEN_SENT_FLAG);
            session.setIsCombined(Constants.SESSION_NEVER_GET_COMBINED_FLAG);
            session.setSurveyDay(Config.daysInSurvey);
            session.setType(Constants.SESSION_TYPE_COMBINED);
            session.setReferenceId("("+referenceId+")");
            session.setToShow(true);

            DBHelper.insertSessionTable(session);
//            DBHelper.updateSessionTable(sessionToCombineId, currentStartTime, currentEndTime);
//            DBHelper.updateSessionTableToCombined(sessionToCombineId, Constants.SESSION_SUBJECTIVELY_COMBINE_FLAG);

            Toast.makeText(CombinationActivity.this, getResources().getString(R.string.reminder_trips_combined_successfully), Toast.LENGTH_SHORT).show();

            Intent resultIntent = new Intent();
            resultIntent.putExtra(Constants.IS_COMBINED_IDENTIFIER, true);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    };

    private class ListSessionAsyncTask extends AsyncTask<String, Void, ArrayList<Session> > {

        private Context mContext = null;

        public ListSessionAsyncTask(Context context){
            mContext = context;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(ArrayList<Session> sessions) {

            mSessions = sessions;

            CombinedListAdapter combinedListAdapter = new CombinedListAdapter(
                    mContext,
                    R.id.otherTripsList,
                    mSessions
            );

            if(confirm != null) {

                if (mSessions.size() == 0)
                    confirm.setVisibility(View.GONE);
                else
                    confirm.setVisibility(View.VISIBLE);
            }

            listview.setAdapter(combinedListAdapter);
        }

        @Override
        protected ArrayList<Session> doInBackground(String... params) {

            ArrayList<Session> sessions = new ArrayList<Session>();

            try {

//                sessions = SessionManager.getRecentIncompleteSessions(sessionToCombineId);
                sessions = SessionManager.getRecentToShowSessions(sessionToCombineId);

                //filtered filled sessions
                for(Session session : sessions){

                    ArrayList<Annotation> annotations = session.getAnnotationsSet().getAnnotationByTag("ESM");
                    if(annotations.size() > 0){

                        sessions.remove(session);
                    }
                }

            }catch (Exception e) {

            }

            return sessions;
        }
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
