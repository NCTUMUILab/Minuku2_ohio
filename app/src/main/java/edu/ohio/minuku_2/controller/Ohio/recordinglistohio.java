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
        Bundle bundle = new Bundle();
        bundle.putString("sessionkey_id",sessionId);
        bundle.putInt("position_id", trip_position);
        Intent intent = new Intent(recordinglistohio.this, AnnotateSessionActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);

    }


    @Override
    public void onResume() {
        super.onResume();
        initSessionList();
    }

    private void initSessionList(){

        listview = (ListView)findViewById(R.id.recording_list);
        listview.setEmptyView(findViewById(R.id.emptyView));

        try{
//            locationDataRecords = new ListSessionAsyncTask().execute(mReviewMode).get();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                new ListSessionAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            else
                new ListSessionAsyncTask(this).execute().get();

        }catch(InterruptedException e) {
            Log.d(TAG,"InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            Log.d(TAG,"ExecutionException");
            e.printStackTrace();
        }


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


    /**
     * Load Session Data from the SessionManager
     */
    private class ListSessionAsyncTask extends AsyncTask<String, Void, ArrayList<Session> > {

        private ProgressDialog dialog = null;
        private Context mContext = null;

        public ListSessionAsyncTask(Context context){
            mContext = context;
        }


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

            Log.d(TAG, "[test show trip] on post return sessions " + mSessions);


            OhioListAdapter ohioListAdapter = new OhioListAdapter(
                    mContext,
                    R.id.recording_list,
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

                sessions = SessionManager.getRecentSessions();

            }catch (Exception e) {
                sessions = new ArrayList<Session>();
                Log.d(TAG,"Exception");
                e.printStackTrace();
            }
            return sessions;

        }
    }


}
