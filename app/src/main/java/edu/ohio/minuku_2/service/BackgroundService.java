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
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.MinukuNotificationManager;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.AnnotationSet;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku.streamgenerator.TransportationModeStreamGenerator;
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

    private SharedPreferences sharedPrefs;

    private int ongoingNotificationID = 8;

    private String ongoingNotificationText;

    private int showOngoingNotificationCount = 0;

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

        intentFilter = new IntentFilter();
        intentFilter.addAction(CONNECTIVITY_ACTION);
        mWifiReceiver = new WifiReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "isBackgroundServiceRunning ? "+isBackgroundServiceRunning);
        CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "isBackgroundRunnableRunning ? "+isBackgroundRunnableRunning);


        //make the WifiReceiver start sending data to the server.
        registerReceiver(mWifiReceiver, intentFilter);


        IntentFilter checkRunnableFilter = new IntentFilter(CHECK_RUNNABLE_ACTION);

        registerReceiver(CheckRunnableReceiver, checkRunnableFilter);

        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarm.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + Constants.PROMPT_SERVICE_REPEAT_MILLISECONDS,
                PendingIntent.getBroadcast(this, 0, new Intent(CHECK_RUNNABLE_ACTION), 0)
        );


        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        ongoingNotificationText = sharedPrefs.getString("ongoingNotificationText", "0 New Trips");

        Constants.daysInSurvey = sharedPrefs.getInt("daysInSurvey", Constants.daysInSurvey);

        if(Constants.daysInSurvey == 0 || Constants.daysInSurvey == -1){

            ongoingNotificationText = "Part B begins tomorrow and lasts 2 weeks.";
        }

        // building the ongoing notification to the foreground
        startForeground(ongoingNotificationID, getOngoingNotification(ongoingNotificationText));


        if (!isBackgroundServiceRunning) {

            Log.d(TAG, "Initialize the Manager");

            isBackgroundServiceRunning = true;

            // do something
            CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "Going to judge the condition is ? "+(!InstanceManager.isInitialized()));

            if(!InstanceManager.isInitialized()) {

                CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "Going to start the runnable.");

                runMainThread();

                InstanceManager.getInstance(this);
                SessionManager.getInstance(this);
                Log.d(TAG, "Managers have been initialized. ");
            }

            SurveyTriggerManager.getInstance(getApplicationContext());

            /**read test file**/
//            FileHelper fileHelper = FileHelper.getInstance(getApplicationContext());
//            FileHelper.readTestFile();
        }

        return START_REDELIVER_INTENT;
    }

    private void runMainThread(){

        mScheduledExecutorService.scheduleAtFixedRate(
                updateStreamManagerRunnable,
                Constants.STREAM_UPDATE_DELAY,
                Constants.STREAM_UPDATE_FREQUENCY,
                TimeUnit.SECONDS);
    }

    Runnable updateStreamManagerRunnable = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "updateStreamManagerRunnable");

            CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "isBackgroundServiceRunning ? "+isBackgroundServiceRunning);
            CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "isBackgroundRunnableRunning ? "+isBackgroundRunnableRunning);

            try {

                isBackgroundRunnableRunning = true;

                streamManager.updateStreamGenerators();

                //update every minute
                if(showOngoingNotificationCount % 12 == 0) {

                    updateOngoingNotification();

                    checkAndRequestPermission();
                }

                showOngoingNotificationCount++;

                //send sleeptime check if it didn't being set after the time they downloaded the app
                long downloadedTime = sharedPrefs.getLong("downloadedTime", -1);

                long currentTime = new Date().getTime();

                if(currentTime - downloadedTime > Constants.MILLISECONDS_PER_MINUTE * 30){

                    String sleepStartTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER);
                    String sleepEndTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER);

                    if(sleepStartTime.equals(Constants.NOT_A_NUMBER) || sleepEndTime.equals(Constants.NOT_A_NUMBER)){

                        //send the notification to remind the user to set their sleeping time
                        NotificationManager mNotificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        String notificationText = "Please select your sleep period.";

                        Notification notification = getSleepNotification(notificationText);

                        mNotificationManager.notify(999, notification);
                    }

                }

            }catch (Exception e){

                isBackgroundRunnableRunning = false;
                CSVHelper.storeToCSV("CheckRunnable.csv", Utils.getStackTrace(e));
            }
        }
    };

    private Notification getSleepNotification(String text){

        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(text);

        Intent resultIntent = new Intent(this, Sleepingohio.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder noti = new Notification.Builder(this);

        return noti.setContentTitle(Constants.APP_FULL_NAME)
                .setContentText(text)
                .setStyle(bigTextStyle)
                .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
    }

    private void checkAndRequestPermission(){

        Log.d(TAG, "checkAndRequestPermission");

        if(!checkLocationPermissions()){

            String text = "Please check your location permission.";
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            Notification notification = getPermissionNotification(text, intent);
            mNotificationManager.notify(MinukuNotificationManager.locationNotificationID, notification);
        }

        if(!checkAppUsagePermissions()){

            String text = "Please check your usage access permission.";
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            Notification notification = getPermissionNotification(text, intent);
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
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(text);

//        Intent resultIntent = new Intent(this, TripListActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder noti = new Notification.Builder(this);

        return noti.setContentTitle(Constants.APP_FULL_NAME)
                .setContentText(text)
                .setStyle(bigTextStyle)
                .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
    }

    private Notification getOngoingNotification(String text){

        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(text);

        Intent resultIntent = new Intent(this, TripListActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder noti = new Notification.Builder(this);

        return noti.setContentTitle(Constants.APP_FULL_NAME)
                .setContentText(text)
                .setStyle(bigTextStyle)
//                .setSmallIcon(R.drawable.dms_icon)
                .setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setOngoing(true)
                .build();
    }

    private void updateOngoingNotification(){

//        Log.d(TAG,"updateOngoingNotification");

        //check how much trips hasn't been filled
//        ArrayList<Session> recentSessions = SessionManager.getRecentSessions();
        ArrayList<Session> recentSessions = SessionManager.getRecentNotBeenCombinedSessions();
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

        ongoingNotificationText = unfilledTrips+" New Trips";

        if(unfilledTrips==1){
            ongoingNotificationText = unfilledTrips + " New Trip";
        }

        Constants.daysInSurvey = sharedPrefs.getInt("daysInSurvey", Constants.daysInSurvey);

        if(Constants.daysInSurvey == 0 || Constants.daysInSurvey == -1){

            ongoingNotificationText = "Part B begins tomorrow and lasts 2 weeks.";
        }

        Notification note = getOngoingNotification(ongoingNotificationText);

        // using the same tag and Id causes the new notification to replace an existing one
        mNotificationManager.notify(ongoingNotificationID, note);
//        note.flags |= Notification.FLAG_NO_CLEAR;

    }

    private void stopTheSessionByServiceClose(){

        //if the background service is killed, set the end time of the ongoing trip (if any) using the current timestamp
        if (SessionManager.getOngoingSessionIdList().size()>0){

            Session session = SessionManager.getSession(SessionManager.getOngoingSessionIdList().get(0)) ;

//            Log.d(TAG, "test ondestory trip get session in onDestroy " + session.getId() + " time: " + session.getStartTime() + " - " + session.getEndTime());

            //if we end the current session, we should update its time and set a long enough flag
            if (session.getEndTime()==0){
                long endTime = ScheduleAndSampleManager.getCurrentTimeInMillis();
                session.setEndTime(endTime);
            }

            boolean isSessionLongEnough = SessionManager.isSessionLongEnough(SessionManager.SESSION_LONGENOUGH_THRESHOLD_DISTANCE, session.getId());

            session.setLongEnough(isSessionLongEnough);

            //end the current session
            SessionManager.endCurSession(session);
//            Log.d(TAG, "test ondestory trip: after remove, now the sesssion manager session list has  " + SessionManager.getInstance().getOngoingSessionList());

        }

//        Log.d(TAG, "test ondestory trip end of ending a trip");
    }

    @Override
    public void onDestroy() {
//        Log.d(TAG, "test ondestory trip Destroying service. Your state might be lost!");

        stopTheSessionByServiceClose();

        isBackgroundServiceRunning = false;
        isBackgroundRunnableRunning = false;

        mNotificationManager.cancel(ongoingNotificationID);

        sharedPrefs.edit().putString("ongoingNotificationText", ongoingNotificationText).apply();

        Utils.ServiceDestroying_StoreToCSV_onDestroy(new Date().getTime(), "TransportationMode.csv");
        Utils.ServiceDestroying_StoreToCSV_onDestroy(new Date().getTime(), "TransportationState.csv");
        Utils.ServiceDestroying_StoreToCSV_onDestroy(new Date().getTime(), "windowdata.csv");

        sharedPrefs.edit().putInt("CurrentState", TransportationModeStreamGenerator.mCurrentState).apply();
        sharedPrefs.edit().putInt("ConfirmedActivityType", TransportationModeStreamGenerator.mConfirmedActivityType).apply();

//        TransportationModeStreamGenerator.mScheduledExecutorService.shutdown();

        unregisterReceiver(mWifiReceiver);
        unregisterReceiver(CheckRunnableReceiver);
    }

    @Override
    public void onTaskRemoved(Intent intent){
        super.onTaskRemoved(intent);

//        stopTheSessionByServiceClose();

        mNotificationManager.cancel(ongoingNotificationID);

        isBackgroundServiceRunning = false;
        isBackgroundRunnableRunning = false;

        sharedPrefs.edit().putString("ongoingNotificationText", ongoingNotificationText).apply();

        Utils.ServiceDestroying_StoreToCSV_onTaskRemoved(new Date().getTime(), "TransportationMode.csv");
        Utils.ServiceDestroying_StoreToCSV_onTaskRemoved(new Date().getTime(), "TransportationState.csv");
        Utils.ServiceDestroying_StoreToCSV_onTaskRemoved(new Date().getTime(), "windowdata.csv");

        sharedPrefs.edit().putInt("CurrentState", TransportationModeStreamGenerator.mCurrentState).apply();
        sharedPrefs.edit().putInt("ConfirmedActivityType", TransportationModeStreamGenerator.mConfirmedActivityType).apply();

//        unregisterReceiver(mWifiReceiver);
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();

        sharedPrefs.edit().putString("ongoingNotificationText", ongoingNotificationText).apply();

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    BroadcastReceiver CheckRunnableReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(CHECK_RUNNABLE_ACTION)) {

                Log.d(TAG, "[check runnable] going to check if the runnable is running");

                CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "going to check if the runnable is running");
                CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "is the runnable running ? "+isBackgroundRunnableRunning);

                if(!isBackgroundRunnableRunning){

                    Log.d(TAG, "[check runnable] the runnable is not running, going to restart it.");

                    CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "the runnable is not running, going to restart it");

                    runMainThread();

                    Log.d(TAG, "[check runnable] the runnable is restarted.");

                    CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "the runnable is restarted");
                }

                PendingIntent pi = PendingIntent.getBroadcast(BackgroundService.this, 0, new Intent(CHECK_RUNNABLE_ACTION), 0);

                AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
                alarm.set(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + Constants.PROMPT_SERVICE_REPEAT_MILLISECONDS,
                        pi
                );
            }
        }
    };

}
