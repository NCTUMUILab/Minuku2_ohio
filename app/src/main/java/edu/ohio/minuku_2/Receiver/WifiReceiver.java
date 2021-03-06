package edu.ohio.minuku_2.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import org.javatuples.Ennead;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Sextet;
import org.javatuples.Triplet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.Utilities.TupleHelper;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.Utils;

/**
 * Created by Lawrence on 2017/8/16.
 */

public class WifiReceiver extends BroadcastReceiver {

    private final String TAG = "WifiReceiver";

    private SharedPreferences sharedPrefs;

    private int year, month, day, hour, min;

    private long latestUpdatedTime = Constants.INVALID_TIME_VALUE;
    private long nowTime = Constants.INVALID_TIME_VALUE;
    private long startTime = Constants.INVALID_TIME_VALUE;
    private long endTime = Constants.INVALID_TIME_VALUE;

    public Context context;

    //P.S. v1.0.4 is for v8
    //P.S. v2.0.4 is for v9
    private String versionNumber = "v2.0.4";
    private String wavesNumber = "14";

    public static final int HTTP_TIMEOUT = 10 * (int) Constants.MILLISECONDS_PER_SECOND;
    public static final int SOCKET_TIMEOUT = 20 * (int) Constants.MILLISECONDS_PER_SECOND;

    private static final String SERVER_OHIO = "http://mcog.asc.ohio-state.edu/apps/";
    private static final String SERVER_NCTU = "https://cmogflaskbackend.minuku.org/apps/";

    private static final String postTripUrl = SERVER_OHIO+"tripdump/";
    private static final String postDumpUrl = SERVER_OHIO+"devicedump/";
    private static final String postSurveyLinkUrl = SERVER_OHIO+"surveydump/";

    private static final String querySurveyLinkUrl = "http://mcog.asc.ohio-state.edu/apps/surveycheck?userid=";
    private static final String queryTripUrl = "http://mcog.asc.ohio-state.edu/apps/tripcheck?userid=";

    private static boolean firstConnect = true;
    private Handler handler = new Handler();

    @Override
    public void onReceive(final Context context, Intent intent) {

        Log.d(TAG, "onReceive");
        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "onReceive");

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        //get timezone, prevent the issue when the user start the app in wifi available environment.
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        int mYear = cal.get(Calendar.YEAR);
        int mMonth = cal.get(Calendar.MONTH)+1;
        int mDay = cal.get(Calendar.DAY_OF_MONTH);
        int mHour = cal.get(Calendar.HOUR_OF_DAY);

        this.context = context;

        sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);

        year = sharedPrefs.getInt("StartYear", mYear);
        month = sharedPrefs.getInt("StartMonth", mMonth);
        day = sharedPrefs.getInt("StartDay", mDay);

        Config.USER_ID = sharedPrefs.getString("userid","NA");
        Config.GROUP_NUM = sharedPrefs.getString("groupNum","NA");
        Config.Email = sharedPrefs.getString("Email", "NA");

        hour = sharedPrefs.getInt("StartHour", mHour);
        min = sharedPrefs.getInt("StartMin",0);

        //Log.d(TAG, "year : "+ year+" month : "+ month+" day : "+ day+" hour : "+ hour+" min : "+ min);

        if (Constants.CONNECTIVITY_CHANGE.equals(intent.getAction())) {

            Log.d(TAG, "firstConnect : "+firstConnect);
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "firstConnect : "+firstConnect);

            //activeNetwork may return null if there's no default Internet
            if (activeNetwork != null &&
                    activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {

                if(firstConnect) {

                    firstConnect = false;

                    Log.d(TAG, "start to wait for 10 min");
                    CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "start to wait for 10 min");

                    //wait for 10 seconds then check if it still in Wi-Fi
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            Log.d(TAG, "It's 10 min, check the network");
                            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "It's 10 min, check the network");

                            ConnectivityManager tenMinCm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo tenMinNetworkActiveInfo = tenMinCm.getActiveNetworkInfo();

                            Log.d(TAG, "tenMinNetworkActiveInfo != null ? "+(tenMinNetworkActiveInfo != null));
                            Log.d(TAG, "Is in Wi-Fi ? "+((tenMinNetworkActiveInfo != null) && (tenMinNetworkActiveInfo.getType() == ConnectivityManager.TYPE_WIFI)));

                            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "tenMinNetworkActiveInfo != null ? "+(tenMinNetworkActiveInfo != null));
                            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Is in Wi-Fi ? "+((tenMinNetworkActiveInfo != null) && (tenMinNetworkActiveInfo.getType() == ConnectivityManager.TYPE_WIFI)));

                            if (tenMinNetworkActiveInfo != null &&
                                    tenMinNetworkActiveInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                                updateSurveyLinkState();
                                updateTripState();

                                uploadData();

                                //the current iteration is done, set it back to true for preparing next time
                                firstConnect= true;
                            }
                        }
                    }, 10 * Constants.MILLISECONDS_PER_MINUTE);

                }
            }else {

                firstConnect= true;

                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "activeNetwork != null ? "+(activeNetwork != null));
                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Not in Wifi.");
            }
        }

    }

    private void updateTripState(){

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "updateTripState...");

        try {

            JSONArray tripDataJson = queryDataFromServer(queryTripUrl);

            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "tripDataJson length : "+tripDataJson.length());

            //based on the createdTime, update the trip data.
            for(int index = 0; index < tripDataJson.length(); index++){

                JSONObject tripDataPiece = new JSONObject(tripDataJson.get(index).toString());

                if(!tripDataPiece.has("CreatedTime"))
                    continue;

                String createdTime = tripDataPiece.getString("CreatedTime");
                Log.d(TAG, "Sent createdTime : "+createdTime);
                CSVHelper.storeToCSV(CSVHelper.CSV_SERVER_DATA_STATE, "Sent createdTime : "+createdTime);

                DBHelper.updateSessionTableByCreatedTime(Long.valueOf(createdTime), Constants.SESSION_IS_ALREADY_SENT_FLAG);
            }
        }catch (JSONException e){

            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "JSONException on checking SurveyLinkState ...");
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, Utils.getStackTrace(e));
        }
    }

    private void updateSurveyLinkState(){

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "updateSurveyLinkState...");

        try {

            JSONArray surveyDataJson = queryDataFromServer(querySurveyLinkUrl);

            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "surveyDataJson length : "+surveyDataJson.length());

            //based on the d and n value, update the survey link data.
            for(int index = 0; index < surveyDataJson.length(); index++){

                JSONObject surveyDataPiece = new JSONObject(surveyDataJson.get(index).toString());

                int d = surveyDataPiece.getInt("d");
                int n = surveyDataPiece.getInt("n");

//                Log.d(TAG, "surveyDataPiece d : " + d + ", n : " + n);

                DBHelper.updateSurveyBydn(d, n);
            }
        }catch (JSONException e){

            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "JSONException on checking SurveyLinkState ...");
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, Utils.getStackTrace(e));
        }
    }

    private JSONArray queryDataFromServer(String queryLink) throws JSONException{

        if(Config.USER_ID.equals(Constants.NOT_A_NUMBER)){
            return new JSONArray();
        }

        String data, link = queryLink+Config.USER_ID;

        JSONArray dataJson = new JSONArray();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                data = new HttpAsyncGetJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        link).get();
            else
                data = new HttpAsyncGetJsonTask().execute(
                        link).get();

            dataJson = new JSONArray(data);

//            Log.d(TAG, "dataJson : " + dataJson);

        } catch (InterruptedException e) {

        } catch (ExecutionException e) {

        } catch (NullPointerException e){

        }

        return dataJson;
    }

    private void uploadData(){

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "update Data...");

        //dump only can be sent when wifi is connected
        long lastSentStarttime = sharedPrefs.getLong("lastSentStarttime", Constants.INVALID_IN_LONG);

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "lastSentStarttime : "+ScheduleAndSampleManager.getTimeString(lastSentStarttime));
        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Is lastSentStarttime equal to "+Constants.INVALID_IN_LONG+" ? "+(lastSentStarttime == Constants.INVALID_IN_LONG));

        if(lastSentStarttime == Constants.INVALID_IN_LONG){

            //if it doesn't response the setting with initialize ones
            //initialize
            long startstartTime = getSpecialTimeInMillis(makingDataFormat(year,month,day,hour,0));
            startTime = sharedPrefs.getLong("StartTime", startstartTime);
            Log.d(TAG, "[show data response] current iteration startTime : " + ScheduleAndSampleManager.getTimeString(startTime));

            //initialize
            long startendTime = getSpecialTimeInMillis(makingDataFormat(year,month,day,hour+1,0));
            endTime = sharedPrefs.getLong("EndTime", startendTime);
            Log.d(TAG, "[show data response] current iteration endTime : " + ScheduleAndSampleManager.getTimeString(endTime));

        }else{

            //if it do reponse the setting with initialize ones
            startTime = Long.valueOf(lastSentStarttime);
            Log.d(TAG, "[show data response] (lastSentStarttime == 0), current iteration startTime : " + ScheduleAndSampleManager.getTimeString(startTime));

            long nextinterval = Constants.MILLISECONDS_PER_HOUR;

            endTime = Long.valueOf(lastSentStarttime) + nextinterval;
            Log.d(TAG, "[show data response] (lastSentStarttime == 0), current iteration endTime : " + ScheduleAndSampleManager.getTimeString(endTime));
        }

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "current iteration startTime : "+ScheduleAndSampleManager.getTimeString(startTime));
        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "current iteration endTime : "+ScheduleAndSampleManager.getTimeString(endTime));

        setNowTime();

        Log.d(TAG,"NowTimeString : " + ScheduleAndSampleManager.getTimeString(nowTime));
        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "current iteration nowTime : "+ScheduleAndSampleManager.getTimeString(nowTime));

        boolean tryToSendData = true;

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "going to send device Data...");
        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "nowTime > endTime ? "+(nowTime > endTime));

        while(nowTime > endTime && tryToSendData) {

            Log.d(TAG,"before send dump data NowTimeString : " + ScheduleAndSampleManager.getTimeString(nowTime));

            Log.d(TAG,"before send dump data EndTimeString : " + ScheduleAndSampleManager.getTimeString(endTime));

            tryToSendData = sendingDumpData();

            //update nowTime
            setNowTime();
        }

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "going to send trip Data...");

        sendingTripData(nowTime);

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "going to send survey Data...");

        sendingSurveyLinkData(nowTime);

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "after send survey Data...");
    }

    private void sendingTripData(long time24HrAgo){

        ArrayList<JSONObject> datas = getSessionData(time24HrAgo);

        for(int index = 0; index < datas.size(); index++){

            JSONObject data = datas.get(index);

            Log.d(TAG, "[test Trip sending] trip data uploading : " + data.toString());

            String curr = getDateCurrentTimeZone(new Date().getTime());

            String lastTimeInServer;

            try {

                CSVHelper.storeToCSV(CSVHelper.CSV_SERVER_DATA_STATE, "Going to send SessionId : "+data.getString("sessionid"));
                Log.d(TAG, "Going to send SessionId : "+data.getString("sessionid"));

                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Trip", "Before sending, getting from the server, the latest EndTime : "+ScheduleAndSampleManager.getTimeString(Long.valueOf(data.getString("EndTime")) * Constants.MILLISECONDS_PER_SECOND));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    lastTimeInServer = new HttpAsyncPostJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            postTripUrl,
                            data.toString(),
                            "Trip",
                            curr).get();
                else
                    lastTimeInServer = new HttpAsyncPostJsonTask().execute(
                            postTripUrl,
                            data.toString(),
                            "Trip",
                            curr).get();

                //if it was updated successfully, return the end time
                Log.d(TAG, "[show data response] Trip lastTimeInServer : " + lastTimeInServer);

                JSONObject lasttimeInServerJson = new JSONObject(lastTimeInServer);

                Log.d(TAG, "[show data response] check sent EndTime : " + data.getString("EndTime"));
                Log.d(TAG, "[show data response] check latest data in server's lastinsert : " + lasttimeInServerJson.getString("lastinsert"));
                Log.d(TAG, "[show data response] check condition : " + data.getString("EndTime").equals(lasttimeInServerJson.getString("lastinsert")));

                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Trip", "After sending, getting from the server, the latest EndTime : "+ScheduleAndSampleManager.getTimeString(Long.valueOf(lasttimeInServerJson.getString("lastinsert")) * Constants.MILLISECONDS_PER_SECOND));
                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Trip", "EndTime == lastinsert ? "+data.getString("EndTime").equals(lasttimeInServerJson.getString("lastinsert")));

                if(data.getString("EndTime").equals(lasttimeInServerJson.getString("lastinsert"))){

                    Log.d(TAG, "Trip sent successfully, EndTime "+ScheduleAndSampleManager.getTimeString(Long.valueOf(lasttimeInServerJson.getString("lastinsert")) * Constants.MILLISECONDS_PER_SECOND));
                    CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Trip sent successfully, EndTime "+ScheduleAndSampleManager.getTimeString(Long.valueOf(lasttimeInServerJson.getString("lastinsert")) * Constants.MILLISECONDS_PER_SECOND));

                    //update the sent Session to already be sent
//                    String sentSessionId = data.getString("sessionid");
//                    DataHandler.updateSession(Integer.valueOf(sentSessionId), Constants.SESSION_IS_ALREADY_SENT_FLAG);
                }else {

                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    private ArrayList<JSONObject> getSessionData(long time24HrAgo){

        Log.d(TAG, "getSessionData");

        ArrayList<JSONObject> sessionJsons = new ArrayList<>();

        ArrayList<String> overTimeSessions = DBHelper.querySessions(time24HrAgo);

        for(int index = 0; index < overTimeSessions.size(); index++){

            try {

                String eachData = overTimeSessions.get(index);

                Session sessionToSend = SessionManager.convertStringToSession(eachData);

                //tell if the session has been labeled. It is in the annotaiton with ESM tag
                ArrayList<Annotation> annotations = sessionToSend.getAnnotationsSet().getAnnotationByTag("ESM");

                JSONObject ESMJSON = null;

                //{"Entire_session":true,"Tag":["ESM"],"Content":"{\"ans1\":\"Walking outdoors.\",\"ans2\":\"Food\",\"ans3\":\"No\",\"ans4\":\"Right before\"}"}

                JSONObject annotatedtripdata = new JSONObject();

                try {

                    //adding the id and group number
                    annotatedtripdata.put("userid", Config.USER_ID);
                    annotatedtripdata.put("group_number", Config.GROUP_NUM);
                    annotatedtripdata.put("device_id", Config.DEVICE_ID);
                    annotatedtripdata.put("version", versionNumber);
                    annotatedtripdata.put("w", wavesNumber);
                    annotatedtripdata.put("android_ver", Build.VERSION.SDK_INT);
                    annotatedtripdata.put("build", getBuildInform());

                    annotatedtripdata.put("dataType", "Trip");

                    //adding the time info.
                    long sessionStartTime = sessionToSend.getStartTime();
                    long sessionEndTime = sessionToSend.getEndTime();
                    long StartTime = sessionStartTime / Constants.MILLISECONDS_PER_SECOND;
                    long EndTime = sessionEndTime / Constants.MILLISECONDS_PER_SECOND;
                    long CreatedTime = sessionToSend.getCreatedTime();
                    //Log.d(TAG, "Annotation StartTime : " + StartTime);
                    //Log.d(TAG, "Annotation EndTime : " + EndTime);

                    annotatedtripdata.put("sessionid", sessionToSend.getId());

                    annotatedtripdata.put("CreatedTime", CreatedTime);
                    annotatedtripdata.put("StartTime", StartTime);
                    annotatedtripdata.put("EndTime", EndTime);
                    annotatedtripdata.put("StartTimeString", ScheduleAndSampleManager.getTimeString(sessionStartTime));
                    annotatedtripdata.put("EndTimeString", ScheduleAndSampleManager.getTimeString(sessionEndTime));

                    annotatedtripdata.put("TripMin", sessionToSend.isLongEnough());
                    annotatedtripdata.put("type", getSessionTypeName(sessionToSend.getType()));

                    String referenceId = sessionToSend.getReferenceId();

                    if(referenceId.equals("")){

                        referenceId = Constants.INVALID_IN_STRING;
                    }

                    annotatedtripdata.put("referenceId", referenceId);
                    annotatedtripdata.put("displayed", sessionToSend.isToShow());

                    //getting the answers in the annotation.
                    if(annotations.size() > 0) {

                        annotatedtripdata.put("completeOrNot", "complete");

                        String content = annotations.get(0).getContent();
                        ESMJSON = new JSONObject(content);
                        Log.d(TAG, "[checking data] the contentofJSON ESMJSONObject  is " + ESMJSON);

                        //convert the time from millisecond to second
                        String annotationOpenTimesString = ESMJSON.getString("openTimes").replace("[", "").replace("]", "");

                        Log.d(TAG, "[checking data] ESMJSON get openTimes " + ESMJSON.getString("openTimes"));

                        String[] openTimes = annotationOpenTimesString.split(", ");

                        String openTimesInLongToShow = "";
                        String openTimesInStringToShow = "";
                        for(int openTimesIndex = 0; openTimesIndex < openTimes.length; openTimesIndex++ ){

                            long openTime = Long.valueOf(openTimes[openTimesIndex]);
                            long openTimeInSec = openTime / Constants.MILLISECONDS_PER_SECOND;

                            openTimesInLongToShow += String.valueOf(openTimeInSec);
                            openTimesInStringToShow += ScheduleAndSampleManager.getTimeString(openTime);

                            if(openTimesIndex != openTimes.length - 1){

                                openTimesInLongToShow += ", ";
                                openTimesInStringToShow += ", ";
                            }
                        }

                        ESMJSON.put("openTimes", openTimesInLongToShow);
                        ESMJSON.put("openTimeString", openTimesInStringToShow);

                        Log.d(TAG, "[checking data] the contentofJSON ESMJSONObject is " + ESMJSON);

                    }else{

                        annotatedtripdata.put("completeOrNot", "incomplete");

                        ESMJSON = new JSONObject();
                        try {

                            ESMJSON.put("openTimes", Constants.NOT_A_NUMBER);
                            ESMJSON.put("openTimeString", Constants.NOT_A_NUMBER);
                            ESMJSON.put("submitTime", Constants.NOT_A_NUMBER);
                            ESMJSON.put("ans1", Constants.NOT_A_NUMBER);
                            ESMJSON.put("ans2", Constants.NOT_A_NUMBER);
                            ESMJSON.put("ans3", Constants.NOT_A_NUMBER);
                            ESMJSON.put("ans4", Constants.NOT_A_NUMBER);
                            ESMJSON.put("optionalNote", Constants.NOT_A_NUMBER);
                        }catch (JSONException e){
                            //e.printStackTrace();
                        }

                        Log.d(TAG, "[checking data] the contentofJSON ESMJSONObject is " + ESMJSON);
                    }

                    //adding the annotations data
                    annotatedtripdata.put("Annotations", ESMJSON);

//                    annotatedtripdata.put("PeriodNumber", sessionToSend.getPeriodNum());
                    annotatedtripdata.put("d", sessionToSend.getSurveyDay());

                    Config.finalDayEndTime = sharedPrefs.getLong("finalDayEndTime", Config.finalDayEndTime);

                    CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Going to send a piece of trip data.");

                    //imply that currently over day 14
                    if(sessionEndTime > Config.finalDayEndTime){

                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "sessionEndTime is bigger than finalDayEndTime");
                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "endTime : "+ScheduleAndSampleManager.getTimeString(sessionEndTime));
                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "finalDayEndTime : "+ScheduleAndSampleManager.getTimeString(Config.finalDayEndTime));

                        continue;
                    }

                } catch (JSONException e) {

                } catch (Exception e) {

                    CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "Trips");
                    CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
                }

                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAFORMAT, "Trip ", annotatedtripdata.toString());

                sessionJsons.add(annotatedtripdata);
            }catch (IndexOutOfBoundsException e){

            }
        }

        return sessionJsons;
    }

    public String getSessionTypeName(int type){

        switch(type) {
            case Constants.SESSION_TYPE_ORIGINAL:
                return "original";
            case Constants.SESSION_TYPE_COMBINED:
                return "combined";
            case Constants.SESSION_TYPE_DELETED:
                return "deleted";
            case Constants.SESSION_TYPE_SPLIT:
                return "split";
            case Constants.SESSION_TYPE_SHORT:
                return "short";
            case Constants.SESSION_TYPE_CHANGED:
                return "changed";
            case Constants.SESSION_TYPE_ORIGINAL_COMBINED:
                return "orig_comb";
            case Constants.SESSION_TYPE_ORIGINAL_SPLIT:
                return "orig_split";

        }
        return "original";
    }

    public void sendingSurveyLinkData(long time24HrAgo){

        long timeOfData = Constants.INVALID_IN_LONG;

        SQLiteDatabase db = DBManager.getInstance().openDatabase();

        Cursor surveyCursor = db.rawQuery("SELECT * FROM "+DBHelper.surveyLink_table +
                " WHERE " + DBHelper.sentOrNot_col + " = "+Constants.SURVEYLINK_SHOULD_BE_SENT_FLAG + " AND "+ DBHelper.generateTime_col + " < " + time24HrAgo , null);

        //where openflag != -1, implies it hasn't been opened or missed, set as clickedtime

        JSONObject surveyJson = new JSONObject();

        int rows = surveyCursor.getCount();

        Log.d(TAG, "[check query] latestSurveyLinkIdFromServer rows : "+ rows);

        if(rows!=0){

            try {

                surveyCursor.moveToFirst();
                for (int i = 0; i < rows; i++) {

                    String link = surveyCursor.getString(DBHelper.COL_INDEX_LINK);

                    String getd1 = link.split("d=")[1];
                    String getd2 = getd1.split("&n=")[0];
                    String d = getd2;

                    String getn1 = link.split("n=")[1];
                    String getn2 = getn1.split("&m=")[0];
                    String n = getn2;

                    String getm1 = link.split("&m=")[1];
                    String m = getm1;

                    String timestamp = surveyCursor.getString(2);

                    //same as openedTime might be null when the user missed it.
                    String clickedtime = surveyCursor.getString(3);

                    //convert into second
                    String timestampInSec = timestamp.substring(0, timestamp.length() - 3);

                    surveyJson.put("userid", Config.USER_ID);
                    surveyJson.put("group_number", Config.GROUP_NUM);
                    surveyJson.put("device_id", Config.DEVICE_ID);
                    surveyJson.put("version", versionNumber);
                    surveyJson.put("w", wavesNumber);
                    surveyJson.put("dataType", "SurveyLink");
                    surveyJson.put("android_ver", Build.VERSION.SDK_INT);
                    surveyJson.put("build", getBuildInform());

                    surveyJson.put("triggerTime", Long.valueOf(timestampInSec));
                    surveyJson.put("triggerTimeString", ScheduleAndSampleManager.getTimeString(Long.valueOf(timestamp)));

                    surveyJson.put("clickedtime", clickedtime);

                    Config.finalDayEndTime = sharedPrefs.getLong("finalDayEndTime", Config.finalDayEndTime);

                    CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Going to send a piece of survey data.");

                    //imply that currently over day 14
                    if(Long.valueOf(timestamp) > Config.finalDayEndTime){

                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "survey time is bigger than finalDayEndTime");
                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "endTime : "+ScheduleAndSampleManager.getTimeString(Long.valueOf(timestamp)));
                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "finalDayEndTime : "+ScheduleAndSampleManager.getTimeString(Config.finalDayEndTime));

                        continue;
                    }

                    if(clickedtime != null && !clickedtime.isEmpty()){

                        long recordClickedtime = Long.valueOf(clickedtime.substring(0, clickedtime.length() - 3));
                        surveyJson.put("clickedtime", recordClickedtime);
                    }else{

                        surveyJson.put("clickedtime", Constants.INVALID_TIME_VALUE);
                    }

                    String completeType = surveyCursor.getString(5);

                    if(completeType.equals(Constants.SURVEY_INCOMPLETE_FLAG) || completeType.equals("-1")){

                        completeType = Constants.TEXT_TO_SERVER_SURVEY_INCOMPLETE;
                    }else if(completeType.equals(Constants.SURVEY_COMPLETE_FLAG)){

                        completeType = Constants.TEXT_TO_SERVER_SURVEY_COMPLETE;
                    }else if(completeType.equals(Constants.SURVEY_ERROR_FLAG)){

                        completeType = Constants.TEXT_TO_SERVER_SURVEY_ERROR;
                    }

                    //clickornot
                    surveyJson.put("completeType", completeType);

                    surveyJson.put("d", Integer.valueOf(d));
                    surveyJson.put("n", Integer.valueOf(n));
                    surveyJson.put("m", Integer.valueOf(m));

                    timeOfData = Long.valueOf(timestampInSec);

                    String curr = getDateCurrentTimeZone(new Date().getTime());

                    Log.d(TAG, "[show data response] SurveyLink data :"+surveyJson.toString());

//                    CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAFORMAT, "SurveyLink", surveyJson.toString());

                    try {

                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Survey Link", "Before sending to the server, the survey completeType : "+surveyJson.get("completeType"));
                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Survey Link", "Before sending to the server, the latest EndTime : "+surveyJson.getString("triggerTimeString"));
                    }catch (JSONException e){
                        e.printStackTrace();
                    }

                    String timeInServer;

                    try {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            timeInServer = new HttpAsyncPostJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                    postSurveyLinkUrl,
                                    surveyJson.toString(),
                                    "SurveyLink",
                                    curr).get();
                        else
                            timeInServer = new HttpAsyncPostJsonTask().execute(
                                    postSurveyLinkUrl,
                                    surveyJson.toString(),
                                    "SurveyLink",
                                    curr).get();

                        Log.d(TAG, "[show data response] SurveyLink timeInServer : "+timeInServer);

                        JSONObject lasttimeInServerJson = new JSONObject(timeInServer);

                        Log.d(TAG, "[show data response] before to next iteration check : "+Long.valueOf(lasttimeInServerJson.getString("lastinsert")));

                        long fromServer = Long.valueOf(lasttimeInServerJson.getString("lastinsert"));

                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Survey Link", "After sending, getting from the server, the latest EndTime : "+ScheduleAndSampleManager.getTimeString(fromServer * Constants.MILLISECONDS_PER_SECOND));
                        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Survey Link", "timeOfData == fromServer ? "+(timeOfData == fromServer));

                        Log.d(TAG, "[check query] going to change the latest id");

                        Log.d(TAG, "[check query] timeOfData : " +timeOfData);
                        Log.d(TAG, "[check query] fromServer : " +fromServer);
                        Log.d(TAG, "[check query] timeOfData == fromServer ? "+ (timeOfData == fromServer));

                        if(timeOfData == fromServer){

                            Log.d(TAG, "[check query] survey sent successfully, time : "+ScheduleAndSampleManager.getTimeString(fromServer * Constants.MILLISECONDS_PER_SECOND));
                            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Survey sent successfully, time : "+ScheduleAndSampleManager.getTimeString(fromServer * Constants.MILLISECONDS_PER_SECOND));

//                            String id = surveyCursor.getString(DBHelper.COL_INDEX_ID);

//                            DBHelper.updateSurveyToAlreadyBeenSent(Integer.valueOf(id));
                        }else {

                            break;
                        }

                        surveyCursor.moveToNext();

                    } catch (InterruptedException e) {
                    } catch (ExecutionException e) {
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }catch (JSONException e){
                e.printStackTrace();
            }catch (Exception e) {

                CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "SurveyLink");
                CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
            }

        }
    }

    private JSONObject getBuildInform() throws JSONException{

        JSONObject buildData = new JSONObject();

        buildData.put("Manufacturer", Build.MANUFACTURER);
        buildData.put("Model", Build.MODEL);
        buildData.put("Product", Build.PRODUCT);

        return buildData;
    }

    private int getSurveyDayByTime(long startTime){

        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String startTimeDate = ScheduleAndSampleManager.getTimeString(startTime, sdf_date);

        Log.d(TAG, "[show data response] startTimeDate : "+startTimeDate);

        ArrayList<String> surveyDaysInString = DBHelper.querySurveyDayWithDate(startTimeDate);

        Log.d(TAG, "[show data response] surveyDaysInString : "+surveyDaysInString);

        int surveyDay = -1;

        if(surveyDaysInString.size() > 0){

            String surveyDayWithDate = surveyDaysInString.get(surveyDaysInString.size() - 1);

            surveyDay = Integer.valueOf(surveyDayWithDate.split(Constants.DELIMITER)[1]);
        }

        return surveyDay;
    }

    public boolean sendingDumpData(){

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Going to send a piece of device data.");


        JSONObject data = new JSONObject();

        long endTimeOfJson = -999;

        try {

            data.put("userid", Config.USER_ID);
            data.put("group_number", Config.GROUP_NUM);
            data.put("device_id", Config.DEVICE_ID);
            data.put("email", Config.Email);
            data.put("version", versionNumber);
            data.put("w", wavesNumber);
            data.put("dataType", "Dump");
            data.put("android_ver", Build.VERSION.SDK_INT);

            long startTimeInSec = startTime/Constants.MILLISECONDS_PER_SECOND;
            long endTimeInSec = endTime/Constants.MILLISECONDS_PER_SECOND;

            data.put("StartTime", startTimeInSec);
            data.put("EndTime", endTimeInSec);

            SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DASH);

            data.put("StartTimeString", ScheduleAndSampleManager.getTimeString(startTime, sdf_now));
            data.put("EndTimeString", ScheduleAndSampleManager.getTimeString(endTime, sdf_now));

            data.put("build", getBuildInform());

            int d = getSurveyDayByTime(startTime);

            data.put("d", d);

            Log.d(TAG, "[show data response] Dump survey day : " + d);

            SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
            data.put("date", ScheduleAndSampleManager.getTimeString(startTime, sdf_date));

            endTimeOfJson = endTimeInSec;
        }catch (JSONException e){
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Dump data JSONException");
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, Utils.getStackTrace(e));

        }

        Config.finalDayEndTime = sharedPrefs.getLong("finalDayEndTime", Config.finalDayEndTime);
        Config.downloadedDayEndTime = sharedPrefs.getLong("downloadedDayEndTime", Config.downloadedDayEndTime);

        Log.d(TAG, "[show data response] endTimeOfJson : "+ScheduleAndSampleManager.getTimeString(endTime));
        Log.d(TAG, "[show data response] final endtime : "+ScheduleAndSampleManager.getTimeString(Config.finalDayEndTime));
        Log.d(TAG, "[show data response] endtime over day 15 ? "+(endTime > Config.finalDayEndTime));

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, " endTimeOfJson : "+ScheduleAndSampleManager.getTimeString(endTime));
        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, " final endtime ? "+ScheduleAndSampleManager.getTimeString(Config.finalDayEndTime));
        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, " endtime over day 15 ? "+(endTime > Config.finalDayEndTime));

        //imply that currently was over day 14
        if(endTime > Config.finalDayEndTime){

            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "endTime is bigger than finalDayEndTime");
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "endTime : "+ScheduleAndSampleManager.getTimeString(endTime));
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "finalDayEndTime : "+ScheduleAndSampleManager.getTimeString(Config.finalDayEndTime));

            return false;
        }

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, " downloadedDayEndTime ? "+ScheduleAndSampleManager.getTimeString(Config.downloadedDayEndTime));

        //TODO before day 0, filter them
        //imply that currently was in day 0 or downloaded day
        if(endTime <= Config.downloadedDayEndTime){

            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "endTime is lower than downloadedDayEndTime");
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "endTime : "+ScheduleAndSampleManager.getTimeString(endTime));
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "downloadedDayEndTime : "+ScheduleAndSampleManager.getTimeString(Config.downloadedDayEndTime));

            Log.d(TAG, "[show data response] endTime is lower than downloadedDayEndTime");
            Log.d(TAG, "[show data response] endTime : " + ScheduleAndSampleManager.getTimeString(endTime));
            Log.d(TAG, "[show data response] downloadedDayEndTime : " + ScheduleAndSampleManager.getTimeString(Config.downloadedDayEndTime));

            //set the next data period to avoid infinite loop
            startTime = endTime;
            endTime = startTime + Constants.MILLISECONDS_PER_HOUR;

            Log.d(TAG, "[show data response] next iteration startTimeString : " + ScheduleAndSampleManager.getTimeString(startTime));
            Log.d(TAG, "[show data response] next iteration endTimeString : " + ScheduleAndSampleManager.getTimeString(endTime));

            //update the last data's startTime.
            sharedPrefs.edit().putLong("lastSentStarttime", startTime).apply();

            return true;
        }

        storeTransporatation(data);
        storeLocation(data);
        storeActivityRecognition(data);
        storeRinger(data);
        storeConnectivity(data);
        storeBattery(data);
        storeAppUsage(data);
        storeActionLog(data);

//        Log.d(TAG,"[show data response] checking data Dump : "+ data.toString());

//        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAFORMAT,"Dump", data.toString());

        try {

            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Dump","Going to send the data with EndTime : "+ data.getString("EndTimeString"));
        }catch (JSONException e){

        }

        String curr = getDateCurrentTimeZone(new Date().getTime());

        String lastTimeInServer;

        try {

            if(data.getString("d").equals("-1")){

                Log.d(TAG, "[show data response] not a correct survey day");

                throw new JSONException("not a correct survey day");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                lastTimeInServer = new HttpAsyncPostJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        postDumpUrl,
                        data.toString(),
                        "Dump",
                        curr).get();
            else
                lastTimeInServer = new HttpAsyncPostJsonTask().execute(
                        postDumpUrl,
                        data.toString(),
                        "Dump",
                        curr).get();

            Log.d(TAG, "[show data response] Dump lastTimeInServer : "+lastTimeInServer);
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED, "Server Response : "+lastTimeInServer);

            JSONObject lasttimeInServerJson = new JSONObject(lastTimeInServer);

            Log.d(TAG, "[show data response] before to next iteration check : " + Long.valueOf(lasttimeInServerJson.getString("lastinsert")));

            long fromServer = Long.valueOf(lasttimeInServerJson.getString("lastinsert"));

            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Dump", "After sending, getting from the server, the latest EndTime : "+ScheduleAndSampleManager.getTimeString(fromServer * Constants.MILLISECONDS_PER_SECOND));
            CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_DATAUPLOADED,"Dump", "endTime == lastinsert ? "+(endTimeOfJson == fromServer));

            //check the data is sent completely
            if(endTimeOfJson == fromServer) {

                //update latestUpdatedTime; due to the format from the server is divided by 1000
                //we get the time unit is second; thus, we have to convert into millis
                latestUpdatedTime = fromServer * Constants.MILLISECONDS_PER_SECOND;

                Log.d(TAG, "[show data response] next iteration latestUpdatedTime : " + latestUpdatedTime);
                Log.d(TAG, "[show data response] next iteration latestUpdatedTimeString : " + ScheduleAndSampleManager.getTimeString(latestUpdatedTime));

                //setting nextime interval
                //improve it to get the value from the server
                startTime = latestUpdatedTime;
                endTime = startTime + Constants.MILLISECONDS_PER_HOUR;

                Log.d(TAG, "[show data response] next iteration startTimeString : " + ScheduleAndSampleManager.getTimeString(startTime));
                Log.d(TAG, "[show data response] next iteration endTimeString : " + ScheduleAndSampleManager.getTimeString(endTime));

                //update the last data's startTime.
                sharedPrefs.edit().putLong("lastSentStarttime", startTime).apply();

                return true;
            }
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        } catch (JSONException e){
        }

        return false;
    }

    private void setNowTime(){

        nowTime = new Date().getTime() - Constants.MILLISECONDS_PER_DAY;

//        nowTime = new Date().getTime(); //TODO for testing
    }

    //use HTTPAsyncTask to poHttpAsyncPostJsonTaskst data
    private class HttpAsyncPostJsonTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String result = null;
            String url = params[0];
            String data = params[1];
            String dataType = params[2];
            String lastSyncTime = params[3];

            Log.d(TAG, "[show data response] going to send " + dataType);

            result = postJSON(url, data, dataType, lastSyncTime);

            return result;
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            Log.d(TAG, "[show data response] get http post result " + result);

            try {

                JSONObject lasttimeInServerJson = new JSONObject(result);

                long fromServer = Long.valueOf(lasttimeInServerJson.getString("lastinsert"));

                CSVHelper.dataUploadingCSV("Response", "After sending, getting from the server, the latest EndTime : " + ScheduleAndSampleManager.getTimeString(fromServer * Constants.MILLISECONDS_PER_SECOND));
            }catch (JSONException e){
                CSVHelper.dataUploadingCSV("Response", "JSONException result : "+result);
                CSVHelper.dataUploadingCSV("Response", Utils.getStackTrace(e));

            }
        }
    }

    public HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public String postJSON (String address, String json, String dataType, String lastSyncTime) {

        //Log.d(TAG, "[postJSON] testbackend post data to " + address);

        InputStream inputStream = null;
        String result = "";

        try {

            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //Log.d(TAG, "[postJSON] testbackend connecting to " + address);

            if (url.getProtocol().toLowerCase().equals("https")) {
                //Log.d(TAG, "[postJSON] [using https]");
                trustAllHosts();
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }


            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());

            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            conn.connect();

            OutputStreamWriter wr= new OutputStreamWriter(conn.getOutputStream());
            wr.write(json);
            wr.close();

            //Log.d(TAG, "Post:\t" + dataType + "\t" + "for lastSyncTime:" + lastSyncTime);

            int responseCode = conn.getResponseCode();

            if(responseCode >= HttpsURLConnection.HTTP_BAD_REQUEST)
                inputStream = conn.getErrorStream();
            else
                inputStream = conn.getInputStream();

            result = convertInputStreamToString(inputStream);

            //Log.d(TAG, "[postJSON] the result response code is " + responseCode);
            //Log.d(TAG, "[postJSON] the result is " + result);

        } catch (NoSuchAlgorithmException e) {
            //e.printStackTrace();
        } catch (KeyManagementException e) {
            //e.printStackTrace();
        } catch (ProtocolException e) {
            //e.printStackTrace();
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        return result;
    }

    /** process result **/
    private String convertInputStreamToString(InputStream inputStream) throws IOException{

        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null){
//            //Log.d(LOG_TAG, "[syncWithRemoteDatabase] " + line);
            result += line;
        }

        inputStream.close();
        return result;

    }

    /***
     * trust all hsot....
     */
    private void trustAllHosts() {

        X509TrustManager easyTrustManager = new X509TrustManager() {

            public void checkClientTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
                // Oh, I am easy!
            }

            public void checkServerTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
                // Oh, I am easy!
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }


        };

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {easyTrustManager};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void storeTransporatation(JSONObject data){

        //Log.d(TAG, "storeTransporatation");

        try {

            JSONArray transportationAndtimestampsJson = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.transportationMode_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            //Log.d(TAG,"SELECT * FROM "+DBHelper.transportationMode_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();
            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String transportation = transCursor.getString(2);
                    String sessionid = transCursor.getString(3);

                    //Log.d(TAG,"transportation : "+transportation+" timestamp : "+timestamp);

                    //convert into second
                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    //<timestamps, Transportation>
                    Triplet<String, String, String> transportationTuple = new Triplet<>(timestampInSec, transportation, sessionid);

                    String dataInPythonTuple = TupleHelper.toPythonTuple(transportationTuple);

                    transportationAndtimestampsJson.put(dataInPythonTuple);

                    transCursor.moveToNext();
                }

                data.put("TransportationMode",transportationAndtimestampsJson);

            }
        }catch (JSONException e){
        }catch (NullPointerException e){
        }catch (Exception e){

            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "TransportationMode");
            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
        }

        //Log.d(TAG,"data : "+ data.toString());

    }

    private void storeLocation(JSONObject data){

        //Log.d(TAG, "storeLocation");
        
        try {

            JSONArray locationAndtimestampsJson = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.STREAM_TYPE_LOCATION +" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            //Log.d(TAG,"SELECT * FROM "+DBHelper.STREAM_TYPE_LOCATION +" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {

                    String timestamp = transCursor.getString(1);
                    String latitude = transCursor.getString(2);
                    String longtitude = transCursor.getString(3);
                    String accuracy = transCursor.getString(4);
                    String altitude = transCursor.getString(5);
                    String speed = transCursor.getString(6);
                    String bearing = transCursor.getString(7);
                    String provider = transCursor.getString(8);
                    String sessionid = transCursor.getString(9);
                    //Log.d(TAG,"timestamp : "+timestamp+" latitude : "+latitude+" longtitude : "+longtitude+" accuracy : "+accuracy);

                    //convert into second
                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    //<timestamp, latitude, longitude, accuracy, altitude, speed, bearing, provider, sessionid>
                    Ennead<String, String, String, String, String, String, String, String, String> locationTuple
                            = new Ennead<>(timestampInSec, latitude, longtitude, accuracy, altitude, speed, bearing, provider, "("+sessionid+")");

                    String dataInPythonTuple = TupleHelper.toPythonTuple(locationTuple);

                    locationAndtimestampsJson.put(dataInPythonTuple);

                    transCursor.moveToNext();
                }

                data.put("Location",locationAndtimestampsJson);
            }
        }catch (JSONException e){
            //e.printStackTrace();
        }catch(NullPointerException e){
            //e.printStackTrace();
        }catch (Exception e){

            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "Location");
            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
        }

    }

    private void storeActivityRecognition(JSONObject data){

        //Log.d(TAG, "storeActivityRecognition");

        try {

            JSONArray arAndtimestampsJson = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.activityRecognition_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            //Log.d(TAG,"SELECT * FROM "+DBHelper.activityRecognition_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String mostProbableActivity = transCursor.getString(2);
                    String probableActivities = transCursor.getString(3);
                    String detectedTime = transCursor.getString(4);
                    String sessionid = transCursor.getString(5);

                    //split the mostProbableActivity into "type:conf"
                    String[] subMostActivity = mostProbableActivity.split(",");

                    String type = subMostActivity[0].split("=")[1];
                    String confidence = subMostActivity[1].split("=")[1].replaceAll("]","");

                    mostProbableActivity = type+":"+confidence;

                    //choose the top two of the probableActivities and split it into "type:conf"
                    String[] subprobableActivities = probableActivities.split("\\,");
//                    //Log.d(TAG, "subprobableActivities : "+ subprobableActivities);

                    int lastIndex = 0;
                    int count = 0;

                    while(lastIndex != -1){

                        lastIndex = probableActivities.indexOf("DetectedActivity",lastIndex);

                        if(lastIndex != -1){
                            count ++;
                            lastIndex += "DetectedActivity".length();
                        }
                    }

                    if(count == 1){
                        String type1 = subprobableActivities[0].split("=")[1];
                        String confidence1 = subprobableActivities[1].split("=")[1].replaceAll("]","");

                        probableActivities = type1+":"+confidence1;

                    }else if(count > 1){
                        String type1 = subprobableActivities[0].split("=")[1];
                        String confidence1 = subprobableActivities[1].split("=")[1].replaceAll("]","");
                        String type2 = subprobableActivities[2].split("=")[1];
                        String confidence2 = subprobableActivities[3].split("=")[1].replaceAll("]","");

                        probableActivities = type1+":"+confidence1+Constants.DELIMITER+type2+":"+confidence2;

                    }

                    //Log.d(TAG,"timestamp : "+timestamp+", mostProbableActivity : "+mostProbableActivity+", probableActivities : "+probableActivities);

                    //convert into Second
                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);
                    String detectedTimeInSec = detectedTime.substring(0, timestamp.length()-3);

                    //<timestamps, MostProbableActivity, ProbableActivities, detectedTimeInSec, sessionid>
                    Quintet<String, String, String, String, String> arTuple = new Quintet<>(timestampInSec, mostProbableActivity, probableActivities, detectedTimeInSec, sessionid);

                    String dataInPythonTuple = TupleHelper.toPythonTuple(arTuple);

                    arAndtimestampsJson.put(dataInPythonTuple);

                    transCursor.moveToNext();
                }

                data.put("ActivityRecognition", arAndtimestampsJson);
            }
        }catch (JSONException e){

        }catch (NullPointerException e){

        }catch (Exception e){

            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "Activity Recognition");
            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
        }

        //Log.d(TAG,"data : "+ data.toString());

    }

    private void storeRinger(JSONObject data){

        //Log.d(TAG, "storeRinger");

        try {

            JSONArray ringerAndtimestampsJson = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.ringer_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            //Log.d(TAG,"SELECT * FROM "+DBHelper.ringer_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String ringerMode = transCursor.getString(2);
                    String audioMode = transCursor.getString(3);
                    String streamVolumeMusic = transCursor.getString(4);
                    String streamVolumeNotification = transCursor.getString(5);
                    String streamVolumeRing = transCursor.getString(6);
                    String streamVolumeVoicecall = transCursor.getString(7);
                    String streamVolumeSystem = transCursor.getString(8);
                    String sessionid = transCursor.getString(9);

                    //Log.d(TAG,"timestamp : "+timestamp+" RingerMode : "+RingerMode+" AudioMode : "+AudioMode+
//                            " StreamVolumeMusic : "+StreamVolumeMusic+" StreamVolumeNotification : "+StreamVolumeNotification
//                            +" StreamVolumeRing : "+StreamVolumeRing +" StreamVolumeVoicecall : "+StreamVolumeVoicecall
//                            +" StreamVolumeSystem : "+StreamVolumeSystem);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    //<timestampInSec, streamVolumeSystem, streamVolumeVoicecall, streamVolumeRing,
                    // streamVolumeNotification, streamVolumeMusic, audioMode, ringerMode, sessionid>
                    Ennead<String, String, String, String, String, String, String, String, String> ringerTuple
                            = new Ennead<>(timestampInSec, streamVolumeSystem, streamVolumeVoicecall, streamVolumeRing,
                            streamVolumeNotification, streamVolumeMusic, audioMode, ringerMode, sessionid);

                    String dataInPythonTuple = TupleHelper.toPythonTuple(ringerTuple);

                    ringerAndtimestampsJson.put(dataInPythonTuple);

                    transCursor.moveToNext();
                }

                data.put("Ringer",ringerAndtimestampsJson);
            }
        }catch (JSONException e){
            //e.printStackTrace();
        }catch(NullPointerException e){
            //e.printStackTrace();
        }catch (Exception e){

            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "Ringer");
            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
        }

        //Log.d(TAG,"data : "+ data.toString());

    }

    private void storeConnectivity(JSONObject data){

        //Log.d(TAG, "storeConnectivity");

        try {

            JSONArray connectivityAndtimestampsJson = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.connectivity_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            //Log.d(TAG,"SELECT * FROM "+DBHelper.connectivity_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String NetworkType = transCursor.getString(2);
                    String IsNetworkAvailable = transCursor.getString(3);
                    String IsConnected = transCursor.getString(4);
                    String IsWifiAvailable = transCursor.getString(5);
                    String IsMobileAvailable = transCursor.getString(6);
                    String IsWifiConnected = transCursor.getString(7);
                    String IsMobileConnected = transCursor.getString(8);
                    String sessionid = transCursor.getString(9);

                    //Log.d(TAG,"timestamp : "+timestamp+" NetworkType : "+NetworkType+" IsNetworkAvailable : "+IsNetworkAvailable
//                            +" IsConnected : "+IsConnected+" IsWifiAvailable : "+IsWifiAvailable
//                            +" IsMobileAvailable : "+IsMobileAvailable +" IsWifiConnected : "+IsWifiConnected
//                            +" IsMobileConnected : "+IsMobileConnected);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    //<timestampInSec, IsMobileConnected, IsWifiConnected, IsMobileAvailable,
                    // IsWifiAvailable, IsConnected, IsNetworkAvailable, NetworkType, sessionid>
                    Ennead<String, String, String, String, String, String, String, String, String> connectivityTuple
                            = new Ennead<>(timestampInSec, IsMobileConnected, IsWifiConnected, IsMobileAvailable,
                            IsWifiAvailable, IsConnected, IsNetworkAvailable, NetworkType, sessionid);

                    String dataInPythonTuple = TupleHelper.toPythonTuple(connectivityTuple);

                    connectivityAndtimestampsJson.put(dataInPythonTuple);

                    transCursor.moveToNext();
                }

                data.put("Connectivity",connectivityAndtimestampsJson);

            }
        }catch (JSONException e){
            //e.printStackTrace();
        }catch(NullPointerException e){
            //e.printStackTrace();
        }catch (Exception e){

            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "Connectivity");
            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
        }

        //Log.d(TAG,"data : "+ data.toString());

    }

    private void storeBattery(JSONObject data){

        //Log.d(TAG, "storeBattery");

        try {

            JSONArray batteryAndtimestampsJson = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.battery_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            //Log.d(TAG,"SELECT * FROM "+DBHelper.battery_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String BatteryLevel = transCursor.getString(2);
                    String BatteryPercentage = transCursor.getString(3);
                    String BatteryChargingState = transCursor.getString(4);
                    String isCharging = transCursor.getString(5);
                    String sessionid = transCursor.getString(6);

                    //Log.d(TAG,"timestamp : "+timestamp+" BatteryLevel : "+BatteryLevel+" BatteryPercentage : "+
//                            BatteryPercentage+" BatteryChargingState : "+BatteryChargingState+" isCharging : "+isCharging);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    //<timestamps, isCharging, BatteryChargingState, BatteryPercentage, BatteryLevel, sessionid>
                    Sextet<String, String, String, String, String, String> batteryTuple
                            = new Sextet<>(timestampInSec, isCharging, BatteryChargingState, BatteryPercentage, BatteryLevel, sessionid);

                    String dataInPythonTuple = TupleHelper.toPythonTuple(batteryTuple);

                    batteryAndtimestampsJson.put(dataInPythonTuple);

                    transCursor.moveToNext();
                }

                data.put("Battery", batteryAndtimestampsJson);
            }
        }catch (JSONException e){
            //e.printStackTrace();
        }catch(NullPointerException e){
            //e.printStackTrace();
        }catch (Exception e){

            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "Battery");
            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
        }

        //Log.d(TAG,"data : "+ data.toString());

    }

    private void storeAppUsage(JSONObject data){

        //Log.d(TAG, "storeAppUsage");

        try {

            JSONArray appUsageAndtimestampsJson = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.appUsage_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.

            int rows = transCursor.getCount();

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String ScreenStatus = transCursor.getString(2);
                    String Latest_Used_App = transCursor.getString(3);
                    String Latest_Foreground_Activity = transCursor.getString(4);
                    String sessionid = transCursor.getString(5);

                    //Log.d(TAG,"timestamp : "+timestamp+" ScreenStatus : "+ScreenStatus+" Latest_Used_App : "+Latest_Used_App+" Latest_Foreground_Activity : "+Latest_Foreground_Activity);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    //<timestamp, ScreenStatus, Latest_Used_App, Latest_Foreground_Activity>
                    Quintet<String, String, String, String, String> appUsageTuple
                            = new Quintet<>(timestampInSec, ScreenStatus, Latest_Used_App, Latest_Foreground_Activity, sessionid);

                    String dataInPythonTuple = TupleHelper.toPythonTuple(appUsageTuple);

                    appUsageAndtimestampsJson.put(dataInPythonTuple);

                    transCursor.moveToNext();
                }

                data.put("AppUsage",appUsageAndtimestampsJson);
            }
        }catch (JSONException e){
            //e.printStackTrace();
        }catch(NullPointerException e){
            //e.printStackTrace();
        }catch (Exception e){

            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "AppUsage");
            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
        }

        //Log.d(TAG,"data : "+ data.toString());

    }

    private void storeActionLog(JSONObject data){

        //Log.d(TAG, "storeActionLog");

        try {

            JSONArray actionLogAndtimestampsJson = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM "+DBHelper.actionLog_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.

            int rows = cursor.getCount();
            if(rows!=0){
                cursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = cursor.getString(1);
                    String action = cursor.getString(2);
                    String userpresent = cursor.getString(3);
                    String sessionid = cursor.getString(4);

                    //convert into second
                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    //<timestamps, action>
                    Quartet<String, String, String, String> actionLogTuple = new Quartet<>(timestampInSec, action, userpresent, sessionid);

                    String dataInPythonTuple = TupleHelper.toPythonTuple(actionLogTuple);

                    actionLogAndtimestampsJson.put(dataInPythonTuple);

                    cursor.moveToNext();
                }

                data.put("ActionLog", actionLogAndtimestampsJson);

            }
        }catch (JSONException e){

        }catch(NullPointerException e){

        }catch (Exception e){

            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, "ActionLog");
            CSVHelper.storeToCSV(CSVHelper.CSV_PULLING_DATA_CHECK, Utils.getStackTrace(e));
        }

        CSVHelper.storeToCSV(CSVHelper.CSV_SESSION_ACTIONLOG_FORMAT, data.toString());

    }

    //the replacement function of IsAlive
    private void sendingUserInform(){

        if(Config.DEVICE_ID.equals("NA")){
            return;
        }

//       ex. http://mcog.asc.ohio-state.edu/apps/servicerec?deviceid=375996574474999&email=none@nobody.com&userid=3333333
//      deviceid=375996574474999&email=none@nobody.com&userid=333333

        int androidVersion = Build.VERSION.SDK_INT;

        String link = Constants.CHECK_IN_URL + "deviceid=" + Config.DEVICE_ID + "&email=" + Config.Email
                +"&userid="+ Config.USER_ID+"&android_ver="+androidVersion
                +"&Manufacturer="+Build.MANUFACTURER+"&Model="+Build.MODEL+"&Product="+Build.PRODUCT;
        String userInformInString;
        JSONObject userInform = null;

        //Log.d(TAG, "user inform link : "+ link);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                userInformInString = new HttpAsyncGetJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        link).get();
            else
                userInformInString = new HttpAsyncGetJsonTask().execute(
                        link).get();

            userInform = new JSONObject(userInformInString);

            //Log.d(TAG, "userInform : " + userInform);

            sharedPrefs.edit().putLong("lastCheckInTime", ScheduleAndSampleManager.getCurrentTimeInMillis()).apply();

        } catch (InterruptedException e) {

        } catch (ExecutionException e) {

        } catch (JSONException e){

        } catch (NullPointerException e){

        }

        //In order to set the survey link
//        setDaysInSurvey(userInform);

    }

    private long getSpecialTimeInMillis(String givenDateFormat){

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE_Slash);
        long timeInMilliseconds = 0;
        try {
            Date mDate = sdf.parse(givenDateFormat);
            timeInMilliseconds = mDate.getTime();
            //Log.d(TAG,"Date in milli :: " + timeInMilliseconds);
        } catch (ParseException e) {
            //e.printStackTrace();
        }
        return timeInMilliseconds;
    }

    public String makingDataFormat(int year,int month,int date,int hour,int min){
        String dataformat= "";

        dataformat = addZero(year)+"/"+addZero(month)+"/"+addZero(date)+" "+addZero(hour)+":"+addZero(min)+":00";
        //Log.d(TAG,"dataformat : " + dataformat);

        return dataformat;
    }

    public String getDateCurrentTimeZone(long timestamp) {
        try{
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeInMillis(timestamp);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date currenTimeZone = (Date) calendar.getTime();
            return sdf.format(currenTimeZone);
        }catch (Exception e) {
            //e.printStackTrace();
        }
        return "";
    }

    private String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }

    private class HttpAsyncGetJsonTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected String doInBackground(String... params) {

            String result=null;

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(HTTP_TIMEOUT);
                connection.setConnectTimeout(SOCKET_TIMEOUT);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                InputStream stream = connection.getInputStream();

                if (stream != null) {

                    reader = new BufferedReader(new InputStreamReader(stream));

                    StringBuffer buffer = new StringBuffer();
                    String line = "";

                    while ((line = reader.readLine()) != null) {
                        buffer.append(line+"\n");
                    }

                    return buffer.toString();
                }else{

                    return "";
                }

            } catch (MalformedURLException e) {
                Log.e(TAG, "MalformedURLException");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IOException");
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {

                }
            }

            return result;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "get http post result " + result);

        }

    }
}
