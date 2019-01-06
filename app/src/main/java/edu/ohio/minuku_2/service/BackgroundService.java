/*
 * Copyright (c) 2016.
 *
 * DReflect and Minuku Libraries by Shriti Raj (shritir@umich.edu) and Neeraj Kumar(neerajk@uci.edu) is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.
 * Based on a work at https://github.com/Shriti-UCI/Minuku-2.
 *
 *
 * You are free to (only if you meet the terms mentioned below) :
 *
 * Share — copy and redistribute the material in any medium or format
 * Adapt — remix, transform, and build upon the material
 *
 * The licensor cannot revoke these freedoms as long as you follow the license terms.
 *
 * Under the following terms:
 *
 * Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
 * NonCommercial — You may not use the material for commercial purposes.
 * ShareAlike — If you remix, transform, or build upon the material, you must distribute your contributions under the same license as the original.
 * No additional restrictions — You may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.
 */

package edu.ohio.minuku_2.service;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.MinukuNotificationManager;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.AnnotationSet;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku.streamgenerator.TransportationModeStreamGenerator;
import edu.ohio.minuku_2.MainActivity;
import edu.ohio.minuku_2.Receiver.RestarterBroadcastReceiver;
import edu.ohio.minuku_2.Receiver.WifiReceiver;
import edu.ohio.minuku_2.Utils;
import edu.ohio.minuku_2.controller.Sleepingohio;
import edu.ohio.minuku_2.controller.TripListActivity;
import edu.ohio.minuku_2.manager.InstanceManager;
import edu.ohio.minuku_2.manager.SurveyTriggerManager;

public class BackgroundService extends Service {

    private static final String TAG = "BackgroundService";

    final static String CHECK_RUNNABLE_ACTION = "checkRunnable";
    final static String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    WifiReceiver mWifiReceiver;
    IntentFilter intentFilter;

    MinukuStreamManager streamManager;
    NotificationManager mNotificationManager;
    private ScheduledExecutorService mScheduledExecutorService;
    ScheduledFuture<?> mScheduledFuture;

    private SharedPreferences sharedPrefs;

    private int ongoingNotificationID = 8;

    private String ongoingNotificationText;

    private int showOngoingNotificationCount;

    public static boolean isBackgroundServiceRunning = false;
    public static boolean isBackgroundRunnableRunning = false;

    public BackgroundService() {
        super();

    }

    @Override
    public void onCreate() {

        sharedPrefs = getSharedPreferences(Constants.sharedPrefString, MODE_PRIVATE);

        isBackgroundServiceRunning = false;
        isBackgroundRunnableRunning = false;

        streamManager = MinukuStreamManager.getInstance();

        mScheduledExecutorService = Executors.newScheduledThreadPool(Constants.STREAM_UPDATE_THREAD_SIZE);

        showOngoingNotificationCount = sharedPrefs.getInt("showOngoingNotificationCount", 0);

        Log.d(TAG, "showOngoingNotificationCount : "+ showOngoingNotificationCount);

        CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_CHECK_IN, "oncreate showOngoingNotificationCount : "+showOngoingNotificationCount);

        intentFilter = new IntentFilter();
//        intentFilter.addAction(CONNECTIVITY_ACTION);
        intentFilter.addAction(Constants.CONNECTIVITY_CHANGE);
        mWifiReceiver = new WifiReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "isBackgroundServiceRunning ? "+isBackgroundServiceRunning);
//        CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "isBackgroundRunnableRunning ? "+isBackgroundRunnableRunning);

        //make the WifiReceiver start sending data to the server.
        registerReceiver(mWifiReceiver, intentFilter);
        registerConnectivityNetworkMonitorForAPI21AndUp();

        IntentFilter checkRunnableFilter = new IntentFilter(CHECK_RUNNABLE_ACTION);

        registerReceiver(CheckRunnableReceiver, checkRunnableFilter);

        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarm.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + Constants.PROMPT_SERVICE_REPEAT_MILLISECONDS,
                PendingIntent.getBroadcast(this, 0, new Intent(CHECK_RUNNABLE_ACTION), 0)
        );


        //TODO set the value to prevent to reset the Default time back
        boolean isSleepTimeSet = sharedPrefs.getBoolean("IsSleepTimeSet", false);
        if(!isSleepTimeSet) {

            Log.d(TAG, "setDefaultSleepTime");
            Utils.setDefaultSleepTime(getApplicationContext());
        }

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        ongoingNotificationText = sharedPrefs.getString("ongoingNotificationText", "0 New Trips");

        Config.daysInSurvey = sharedPrefs.getInt("daysInSurvey", Config.daysInSurvey);

        if(Config.daysInSurvey == 0 || Config.daysInSurvey == -1){

            ongoingNotificationText = Constants.NOTIFICATION_TEXT_DAY_0;
        }

        createSurveyNotificationChannel();
        createNotificationChannel();
        createPermissionNotiChannel();
        createSleepTimeNotiChannel();

        // building the ongoing notification to the foreground
        startForeground(ongoingNotificationID, getOngoingNotification(ongoingNotificationText));


        if (!isBackgroundServiceRunning) {

            Log.d(TAG, "Initialize the Manager");

            isBackgroundServiceRunning = true;

//            CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "Going to judge the condition is ? "+(!InstanceManager.isInitialized()));

            Log.d(TAG, "Is InstanceManager not initialized ? "+(!InstanceManager.isInitialized()));

            if(!InstanceManager.isInitialized()) {

//                CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "Going to start the runnable.");

                InstanceManager.getInstance(this);
                SessionManager.getInstance(this);
                Log.d(TAG, "Managers have been initialized. ");
            }

            runMainThread();

            SurveyTriggerManager.getInstance(getApplicationContext());

            /**read test file**/
//            FileHelper fileHelper = FileHelper.getInstance(getApplicationContext());
//            FileHelper.readTestFile();
        }

        return START_REDELIVER_INTENT;
    }

    private void runMainThread(){

        mScheduledFuture = mScheduledExecutorService.scheduleAtFixedRate(
                updateStreamManagerRunnable,
                Constants.STREAM_UPDATE_DELAY,
                Constants.STREAM_UPDATE_FREQUENCY,
                TimeUnit.SECONDS);
    }

    Runnable updateStreamManagerRunnable = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "updateStreamManagerRunnable");

//            CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "isBackgroundServiceRunning ? "+isBackgroundServiceRunning);
//            CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "isBackgroundRunnableRunning ? "+isBackgroundRunnableRunning);

            try {

                isBackgroundRunnableRunning = true;

                //stop update it at the day after the final survey day
                if(Config.daysInSurvey > Constants.FINALDAY) {

                    streamManager.updateStreamGenerators();
                }

                //make sure that the device id will not disappear
                if(Config.DEVICE_ID.equals(Constants.INVALID_IN_STRING)){

                    Utils.getDeviceid(BackgroundService.this);
                }

                //update every 2 minutes
                if(showOngoingNotificationCount % 12 == 0) {

                    checkAndRequestPermission();
                }

                //update every 30 minutes
                if(showOngoingNotificationCount % 180 == 0){

                    CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_CHECK_IN, "update ongoing showOngoingNotificationCount : "+showOngoingNotificationCount);

                    updateOngoingNotification();
                }

                //update every 3 hours
                /*if(showOngoingNotificationCount % 1080 == 0){

                    //check the response time is already 3 hours
                    sendingUserInform();
                }*/

                showOngoingNotificationCount++;

                showOngoingNotificationCount %= 1080;

                sharedPrefs.edit().putInt("showOngoingNotificationCount", showOngoingNotificationCount).apply();

            }catch (Exception e){

//                isBackgroundRunnableRunning = false;
//                CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "Something wrong in the runnable process.");
//                CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, Utils.getStackTrace(e));
            }
        }
    };

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
                userInformInString = new HttpAsyncGetUserInformFromServer().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        link).get();
            else
                userInformInString = new HttpAsyncGetUserInformFromServer().execute(
                        link).get();

            userInform = new JSONObject(userInformInString);

            Log.d(TAG, "userInform : " + userInform);

            sharedPrefs.edit().putLong("lastCheckInTime", ScheduleAndSampleManager.getCurrentTimeInMillis()).apply();

        } catch (InterruptedException e) {

        } catch (ExecutionException e) {

        } catch (JSONException e){

        } catch (NullPointerException e){

        }

        //In order to set the survey link
//        setDaysInSurvey(userInform);

    }

    private Notification getSleepNotification(String text){

        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle(Constants.APP_NAME);
        bigTextStyle.bigText(text);

        Intent resultIntent = new Intent(this, Sleepingohio.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder noti = new Notification.Builder(this)
                .setContentTitle(Constants.APP_FULL_NAME)
                .setContentText(text)
                .setStyle(bigTextStyle)
                .setContentIntent(pending)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return noti
                    .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                    .setChannelId(Constants.SLEEPTIME_CHANNEL_ID)
                    .build();
        } else {
            return noti
                    .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                    .build();
        }

    }

    private void checkAndRequestPermission(){

        Log.d(TAG, "checkAndRequestPermission");

        if(!checkLocationPermissions()){

            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            Notification notification = getPermissionNotification(Constants.NOTIFICATION_TEXT_LOCATION, intent);
            mNotificationManager.notify(MinukuNotificationManager.locationNotificationID, notification);
        }

        if(!checkAppUsagePermissions()){

            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            Notification notification = getPermissionNotification(Constants.NOTIFICATION_TEXT_APPUSAGE, intent);
            mNotificationManager.notify(MinukuNotificationManager.appusageNotificationID, notification);
        }
    }

    private boolean checkLocationPermissions() {

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {

        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {

        }

        if(!gps_enabled && !network_enabled) {

            //trigger notification
            return false;
        }

        return true;
    }

    private boolean checkAppUsagePermissions() {

        //in lollipop there are no app usage; thus, no need to send notification
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }

        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);

            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private Notification getPermissionNotification(String text, Intent resultIntent){

        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle(Constants.APP_NAME);
        bigTextStyle.bigText(text);

//        Intent resultIntent = new Intent(this, TripListActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder noti = new Notification.Builder(this)
                .setContentTitle(Constants.APP_FULL_NAME)
                .setContentText(text)
                .setStyle(bigTextStyle)
                .setContentIntent(pending)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return noti
                    .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                    .setChannelId(Constants.PERMIT_CHANNEL_ID)
                    .build();
        } else {
            return noti
                    .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                    .build();
        }
    }

    private Notification getOngoingNotification(String text){

        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle(Constants.APP_NAME);
        bigTextStyle.bigText(text);

        Intent resultIntent = new Intent(this, TripListActivity.class);

        //TODO test
        if(Config.daysInSurvey == 0 || Config.daysInSurvey == -1 || Config.daysInSurvey > Constants.FINALDAY){

            resultIntent = new Intent(this, MainActivity.class);
        }

        PendingIntent pending = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder noti = new Notification.Builder(this)
                .setContentTitle(Constants.APP_FULL_NAME)
                .setContentText(text)
                .setStyle(bigTextStyle)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return noti
                    .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                    .setChannelId(Constants.ONGOING_CHANNEL_ID)
                    .build();
        } else {
            return noti
                    .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                    .build();
        }

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = Constants.ONGOING_CHANNEL_NAME;
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(Constants.ONGOING_CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createSurveyNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = Constants.SURVEY_CHANNEL_NAME;
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(Constants.SURVEY_CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createPermissionNotiChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = Constants.PERMIT_CHANNEL_NAME;
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(Constants.PERMIT_CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createSleepTimeNotiChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = Constants.SLEEPTIME_CHANNEL_NAME;
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(Constants.SLEEPTIME_CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateOngoingNotification(){

        Log.d(TAG,"updateOngoingNotification");

        //check how much trips hasn't been filled
        ArrayList<Session> recentSessions = SessionManager.getRecentToShowSessions();
        ArrayList<Integer> ongoingSessionList = SessionManager.getOngoingSessionIdList();

        int unfilledTrips = 0;

        if(recentSessions.size() != 0) {

            for (Session session : recentSessions) {
                AnnotationSet annotationSet = session.getAnnotationsSet();

                int id = session.getId();

                // we don't want to show the ongoing one.
                if(ongoingSessionList.contains(id)){
                    continue;
                }

                ArrayList<Annotation> annotations = annotationSet.getAnnotationByTag("ESM");
                if (annotations == null || annotations.size() == 0) {
                    unfilledTrips++;
                }
            }
        }

        ongoingNotificationText = unfilledTrips + Constants.NOTIFICATION_TEXT_NEW_TRIPS;

        if(unfilledTrips==1){

            ongoingNotificationText = unfilledTrips + Constants.NOTIFICATION_TEXT_NEW_TRIP;
        }

        Config.daysInSurvey = sharedPrefs.getInt("daysInSurvey", Config.daysInSurvey);

        if(Config.daysInSurvey == 0 || Config.daysInSurvey == -1){

            ongoingNotificationText = Constants.NOTIFICATION_TEXT_DAY_0;
        }else if(Config.daysInSurvey > Constants.FINALDAY){

            if(Config.daysInSurvey == Constants.FINALDAY + 1) {

                if(unfilledTrips == 0){

                    ongoingNotificationText = Constants.NOTIFICATION_TEXT_FINAL_DAY_PLUS_1_WITHOUT_TRIPS;
                }else{

                    ongoingNotificationText = Constants.NOTIFICATION_TEXT_FINAL_DAY_PLUS_1_WITH_TRIPS;
                }
            } else {

                boolean isFinalButtonClicked = sharedPrefs.getBoolean("finalButtonClicked", false);
                if(isFinalButtonClicked){

                    ongoingNotificationText = Constants.NOTIFICATION_TEXT_AFTER_FINAL_DAY_PLUS_1_WAIT_DATA_TRANSFER;
                } else {

                    ongoingNotificationText = Constants.NOTIFICATION_TEXT_AFTER_FINAL_DAY_PLUS_1;
                }
            }

//            checkingRemovedFromForeground();
        }

        Notification note = getOngoingNotification(ongoingNotificationText);

        // using the same tag and Id causes the new notification to replace an existing one
        mNotificationManager.notify(ongoingNotificationID, note);

    }

    private void stopTheSessionByServiceClose(){

        //if the background service is killed, set the end time of the ongoing trip (if any) using the current timestamp
        if (SessionManager.getOngoingSessionIdList().size()>0){

            Session session = SessionManager.getSession(SessionManager.getOngoingSessionIdList().get(0)) ;

            //if we end the current session, we should update its time and set a long enough flag
            if (session.getEndTime()==0){

                long endTime = ScheduleAndSampleManager.getCurrentTimeInMillis();
                session.setEndTime(endTime);
            }

            boolean isSessionLongEnough = SessionManager.isSessionLongEnough(SessionManager.SESSION_LONGENOUGH_THRESHOLD_DISTANCE, session.getId());

            session.setLongEnough(isSessionLongEnough);

            //end the current session
            SessionManager.endCurSession(session);
        }
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "onDestroy");

        stopTheSessionByServiceClose();

        sendBroadcastToStartService();

        isBackgroundServiceRunning = false;
        isBackgroundRunnableRunning = false;

        mNotificationManager.cancel(ongoingNotificationID);

        sharedPrefs.edit().putString("ongoingNotificationText", ongoingNotificationText).apply();

//        Utils.ServiceDestroying_StoreToCSV_onDestroy(new Date().getTime(), CSVHelper.CSV_TRANSPORTATIONMODE);
        Utils.ServiceDestroying_StoreToCSV_onDestroy(new Date().getTime(), "TransportationState.csv");
        Utils.ServiceDestroying_StoreToCSV_onDestroy(new Date().getTime(), "windowdata.csv");

        sharedPrefs.edit().putInt("CurrentState", TransportationModeStreamGenerator.mCurrentState).apply();
        sharedPrefs.edit().putInt("ConfirmedActivityType", TransportationModeStreamGenerator.mConfirmedActivityType).apply();

//        checkingRemovedFromForeground();
        removeRunnable();

        unregisterReceiver(mWifiReceiver);
    }

    @Override
    public void onTaskRemoved(Intent intent){
        super.onTaskRemoved(intent);

//        stopTheSessionByServiceClose();

        mNotificationManager.cancel(ongoingNotificationID);

        isBackgroundServiceRunning = false;
        isBackgroundRunnableRunning = false;

        sharedPrefs.edit().putString("ongoingNotificationText", ongoingNotificationText).apply();

//        Utils.ServiceDestroying_StoreToCSV_onTaskRemoved(new Date().getTime(), CSVHelper.CSV_TRANSPORTATIONMODE);
        Utils.ServiceDestroying_StoreToCSV_onTaskRemoved(new Date().getTime(), "TransportationState.csv");
        Utils.ServiceDestroying_StoreToCSV_onTaskRemoved(new Date().getTime(), "windowdata.csv");

        sharedPrefs.edit().putInt("CurrentState", TransportationModeStreamGenerator.mCurrentState).apply();
        sharedPrefs.edit().putInt("ConfirmedActivityType", TransportationModeStreamGenerator.mConfirmedActivityType).apply();

//        checkingRemovedFromForeground();
        removeRunnable();

        sendBroadcastToStartService();

    }

    private void sendBroadcastToStartService(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            sendBroadcast(new Intent(this, RestarterBroadcastReceiver.class).setAction(Constants.CHECK_SERVICE_ACTION));
        } else {

            Intent checkServiceIntent = new Intent(Constants.CHECK_SERVICE_ACTION);
            sendBroadcast(checkServiceIntent);
        }
    }

    private void checkingRemovedFromForeground(){

        if(Config.daysInSurvey > Constants.FINALDAY + 1){

            Log.d(TAG,"stopForeground");

            stopForeground(true);

            try {

                unregisterReceiver(CheckRunnableReceiver);
            }catch (IllegalArgumentException e){

            }

            mScheduledExecutorService.shutdown();
        }
    }

    private void removeRunnable(){

        mScheduledFuture.cancel(true);
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();

        sharedPrefs.edit().putString("ongoingNotificationText", ongoingNotificationText).apply();
    }

    private void registerConnectivityNetworkMonitorForAPI21AndUp() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest.Builder builder = new NetworkRequest.Builder();

        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {

                    @Override
                    public void onCapabilitiesChanged(Network network, final NetworkCapabilities networkCapabilities){

                        sendBroadcast(
                                getConnectivityIntent("onCapabilitiesChanged : "+networkCapabilities.toString())
                        );
                    }
                }
        );

    }

    private Intent getConnectivityIntent(String message) {

        Intent intent = new Intent();

        intent.setAction(Constants.CONNECTIVITY_CHANGE);

        intent.putExtra("message", message);

        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class HttpAsyncGetUserInformFromServer extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute(){

        }

        @Override
        protected String doInBackground(String... params) {

            String result=null;

            int HTTP_TIMEOUT = 10 * (int) Constants.MILLISECONDS_PER_SECOND;
            int SOCKET_TIMEOUT = 20 * (int) Constants.MILLISECONDS_PER_SECOND;

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

        @Override
        protected void onPostExecute(String result) {

        }
    }

    BroadcastReceiver CheckRunnableReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(CHECK_RUNNABLE_ACTION)) {

                //we don't need ongoing notification after day FINALDAY + 1

                Log.d(TAG, "[check runnable] going to check if the runnable is running");

//                CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "going to check if the runnable is running");
//                CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "is the runnable running ? " + isBackgroundRunnableRunning);

                if (!isBackgroundRunnableRunning) {

                    Log.d(TAG, "[check runnable] the runnable is not running, going to restart it.");

//                    CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "the runnable is not running, going to restart it");

                    runMainThread();

                    Log.d(TAG, "[check runnable] the runnable is restarted.");

//                    CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "the runnable is restarted");
                }

                PendingIntent pi = PendingIntent.getBroadcast(BackgroundService.this, 0, new Intent(CHECK_RUNNABLE_ACTION), 0);

                AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    alarm.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + Constants.PROMPT_SERVICE_REPEAT_MILLISECONDS,
                            pi);
                }else{

                    alarm.set(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + Constants.PROMPT_SERVICE_REPEAT_MILLISECONDS,
                            pi
                    );
                }
            }
        }
    };

}
