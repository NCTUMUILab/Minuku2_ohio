package edu.ohio.minuku_2.controller.Ohio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/7/5.
 */

public class recordinglistohio extends Activity {

    private final static String TAG = "recordinglistohio";

    private Context mContext;
    private ListView listview;
    private ArrayList<String> liststr;
    private String[] list = {"2017/7/5 10:00","2017/7/6 11:00"};
    private ArrayAdapter<String> listAdapter;
    private int Trip_size;
    String mReviewMode = "mReviewMode";

    ArrayList<String> mSessionIds;

    public recordinglistohio(){}

    public recordinglistohio(Context mContext){
        this.mContext = mContext;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recordinglist_ohio);
        liststr = new ArrayList<String>();
        mSessionIds = new ArrayList<String>();

        Trip_size = 0;
    }

    private void startAnnotateActivity(int trip_position) {

        String sesionId =  mSessionIds.get(trip_position); //reverse the order of Trip for showing.
        Log.d(TAG, "click on the session id " + sesionId + " at position " + trip_position);

        Bundle bundle = new Bundle();
        bundle.putString("sessionkey_id", sesionId);
        bundle.putInt("position_id", trip_position);
        Intent intent = new Intent(recordinglistohio.this, AnnotateSessionActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);

    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG,"onResume");

        initSessionList();

        Log.d(TAG,"test trip result on resume "  );
    }

    private void initSessionList(){

        Log.d(TAG,"initSessionList");

        ArrayList<String> sessionIds = null;

        listview = (ListView)findViewById(R.id.recording_list);
        listview.setEmptyView(findViewById(R.id.emptyView));

        try{

//            locationDataRecords = new ListSessionAsyncTask().execute(mReviewMode).get();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                sessionIds = new ListSessionAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            else
                sessionIds = new ListSessionAsyncTask().execute().get();


            Log.d(TAG,"test trip result after execute async " + sessionIds );

            mSessionIds = sessionIds;


        }catch(InterruptedException e) {
            Log.d(TAG,"InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            Log.d(TAG,"ExecutionException");
            e.printStackTrace();
        }

        OhioListAdapter ohioListAdapter = new OhioListAdapter(
                this,
                R.id.recording_list,
                mSessionIds
        );

        listview.setAdapter(ohioListAdapter);

        Log.d(TAG,"test trip result after setadatori " + sessionIds );

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                if(!OhioListAdapter.dataPos.contains(position)) {

                    startAnnotateActivity(position);

                }
            }
        });


        Log.d(TAG,"test trip result end of initSessionList" );

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            Log.d(TAG, " onKeyDown");

            Intent intent = getIntent();
            Bundle bundle = new Bundle();
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    private class ListSessionAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {

        private ProgressDialog dialog = null;

        @Override
        protected void onPreExecute() {
            Log.d(TAG,"onPreExecute");
            this.dialog = ProgressDialog.show(recordinglistohio.this, "Working...","Loading...",true,false);
        }


        @Override
        protected void onPostExecute(ArrayList<String> result) {
            super.onPostExecute(result);

            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
        }


    /*
        @Override
        protected ArrayList<String> doInBackground(String... params) {

            Log.d(TAG, "listRecordAsyncTask going to list recording");

            ArrayList<String> locationDataRecords = new ArrayList<String>();

            try {

                locationDataRecords = SessionManager.getTripDatafromSQLite();
//                locationDataRecords = SessionManager.getInstance().getTripDatafromSQLite();
//                Trip_size = SessionManager.getInstance().getSessionidForTripSize();
                Trip_size = SessionManager.getInstance().getTrip_size();

//                Log.d(TAG,"locationDataRecords(0) : " + locationDataRecords.get(0));
//                Log.d(TAG,"locationDataRecords(max) : " + locationDataRecords.get(locationDataRecords.size()-1));

//                if(locationDataRecords.isEmpty())
//                    ;

                Log.d(TAG,"try locationDataRecords");
            }catch (Exception e) {
                locationDataRecords = new ArrayList<String>();
                Log.d(TAG,"Exception");
                e.printStackTrace();
            }
//            return locationDataRecords;

            return locationDataRecords;

        }
*/
        @Override
        /*
        this async task obtain a list of sessions
         */
        protected ArrayList<String> doInBackground(String... params) {

            Log.d(TAG, "listRecordAsyncTask going to list recording");

            ArrayList<String> Sessions = new ArrayList<String>();

            try {

                Sessions = SessionManager.getSessions();
                Trip_size = SessionManager.getInstance().getTrip_size();

                Log.d(TAG," in doInBackground sessions :  " + Sessions.toString());

            }catch (Exception e) {
                Sessions = new ArrayList<String>();
                Log.d(TAG,"Exception");
                e.printStackTrace();
            }
            return Sessions;

        }
    }


}
