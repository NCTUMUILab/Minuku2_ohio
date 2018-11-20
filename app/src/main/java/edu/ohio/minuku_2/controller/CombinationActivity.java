package edu.ohio.minuku_2.controller;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

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
//                listShow.set(position, chkItem.isChecked());

                Session sessionChosen = mSessions.get(position);

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "List - trip to combine - Chosen to be combined sessionid : "+sessionChosen.getId() + " - sessionid : " +sessionToCombineId);

                if(chkItem.isChecked())
                    sessionPosList.add(position);
                else{

                    try{

                        sessionPosList.remove(position);
                    }catch (Exception e){

                    }
                }

            }
        });

        confirm = (Button)findViewById(R.id.confirm);
        confirm.setOnClickListener(confirming);

        if(mSessions.size()==0)
            confirm.setVisibility(View.GONE);
        else
            confirm.setVisibility(View.VISIBLE);
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
//                DBHelper.updateRecordsInSessionConcat(DBHelper.STREAM_TYPE_LOCATION, sessionid, sessionToCombineId);
                DBHelper.updateRecordsInSession(DBHelper.STREAM_TYPE_LOCATION, sessionid, newSessionId);

                if(index == 0){
                    referenceId += sessionid;
                }else{
                    referenceId += "," + sessionid;
                }
            }

            referenceId += ","+sessionToCombineId;

            DBHelper.hideSessionTable(sessionToCombineId, Constants.SESSION_TYPE_CHANGED);
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

//                sessions = SessionManager.getRecentNotBeenCombinedSessions(sessionToCombineId);
                sessions = SessionManager.getRecentToShowSessions(sessionToCombineId);
            }catch (Exception e) {

            }

            return sessions;
        }
    }

}
