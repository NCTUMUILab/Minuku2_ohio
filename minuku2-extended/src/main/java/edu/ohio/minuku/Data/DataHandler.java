package edu.ohio.minuku.Data;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.model.DataRecord.ActivityRecognitionDataRecord;
import edu.ohio.minuku.streamgenerator.ActivityRecognitionStreamGenerator;

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

    public static ArrayList<ActivityRecognitionDataRecord> getActivityRecognitionRecordsBetweenTimes(long startTime, long endTime){

        Log.d(TAG, "getActivityRecognitionRecordsBetweenTimes");
        CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "getActivityRecognitionRecordsBetweenTimes");

        ArrayList<ActivityRecognitionDataRecord> activityRecognitionDataRecords = new ArrayList<>();

        ArrayList<String> datas = DBHelper.queryRecordsBetweenTimes(DBHelper.activityRecognition_table, startTime, endTime);

        for(String data : datas){

            Log.d(TAG, "data : "+data);
            CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "data : "+data);

            //sMostProbableActivity, sProbableActivities, sLatestDetectionTime, String.valueOf(session_id)
            String[] subData = data.split(Constants.DELIMITER);

            //split the mostProbableActivity into "type:conf"
            String mostProbableActivityString = subData[2];
            Log.d(TAG, "mostProbableActivityString : "+mostProbableActivityString);
            CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "mostProbableActivityString : "+mostProbableActivityString);

            String[] subMostActivity = mostProbableActivityString.split(",");
            String type = subMostActivity[0].split("=")[1];
            String confidence = subMostActivity[1].split("=")[1].replaceAll("]","");

            Log.d(TAG, "typeString : "+type);
            Log.d(TAG, "type : "+ ActivityRecognitionStreamGenerator.zzsu(type)+", typeNum : "+type+", confidence : "+confidence);
            CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "typeString : "+type);
            CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "type : "+ActivityRecognitionStreamGenerator.zzsu(type)+", typeNum : "+type+", confidence : "+confidence);

            DetectedActivity mostProbableActivity = new DetectedActivity(ActivityRecognitionStreamGenerator.zzsu(type), Integer.valueOf(confidence));


            ArrayList<DetectedActivity> probableActivity = new ArrayList<>();
            String probableActivitiesString = subData[3];
            Log.d(TAG, "probableActivities : "+probableActivitiesString);
            CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "probableActivities : "+probableActivitiesString);

            //[DetectedActivity [type=ON_BICYCLE, confidence=100], DetectedActivity [type=ON_FOOT, confidence=100], DetectedActivity [type=STILL, confidence=100]]
            String[] subprobableActivities = probableActivitiesString.split("], ");

            for(String eachProbableActivities : subprobableActivities){

                eachProbableActivities = eachProbableActivities.replaceAll("]", "");
                //[DetectedActivity [type=ON_BICYCLE, confidence=100
                String eachType = eachProbableActivities.substring(eachProbableActivities.lastIndexOf("type=")+"type=".length(), eachProbableActivities.indexOf(","));
                Log.d(TAG, "eachType : "+eachType);
                CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "eachType : "+eachType);
                CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "eachTypeNum : "+ActivityRecognitionStreamGenerator.zzsu(eachType));

                String eachConf = eachProbableActivities.substring(eachProbableActivities.lastIndexOf("confidence=")+"confidence=".length());
                Log.d(TAG, "eachConf : "+eachConf);
                CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "eachConf : "+eachConf);

                DetectedActivity eachActivity = new DetectedActivity(ActivityRecognitionStreamGenerator.zzsu(eachType), Integer.valueOf(eachConf));

                probableActivity.add(eachActivity);
            }

            long sLatestDetectionTime = Long.valueOf(subData[4]);

            String session_id = subData[5];
            Log.d(TAG, "sLatestDetectionTime : "+sLatestDetectionTime+", session_id : "+session_id);
            CSVHelper.storeToCSV(CSVHelper.CSV_AR_DATA, "sLatestDetectionTime : "+sLatestDetectionTime+", session_id : "+session_id);

            ActivityRecognitionDataRecord activityRecognitionDataRecord = new ActivityRecognitionDataRecord(mostProbableActivity, probableActivity, sLatestDetectionTime, session_id);

            activityRecognitionDataRecords.add(activityRecognitionDataRecord);
        }

        return activityRecognitionDataRecords;
    }

}
