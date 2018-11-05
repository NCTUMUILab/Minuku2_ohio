package edu.ohio.minuku.Data;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.config.Constants;

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

    public static ArrayList<String> getDataByTime(String sourceName, long startTime, long endTime) {

        //for each record type get data
        ArrayList<String> resultList = new ArrayList<String>();

        //first know which table and column to query..
        String tableName= sourceName;

        //get data from the table
        if (tableName !=null) {

            resultList = DBHelper.queryRecords(sourceName, startTime, endTime);

        }

        return resultList;
    }

    public static ArrayList<String> getDataBySession(int sessionId, String sourceName) {

        //Log.d(TAG, "[test show trip] getDataBySession, seession id" + sessionId + " table name " + DBHelper.STREAM_TYPE_LOCATION);
        ArrayList<String> resultList = new ArrayList<String>();

        //first know which table and column to query..
        String tableName = sourceName;
        //Log.d(TAG, "[test show trip] before queryRecordinSEssion");
        resultList =DBHelper.queryRecordsInSession(tableName, sessionId);
        //Log.d(TAG, "[test show trip] got " + resultList.size() + " of results from queryRecordsInSession");

        return resultList;
    }


    public static ArrayList<String> getDataBySession(int sessionId, String sourceName, long startTime, long endTime) {

        //for each record type get data
        ArrayList<String> resultList = new ArrayList<String>();

        //first know which table and column to query..
        String tableName= sourceName;

        //get data from the table
        if (tableName !=null) {
            resultList = DBHelper.queryRecordsInSession(tableName, sessionId, startTime, endTime);
        }

        return resultList;
    }

    public static long getTimeOfLastSavedRecordInSession(int sessionId, String sourceName) {

        ArrayList<String> res = new ArrayList<String>();

        long latestTime = 0;

        res = DBHelper.queryLastRecord(
                sourceName,
                sessionId);

        if (res!=null && res.size()>0){

            String lastRecord = res.get(0);
            long time = Long.parseLong(lastRecord.split(Constants.DELIMITER)[DBHelper.COL_INDEX_RECORD_TIMESTAMP_LONG] );

            //compare time
            if (time>latestTime){
                latestTime = time;
//                //Log.d(TAG, "[test show trip] update, latest time is changed to  " + ScheduleAndSampleManager.getTimeString(time));

            }
        }

//        //Log.d(TAG, "[test show trip]return the latest time: " + ScheduleAndSampleManager.getTimeString(latestTime));

        return latestTime;

    }

    public static ArrayList<String> getSurveyData(long startTime, long endTime){

        //Log.d(TAG, "getSurveyData");

        ArrayList<String> results;

        //get the data from the query.
        results = DBHelper.querySurveyLinkBetweenTimes(startTime, endTime);

        return results;
    }

    public static String getLatestSurveyData(){

        //Log.d(TAG, "getLatestSurveyData");

        String result = "";

        //get the data from the query.
        result = DBHelper.queryLatestSurveyLink();

        return result;
    }

    public static String getLatestOpenedSurveyData(){

        //Log.d(TAG, "getLatestOpenedSurveyData");

        String result = "";

        //get the data from the query.
        result = DBHelper.queryLatestOpenedSurveyLink();

        return result;
    }

    public static ArrayList<String> getSurveysAreOpenedOrMissed(){

        ArrayList<String> resultInArray = DBHelper.getSurveysAreOpenedOrMissed();

        return resultInArray;
    }

    public static ArrayList<String> getSurveys(){

        ArrayList<String> resultInArray = DBHelper.querySurveyLinks();

        return resultInArray;
    }

    public static void updateSession(int id, int toBeSent){

        Log.d(TAG, "[storing sitename] Sitename going to store session : "+ id);

        Log.d(TAG, "updateSession");
        DBHelper.updateSessionTable(id, toBeSent);
    }

    public static void updateSurveyMissTime(String id, String colName){
        //Log.d(TAG, "updateSurveyMissTime");
        DBHelper.updateSurveyMissTime(id, colName);
        CSVHelper.storeToCSV_IntervalSurveyUpdated(false);
    }

    public static void updateSurveyOpenFlagAndTime(String id){
        //Log.d(TAG, "updateSurveyOpenFlag");
        DBHelper.updateSurveyOpenTime(id);
        CSVHelper.storeToCSV_IntervalSurveyUpdated(true);
    }



}
