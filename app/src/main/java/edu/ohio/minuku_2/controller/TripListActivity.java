package edu.ohio.minuku_2.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/7/5.
 */

public class TripListActivity extends Activity {

    private final static String TAG = "TripListActivity";

    private Context mContext;
    private ListView listview;
    private ArrayList<String> liststr;
    private ArrayAdapter<String> listAdapter;

    ArrayList<Session> mSessions;

    public TripListActivity(){}

    public TripListActivity(Context mContext){
        this.mContext = mContext;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        if(Config.daysInSurvey == 0|| Config.daysInSurvey == -1){

            setContentView(R.layout.triplist_day0);
        }else{

            setContentView(R.layout.triplist_activity);
            liststr = new ArrayList<String>();
            mSessions = new ArrayList<Session>();
        }
    }


    private void startAnnotateActivity(int trip_position) {

        String sessionId = String.valueOf(mSessions.get(trip_position).getId());
        Bundle bundle = new Bundle();
        bundle.putString("sessionkey_id",sessionId);
        bundle.putInt("position_id", trip_position);
        Intent intent = new Intent(TripListActivity.this, AnnotateSessionActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);

    }

    @Override
    public void onResume() {
        super.onResume();

        if(Config.daysInSurvey != 0 && Config.daysInSurvey != -1) {
            initSessionList();
        }
    }

    private void initSessionList(){

        listview = (ListView)findViewById(R.id.tripList);

        if(Config.daysInSurvey > Constants.FINALDAY){

            TextView emptyView = (TextView) findViewById(R.id.emptyViewText);
            emptyView.setText("All trips completed.");
        }

        listview.setEmptyView(findViewById(R.id.emptyView));

        try{

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                new ListSessionAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            else
                new ListSessionAsyncTask(this).execute().get();

        }catch(InterruptedException e) {
//            Log.d(TAG,"InterruptedException");
//            e.printStackTrace();
        } catch (ExecutionException e) {
//            Log.d(TAG,"ExecutionException");
//            e.printStackTrace();
        }


        //clickListener on the session
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "List - trip to annotate");

                if(!OhioListAdapter.dataPos.contains(position)) {
//                    Log.d(TAG, "[[test show trip]] click on the session position " + position);
                    startAnnotateActivity(position);
                }
            }
        });

    }

    /**
     * Load Session Data from the SessionManager
     */
    private class ListSessionAsyncTask extends AsyncTask<String, Void, ArrayList<Session> > {

        private Context mContext = null;

        public ListSessionAsyncTask(Context context){
            mContext = context;
        }


        @Override
        protected void onPreExecute() {
//            Log.d(TAG,"[test show trip] onPreExecute");
        }


        @Override
        protected void onPostExecute(ArrayList<Session> sessions) {

            mSessions = sessions;

//            Log.d(TAG, "[test show trip] on post return sessions " + mSessions);


            OhioListAdapter ohioListAdapter = new OhioListAdapter(
                    mContext,
                    R.id.tripList,
                    mSessions
            );

            listview.setAdapter(ohioListAdapter);
        }

        /*
        this async task obtain a list of sessions
         */
        @Override
        protected ArrayList<Session> doInBackground(String... params) {

            ArrayList<Session> sessions = new ArrayList<Session>();

            try {

//                sessions = SessionManager.getRecentSessions();
                sessions = SessionManager.getRecentNotBeenCombinedSessions();

            }catch (Exception e) {
//                Log.d(TAG,"Exception");
//                e.printStackTrace();
            }
            return sessions;

        }
    }


}
