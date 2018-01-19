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

import edu.ohio.minuku.DBHelper.DBHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.streamgenerator.ConnectivityStreamGenerator;

/**
 * Created by Lawrence on 2017/8/16.
 */

public class WifiReceiver extends BroadcastReceiver {

    private final String TAG = "WifiReceiver";

    private Handler mMainThread;

    private SharedPreferences sharedPrefs;

    private Runnable runnable = null;

    private int year,month,day,hour,min;
    private int lastDay;

    private long _id = -9;
    private long latestUpdatedTime = -9999;
    private long nowTime = -9999;
    private long startTime = -9999;
    private long endTime = -9999;

    public static final int HTTP_TIMEOUT = 10000; // millisecond
    public static final int SOCKET_TIMEOUT = 20000; // millisecond

    private boolean noDataFlag1 = false;
    private boolean noDataFlag2 = false;
    private boolean noDataFlag3 = false;
    private boolean noDataFlag4 = false;
    private boolean noDataFlag5 = false;
    private boolean noDataFlag6 = false;
    private boolean noDataFlag7 = false;

    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";

//    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";//  /Android/data/edu.ohio.minuku_2/wifi/

//    private static final String postTripUrl = "http://ec2-52-14-68-199.us-east-2.compute.amazonaws.com/";
//    private static final String postDumpUrl = "http://ec2-52-14-68-199.us-east-2.compute.amazonaws.com/";

    private static final String postTripUrl = "http://mcog.asc.ohio-state.edu/apps/tripdump/";
    private static final String postDumpUrl = "http://mcog.asc.ohio-state.edu/apps/devicedump/";

    public static int mainThreadUpdateFrequencyInSeconds = 10;
    public static long mainThreadUpdateFrequencyInMilliseconds = mainThreadUpdateFrequencyInSeconds *Constants.MILLISECONDS_PER_SECOND;

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

//        mDay++; //start the task tomorrow.

        sharedPrefs = context.getSharedPreferences("edu.umich.minuku_2", context.MODE_PRIVATE);

        year = sharedPrefs.getInt("StartYear", mYear);
        month = sharedPrefs.getInt("StartMonth", mMonth);
        day = sharedPrefs.getInt("StartDay", mDay);

        lastDay = sharedPrefs.getInt("lastDay", day);

        Constants.USER_ID = sharedPrefs.getString("userid","NA");
        Constants.GROUP_NUM = sharedPrefs.getString("groupNum","NA");

        hour = sharedPrefs.getInt("StartHour", 0);
        min = sharedPrefs.getInt("StartMin",0);

        Log.d(TAG, "year : "+ year+" month : "+ month+" day : "+ day+" hour : "+ hour+" min : "+ min);

        if (activeNetwork != null) {
            // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                Log.d(TAG,"Wifi activeNetwork");

                //do the work here.
                MakingJsonDumpDataMainThread();

            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                //we might no need to use this.
                // connected to the mobile provider's data plan
                Log.d(TAG, "MOBILE activeNetwork" ) ;
                if(runnable!=null){
//                    mMainThread.removeCallbacks(runnable);

                }
            }
        } else {
            // not connected to the internet
            Log.d(TAG, "no Network" ) ;
            if(runnable!=null) {
//                mMainThread.removeCallbacks(runnable);
            }
        }

    }

    public void MakingJsonDumpDataMainThread(){

        Log.d(TAG, "MakingJsonDumpDataMainThread") ;

        mMainThread = new Handler();

        runnable = new Runnable() {

            @Override
            public void run() {
/*
                //TODO at official min should replaced by 0.
//                Trip_startTime = getSpecialTimeInMillis(year,month,day,hour,min);
                long startstartTime = getSpecialTimeInMillis(makingDataFormat(year,month,day,hour,min));
                startstartTime /= 1000;
                startTime = sharedPrefs.getLong("StartTime", startstartTime);
//                Trip_startTime = sharedPrefs.getLong("Trip_startTime", getSpecialTimeInMillis(year,month,day,hour,min));
//                Log.d(TAG,"Start : "+ getSpecialTimeInMillis(makingDataFormat(year,month,day,hour,min)));
                Log.d(TAG,"Start year : "+ year+" month : "+ month+" day : "+ day+" hour : "+ hour+" min : "+ min);
//                Log.d(TAG,"StartTime : " + getTimeString(getSpecialTimeInMillis(makingDataFormat(year,month,day,hour,min))));
                Log.d(TAG,"StartTimeString : " + getTimeString(startTime));
                Log.d(TAG,"StartTime : " + startTime);

                long startendTime = getSpecialTimeInMillis(makingDataFormat(year,month,day,hour+1,min));
                startendTime /= 1000;
                endTime = sharedPrefs.getLong("EndTime", startendTime);
//                Trip_endTime = sharedPrefs.getLong("Trip_endTime", getSpecialTimeInMillis(year,month,day,hour,min+10));
                Log.d(TAG,"End year : "+ year+" month : "+ month+" day : "+ day+" hour+1 : "+ (hour+1)+" min : "+ min);
                Log.d(TAG,"EndTimeString : " + getTimeString(endTime));
                Log.d(TAG,"EndTime : " + endTime);

                nowTime = new Date().getTime();//getCurrentTimeInMillis();//
                Log.d(TAG,"NowTimeString : " + getTimeString(nowTime));
                Log.d(TAG,"NowTime : " + nowTime);

                TimeZone tz = TimeZone.getDefault();
                Calendar cal = Calendar.getInstance(tz);
                int mDay = cal.get(Calendar.DAY_OF_MONTH);

                lastDay = sharedPrefs.getInt("lastDay", day);

                Log.d(TAG, "day : " + lastDay + ", mDay : "+ mDay);

                if(lastDay != mDay) {

                    _id = sharedPrefs.getInt("Trip_id", 1);

                    int Year = cal.get(Calendar.YEAR);
                    int Month = cal.get(Calendar.MONTH)+1;

                    long startTime = getSpecialTimeInMillis(makingDataFormat(Year, Month, lastDay));
                    long endTime = getSpecialTimeInMillis(makingDataFormat(Year, Month, lastDay+1));

                    MakingAnnotatedTripData(startTime, endTime); //update Trip data when we get the new one.

                    sharedPrefs.edit().putInt("lastDay", mDay).apply();
                }

                if(nowTime > endTime) {
                    MakingJsonDumpData();
                    //setting nextime interval
                    latestUpdatedTime = endTime;
                    startTime = latestUpdatedTime;

//                    long nextinterval = getSpecialTimeInMillis(makingDataFormat(0,0,0,0,5));
                    long nextinterval = 1 * 60 * 60000; //1 hr

                    endTime = startTime + nextinterval;//getSpecialTimeInMillis(0,0,0,0,10);

                    Log.d(TAG,"latestUpdatedTime : " + latestUpdatedTime);
                    Log.d(TAG,"latestUpdatedTime + 1 hour : " + latestUpdatedTime+ nextinterval);

                    sharedPrefs.edit().putLong("StartTime", startTime).apply();
                    sharedPrefs.edit().putLong("EndTime", endTime).apply();
                }*/

//                hour++;
//                if(hour>24)
//                    hour %= 24;

//                sharedPrefs.edit().putInt("StartHour", hour).apply();

//                MakingJsonDumpData();

                // Trip, isAlive
                if(ConnectivityStreamGenerator.mIsWifiConnected && ConnectivityStreamGenerator.mIsMobileConnected) {

                    sendingIsAliveData();

                };

                mMainThread.postDelayed(this, mainThreadUpdateFrequencyInMilliseconds);

            }
        };

        mMainThread.post(runnable);
    }

    private void sendingIsAliveData(){

        //making isAlive
        JSONObject data = new JSONObject();
        try {
            long currentTime = new Date().getTime();
            String currentTimeString = getTimeString(currentTime);

            data.put("time", currentTime);
            data.put("timeString", currentTimeString);
            data.put("device_id", Constants.DEVICE_ID);

        }catch (JSONException e){
            e.printStackTrace();
        }

        Log.d(TAG, "isAlive data uploading : " + data.toString());

        String curr = getDateCurrentTimeZone(new Date().getTime());

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
            /*new HttpAsyncPostJsonTask().execute(postTripUrl,
                    annotatedtripdata.toString(),
                    "Trip",
                    curr);*/
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

    public void MakingJsonDumpData(){

        Log.d(TAG, "MakingJsonDumpData") ;

        JSONObject data = new JSONObject();

        try {
            data.put("user_id", Constants.USER_ID);
            data.put("group_number", Constants.GROUP_NUM);
            data.put("device_id", Constants.DEVICE_ID);

//            data.put("type", "Transportation");

//            long Trip_startTime = getSpecialTimeInMillis(year,month,day,hour);
//            long Trip_endTime = getSpecialTimeInMillis(year,month,day,hour+1);

            data.put("start", String.valueOf(startTime));
            data.put("end", String.valueOf(endTime));
            data.put("Trip_startTime", getTimeString(startTime));
            data.put("Trip_endTime", getTimeString(endTime));
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

        Log.d(TAG,"final data : "+ data.toString());

        //TODO check there have Data or not store in external
//        if(noDataFlag1 && noDataFlag2 && noDataFlag3 && noDataFlag4 && noDataFlag5 && noDataFlag6 && noDataFlag7) {
//            storeToLocalFolder(data);
        /*
            noDataFlag1 = false;
            noDataFlag2 = false;
            noDataFlag3 = false;
            noDataFlag4 = false;
            noDataFlag5 = false;
            noDataFlag6 = false;
            noDataFlag7 = false;

        }*/

        String curr =  getDateCurrentTimeZone(new Date().getTime());

        //TODO upload to MongoDB
        /*new HttpAsyncPostJsonTask().execute(postDumpUrl,
                data.toString(),
                "Dump",
                curr);*/
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

        return  result;
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
            JSONArray timestamps = new JSONArray();

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
                    timestamps.put(timestamp);

                    transCursor.moveToNext();
                }

                transportationAndtimestampsJson.put("Transportation",transportations);
                transportationAndtimestampsJson.put("timestamps",timestamps);

                data.put("TransportationMode",transportationAndtimestampsJson);

            }else
                noDataFlag1 = true;

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
            JSONArray timestamps = new JSONArray();

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor transCursor = db.rawQuery("SELECT * FROM "+DBHelper.location_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM "+DBHelper.location_table+" WHERE "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ");

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
                    timestamps.put(timestamp);

                    transCursor.moveToNext();
                }

                locationAndtimestampsJson.put("Accuracy",accuracys);
                locationAndtimestampsJson.put("Longtitudes",longtitudes);
                locationAndtimestampsJson.put("Latitudes",latitudes);
                locationAndtimestampsJson.put("timestamps",timestamps);

                data.put("Location",locationAndtimestampsJson);

            }else
                noDataFlag2 = true;

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
            JSONArray timestamps = new JSONArray();

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
                    timestamps.put(timestamp);

                    transCursor.moveToNext();
                }

                arAndtimestampsJson.put("MostProbableActivity",mostProbableActivityz);
                arAndtimestampsJson.put("ProbableActivities",probableActivitiesz);
                arAndtimestampsJson.put("timestamps",timestamps);

                data.put("ActivityRecognition",arAndtimestampsJson);

            }else
                noDataFlag3 = true;

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
            JSONArray timestamps = new JSONArray();

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
                    timestamps.put(timestamp);

                    transCursor.moveToNext();
                }

                ringerAndtimestampsJson.put("RingerMode",RingerModes);
                ringerAndtimestampsJson.put("AudioMode",AudioModes);
                ringerAndtimestampsJson.put("StreamVolumeMusic",StreamVolumeMusics);
                ringerAndtimestampsJson.put("StreamVolumeNotification",StreamVolumeNotifications);
                ringerAndtimestampsJson.put("StreamVolumeRing",StreamVolumeRings);
                ringerAndtimestampsJson.put("StreamVolumeVoicecall",StreamVolumeVoicecalls);
                ringerAndtimestampsJson.put("StreamVolumeSystem",StreamVolumeSystems);
                ringerAndtimestampsJson.put("timestamps",timestamps);

                data.put("Ringer",ringerAndtimestampsJson);

            }else
                noDataFlag4 = true;

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
            JSONArray timestamps = new JSONArray();

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
                    timestamps.put(timestamp);

                    transCursor.moveToNext();
                }

                connectivityAndtimestampsJson.put("NetworkType",NetworkTypes);
                connectivityAndtimestampsJson.put("IsNetworkAvailable",IsNetworkAvailables);
                connectivityAndtimestampsJson.put("IsConnected",IsConnecteds);
                connectivityAndtimestampsJson.put("IsWifiAvailable",IsWifiAvailables);
                connectivityAndtimestampsJson.put("IsMobileAvailable",IsMobileAvailables);
                connectivityAndtimestampsJson.put("IsWifiConnected",IsWifiConnecteds);
                connectivityAndtimestampsJson.put("IsMobileConnected",IsMobileConnecteds);
                connectivityAndtimestampsJson.put("timestamps",timestamps);

                data.put("Connectivity",connectivityAndtimestampsJson);

            }else
                noDataFlag5 = true;

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
            JSONArray timestamps = new JSONArray();

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
                    timestamps.put(timestamp);

                    transCursor.moveToNext();
                }

                batteryAndtimestampsJson.put("BatteryLevel",BatteryLevels);
                batteryAndtimestampsJson.put("BatteryPercentage",BatteryPercentages);
                batteryAndtimestampsJson.put("BatteryChargingState",BatteryChargingStates);
                batteryAndtimestampsJson.put("isCharging",isChargings);
                batteryAndtimestampsJson.put("timestamps",timestamps);

                data.put("Battery",batteryAndtimestampsJson);

            }else
                noDataFlag6 = true;

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
            JSONArray timestamps = new JSONArray();

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
                    timestamps.put(timestamp);

                    transCursor.moveToNext();
                }

                appUsageAndtimestampsJson.put("ScreenStatus",ScreenStatusz);
                appUsageAndtimestampsJson.put("Latest_Used_App",Latest_Used_Apps);
//                appUsageAndtimestampsJson.put("Latest_Foreground_Activity",Latest_Foreground_Activitys);
                appUsageAndtimestampsJson.put("timestamps",timestamps);

                data.put("AppUsage",appUsageAndtimestampsJson);

            }else
                noDataFlag7 = true;

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
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW);
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

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_for_storing);
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
}
