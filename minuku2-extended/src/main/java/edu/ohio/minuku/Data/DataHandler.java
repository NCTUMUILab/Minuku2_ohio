package edu.ohio.minuku.Data;

import android.content.Context;

import java.util.ArrayList;

import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;

/**
 * Created by armuro on 1/23/18.
 */

public class DataHandler {
    private static final DataHandler ourInstance = new DataHandler();

    public static DataHandler getInstance() {
        return ourInstance;
    }

    private static final String TAG = "DataHandler";
    private static Context mContext;

    private DataHandler() {
    }

    public static ArrayList<String> getDataBySession(int sessionId, String sourceName) {

        Log.d(TAG, "[test show trip] getDataBySession, seession id" + sessionId + " table name " + DBHelper.STREAM_TYPE_LOCATION);
        ArrayList<String> resultList = new ArrayList<String>();

        //first know which table and column to query..
        String tableName = sourceName;
        Log.d(TAG, "[test show trip] before queryRecordinSEssion");
        resultList =DBHelper.queryRecordsInSession(tableName, sessionId);
        Log.d(TAG, "[test show trip] got " + resultList.size() + " of results from queryRecordsInSession");

        return resultList;
    }


    public static ArrayList<String> getDataBySession(int sessionId, String sourceName, long startTime, long endTime) {

        //for each record type get data
        Log.d(TAG, "[test show trip][testgetdata] getting data from  sessiom " + sessionId + " - " + sourceName);

        ArrayList<String> resultList = new ArrayList<String>();

        //first know which table and column to query..
        String tableName= sourceName;
        Log.d(TAG, "[test show trip][testgetdata] getting data from  " + tableName);

        //get data from the table
        if (tableName !=null) {
            resultList = DBHelper.queryRecordsInSession(tableName, sessionId, startTime, endTime);
            Log.d(TAG, "[test show trip][testgetdata] the result from " + tableName + " is  " + resultList);
        }

        return resultList;
    }

    public static long getTimeOfLastSavedRecordInSession(int sessionId, String sourceName) {

        ArrayList<String> res = new ArrayList<String>();

        long latestTime = 0;

        res = DBHelper.queryLastRecord(
                sourceName,
                sessionId);

        Log.d(TAG, "[test show trip] testgetdata the last record is " + res);

        if (res!=null && res.size()>0){

            String lastRecord = res.get(0);
            long time = Long.parseLong(lastRecord.split(Constants.DELIMITER)[DBHelper.COL_INDEX_RECORD_TIMESTAMP_LONG] );
            Log.d(TAG, "[test show trip] testgetdata the last record time is " + ScheduleAndSampleManager.getTimeString(time));

            //compare time
            if (time>latestTime){
                latestTime = time;
                Log.d(TAG, "[test show trip] update, latest time is changed to  " + ScheduleAndSampleManager.getTimeString(time));

            }
        }

        Log.d(TAG, "[test show trip]return the latest time: " + ScheduleAndSampleManager.getTimeString(latestTime));

        return latestTime;

    }



}
