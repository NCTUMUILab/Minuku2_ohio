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
import edu.ohio.minuku.manager.TripManager;
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

    ArrayList<String> mlocationDataRecords;

    public recordinglistohio(){}

    public recordinglistohio(Context mContext){
        this.mContext = mContext;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recordinglist_ohio);
        liststr = new ArrayList<String>();
        mlocationDataRecords = new ArrayList<String>();

        Trip_size = 0;
    }

    private void startAnnotateActivity(int position) {

        Log.d(TAG, "Trip_size : " + Trip_size + " position : " + (position+1)); // (position+1) is caused from the index problem. At "List", it start from "0" but the Trip_size is started from "1";

        position = Trip_size - (position+1);//reverse the order.

        String key =  mlocationDataRecords.get(position); //reverse the order of Trip for showing.
//        Session session =  mSessions.get(position);
        Log.d(TAG, "the location trip at " + position + " being selected is " + key);

        Bundle bundle = new Bundle();
//        bundle.putString("timekey_id", key);
        bundle.putString("sessionkey_id", key);
        bundle.putInt("position_id", position);
        Intent intent = new Intent(recordinglistohio.this, annotateohio.class);
        intent.putExtras(bundle);

        startActivity(intent);

    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG,"onResume");

        initrecordinglistohio();

    }

    private void initrecordinglistohio(){

        Log.d(TAG,"initrecordinglistohio");

        ArrayList<String> locationDataRecords = null;

        listview = (ListView)findViewById(R.id.recording_list);
        listview.setEmptyView(findViewById(R.id.emptyView));

        try{

            Log.d(TAG,"ListRecordAsyncTask");

//            locationDataRecords = new ListRecordAsyncTask().execute(mReviewMode).get();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                locationDataRecords = new ListRecordAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            else
                locationDataRecords = new ListRecordAsyncTask().execute().get();


            Log.d(TAG,"locationDataRecords = new ListRecordAsyncTask().execute().get();");

            mlocationDataRecords = locationDataRecords;

        }catch(InterruptedException e) {
            Log.d(TAG,"InterruptedException");
            e.printStackTrace();
        } catch (ExecutionException e) {
            Log.d(TAG,"ExecutionException");
            e.printStackTrace();
        }

//        liststr.add(mlocationDataRecords.get());

//        listAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,list);

        OhioListAdapter ohioListAdapter = new OhioListAdapter(
                this,
                R.id.recording_list,
                mlocationDataRecords
        );

        /*listAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,mlocationDataRecords);

        listview.setAdapter(listAdapter);*/

        listview.setAdapter(ohioListAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                /*if(!OhioListAdapter.dataPos.contains(position)) {

                    startAnnotateActivity(position);

                }*/

                if(!OhioListAdapter.dataPos.contains(position)) {

                    startAnnotateActivity(position);

                }
            }
        });

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


    private class ListRecordAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {

        private ProgressDialog dialog = null;

        @Override
        protected void onPreExecute() {
            Log.d(TAG,"onPreExecute");
//            this.dialog = new ProgressDialog(recordinglistohio.this);
            this.dialog = ProgressDialog.show(recordinglistohio.this, "Working...","Loading...",true,false);

//            this.dialog.setMessage("Loading...");
//            this.dialog.setCancelable(false);
//            this.dialog.show();
        }

    /*
        @Override
        protected ArrayList<String> doInBackground(String... params) {

            Log.d(TAG, "listRecordAsyncTask going to list recording");

            ArrayList<String> locationDataRecords = new ArrayList<String>();

            try {

                locationDataRecords = TripManager.getTripDatafromSQLite();
//                locationDataRecords = TripManager.getInstance().getTripDatafromSQLite();
//                Trip_size = TripManager.getInstance().getSessionidForTripSize();
                Trip_size = TripManager.getInstance().getTrip_size();

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
        protected ArrayList<String> doInBackground(String... params) {

            Log.d(TAG, "listRecordAsyncTask going to list recording");

            ArrayList<String> locationDataRecords = new ArrayList<String>();

            try {

                locationDataRecords = TripManager.getTripData();
//                locationDataRecords = TripManager.getInstance().getTripDatafromSQLite();
//                Trip_size = TripManager.getInstance().getSessionidForTripSize();
                Trip_size = TripManager.getInstance().getTrip_size();

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

        // onPostExecute displays the results of the AsyncTask.
        /*
        @Override
        protected void onPostExecute(ArrayList<String> result) {

            super.onPostExecute(result);

            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
        }
        */
    }


}
