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
import android.os.Environment;
import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
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
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku.streamgenerator.ConnectivityStreamGenerator;
import edu.ohio.minuku_2.Utils;

/**
 * Created by Lawrence on 2017/8/16.
 */

public class WifiReceiver extends BroadcastReceiver {

    private final String TAG = "WifiReceiver";

    private Handler mMainThread;
    private Handler UserInformThread;

    private SharedPreferences sharedPrefs;

    private Runnable runnable = null;
    private Runnable UserInformRunnable = null;

    private int year,month,day,hour,min;
    private int lastDay;

    private long _id = -9;
    private long latestUpdatedTime = -9999;
    private long nowTime = -9999;
    private long startTime = -9999;
    private long endTime = -9999;

    public Context context;

    public static final int HTTP_TIMEOUT = 10000; // millisecond
    public static final int SOCKET_TIMEOUT = 20000; // millisecond

    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";

    private static final String postTripUrl = "http://mcog.asc.ohio-state.edu/apps/tripdump/";
    private static final String postDumpUrl = "http://mcog.asc.ohio-state.edu/apps/devicedump/";

    public static int mainThreadUpdateFrequencyInSeconds = 10; //originally 10s.
    public static int sendingUserInformThreadUpdateFrequencyInSeconds = 3;

    public static long mainThreadUpdateFrequencyInMilliseconds = mainThreadUpdateFrequencyInSeconds * Constants.MILLISECONDS_PER_SECOND;
    public static long sendingUserInformThreadUpdateFrequencyInMilliseconds
            = sendingUserInformThreadUpdateFrequencyInSeconds * Constants.MILLISECONDS_PER_HOUR;

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive");

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        //get timzone //prevent the issue when the user start the app in wifi available environment.
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        int mYear = cal.get(Calendar.YEAR);
        int mMonth = cal.get(Calendar.MONTH)+1;
        int mDay = cal.get(Calendar.DAY_OF_MONTH);

        this.context = context;

        sharedPrefs = context.getSharedPreferences("edu.umich.minuku_2", context.MODE_PRIVATE);

        year = sharedPrefs.getInt("StartYear", mYear);
        month = sharedPrefs.getInt("StartMonth", mMonth);
        day = sharedPrefs.getInt("StartDay", mDay);

        lastDay = sharedPrefs.getInt("lastDay", day);

        Constants.USER_ID = sharedPrefs.getString("userid","NA");
        Constants.GROUP_NUM = sharedPrefs.getString("groupNum","NA");
        Constants.Email = sharedPrefs.getString("Email", "NA");

        hour = sharedPrefs.getInt("StartHour", 0);
        min = sharedPrefs.getInt("StartMin",0);

        Log.d(TAG, "year : "+ year+" month : "+ month+" day : "+ day+" hour : "+ hour+" min : "+ min);

        if (activeNetwork != null) {
            // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                Log.d(TAG,"Wifi activeNetwork");

                Log.d(TAG, "checking is there a runnable running");

                if(runnable==null) {

                    Log.d(TAG, "there is no runnable running yet.");

                    MakingJsonDataMainThread();
                    SendingUserInformThread();
                }
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                Log.d(TAG, "MOBILE activeNetwork");

                Log.d(TAG, "checking is there a runnable running");

                if(runnable==null){

                    Log.d(TAG, "there is no runnable running yet.");

                    MakingJsonDataMainThread();
                    SendingUserInformThread();
                }
            }
        } else {
            // not connected to the internet
            Log.d(TAG, "no Network" ) ;
            if(runnable!=null) {
                //TODO check the switch of the wifi and mobile could keep the runnable run.
//                Log.d(TAG, "killing thread" ) ;
//                mMainThread.removeCallbacks(runnable);
            }
        }

    }

    public void SendingUserInformThread(){

        UserInformThread = new Handler();

        UserInformRunnable = new Runnable() {

            @Override
            public void run() {

                Log.d(TAG, "SendingUserInformThread");

//                Log.d(TAG, "mainThreadUpdateFrequencyInMilliseconds : "+mainThreadUpdateFrequencyInMilliseconds);
//                Log.d(TAG, "sendingUserInformThreadUpdateFrequencyInMilliseconds : "+sendingUserInformThreadUpdateFrequencyInMilliseconds);

                // Trip, isAlive
                if(ConnectivityStreamGenerator.mIsWifiConnected || ConnectivityStreamGenerator.mIsMobileConnected) {

                    Log.d(TAG, "loading UserInform data");

                    //TODO replace the UserInform
                    //by sending http://mcog.asc.ohio-state.edu/apps/servicerec?deviceid=3559960704778000&email=test.com&userId=XXXX
                    sendingUserInform();

                }

                UserInformThread.postDelayed(this, sendingUserInformThreadUpdateFrequencyInMilliseconds);

            }
        };

        UserInformThread.post(UserInformRunnable);
    }

    public void MakingJsonDataMainThread(){

        mMainThread = new Handler();

        runnable = new Runnable() {

            @Override
            public void run() {

                Log.d(TAG, "MakingJsonDataMainThread") ;

                Log.d(TAG, "loading Dump data");

                //dump only can be sent when wifi is connected
                if(ConnectivityStreamGenerator.mIsWifiConnected){

                    //TODO update endtime to get the latest data's time from MongoDB
                    //TODO endtime = latest data's time + nextinterval

                    long lastSentStarttime = sharedPrefs.getLong("lastSentStarttime", 0);

                    if(lastSentStarttime == 0){
                        //if it doesn't reponse the setting with initialize ones
                        //initialize
                        long startstartTime = getSpecialTimeInMillis(makingDataFormat(year,month,day,hour,min));
                        startTime = sharedPrefs.getLong("StartTime", startstartTime); //default
                        Log.d(TAG,"StartTimeString : " + getTimeString(startTime));


                        //initialize
                        long startendTime = getSpecialTimeInMillis(makingDataFormat(year,month,day,hour+1,min));
                        endTime = sharedPrefs.getLong("EndTime", startendTime);
                        Log.d(TAG,"EndTimeString : " + getTimeString(endTime));

                    }else{
                        //if it do reponse the setting with initialize ones
                        startTime = Long.valueOf(lastSentStarttime);
                        Log.d(TAG,"StartTimeString : " + getTimeString(startTime));

                        long nextinterval = 1 * 60 * 60000; //1 hr

                        endTime = Long.valueOf(lastSentStarttime) + nextinterval;
                        Log.d(TAG,"EndTimeString : " + getTimeString(endTime));

                    }

                    nowTime = new Date().getTime();
                    Log.d(TAG,"NowTimeString : " + getTimeString(nowTime));
                    Log.d(TAG,"NowTime : " + nowTime);

                    if(nowTime > endTime && ConnectivityStreamGenerator.mIsWifiConnected == true) {

                        sendingDumpData();

                        //setting nextime interval
                        latestUpdatedTime = endTime;
                        startTime = latestUpdatedTime;

                        long nextinterval = 1 * 60 * 60000; //1 hr

                        endTime = startTime + nextinterval;

                        Log.d(TAG,"latestUpdatedTime : " + latestUpdatedTime);
                        Log.d(TAG,"latestUpdatedTime + 1 hour : " + latestUpdatedTime + nextinterval);

                        //update the last data's startTime.
                        sharedPrefs.edit().putLong("lastSentStarttime", startTime).apply();

                    }

                }

                // Trip, isAlive
                if(ConnectivityStreamGenerator.mIsWifiConnected || ConnectivityStreamGenerator.mIsMobileConnected) {

                    Log.d(TAG, "loading IsAlive data");

                    //TODO replace the isAlive
                    //by sending http://mcog.asc.ohio-state.edu/apps/servicerec?deviceid=3559960704778000&email=test.com&userId=XXXX
//                    sendingIsAliveData();
//                    sendingUserInform();

                    Log.d(TAG, "loading Annotated Trip data");

                    sendingAnnotatedTripData();

                }

                mMainThread.postDelayed(this, mainThreadUpdateFrequencyInMilliseconds);

            }
        };

        mMainThread.post(runnable);
    }

    //TODO same function in MainAcitvity, need to intergrate.
    //the replacement function of IsAlive
    private void sendingUserInform(){

        if(Constants.DEVICE_ID.equals("NA")){
            return;
        }

//       ex. http://mcog.asc.ohio-state.edu/apps/servicerec?deviceid=375996574474999&email=none@nobody.com&userid=3333333
//      deviceid=375996574474999&email=none@nobody.com&userid=3333333
        String link = Constants.checkInUrl + "deviceid=" + Constants.DEVICE_ID + "&email=" + Constants.Email+"&userid="+Constants.USER_ID;
        String userInformInString = null;
        JSONObject userInform = null;

        Log.d(TAG, "user inform link : "+ link);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                userInformInString = new HttpAsyncGetUserInformFromServer().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        link).get();
            else
                userInformInString = new HttpAsyncGetUserInformFromServer().execute(
                        link).get();

            userInform = new JSONObject(userInformInString);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e){
            e.printStackTrace();
        }

        Log.d(TAG, "userInform : " + userInform);

        //In order to set the survey link
        setDaysInSurvey(userInform);

//        setMidnightStart(userInform);
    }

    private void setDaysInSurvey(JSONObject userInform){

        try{
            Constants.daysInSurvey = userInform.getInt("daysinsurvey");

            sharedPrefs.edit().putInt("daysInSurvey", Constants.daysInSurvey).apply();

            Log.d(TAG, "daysInSurvey : "+ Constants.daysInSurvey);
        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    private void setMidnightStart(JSONObject userInform){

        try{
            Constants.midnightstart = userInform.getLong("midnightstart");

            sharedPrefs.edit().putLong("midnightstart", Constants.midnightstart).apply();

            Log.d(TAG, "midnightstart : "+ Constants.midnightstart);
        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    private void sendingIsAliveData(){

        //making isAlive
        JSONObject data = new JSONObject();
        try {
            long currentTime = new Date().getTime();
            String currentTimeString = getTimeString(currentTime);

            long currentTimeInSec = currentTime/Constants.MILLISECONDS_PER_SECOND;

            data.put("time", currentTimeInSec);
            data.put("timeString", currentTimeString);

            data.put("user_id", Constants.USER_ID);
            data.put("group_number", Constants.GROUP_NUM);
            data.put("device_id", Constants.DEVICE_ID);
            data.put("email", Constants.Email);

        }catch (JSONException e){
            e.printStackTrace();
        }

        Log.d(TAG,"checking data IsAlive : "+ data.toString());

        //TODO wait for the URL to send the isAlive data
        /*try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                new HttpAsyncPostJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        postIsAliveUrl_insert + Constants.DEVICE_ID,
                        data.toString(),
                        "isAlive",
                        curr).get();
            else
                new HttpAsyncPostJsonTask().execute(
                        postIsAliveUrl_insert + Constants.DEVICE_ID,
                        data.toString(),
                        "isAlive",
                        curr).get();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }*/

    }

    public void sendingAnnotatedTripData(){

        Log.d(TAG, "sendingAnnotatedTripData") ;

        //getting the latest sessionid from the server side
        //TODO improve it to get the real latest session id
        int latestSessionidFromServer = sharedPrefs.getInt("latestSessionidFromServer", 1);

        Session mSession = null;

        try {
            mSession = SessionManager.getSession(latestSessionidFromServer);

            //No session yet, wait for next time.
        }catch (IndexOutOfBoundsException e){
            e.printStackTrace();
            return;
        }
        //tell if the session has been labeled. It is in the annotaiton with ESM tag
        ArrayList<Annotation> annotations = mSession.getAnnotationsSet().getAnnotationByTag("ESM");

        JSONObject ESMJSON=null;

        //{"Entire_session":true,"Tag":["ESM"],"Content":"{\"ans1\":\"Walking outdoors.\",\"ans2\":\"Food\",\"ans3\":\"No\",\"ans4\":\"Right before\"}"}
        if (annotations.size()>0){

            JSONObject annotatedtripdata = new JSONObject();

            try {

                //getting the answers in the annotation.
                String content = annotations.get(0).getContent();
                ESMJSON = new JSONObject(content);
                Log.d(TAG, "[test show trip] the contentofJSON ESMJSONObject  is " + ESMJSON );

                //adding the id and group number
                annotatedtripdata.put("user_id", Constants.USER_ID);
                annotatedtripdata.put("group_number", Constants.GROUP_NUM);
                annotatedtripdata.put("device_id", Constants.DEVICE_ID);

                //adding the time info.
                long sessionStartTime = mSession.getStartTime();
                long sessionEndTime = mSession.getEndTime();
                long StartTime = sessionStartTime / Constants.MILLISECONDS_PER_SECOND;
                long EndTime = sessionEndTime / Constants.MILLISECONDS_PER_SECOND;

                Log.d(TAG, "Annotation StartTime : " + StartTime);
                Log.d(TAG, "Annotation EndTime : " + EndTime);

                annotatedtripdata.put("StartTime", StartTime);
                annotatedtripdata.put("EndTime", EndTime);
                annotatedtripdata.put("StartTimeString", ScheduleAndSampleManager.getTimeString(sessionStartTime));
                annotatedtripdata.put("EndTimeString", ScheduleAndSampleManager.getTimeString(sessionEndTime));

                //convert the time from millisecond to second
                long annotationOpenTimes = ESMJSON.getLong("openTimes");
                ESMJSON.put("openTimes", annotationOpenTimes / Constants.MILLISECONDS_PER_SECOND);

                //adding the annotations data
                annotatedtripdata.put("Annotations", ESMJSON);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            String curr = getDateCurrentTimeZone(new Date().getTime());

//            {"user_id":"147866","group_number":"1","device_id":"352875086398624","StartTime":1519629286657,"EndTime":1519630452475,
//              "StartTimeString":"2018\/02\/26 15:14:46 +0800","EndTimeString":"2018\/02\/26 15:34:12 +0800",
//              "Annotations":{"openTimes":"[1519632082274]","submitTime":0,"ans1":"Walking outdoors.","ans2":"Yes","ans3":"Yes","ans4":"On time"}}
            Log.d(TAG, "checking data annotatedtrip : "+annotatedtripdata.toString());

            //TODO upload to MongoDB
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    new HttpAsyncPostJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            postTripUrl,
                            annotatedtripdata.toString(),
                            "Trip",
                            curr).get();
                else
                    new HttpAsyncPostJsonTask().execute(
                            postTripUrl,
                            annotatedtripdata.toString(),
                            "Trip",
                            curr).get();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }

    }

    public void MakingAnnotatedTripData(long startTime, long endTime){
        Log.d(TAG, "MakingAnnotatedTripData") ;
//        JSONObject data = new JSONObject();

        boolean upload = true;

        JSONObject annotatedtripdata = new JSONObject();

        try {
            annotatedtripdata.put("user_id", Constants.USER_ID);
            annotatedtripdata.put("group_number", Constants.GROUP_NUM);
            annotatedtripdata.put("device_id", Constants.DEVICE_ID);

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
//            Cursor transCursor = db.rawQuery("SELECT * FROM " + DBHelper.annotate_table + " WHERE " + DBHelper.id + " = '" + _id + "' ", null);
//            Log.d(TAG, "SELECT * FROM " + DBHelper.annotate_table + " WHERE " + DBHelper.id + " = '" + _id + "' ");
            Cursor transCursor = db.rawQuery("SELECT * FROM " + DBHelper.annotate_table + " WHERE " + DBHelper.Trip_startTime + " BETWEEN '" + startTime + "' "+"AND"+" '"+endTime+"' ", null);
            Log.d(TAG, "SELECT * FROM " + DBHelper.annotate_table + " WHERE " + DBHelper.Trip_startTime + " BETWEEN " + startTime + "' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            Log.d(TAG, "rows : " + rows);
            transCursor.moveToFirst();

//            if (rows != 0) {
            while(rows != 0){

//                JSONObject annotatedtripdata = new JSONObject();

                annotatedtripdata.put("user_id", Constants.USER_ID);
                annotatedtripdata.put("group_number", Constants.GROUP_NUM);
                annotatedtripdata.put("device_id", Constants.DEVICE_ID);

                String start = transCursor.getString(2);
                String end = transCursor.getString(3);
//                String activityType = transCursor.getString(5);
//                String preplan = transCursor.getString(6);
                String ans1 = transCursor.getString(5);
                String ans2 = transCursor.getString(6);
                String ans3 = transCursor.getString(7);
                String ans4 = transCursor.getString(8);
//                String ans4_1 = transCursor.getString(10);
//                String ans4_2 = transCursor.getString(11);
//                String lat = transCursor.getString(12);
//                String lng = transCursor.getString(13);
                String lat = transCursor.getString(9);
                String lng = transCursor.getString(10);
                String tripType = transCursor.getString(11);

                annotatedtripdata.put("start", getSpecialTimeInMillis(start));
                annotatedtripdata.put("end", getSpecialTimeInMillis(end));
                annotatedtripdata.put("startTime", start);
                annotatedtripdata.put("endTime", end);

                JSONArray latLngs = new JSONArray();
                latLngs.put(lat);
                latLngs.put(lng);

                //TODO from here we need to split it into several jsonarray for each trip
//                annotatedtripdata.put("ActivityType", activityType);
                annotatedtripdata.put("TripType", tripType);
//                annotatedtripdata.put("Preplan", preplan);
                annotatedtripdata.put("ques1", ans1);
                annotatedtripdata.put("ques2", ans2);
                annotatedtripdata.put("ques3", ans3);
                annotatedtripdata.put("ques4", ans4);
//                annotatedtripdata.put("ques4_1", ans4_1);
//                annotatedtripdata.put("ques4_2", ans4_2);

                Log.d(TAG, "final annotatedtripdata : " + annotatedtripdata.toString());

                String curr = getDateCurrentTimeZone(new Date().getTime());

//                storeTripToLocalFolder(annotatedtripdata);

                //TODO upload to MongoDB
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                        new HttpAsyncPostJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                postTripUrl,
                                annotatedtripdata.toString(),
                                "Trip",
                                curr).get();
                    else
                        new HttpAsyncPostJsonTask().execute(
                                postTripUrl,
                                annotatedtripdata.toString(),
                                "Trip",
                                curr).get();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                rows--;

                transCursor.moveToNext();

            }
        } catch (JSONException e) {
            upload = false;
            e.printStackTrace();
        }catch (Exception e) {
            upload = false;
            e.printStackTrace();
        } finally {
            if (upload) {
                _id++;
                sharedPrefs.edit().putLong("Trip_id", _id);
                sharedPrefs.edit().apply();
            }
        }

    }

    public void sendingDumpData(){

        Log.d(TAG, "sendingDumpData") ;

        JSONObject data = new JSONObject();

        try {
            data.put("user_id", Constants.USER_ID);
            data.put("group_number", Constants.GROUP_NUM);
            data.put("device_id", Constants.DEVICE_ID);

            long startTimeInSec = startTime/Constants.MILLISECONDS_PER_SECOND;
            long endTimeInSec = endTime/Constants.MILLISECONDS_PER_SECOND;

            data.put("start", startTimeInSec);
            data.put("end", endTimeInSec);
            data.put("startTimeString", getTimeString(startTime));
            data.put("endTimeString", getTimeString(endTime));
        }catch (JSONException e){
            e.printStackTrace();
        }

        storeTransporatation(data);
        storeLocation(data);
        storeActivityRecognition(data);
        storeRinger(data);
        storeConnectivity(data);
        storeBattery(data);
        storeAppUsage(data);

        Log.d(TAG,"checking data Dump : "+ data.toString());

        String curr =  getDateCurrentTimeZone(new Date().getTime());

        //TODO upload to MongoDB
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                new HttpAsyncPostJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    postDumpUrl,
                    data.toString(),
                    "Dump",
                    curr).get();
            else
                new HttpAsyncPostJsonTask().execute(
                        postDumpUrl,
                        data.toString(),
                        "Dump",
                        curr).get();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    //use HTTPAsyncTask to poHttpAsyncPostJsonTaskst data
    private class HttpAsyncPostJsonTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String result=null;
            String url = params[0];
            String data = params[1];
            String dataType = params[2];
            String lastSyncTime = params[3];

            postJSON(url, data, dataType, lastSyncTime);

            return result;
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "get http post result " + result);
        }

    }

    public HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public String postJSON (String address, String json, String dataType, String lastSyncTime) {

        Log.d(TAG, "[postJSON] testbackend post data to " + address);

        InputStream inputStream = null;
        String result = "";

        try {

            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(TAG, "[postJSON] testbackend connecting to " + address);

            if (url.getProtocol().toLowerCase().equals("https")) {
                Log.d(TAG, "[postJSON] [using https]");
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

            Log.d(TAG, "Post:\t" + dataType + "\t" + "for lastSyncTime:" + lastSyncTime);

            int responseCode = conn.getResponseCode();

            if(responseCode >= 400)
                inputStream = conn.getErrorStream();
            else
                inputStream = conn.getInputStream();

            result = convertInputStreamToString(inputStream);

            Log.d(TAG, "[postJSON] the result response code is " + responseCode);
            Log.d(TAG, "[postJSON] the result is " + result);

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /** process result **/
    private String convertInputStreamToString(InputStream inputStream) throws IOException{

        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null){
//            Log.d(LOG_TAG, "[syncWithRemoteDatabase] " + line);
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
            e.printStackTrace();
        }
    }

    private void storeTransporatation(JSONObject data){

        Log.d(TAG, "storeTransporatation");

        try {

            JSONObject transportationAndtimestampsJson = new JSONObject();

            JSONArray transportations = new JSONArray();
            JSONArray timestampsInSec = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.transportationMode_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM "+DBHelper.transportationMode_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            Log.d(TAG, "rows : "+rows);

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String transportation = transCursor.getString(2);

                    Log.d(TAG,"transportation : "+transportation+" timestamp : "+timestamp);

                    transportations.put(transportation);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    timestampsInSec.put(timestampInSec);

                    transCursor.moveToNext();
                }

                transportationAndtimestampsJson.put("Transportation",transportations);
                transportationAndtimestampsJson.put("timestamps",timestampsInSec);

                data.put("TransportationMode",transportationAndtimestampsJson);

            }

        }catch (JSONException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        Log.d(TAG,"data : "+ data.toString());

    }

    private void storeLocation(JSONObject data){

        Log.d(TAG, "storeLocation");

        try {

            JSONObject locationAndtimestampsJson = new JSONObject();

            JSONArray accuracys = new JSONArray();
            JSONArray longtitudes = new JSONArray();
            JSONArray latitudes = new JSONArray();
            JSONArray timestampsInSec = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.STREAM_TYPE_LOCATION +" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM "+DBHelper.STREAM_TYPE_LOCATION +" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            Log.d(TAG, "rows : "+rows);

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String latitude = transCursor.getString(2);
                    String longtitude = transCursor.getString(3);
                    String accuracy = transCursor.getString(4);

                    Log.d(TAG,"timestamp : "+timestamp+" latitude : "+latitude+" longtitude : "+longtitude+" accuracy : "+accuracy);

                    accuracys.put(accuracy);
                    longtitudes.put(longtitude);
                    latitudes.put(latitude);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    timestampsInSec.put(timestampInSec);

                    transCursor.moveToNext();
                }

                locationAndtimestampsJson.put("Accuracy",accuracys);
                locationAndtimestampsJson.put("Longtitudes",longtitudes);
                locationAndtimestampsJson.put("Latitudes",latitudes);
                locationAndtimestampsJson.put("timestamps",timestampsInSec);

                data.put("Location",locationAndtimestampsJson);

            }

        }catch (JSONException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        Log.d(TAG,"data : "+ data.toString());

    }

    private void storeActivityRecognition(JSONObject data){

        Log.d(TAG, "storeActivityRecognition");

        try {

            JSONObject arAndtimestampsJson = new JSONObject();

            JSONArray mostProbableActivityz = new JSONArray();
            JSONArray probableActivitiesz = new JSONArray();
            JSONArray timestampsInSec = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.activityRecognition_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM "+DBHelper.activityRecognition_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            Log.d(TAG, "rows : "+rows);

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String mostProbableActivity = transCursor.getString(2);
                    String probableActivities = transCursor.getString(3);

                    //split the mostProbableActivity into "type:conf"
                    String[] subMostActivity = mostProbableActivity.split(",");

                    String type = subMostActivity[0].split("=")[1];
                    String confidence = subMostActivity[1].split("=")[1].replaceAll("]","");

                    mostProbableActivity = type+":"+confidence;

                    //choose the top two of the probableActivities and split it into "type:conf"
                    String[] subprobableActivities = probableActivities.split("\\,");
//                    Log.d(TAG, "subprobableActivities : "+ subprobableActivities);

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

                        probableActivities = type1+":"+confidence1+","+type2+":"+confidence2;

                    }

                    Log.d(TAG,"timestamp : "+timestamp+", mostProbableActivity : "+mostProbableActivity+", probableActivities : "+probableActivities);

                    mostProbableActivityz.put(mostProbableActivity);
                    probableActivitiesz.put(probableActivities);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    timestampsInSec.put(timestampInSec);

                    transCursor.moveToNext();
                }

                arAndtimestampsJson.put("MostProbableActivity",mostProbableActivityz);
                arAndtimestampsJson.put("ProbableActivities",probableActivitiesz);
                arAndtimestampsJson.put("timestamps",timestampsInSec);

                data.put("ActivityRecognition",arAndtimestampsJson);

            }

        }catch (JSONException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        Log.d(TAG,"data : "+ data.toString());

    }

    private void storeRinger(JSONObject data){

        Log.d(TAG, "storeRinger");

        try {

            JSONObject ringerAndtimestampsJson = new JSONObject();

            JSONArray StreamVolumeSystems = new JSONArray();
            JSONArray StreamVolumeVoicecalls = new JSONArray();
            JSONArray StreamVolumeRings = new JSONArray();
            JSONArray StreamVolumeNotifications = new JSONArray();
            JSONArray StreamVolumeMusics = new JSONArray();
            JSONArray AudioModes = new JSONArray();
            JSONArray RingerModes = new JSONArray();
            JSONArray timestampsInSec = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.ringer_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM "+DBHelper.ringer_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            Log.d(TAG, "rows : "+rows);

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String RingerMode = transCursor.getString(2);
                    String AudioMode = transCursor.getString(3);
                    String StreamVolumeMusic = transCursor.getString(4);
                    String StreamVolumeNotification = transCursor.getString(5);
                    String StreamVolumeRing = transCursor.getString(6);
                    String StreamVolumeVoicecall = transCursor.getString(7);
                    String StreamVolumeSystem = transCursor.getString(8);

                    Log.d(TAG,"timestamp : "+timestamp+" RingerMode : "+RingerMode+" AudioMode : "+AudioMode+
                            " StreamVolumeMusic : "+StreamVolumeMusic+" StreamVolumeNotification : "+StreamVolumeNotification
                            +" StreamVolumeRing : "+StreamVolumeRing +" StreamVolumeVoicecall : "+StreamVolumeVoicecall
                            +" StreamVolumeSystem : "+StreamVolumeSystem);

                    StreamVolumeSystems.put(StreamVolumeSystem);
                    StreamVolumeVoicecalls.put(StreamVolumeVoicecall);
                    StreamVolumeRings.put(StreamVolumeRing);
                    StreamVolumeNotifications.put(StreamVolumeNotification);
                    StreamVolumeMusics.put(StreamVolumeMusic);
                    AudioModes.put(AudioMode);
                    RingerModes.put(RingerMode);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    timestampsInSec.put(timestampInSec);

                    transCursor.moveToNext();
                }

                ringerAndtimestampsJson.put("RingerMode",RingerModes);
                ringerAndtimestampsJson.put("AudioMode",AudioModes);
                ringerAndtimestampsJson.put("StreamVolumeMusic",StreamVolumeMusics);
                ringerAndtimestampsJson.put("StreamVolumeNotification",StreamVolumeNotifications);
                ringerAndtimestampsJson.put("StreamVolumeRing",StreamVolumeRings);
                ringerAndtimestampsJson.put("StreamVolumeVoicecall",StreamVolumeVoicecalls);
                ringerAndtimestampsJson.put("StreamVolumeSystem",StreamVolumeSystems);
                ringerAndtimestampsJson.put("timestamps",timestampsInSec);

                data.put("Ringer",ringerAndtimestampsJson);

            }

        }catch (JSONException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        Log.d(TAG,"data : "+ data.toString());

    }

    private void storeConnectivity(JSONObject data){

        Log.d(TAG, "storeConnectivity");

        try {

            JSONObject connectivityAndtimestampsJson = new JSONObject();

            JSONArray IsMobileConnecteds = new JSONArray();
            JSONArray IsWifiConnecteds = new JSONArray();
            JSONArray IsMobileAvailables = new JSONArray();
            JSONArray IsWifiAvailables = new JSONArray();
            JSONArray IsConnecteds = new JSONArray();
            JSONArray IsNetworkAvailables = new JSONArray();
            JSONArray NetworkTypes = new JSONArray();
            JSONArray timestampsInSec = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.connectivity_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM "+DBHelper.connectivity_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            Log.d(TAG, "rows : "+rows);

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

                    Log.d(TAG,"timestamp : "+timestamp+" NetworkType : "+NetworkType+" IsNetworkAvailable : "+IsNetworkAvailable
                            +" IsConnected : "+IsConnected+" IsWifiAvailable : "+IsWifiAvailable
                            +" IsMobileAvailable : "+IsMobileAvailable +" IsWifiConnected : "+IsWifiConnected
                            +" IsMobileConnected : "+IsMobileConnected);

                    IsMobileConnecteds.put(IsMobileConnected);
                    IsWifiConnecteds.put(IsWifiConnected);
                    IsMobileAvailables.put(IsMobileAvailable);
                    IsWifiAvailables.put(IsWifiAvailable);
                    IsConnecteds.put(IsConnected);
                    IsNetworkAvailables.put(IsNetworkAvailable);
                    NetworkTypes.put(NetworkType);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    timestampsInSec.put(timestampInSec);

                    transCursor.moveToNext();
                }

                connectivityAndtimestampsJson.put("NetworkType",NetworkTypes);
                connectivityAndtimestampsJson.put("IsNetworkAvailable",IsNetworkAvailables);
                connectivityAndtimestampsJson.put("IsConnected",IsConnecteds);
                connectivityAndtimestampsJson.put("IsWifiAvailable",IsWifiAvailables);
                connectivityAndtimestampsJson.put("IsMobileAvailable",IsMobileAvailables);
                connectivityAndtimestampsJson.put("IsWifiConnected",IsWifiConnecteds);
                connectivityAndtimestampsJson.put("IsMobileConnected",IsMobileConnecteds);
                connectivityAndtimestampsJson.put("timestamps",timestampsInSec);

                data.put("Connectivity",connectivityAndtimestampsJson);

            }

        }catch (JSONException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        Log.d(TAG,"data : "+ data.toString());

    }

    private void storeBattery(JSONObject data){

        Log.d(TAG, "storeBattery");

        try {

            JSONObject batteryAndtimestampsJson = new JSONObject();

            JSONArray BatteryLevels = new JSONArray();
            JSONArray BatteryPercentages = new JSONArray();
            JSONArray BatteryChargingStates = new JSONArray();
            JSONArray isChargings = new JSONArray();
            JSONArray timestampsInSec = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.battery_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM "+DBHelper.battery_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            Log.d(TAG, "rows : "+rows);

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String BatteryLevel = transCursor.getString(2);
                    String BatteryPercentage = transCursor.getString(3);
                    String BatteryChargingState = transCursor.getString(4);
                    String isCharging = transCursor.getString(5);

                    Log.d(TAG,"timestamp : "+timestamp+" BatteryLevel : "+BatteryLevel+" BatteryPercentage : "+
                            BatteryPercentage+" BatteryChargingState : "+BatteryChargingState+" isCharging : "+isCharging);

                    BatteryLevels.put(BatteryLevel);
                    BatteryPercentages.put(BatteryPercentage);
                    BatteryChargingStates.put(BatteryChargingState);
                    isChargings.put(isCharging);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    timestampsInSec.put(timestampInSec);

                    transCursor.moveToNext();
                }

                batteryAndtimestampsJson.put("BatteryLevel",BatteryLevels);
                batteryAndtimestampsJson.put("BatteryPercentage",BatteryPercentages);
                batteryAndtimestampsJson.put("BatteryChargingState",BatteryChargingStates);
                batteryAndtimestampsJson.put("isCharging",isChargings);
                batteryAndtimestampsJson.put("timestamps",timestampsInSec);

                data.put("Battery",batteryAndtimestampsJson);

            }

        }catch (JSONException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        Log.d(TAG,"data : "+ data.toString());

    }

    private void storeAppUsage(JSONObject data){

        Log.d(TAG, "storeAppUsage");

        try {

            JSONObject appUsageAndtimestampsJson = new JSONObject();

            JSONArray ScreenStatusz = new JSONArray();
            JSONArray Latest_Used_Apps = new JSONArray();
            JSONArray Latest_Foreground_Activitys = new JSONArray();
            JSONArray timestampsInSec = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.appUsage_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM "+DBHelper.appUsage_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

            int rows = transCursor.getCount();

            Log.d(TAG, "rows : "+rows);

            if(rows!=0){
                transCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    String timestamp = transCursor.getString(1);
                    String ScreenStatus = transCursor.getString(2);
                    String Latest_Used_App = transCursor.getString(3);
                    String Latest_Foreground_Activity = transCursor.getString(4);

                    Log.d(TAG,"timestamp : "+timestamp+" ScreenStatus : "+ScreenStatus+" Latest_Used_App : "+Latest_Used_App+" Latest_Foreground_Activity : "+Latest_Foreground_Activity);

                    ScreenStatusz.put(ScreenStatus);
                    Latest_Used_Apps.put(Latest_Used_App);
                    Latest_Foreground_Activitys.put(Latest_Foreground_Activity);

                    String timestampInSec = timestamp.substring(0, timestamp.length()-3);

                    timestampsInSec.put(timestampInSec);

                    transCursor.moveToNext();
                }

                appUsageAndtimestampsJson.put("ScreenStatus",ScreenStatusz);
                appUsageAndtimestampsJson.put("Latest_Used_App",Latest_Used_Apps);
//                appUsageAndtimestampsJson.put("Latest_Foreground_Activity",Latest_Foreground_Activitys);
                appUsageAndtimestampsJson.put("timestamps",timestampsInSec);

                data.put("AppUsage",appUsageAndtimestampsJson);

            }


        }catch (JSONException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        Log.d(TAG,"data : "+ data.toString());

    }

    public String makingDataFormat(int year,int month,int date){
        String dataformat= "";

//        dataformat = addZero(year)+"-"+addZero(month)+"-"+addZero(date)+" "+addZero(hour)+":"+addZero(min)+":00";
        dataformat = addZero(year)+"/"+addZero(month)+"/"+addZero(date)+" "+"00:00:00";
        Log.d(TAG,"dataformat : " + dataformat);

        return dataformat;
    }


    private long getSpecialTimeInMillis(String givenDateFormat){
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE_Slash);
        long timeInMilliseconds = 0;
        try {
            Date mDate = sdf.parse(givenDateFormat);
            timeInMilliseconds = mDate.getTime();
            Log.d(TAG,"Date in milli :: " + timeInMilliseconds);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timeInMilliseconds;
    }

    private long getSpecialTimeInMillis(int year,int month,int date,int hour,int min){
//        TimeZone tz = TimeZone.getDefault(); tz
        Calendar cal = Calendar.getInstance();
//        cal.set(year,month,date,hour,min,0);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, date);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);

        long t = cal.getTimeInMillis();

        return t;
    }

    private void storeTripToLocalFolder(JSONObject completedJson){
        Log.d(TAG, "storeTripToLocalFolder");

        String sFileName = "Trip_"+getTimeString(startTime)+"_"+getTimeString(endTime)+".json";

        Log.d(TAG, "sFileName : "+ sFileName);

        try {
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            FileWriter fileWriter = new FileWriter(root+sFileName, true);
            fileWriter.write(completedJson.toString());
            fileWriter.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    private void storeToLocalFolder(JSONObject completedJson){
        Log.d(TAG, "storeToLocalFolder");

        String sFileName = "Dump_"+getTimeString(startTime)+"_"+getTimeString(endTime)+".json";

        Log.d(TAG, "sFileName : "+ sFileName);

        try {
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            FileWriter fileWriter = new FileWriter(root+sFileName, true);
            fileWriter.write(completedJson.toString());
            fileWriter.close();

        } catch(IOException e) {
            e.printStackTrace();
        }

    }
    //TODO remember the format is different from the normal one.
    public static String getTimeString(long time){

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_Dash);
        String currentTimeString = sdf_now.format(time);

        return currentTimeString;
    }

    public String makingDataFormat(int year,int month,int date,int hour,int min){
        String dataformat= "";

        dataformat = addZero(year)+"/"+addZero(month)+"/"+addZero(date)+" "+addZero(hour)+":"+addZero(min)+":00";
        Log.d(TAG,"dataformat : " + dataformat);

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
            e.printStackTrace();
        }
        return "";
    }

    private String getmillisecondToDateWithTime(long timeStamp){

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH)+1;
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int mhour = calendar.get(Calendar.HOUR_OF_DAY);
        int mMin = calendar.get(Calendar.MINUTE);
        int mSec = calendar.get(Calendar.SECOND);

        return addZero(mYear)+"/"+addZero(mMonth)+"/"+addZero(mDay)+" "+addZero(mhour)+":"+addZero(mMin)+":"+addZero(mSec);

    }

    private String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }

    /**get the current time in milliseconds**/
    private long getCurrentTimeInMillis(){
        //get timzone
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        //get the date of now: the first month is Jan:0
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int Hour = cal.get(Calendar.HOUR);
        int Min = cal.get(Calendar.MINUTE);

        long t = getSpecialTimeInMillis(year,month,day,Hour,Min);
        return t;
    }

    private class HttpAsyncGetUserInformFromServer extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute(){
            Log.d(TAG, "onPreExecute");
            Utils.checkinresponseStoreToCSV(ScheduleAndSampleManager.getCurrentTimeInMillis(), context);
        }

        @Override
        protected String doInBackground(String... params) {

            String result=null;
//            String url = params[0];

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d(TAG, "Response : " + line);
                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
                    e.printStackTrace();
                }
            }

            return result;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "get http post result " + result);

            Utils.checkinresponseStoreToCSV(ScheduleAndSampleManager.getCurrentTimeInMillis(), context, result);
        }

    }
}
