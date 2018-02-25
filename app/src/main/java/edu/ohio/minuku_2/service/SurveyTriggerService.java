package edu.ohio.minuku_2.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.opencsv.CSVWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import edu.ohio.minuku.DataHandler;
import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.DataRecord.ConnectivityDataRecord;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku.service.TransportationModeService;
import edu.ohio.minuku.streamgenerator.ConnectivityStreamGenerator;
import edu.ohio.minuku_2.Constant;
import edu.ohio.minuku_2.R;
import edu.ohio.minuku_2.controller.Ohio.SurveyActivity;
import edu.ohio.minuku_2.model.CheckFamiliarOrNotDataRecord;
import edu.ohio.minuku_2.streamgenerator.CheckFamiliarOrNotStreamGenerator;
import edu.ohio.minukucore.exception.StreamNotFoundException;

/**
 * Created by Lawrence on 2017/4/23.
 */

public class SurveyTriggerService extends Service {

    private static final String TAG = "SurveyTriggerService";

    private static Context serviceInstance;
    private static Handler mMainThread;
    private static Context mContext;
    Intent intent = new Intent();
    private ProgressDialog loadingProgressDialog;

//    private static AlarmManager mAlarmManager;

    ScheduledFuture<?> scheduledFuture= null;
    private ScheduledExecutorService mScheduledExecutorService;
    public static final int REFRESH_FREQUENCY = 10; //20s, 10000ms TODO convert to 20s
    public static final int BACKGROUND_RECORDING_INITIAL_DELAY = 0;

    private final int URL_TIMEOUT = 5 * 1000;

    public static final int HTTP_TIMEOUT = 10000; // millisecond
    public static final int SOCKET_TIMEOUT = 20000; // millisecond

    private float latitude;
    private float longitude;
    private float accuracy;

    private String userid;

    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";

    private final long One_Hour_In_milliseconds = 60 * 60 * 1000;

    private static String today;
    private String lastTimeSend_today;
    private int current_hour;
    private int last_hour;

    private int notifyID = 1;
    private int qua_notifyID = 2;
    private int interval_notifyID = 3; //used to 3
    //qua is equal to interval now

    private static ArrayList<Long> interval_sampled_times;

    private static int interval_sample_number;

    private static int interval_sampled;
    private int walkoutdoor_sampled;
    private int walk_first_sampled;
    private int walk_second_sampled;
    private int walk_third_sampled;

    private int countingTimeForIndoorOutdoor;
    private boolean sameTripPrevent;

    private long last_Survey_Time;
    private long last_Walking_Survey_Time;

    private long vibrate[] = {0,200,800,500};
    public static long time_base = 0;

    private String transportation;

    private static CSVWriter csv_writer = null;

    private boolean testingserverThenFail = false;
    private boolean isServiceRunning;
    private boolean alarmregistered;

    private String participantID, groupNum;
    private int weekNum, dailySurveyNum, dailyResponseNum;

    private final String link = "https://osu.az1.qualtrics.com/jfe/form/SV_6xjrFJF4YwQwuMZ";//"https://osu.az1.qualtrics.com/jfe/form/SV_6xjrFJF4YwQwuMZ";
    private String inhome_test = "";
//    private String NotificationText = "Please click to fill the questionnaire";

    private String curr =  getDateCurrentTimeZone(new Date().getTime());
    private String apikeyfortesting = "TiKLVvkv4m1XTWhmIjCx4A464jBTP0ZE";
    private String testUrl = "https://api.mlab.com/api/1/databases/mobility_study/collections/real_time?apiKey="+apikeyfortesting;

    private NotificationManager mNotificationManager;


    public static CheckFamiliarOrNotStreamGenerator checkFamiliarOrNotStreamGenerator;

    private static SharedPreferences sharedPrefs;

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        unregisterActionAlarmReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public SurveyTriggerService(){}

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d(TAG, "onCreate");

        sharedPrefs = getSharedPreferences("edu.umich.minuku_2", MODE_PRIVATE);

        serviceInstance = this;

        mContext = this;

//        testingserverThenFail = false;

        transportation = "NA";

        last_hour = sharedPrefs.getInt("last_hour",-99);
        lastTimeSend_today = sharedPrefs.getString("lastTimeSend_today","NA");

        isServiceRunning = false;

        interval_sampled = sharedPrefs.getInt("interval_sampled", 0);

        alarmregistered = sharedPrefs.getBoolean("alarmregistered",false);

        sameTripPrevent = sharedPrefs.getBoolean("sameTripPrevent", false);

        countingTimeForIndoorOutdoor = sharedPrefs.getInt("countingTimeForIndoorOutdoor", 0);

        time_base = ScheduleAndSampleManager.getCurrentTimeInMillis();

        mScheduledExecutorService = Executors.newScheduledThreadPool(REFRESH_FREQUENCY);

//        mAlarmManager = (AlarmManager)mContext.getSystemService( mContext.ALARM_SERVICE );

        registerActionAlarmReceiver();

        try {
            checkFamiliarOrNotStreamGenerator = (CheckFamiliarOrNotStreamGenerator) MinukuStreamManager.getInstance().getStreamGeneratorFor(CheckFamiliarOrNotDataRecord.class);
        }catch(StreamNotFoundException e){
            Log.e(TAG,"checkFamiliarOrNotStreamGenerator haven't created yet.");
        }
    }

    public static Context getInstance() {
        if(SurveyTriggerService.serviceInstance == null) {
            try {
                SurveyTriggerService.serviceInstance = new SurveyTriggerService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return SurveyTriggerService.serviceInstance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("SurveyTriggerService", "[test service running] going to start the probe service, isServiceRunning:  " + isServiceRunning());

        int permissionStatus= ContextCompat.checkSelfPermission(SurveyTriggerService.this, android.Manifest.permission.READ_PHONE_STATE);
        if(permissionStatus== PackageManager.PERMISSION_GRANTED) {
            TelephonyManager mngr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            Constants.DEVICE_ID = mngr.getDeviceId();
            userid = Constants.DEVICE_ID;
        }

        last_hour = sharedPrefs.getInt("last_hour",-99);
        lastTimeSend_today = sharedPrefs.getString("lastTimeSend_today","NA");
        interval_sampled = sharedPrefs.getInt("interval_sampled", 0);
        alarmregistered = sharedPrefs.getBoolean("alarmregistered",false);

        Constants.USER_ID = sharedPrefs.getString("userid","NA");

        Log.d(TAG, "onStartCommand");

        if(!isServiceRunning) {

            //we use an alarm to keep the service awake
            AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
            alarm.set(
                    AlarmManager.RTC_WAKEUP,     //
                    System.currentTimeMillis() + Constants.PROMPT_SERVICE_REPEAT_MILLISECONDS,
                    PendingIntent.getService(this, 0, new Intent(this, SurveyTriggerService.class), 0)
            );

            startService();

            isServiceRunning = true;

        }
        else {
            Log.d(TAG, "test AR service start test combine the service is restarted! the service is running");
//            isServiceRunning = false;
        }

//        return START_STICKY;
        return START_REDELIVER_INTENT;
    }


    public void startService(){

        Log.d(TAG,"startService");

        runMainThread();

    }

    public void runMainThread(){

        Log.d(TAG,"runMainThread");

        if(scheduledFuture!= null)
            scheduledFuture.cancel(true);

        Log.d(TAG,"scheduledFuture is not null : " + (scheduledFuture != null));

        scheduledFuture = mScheduledExecutorService.scheduleAtFixedRate(
                RunningForGivingSurveyOrNot,
                BACKGROUND_RECORDING_INITIAL_DELAY,
                REFRESH_FREQUENCY,
                TimeUnit.SECONDS);

    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }


    public JSONObject readJsonFromUrl(String urlPath) throws IOException, JSONException, SocketTimeoutException {
//        InputStream is = new URL(urlPath).openStream();

        URL url = new URL(urlPath);
        URLConnection con = url.openConnection();
        con.setConnectTimeout(URL_TIMEOUT);
        con.setReadTimeout(URL_TIMEOUT);
        InputStream is = con.getInputStream();

        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    private static long getSpecialTimeInMillis(String givenDateFormat){
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

    public static String getTimeString(long time){

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_SLASH);
        String currentTimeString = sdf_now.format(time);

        return currentTimeString;
    }

    public static void settingIntervalSampling(long startTimeAfterWalkingSampling){

        Log.d(TAG, "settingIntervalSampling");

        String yMdformat = today;

        Log.d(TAG, "yMdformat : " + yMdformat);
        interval_sampled_times = new ArrayList<>();
        ArrayList<Long> times = new ArrayList<Long>();
        long now = ScheduleAndSampleManager.getCurrentTimeInMillis();
        Random random = new Random(now);
        int sample_period;
        //TODO set the start and end time.
        String sleepingstartTime = sharedPrefs.getString("SleepingStartTime", "22:00"); //"22:00"
        String sleepingendTime = sharedPrefs.getString("SleepingEndTime", "08:00"); //"08:00"

        Log.d(TAG, "sleepingstartTime : " + yMdformat+" "+sleepingstartTime+":00");

        long endTime = getSpecialTimeInMillis(yMdformat+" "+sleepingstartTime+":00");
        long startTime; //because they are wake up.

        if(startTimeAfterWalkingSampling != -9999){ //if the user have one or more walking sampling.
            startTime = startTimeAfterWalkingSampling;

        }else {
            //default is not working.
//            startTime = getSpecialTimeInMillis(yMdformat+" "+sleepingendTime+":00");

            startTime = ScheduleAndSampleManager.getCurrentTimeInMillis();

            //make sure that the user will not get any notification to day.
            if(startTime > endTime){
                startTime = endTime;
            }

            //because "else" will be called at first interval sampling.
            interval_sampled = 0;
            sharedPrefs.edit().putInt("interval_sampled", interval_sampled).apply();
        }

        long sub_startTime = startTime;
        long sub_endTime  = endTime;
        long min_interval = (endTime - startTime)/(interval_sample_number+4); //90 * 60 * 1000  (e-s)/10

        Log.d(TAG, "endTime : "+endTime);
        Log.d(TAG, "startTime : "+startTime);

        //1. first get the entire sampling period

        //repeatedly find the subsampling period
        for (int i = 0; i < interval_sample_number; i++){

            Log.d(TAG, "interval_sample_number : "+i);

            //2. divide the period by the number of sample
            sample_period = (int) (sub_endTime - sub_startTime);

            try {

                Log.d(TAG, "sample_period : "+sample_period);
                Log.d(TAG, "interval_sample_number : "+interval_sample_number);
                Log.d(TAG, "i : "+i);

                int sub_sample_period = sample_period/(interval_sample_number -i);

                Log.d(TAG, "sub_sample_period : "+sub_sample_period);

                //3. random within the sub sample period
                long time =  random.nextInt(sub_sample_period) + sub_startTime;

				Log.d(TAG, "testRandom semi sampling: the " + i + " sampling period is from " + getTimeString(sub_startTime) + " to " + getTimeString(sub_endTime) +
				" divided by " + (interval_sample_number-i) + " each period is " + (sub_sample_period/60/1000) + " minutes long, " + " the sampled time within the period is " +
						getTimeString(time) );

                Log.d(TAG, "testRandom  the sampled time within the period is " + getTimeString(time) );

                //4. save the sampled time
                times.add(time);

                //5. the next startime is the previous sample time + min interval. We do this to ensure that the next sampled time is
                //not too close to the previous sampled time.
                sub_startTime = time +  min_interval;

				Log.d(TAG, "testRandom semi sampling: the new start time is " + getTimeString(sub_startTime));

                //6. if the next start time is later than the overall end time, stop the sampling.
                if (sub_startTime >= sub_endTime)
                    break;

            }catch(Exception e){
                e.printStackTrace();
                android.util.Log.e(TAG, "exception", e);
            }

        }

        //setting them to the alarmManager.
        if (times!=null) {

            interval_sampled_times = times;
            for (int i = 0; i < times.size(); i++) {
                long time = times.get(i);
                int request_code = generatePendingIntentRequestCode(time);
                //create an alarm for a time
                Intent intent = new Intent(Constants.Interval_Sample);

                //cancel the last time ones here.
                int last_request_code = sharedPrefs.getInt("last_request_code_"+i, -999);
                cancelAlarmIfExists(mContext, last_request_code, intent);

                PendingIntent pi = PendingIntent.getBroadcast(mContext, request_code, intent, 0);

                Log.d(TAG, "time : " + getTimeString(time));

                //for the reset one
                AlarmManager alarmManager = (AlarmManager)mContext.getSystemService( mContext.ALARM_SERVICE );
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);

                //update the latest request_code to be the last request_code.
                sharedPrefs.edit().putInt("last_request_code_"+i, request_code).apply();
                sharedPrefs.edit().putString("sampled_times_"+i, getTimeString(time)).apply();
            }

        }

        if (isExternalStorageWritable())
            storeToCSVinterval(today);

    }

    public static void cancelAlarmIfExists(Context mContext,int requestCode,Intent intent){
        try {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent,0);
            AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancelAlarmAll(Context mContext){
        try {
            Intent intent = new Intent(Constants.Interval_Sample);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,0);
            AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Runnable RunningForGivingSurveyOrNot = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "RunningForGivingSurveyOrNot");

            userid = Constants.DEVICE_ID;

            //variable for link
            participantID = sharedPrefs.getString("userid", "NA");
            groupNum = sharedPrefs.getString("groupNum", "NA");
            weekNum = sharedPrefs.getInt("weekNum", 0);
            dailySurveyNum = sharedPrefs.getInt("dailySurveyNum", 0);
            dailyResponseNum = sharedPrefs.getInt("dailyResponseNum", 0);

            //check if the
            Date curDate = new Date(System.currentTimeMillis());
            String dateformat = "yyyy/MM/dd";
            SimpleDateFormat df = new SimpleDateFormat(dateformat);
            today = df.format(curDate);

            /** setting third of the day**/

            //get sleep time and get up time from sharedpreference
            String startSleepingTime = sharedPrefs.getString("SleepingStartTime","22:00");
            String endSleepingTime = sharedPrefs.getString("SleepingEndTime","08:00");

            Log.d(TAG,"startSleepingTime : "+ startSleepingTime + " endSleepingTime : "+ endSleepingTime);

            //calculate the bed time and gettime for today
            long bedTime = getSpecialTimeInMillis(today+" "+startSleepingTime+":00");
            long startDay_settingthird = getSpecialTimeInMillis(today+" "+endSleepingTime+":00");

            //get the initial point of each third

            //awake time is between the time of gettuping and going to bed
            long awakeTime = bedTime - startDay_settingthird;

            //the amount for each third
            long third_interval = (awakeTime)/3;

            Log.d(TAG, "third_interval is " + third_interval);

            //the endtime of the first, second, third third
            long endOfFirstThird = startDay_settingthird + third_interval;
            long endOfSecondThird = startDay_settingthird + third_interval * 2;
            long endOfThirdThird = startDay_settingthird + third_interval * 3;

            current_hour = getCurrentHour();

            Log.d(TAG, "today is " + today);
            Log.d(TAG, "endOfFirstThird is " + endOfFirstThird);
            Log.d(TAG, "endOfSecondThird is " + endOfSecondThird);
            Log.d(TAG, "endOfThirdThird is " + endOfThirdThird);

            //initialize
            testingserverThenFail = false;

            Log.d(TAG, "userid: " + userid);

            //after a day
            if (!lastTimeSend_today.equals(today)) {

                walkoutdoor_sampled = 0;
                sharedPrefs.edit().putInt("walkoutdoor_sampled", walkoutdoor_sampled).apply();

                walk_first_sampled = 0;
                sharedPrefs.edit().putInt("walk_first_sampled", walk_first_sampled).apply();
                walk_second_sampled = 0;
                sharedPrefs.edit().putInt("walk_second_sampled", walk_second_sampled).apply();
                walk_third_sampled = 0;
                sharedPrefs.edit().putInt("walk_third_sampled", walk_third_sampled).apply();

                lastTimeSend_today = today;
                sharedPrefs.edit().putString("lastTimeSend_today", lastTimeSend_today).apply();

                //total 6 surveys a day
                interval_sample_number = 6;
                sharedPrefs.edit().putInt("interval_sample_number", interval_sample_number).apply();

                last_Survey_Time = -999;
                sharedPrefs.edit().putLong("last_Survey_Time", last_Survey_Time).apply();

                last_Walking_Survey_Time = -999;
                sharedPrefs.edit().putLong("last_Walking_Survey_Time", last_Walking_Survey_Time).apply();

                //default
                settingIntervalSampling(-9999);

                //setting up the parameter for the survey link
                setUpSurveyLink();

            }

            //temporarily remove the trigger limit
            if (!checkSleepingTime()) {

                /** see if the user is walking in the last minute **/
                long now = ScheduleAndSampleManager.getCurrentTimeInMillis();
                long lastminute = now - Constant.MILLISECONDS_PER_MINUTE;

                //1. we get walking session
                int sessionId = SessionManager.getOngoingSessionIdList().get(0);
                Session session = SessionManager.getSession(sessionId);

                //see if the session is walking session
                ArrayList<Annotation> annotations = session.getAnnotationsSet().getAnnotationByContent(TransportationModeService.TRANSPORTATION_MODE_NAME_ON_FOOT);

                //session startTime
                long sessionStartTime = session.getStartTime();

                //if the session is on foot and has lasted for one minute
                if (annotations.size() > 0  && sessionStartTime < lastminute && isWalkingOutdoor(lastminute, now)) {
                    Log.d(TAG, "[test sampling] the user has been walking for a miunute");

                    walkoutdoor_sampled = sharedPrefs.getInt("walkoutdoor_sampled", 0);

                    //the user is walking outdoor we need to trigger a survey
                    if (walkoutdoor_sampled < 3 && sameTripPrevent == false) {

                        //determine whether to send a walking triggered survey by checking whether there has triggered one survey before
                        boolean sendSurveyFlag = true;

                        if (now <= endOfFirstThird) {
                            if (walk_first_sampled >= 1)
                                sendSurveyFlag = false;
                        } else if (now <= endOfSecondThird && now > endOfFirstThird) {
                            if (walk_second_sampled >= 1)
                                sendSurveyFlag = false;
                        } else if (now <= endOfThirdThird && now > endOfSecondThird) {
                            if (walk_third_sampled >= 1)
                                sendSurveyFlag = false;
                        }

                        //send notification
                        if (sendSurveyFlag){
                            if (timeForSurvey())
                                triggerWalkingSurvey(endOfFirstThird, endOfSecondThird, endOfFirstThird);
                        }

                    }

                }

                //TODO: if the walking has ended, we should dismiss the notification
                if(MinukuStreamManager.cancelWalkingSurveyFlag){
                    try{
                        mNotificationManager.cancel(qua_notifyID);
                    }catch (Exception e){
                        e.printStackTrace();
                        Log.d(TAG, "No previous walking survey yet.");
                    }finally {
                        MinukuStreamManager.cancelWalkingSurveyFlag = false;
                    }
                }


            }else{
                Log.d(TAG, "checkSleepingTime true.");
            }
            StoreToCSV(new Date().getTime(), interval_sampled, walkoutdoor_sampled, walk_first_sampled, walk_second_sampled, walk_third_sampled);

        }
    };


    private boolean timeForSurvey() {

        long now  = ScheduleAndSampleManager.getCurrentTimeInMillis();
        boolean isTimeToSendSurvey = false;

        //set the default 0 so that we can send the first survey
        long last_Survey_Time =sharedPrefs.getLong("last_Survey_Time", 0 );

        Log.d(TAG, "last_Survey_Time : "+last_Survey_Time);

        if ( now - last_Survey_Time > Constants.MILLISECONDS_PER_HOUR
                && now - last_Walking_Survey_Time > 2 * Constants.MILLISECONDS_PER_HOUR) {

            isTimeToSendSurvey = true;
        }
        return isTimeToSendSurvey;

    }


    /**
     *
     */
    private void setUpSurveyLink() {
        Log.d(TAG, "setUpSurveyLink");

        //Counting the day of the experiments
        int TaskDayCount = sharedPrefs.getInt("TaskDayCount", Constants.TaskDayCount);
        TaskDayCount++;

        if ((TaskDayCount - 1) % 7 == 0) { //1, 8, 15
            weekNum++;
        }

        dailySurveyNum = TaskDayCount % 7;
        if (dailySurveyNum == 0)
            dailySurveyNum = 7;

        dailyResponseNum = 0;


        sharedPrefs.edit().putInt("TaskDayCount", TaskDayCount).apply();
        sharedPrefs.edit().putInt("weekNum", weekNum).apply();
        sharedPrefs.edit().putInt("dailySurveyNum", dailySurveyNum).apply();
        sharedPrefs.edit().putInt("dailyResponseNum", dailyResponseNum).apply();

    }


    /**
     *
     */
    private void sendSurveyLink() {

        //cancel the survey if it exists
        //the new notification should replace the old one even if it is not the same id.
        try{
            mNotificationManager.cancel(qua_notifyID);
        }catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, "no old walking notification");
//            android.util.Log.e(TAG, "exception", e);
        }

        try{
            mNotificationManager.cancel(interval_notifyID);
        }catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, "no old random notification");
//            android.util.Log.e(TAG, "exception", e);
        }

        //TODO conceal it if the mechanism is right
        try{
            mNotificationManager.cancel(1);
        }catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, "no old test notification");
//            android.util.Log.e(TAG, "exception", e);
        }

        intervalQualtrics();
        addSurveyLinkToDB();

        //after we send out the survey decrease the needed number for the interval_sample_number
        interval_sample_number--;
        sharedPrefs.edit().putInt("interval_sample_number", interval_sample_number).apply();

    }


    /**
     * the next interval-based esm should be based on the last time the user opened the questionnaire
     */
    public static void setUpNextIntervalSurvey() {
        //reset the Interval Sampleing after we send the survey
        if (interval_sampled < 3) {
            settingIntervalSampling(new Date().getTime());//TODO input time
        } else { //cancel all the interval today if the number of the sample are enough.
            //TODO might not work
            cancelAlarmAll(mContext);
        }

    }



    /**
     *
     */
    private void triggerIntervalSurvey() {

        //send survey
        sendSurveyLink();

        //after sending survey, update the preference
        interval_sampled++;
        sharedPrefs.edit().putInt("interval_sampled", interval_sampled).apply();

        //reset the Interval Sampleing after we send the survey
        if (interval_sampled < 3) {
            settingIntervalSampling(new Date().getTime());//TODO input time

        }else{ //cancel all the interval today if the number of the sample are enough.
            //TODO might not work
            cancelAlarmAll(mContext);
        }

        //update the last survey time
        last_Survey_Time = new Date().getTime();
        sharedPrefs.edit().putLong("last_Survey_Time", last_Survey_Time).apply();

        //no need to set the next interval time because it is belonging to interval survey.
//        setUpNextIntervalSurvey();

    }


    /**
     *
     * @param endOfFirstThird
     * @param endOfSecondThird
     * @param endOfThirdThird
     */
    private void triggerWalkingSurvey(long endOfFirstThird, long endOfSecondThird, long endOfThirdThird) {

        long currentTime = ScheduleAndSampleManager.getCurrentTimeInMillis();

        //send survey
        sendSurveyLink();

        //after sending survey, updatge the preference
        walkoutdoor_sampled++;
        sharedPrefs.edit().putInt("walkoutdoor_sampled", walkoutdoor_sampled).apply();

        //after sending survey, updatge the preference
        if (currentTime <= endOfFirstThird) {
            walk_first_sampled++;
            sharedPrefs.edit().putInt("walk_first_sampled", walk_first_sampled).apply();
        } else if (currentTime <= endOfSecondThird && currentTime > endOfFirstThird) {
            walk_second_sampled++;
            sharedPrefs.edit().putInt("walk_second_sampled", walk_second_sampled).apply();
        } else if (currentTime <= endOfThirdThird && currentTime > endOfSecondThird) {
            walk_third_sampled++;
            sharedPrefs.edit().putInt("walk_third_sampled", walk_third_sampled).apply();
        }

        last_Walking_Survey_Time = new Date().getTime();
        sharedPrefs.edit().putLong("last_Walking_Survey_Time", last_Walking_Survey_Time).apply();

        //preventing the same on_foot trip will trigger two notifications.
        sameTripPrevent = true;
        sharedPrefs.edit().putBoolean("sameTripPrevent", sameTripPrevent).apply();

        setUpNextIntervalSurvey();

    }


    public boolean isWalkingOutdoor(long walkingStarTime, long walkingEndtime) {

        //get locations in the last minute
        ArrayList<LatLng> latlngs = getLocationRecordInLastMinute(walkingStarTime, walkingEndtime);

        //get distance from the last minute
        double dist = calculateTotalDistanceOfPath(latlngs);

        //50 < dist < 150 means walking outdoor
        if(dist >= 50 && dist <= 150){
            return true;
        }
        else
            return false;

    }


    public String getDateCurrentTimeZone(long timestamp) {
        try{
            Calendar calendar = Calendar.getInstance();
//            TimeZone tz = TimeZone.getDefault(); //TODO maybe need to add it back
            calendar.setTimeInMillis(timestamp);
//            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date currenTimeZone = (Date) calendar.getTime();
            return sdf.format(currenTimeZone);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "";
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

    private double calculateTotalDistanceOfPath(ArrayList<LatLng> latLngs){

        double dist = -1;

        for(int index = 0; index < latLngs.size()-1; index++){
            LatLng latLng = latLngs.get(index);
            LatLng latLng2 = latLngs.get(index+1);
            float[] results = new float[1];
            Location.distanceBetween(latLng.latitude,latLng.longitude,latLng2.latitude,latLng2.longitude, results);
            dist += results[0];
        }

        return dist;

    }

    private ArrayList<LatLng> getLocationRecordInLastMinute(long lastminute, long now) {

        ArrayList<LatLng> LatLngs = new ArrayList<LatLng>();

        Log.d(TAG, "[test sampling] getLocationRecordInLastMinute ");

        //get data from the database
        ArrayList<String> data = DBHelper.queryRecordsBetweenTimes(DBHelper.STREAM_TYPE_LOCATION, lastminute, now);

        Log.d(TAG, "[test sampling] getLocationRecordInLastMinute get data:" + data.size() + "rows");

        for (int i = 0; i < data.size(); i++) {

            String[] record = data.get(i).split(Constants.DELIMITER);
            double lat = Double.parseDouble(record[2]);
            double lng = Double.parseDouble(record[3]);

            LatLngs.add(new LatLng(lat, lng));

        }

        return LatLngs;
    }

//add to DB in order to display it in the linklistOhio.java
    public void addSurveyLinkToDB(){
        Log.d(TAG, "addSurveyLinkToDB");

        dailyResponseNum++;
        sharedPrefs.edit().putInt("dailyResponseNum", dailyResponseNum).apply();

        String mob = "";
        try {
            ConnectivityDataRecord connectivityDataRecord = ConnectivityStreamGenerator.toOtherconnectDataRecord;

            if (connectivityDataRecord.getIsMobileConnected()) {
                mob = "1";
            } else {
                mob = "0";
            }
        }catch (Exception e){
            e.printStackTrace();
            mob = "0";
        }

        String linktoShow = link + "?p="+participantID + "&g=" + groupNum + "&w=" + weekNum + "&d=" + dailySurveyNum + "&r=" + dailyResponseNum + "&m=" + mob;

        ContentValues values = new ContentValues();

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            values.put(DBHelper.generateTime_col, new Date().getTime());
            values.put(DBHelper.link_col, linktoShow);
//            values.put(DBHelper.openFlag_col, 0); //they can't enter the link by the notification.

//            db.insert(DBHelper.checkFamiliarOrNotLinkList_table, null, values);
            db.insert(DBHelper.surveyLink_table, null, values);

        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
        finally {
            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection
        }
    }


    private void notiQualtrics(){

        Log.d(TAG,"notiQualtrics");

        //TODO if the last link haven't been opened, confirm it's missed, setting the missed time.
        String latestLinkData = DataHandler.getLatestSurveyData();
        //if there have data in DB
        if(!latestLinkData.equals("")) {

            String clickOrNot = latestLinkData.split(Constants.DELIMITER)[5];

            String id = latestLinkData.split(Constants.DELIMITER)[0];

            DataHandler.updateSurveyMissTime(id, DBHelper.missedTime_col);

        }
        String notiText = "You have a new walking survey(Walking)"+"\r\n"
                +"Walking : "+walkoutdoor_sampled+" ; Random : "+interval_sampled;

        Intent resultIntent = new Intent(SurveyTriggerService.this, SurveyActivity.class);
        PendingIntent pending = PendingIntent.getActivity(SurveyTriggerService.this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);//Context.
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(notiText);

        Notification note = new Notification.Builder(mContext)
                .setContentTitle(Constants.APP_NAME)
                .setContentText(notiText)
                .setContentIntent(pending)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.drawable.self_reflection)
                .setAutoCancel(true)
                .build();

        // using the same tag and Id causes the new notification to replace an existing one
        mNotificationManager.notify(qua_notifyID, note); //String.valueOf(System.currentTimeMillis()),
        note.flags = Notification.FLAG_AUTO_CANCEL;

    }

    private void showTransportationAndIsHome(){

        Log.e(TAG,"showTransportationAndIsHome");

        /*String inhomeornot="not working yet";

        if(home==1)
            inhomeornot = "home";
        else if(home!=1 && dist<=200)
            inhomeornot = "near home";
        else if(home!=1 && dist>200)
            inhomeornot = "faraway";*/

        String NotificationText = "";

        try {
//            String local_transportation = TransportationModeStreamGenerator.toCheckFamiliarOrNotTransportationModeDataRecord.getConfirmedActivityString();

            String local_transportation = transportation;

            ArrayList<String> sampled_times = new ArrayList<String>();

            for(int i = 0; i < 6 ; i++) {
                String each_time = sharedPrefs.getString("sampled_times_" + i,"NA");
                Log.d(TAG,"each_time : "+each_time);
                String getRidOfDate = each_time.split(" ")[1];
                sampled_times.add(getRidOfDate);
            }

            int walkoutdoor = sharedPrefs.getInt("walkoutdoor_sampled", -99);
            int interval = sharedPrefs.getInt("interval_sampled", -99);

            NotificationText = "walking outdoor count : "+walkoutdoor
                    + "\r\n" +"random time : "
                    +(sampled_times.get(0))+", "+(sampled_times.get(1))+", "
                    +(sampled_times.get(2))+", "+(sampled_times.get(3))+", "
                    +(sampled_times.get(4))+", "+(sampled_times.get(5))+", "
                    + "\r\n" +"random count : "+interval;
/*
            NotificationText = "Current Transportation Mode: " + local_transportation
                    + "\r\n" + "ActivityRecognition : " + ActivityRecognitionStreamGenerator.getActivityNameFromType(ActivityRecognitionStreamGenerator.sMostProbableActivity.getType())
                    + "\r\n" + "interval sampling count : " + interval_sampled
                    + "\r\n" + "Is_home: " + inhomeornot
                    + "\r\n" + "Home, moving: " + daily_count_HomeMove
                    + "\r\n" + "Home, not moving: " + daily_count_HomeNotMove
                    + "\r\n" + "Near home, moving: " + daily_count_NearHomeMove
                    + "\r\n" + "Near home, not moving: " + daily_count_NearHomeNotMove
                    + "\r\n" + "Faraway, moving: " + daily_count_FarawayMove
                    + "\r\n" + "Faraway, not moving: " + daily_count_FarawayNotMove
                    + "\r\n" + "Total: " + (daily_count_HomeMove + daily_count_HomeNotMove
                                          + daily_count_NearHomeMove + daily_count_NearHomeNotMove
                                          + daily_count_FarawayMove + daily_count_FarawayNotMove);
            if(testingserverThenFail)
                NotificationText = NotificationText + "\r\n" + "Server Error";
*/

            Log.e(TAG, "getConfirmedActivityString : " + local_transportation);
        }catch (Exception e){
            e.printStackTrace();
        }

        // pending implicit intent to view url
        /*Intent resultIntent = new Intent(Intent.ACTION_VIEW);
        resultIntent.setData(Uri.parse(link));*/
//        Intent resultIntent = new Intent(SurveyTriggerService.this, SurveyActivity.class);
//        PendingIntent pending = PendingIntent.getActivity(SurveyTriggerService.this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);//Context.
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(NotificationText);

        Notification note = null;

        if(testingserverThenFail){
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            note = new Notification.Builder(mContext)
                    .setContentTitle(Constants.APP_NAME)
                    .setContentText(NotificationText)
//                .setContentIntent(pending)
                    .setStyle(bigTextStyle)
                    .setSmallIcon(R.drawable.self_reflection)
                    .setAutoCancel(true)
                    .setSound(alarmSound)
                    .build();
        }else{
            note = new Notification.Builder(mContext)
                    .setContentTitle(Constants.APP_NAME)
                    .setContentText(NotificationText)
//                .setContentIntent(pending)
                    .setStyle(bigTextStyle)
                    .setSmallIcon(R.drawable.self_reflection)
                    .setAutoCancel(true)
                    .build();
        }

        //note.flags |= Notification.FLAG_NO_CLEAR;
        //startForeground( 42, note );

        // using the same tag and Id causes the new notification to replace an existing one
        mNotificationManager.notify(notifyID, note); //String.valueOf(System.currentTimeMillis()),
        note.flags = Notification.FLAG_AUTO_CANCEL;

        //note.setContentText(NotificationText);
        //mNotificationManager.notify(String.valueOf(System.currentTimeMillis()), 1, note.build());

    }

    public boolean checkSleepingTime(){

        String startSleepingTime = sharedPrefs.getString("SleepingStartTime","22:00");
        String endSleepingTime = sharedPrefs.getString("SleepingEndTime","08:00");

        Log.d(TAG, "startSleepingTime : " + startSleepingTime + " endSleepingTime : " + endSleepingTime);

        if(startSleepingTime!=null && endSleepingTime!=null) {

            String[] startSleepingTimeArray = startSleepingTime.split(":");
            String[] endSleepingTimeArray = endSleepingTime.split(":");

            Log.d(TAG, "startSleepingHour : " + startSleepingTimeArray[0] + " startSleepingMin : " + startSleepingTimeArray[1]
                    + " endSleepingHour : " + endSleepingTimeArray[0] + " endSleepingMin : " + endSleepingTimeArray[1]);

            int startSleepingHour = Integer.valueOf(startSleepingTimeArray[0]);
            int startSleepingMin = Integer.valueOf(startSleepingTimeArray[1]);
            int endSleepingHour = Integer.valueOf(endSleepingTimeArray[0]);
            int endSleepingMin = Integer.valueOf(endSleepingTimeArray[1]);

            Log.d(TAG, "int startSleepingHour : " + String.valueOf(startSleepingHour) + " startSleepingMin : " + String.valueOf(startSleepingMin)
                    + " endSleepingHour : " + String.valueOf(endSleepingHour) + " endSleepingMin : " + String.valueOf(endSleepingMin));

            TimeZone tz = TimeZone.getDefault();
            java.util.Calendar cal = java.util.Calendar.getInstance(tz);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int min = cal.get(Calendar.MINUTE);
            Log.d(TAG, "hour : " + hour + " min : " + min);

            if (startSleepingHour < endSleepingHour) {

                if (hour >= startSleepingHour && hour <= endSleepingHour) {

                    return true;
                }

            } else if (startSleepingHour == endSleepingHour) {

                if (startSleepingMin < endSleepingMin) {

                    if (min >= startSleepingMin && min <= endSleepingMin)
                        return true;
                    else
                        return false;

                } else if (startSleepingMin == endSleepingMin)
                    return false;
                else {

                    if (min >= endSleepingMin && min <= startSleepingMin)
                        return false;
                    else
                        return true;

                }

            } else {

                if (min >= endSleepingMin && min <= startSleepingMin)
                    return false;
                else
                    return true;

            }
        }
        return false;

    }


    public void StoreToCSV(long timestamp, long random, long walktotal, long walkfirst, long walksecond, long walkthird){

        Log.d(TAG,"StoreToCSV");

        String sFileName = "SamplingCheck.csv";

        Boolean SamplingCheckfirstOrNot = sharedPrefs.getBoolean("SamplingCheckfirstOrNot", true);

        try {
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH + sFileName, true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"timestamp","timeString","Latitude","Longitude","Accuracy"});
            String timeString = getTimeString(timestamp);

            if(SamplingCheckfirstOrNot) {
                data.add(new String[]{"timestamp", "timeString", "random_survey", "walktotal_survey", "walkfirst_survey", "walksecond_survey", "walkthird_survey"});
                sharedPrefs.edit().putBoolean("SamplingCheckfirstOrNot", false).apply();
            }

            data.add(new String[]{String.valueOf(timestamp), timeString, String.valueOf(random), String.valueOf(walktotal), String.valueOf(walkfirst), String.valueOf(walksecond), String.valueOf(walkthird)});

            csv_writer.writeAll(data);

            csv_writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static void storeToCSVinterval(String todayDate){

        String sFileName = "Interval_"+todayDate+".csv";

        sFileName = sFileName.replace("/","-");

        Log.d(TAG, "sFileName : " + sFileName);

        try {
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);

            Log.d(TAG, "root : " + root);

            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "interval_sampled_times : "+interval_sampled_times);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"","HomeMove","HomeNotMove","NearHomeMove","NearHomeNotMove","FarawayMove","FarawayNotMove"});

            String[] addToCSVFile = new String[]{};
            List<String> addToCSV = new ArrayList<String>();

            for(int index=0 ; index < interval_sample_number ; index++){
                addToCSV.add(getTimeString(interval_sampled_times.get(index)));
            }

            addToCSVFile = addToCSV.toArray(addToCSVFile);

            data.add(addToCSVFile);

            csv_writer.writeAll(data);

            csv_writer.close();

        } catch(IOException e) {
            e.printStackTrace();
            android.util.Log.e(TAG, "exception", e);
        } catch (IndexOutOfBoundsException e2){
            e2.printStackTrace();
            android.util.Log.e(TAG, "exception", e2);

        }

    }

    public static int generatePendingIntentRequestCode(long time){

        int code = 0;

        if (time-time_base > 1000000000){
            time_base = ScheduleAndSampleManager.getCurrentTimeInMillis();
        }

        return (int) (time-time_base);
    }

    private int getCurrentHour(){
        TimeZone tz = TimeZone.getDefault();
        java.util.Calendar cal = java.util.Calendar.getInstance(tz);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        return hour;
    }

    public static boolean isServiceRunning() {
        return serviceInstance != null;
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    private void intervalQualtrics(){

        //TODO if the last link haven't been opened, setting it into missed.
        String latestLinkData = DataHandler.getLatestSurveyData();
        //if there have data in DB
        if(!latestLinkData.equals("")) {

            String clickOrNot = latestLinkData.split(Constants.DELIMITER)[5];

            String id = latestLinkData.split(Constants.DELIMITER)[0];

            DataHandler.updateSurveyMissTime(id, DBHelper.missedTime_col);

        }

        String notiText = "You have a new random survey(Random)"+"\r\n"
                +"Walking : "+walkoutdoor_sampled+" ;Random : "+interval_sampled;

        Log.d(TAG,"intervalQualtrics");

        Intent resultIntent = new Intent(SurveyTriggerService.this, SurveyActivity.class);
        PendingIntent pending = PendingIntent.getActivity(SurveyTriggerService.this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);//Context.
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(notiText);

        Notification note = new Notification.Builder(mContext)
                .setContentTitle(Constants.APP_NAME)
                .setContentText(notiText)
                .setContentIntent(pending)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.drawable.self_reflection)
                .setAutoCancel(true)
                .build();

        // using the same tag and Id causes the new notification to replace an existing one
        mNotificationManager.notify(interval_notifyID, note); //String.valueOf(System.currentTimeMillis()),
        note.flags = Notification.FLAG_AUTO_CANCEL;

    }



    public void registerActionAlarmReceiver(){

        alarmregistered = true;

        sharedPrefs.edit().putBoolean("alarmregistered", alarmregistered);
        sharedPrefs.edit().commit();

        //register action alarm
        IntentFilter alarm_filter = new IntentFilter(Constants.Interval_Sample);
        mContext.registerReceiver(IntervalSampleReceiver, alarm_filter);

    }

    public void unregisterActionAlarmReceiver(){
        mContext.unregisterReceiver(IntervalSampleReceiver);

        alarmregistered = false;

        sharedPrefs.edit().putBoolean("alarmregistered", alarmregistered);
        sharedPrefs.edit().commit();
    }

    public void addToDB_interval(String link){
        Log.d(TAG, "addSurveyLinkToDB");

        dailyResponseNum++;
        sharedPrefs.edit().putInt("dailyResponseNum", dailyResponseNum).apply();
        String mob = "";
        try {
            ConnectivityDataRecord connectivityDataRecord = ConnectivityStreamGenerator.toOtherconnectDataRecord;

            if (connectivityDataRecord.getIsMobileConnected()) {
                mob = "1";
            } else {
                mob = "0";
            }
        }catch (Exception e){
            e.printStackTrace();
            mob = "0";
        }

        String linktoShow = link + "?p="+participantID + "&g=" + groupNum + "&w=" + weekNum + "&d=" + dailySurveyNum + "&r=" + dailyResponseNum + "&m=" + mob;

        ContentValues values = new ContentValues();

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            values.put(DBHelper.TIME, new Date().getTime());
            values.put(DBHelper.link_col, linktoShow);
            values.put(DBHelper.openFlag_col, 0); //they can't enter the link by the notification.

            db.insert(DBHelper.intervalSampleLinkList_table, null, values);
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
        finally {
            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection
        }
    }

    BroadcastReceiver IntervalSampleReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Constants.Interval_Sample)){
                Log.d(TAG, "In IntervalSampleReceiver");

                if (interval_sampled < 3 && timeForSurvey()) {
                    triggerIntervalSurvey();
                }

            }

        }
    };

}
