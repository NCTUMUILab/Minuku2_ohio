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
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/7/5.
 */

public class recordinglistohio extends Activity {

    private final static String TAG = "recordinglistohio";

    private Context mContext;
    private ListView listview;
    private ArrayList<String> liststr;
    private ArrayAdapter<String> listAdapter;
    String mReviewMode = "mReviewMode";

    ArrayList<Session> mSessions;

    public recordinglistohio(){}

    public recordinglistohio(Context mContext){
        this.mContext = mContext;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recordinglist_ohio);
        liststr = new ArrayList<String>();
        mSessions = new ArrayList<Session>();
    }

    private void startAnnotateActivity(int trip_position) {

        String sessionId = String.valueOf(mSessions.get(trip_position).getId());
        Log.d(TAG, "[test show trip] start AnnotateActivity the session " +sessionId );
        Bundle bundle = new Bundle();
        bundle.putString("sessionkey_id",sessionId);
        bundle.putInt("position_id", trip_position);
        Intent intent = new Intent(recordinglistohio.this, AnnotateSessionActivity.class);
        intent.putExtras(bundle);
        Log.d(TAG, "[test show trip] aftger adding extra");
        startActivity(intent);

    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG,"onResume");

        initSessionList();

        Log.d(TAG,"[test show trip] result on resume "  );
    }

    private void initSessionList(){

        Log.d(TAG,"[test show trip] initSessionList");


        listview = (ListView)findViewById(R.id.recording_list);
        listview.setEmptyView(findViewById(R.id.emptyView));

        try{

//            locationDataRecords = new ListSessionAsyncTask().execute(mReviewMode).get();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                new ListSessionAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            else
                new ListSessionAsyncTask().execute().get();


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
                mSessions
        );

        listview.setAdapter(ohioListAdapter);

        //clickListener on the session
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                if(!OhioListAdapter.dataPos.contains(position)) {
                    Log.d(TAG, "[[test show trip]] click on the session position " + position);
                    startAnnotateActivity(position);

                }
            }
        });
        ;

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


    private class ListSessionAsyncTask extends AsyncTask<String, Void, ArrayList<Session> > {


        private ProgressDialog dialog = null;

        @Override
        protected void onPreExecute() {
            Log.d(TAG,"[test show trip] onPreExecute");
            this.dialog = ProgressDialog.show(recordinglistohio.this, "Working...","Loading...",true,false);
        }


        @Override
        protected void onPostExecute(ArrayList<Session> sessions) {

            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }

            mSessions = sessions;

        }

        /*
        this async task obtain a list of sessions
         */
        @Override
        protected ArrayList<Session> doInBackground(String... params) {

            Log.d(TAG, "[test show trip] listRecordAsyncTask going to list recording");

            ArrayList<Session> Sessions = new ArrayList<Session>();

            try {

                Log.d(TAG,"[test show trip]  before reading essions:  " + Sessions.toString());
                Sessions = SessionManager.getRecentSessions();
                Log.d(TAG,"[test show trip]  after reading sessions:  " + Sessions.toString());
//                Trip_size = SessionManager.getInstance().getTrip_size();

                Log.d(TAG,"[test show trip]  after getting trip size :  " + Sessions.toString());

            }catch (Exception e) {
                Sessions = new ArrayList<Session>();
                Log.d(TAG,"Exception");
                e.printStackTrace();
            }
            return Sessions;

        }
    }


}
