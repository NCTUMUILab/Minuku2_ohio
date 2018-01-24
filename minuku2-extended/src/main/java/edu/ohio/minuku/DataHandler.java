package edu.ohio.minuku;

import java.util.ArrayList;

import edu.ohio.minuku.DBHelper.DBHelper;
import edu.ohio.minuku.logger.Log;

/**
 * Created by Lawrence on 2018/1/23.
 */

public class DataHandler {

    private static final String TAG = "DataHandler";


    public static ArrayList<String> getSurveyData(long startTime, long endTime){

        Log.d(TAG, "getSurveyData");

        ArrayList<String> results = new ArrayList<String>();

        //get the data from the query.
        results = DBHelper.querySurveyLinkBetweenTimes(startTime, endTime);

        return results;
    }

    public static String getLatestSurveyData(){

        Log.d(TAG, "getLatestSurveyData");

        String result = "";

        //get the data from the query.
        result = DBHelper.queryLatestSurveyLink();

        return result;
    }

    public static void updateSurveyMissTime(String id, String colName){
        Log.d(TAG, "updateSurveyMissTime");
        DBHelper.updateSurveyMissTime(id, colName);
    }

    public static void updateSurveyOpenFlagAndTime(String id){
        Log.d(TAG, "updateSurveyOpenFlag");
        DBHelper.updateSurveyOpenTime(id);
    }

}
