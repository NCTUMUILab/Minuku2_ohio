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

import edu.ohio.minuku.DBHelper.DBHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.model.DataRecord.ConnectivityDataRecord;
import edu.ohio.minuku.model.DataRecord.TelephonyDataRecord;
import edu.ohio.minuku.service.ActivityRecognitionService;
import edu.ohio.minuku.streamgenerator.ConnectivityStreamGenerator;
import edu.ohio.minuku.streamgenerator.LocationStreamGenerator;
import edu.ohio.minuku.streamgenerator.TelephonyStreamGenerator;
import edu.ohio.minuku_2.R;
import edu.ohio.minuku_2.controller.Ohio.linkListohio;
import edu.ohio.minuku_2.model.CheckFamiliarOrNotDataRecord;
import edu.ohio.minuku_2.streamgenerator.CheckFamiliarOrNotStreamGenerator;
import edu.ohio.minukucore.exception.StreamNotFoundException;

//import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/4/23.
 */

public class CheckFamiliarOrNotService extends Service {

    private final static String TAG = "CheckFamiliarOrNotService";

    //public ContextManager mContextManager; //TODO might be removed for the new logic in this code.
    private static Context serviceInstance;
    private static Handler mMainThread;
    private Context mContext;
    Intent intent = new Intent();
    private ProgressDialog loadingProgressDialog;

    private static AlarmManager mAlarmManager;

    ScheduledFuture<?> scheduledFuture= null;
    private ScheduledExecutorService mScheduledExecutorService;
    public static final int REFRESH_FREQUENCY = 10; //10s, 10000ms TODO convert to 60 * 5
    public static final int BACKGROUND_RECORDING_INITIAL_DELAY = 0;

    private final int URL_TIMEOUT = 5 * 1000; //TODO try 5s

    public static final int HTTP_TIMEOUT = 10000; // millisecond
    public static final int SOCKET_TIMEOUT = 20000; // millisecond

    private float latitude;
    private float longitude;
    private float accuracy;

    private String userid;

    private int home = 0;
    private int neighbor = 0;
    private int outside = 0;
    private float dist=0;
    private long nowtime=0;
    private long lastTimeSend_HomeMove=0;
    private long lastTimeSend_HomeNotMove=0;
    private long lastTimeSend_NearHomeMove=0;
    private long lastTimeSend_NearHomeNotMove=0;
    private long lastTimeSend_FarawayMove=0;
    private long lastTimeSend_FarawayNotMove=0;

    private float diffTime_HomeMove=0;
    private float diffTime_HomeNotMove=0;
    private float diffTime_NearHomeMove=0;
    private float diffTime_NearHomeNotMove=0;
    private float diffTime_FarawayMove=0;
    private float diffTime_FarawayNotMove=0;

    final private double period_onehalfHour = 1.5;

    final private double period_HomeMove = period_onehalfHour; //1;  //0.1 = 6min, 1 = 60min
    final private double period_HomeNotMove = period_onehalfHour; //1;
    final private double period_NearHomeMove = period_onehalfHour; //0.5; //0.5
    final private double period_NearHomeNotMove = period_onehalfHour; //0.5;
    final private double period_FarawayMove = period_onehalfHour; //0.5;
    final private double period_FarawayNotMove = period_onehalfHour; //0.5;

    private int daily_count_HomeMove = 0;
    private int daily_count_HomeNotMove = 0;
    private int daily_count_NearHomeMove = 0;
    private int daily_count_NearHomeNotMove = 0;
    private int daily_count_FarawayMove = 0;
    private int daily_count_FarawayNotMove = 0;

    private int countforResponsed = 0;

    private int probability_low_HomeMove = 10;
    private int probability_normal_HomeMove = 30;
    private int probability_high_HomeMove = 50;

    private int probability_low_HomeNotMove = 10;
    private int probability_normal_HomeNotMove = 30;
    private int probability_high_HomeNotMove = 50;

    private int probability_low_NearHomeMove = 20;
    private int probability_normal_NearHomeMove = 40;
    private int probability_high_NearHomeMove = 60;

    private int probability_low_NearHomeNotMove = 20;
    private int probability_normal_NearHomeNotMove = 40;
    private int probability_high_NearHomeNotMove = 60;

    private int probability_low_FarawayMove = 30;
    private int probability_normal_FarawayMove = 50;
    private int probability_high_FarawayMove = 70;

    private int probability_low_FarawayNotMove = 30;
    private int probability_normal_FarawayNotMove = 50;
    private int probability_high_FarawayNotMove = 70;

    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";

    private final long One_Hour_In_milliseconds = 60 * 60 * 1000;

    private String today;
    private String lastTimeSend_today;
    private int current_hour;
    private int last_hour;

    private int notifyID = 1;
    private int qua_notifyID = 2;
    private int interval_notifyID = 3; //used to 3
    //qua is equal to interval now

    ArrayList<Long> interval_sampled_times;

    private int interval_sample_number;

    private int interval_sampled;
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

    private CSVWriter csv_writer = null;

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

    private SharedPreferences sharedPrefs;

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public CheckFamiliarOrNotService(){}

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
        isServiceRunning = sharedPrefs.getBoolean("isServiceRunning", false);

        interval_sampled = sharedPrefs.getInt("interval_sampled", 0);

        alarmregistered = sharedPrefs.getBoolean("alarmregistered",false);

        sameTripPrevent = sharedPrefs.getBoolean("sameTripPrevent", false);

        countingTimeForIndoorOutdoor = sharedPrefs.getInt("countingTimeForIndoorOutdoor", 0);

        time_base = getCurrentTimeInMillis();

        mScheduledExecutorService = Executors.newScheduledThreadPool(REFRESH_FREQUENCY);

        mAlarmManager = (AlarmManager)mContext.getSystemService( mContext.ALARM_SERVICE );

        registerActionAlarmReceiver();

        createCSV();

        try {
            checkFamiliarOrNotStreamGenerator = (CheckFamiliarOrNotStreamGenerator) MinukuStreamManager.getInstance().getStreamGeneratorFor(CheckFamiliarOrNotDataRecord.class);
        }catch(StreamNotFoundException e){
            Log.e(TAG,"checkFamiliarOrNotStreamGenerator haven't created yet.");
        }
    }

    public static Context setCheckFamiliarOrNotService(){
        return serviceInstance;
    }

    public static Context getInstance() {
        if(CheckFamiliarOrNotService.serviceInstance == null) {
            try {
                CheckFamiliarOrNotService.serviceInstance = new CheckFamiliarOrNotService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return CheckFamiliarOrNotService.serviceInstance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("CheckFamiliarOrNotService", "[test service running] going to start the probe service, isServiceRunning:  " + isServiceRunning());

        int permissionStatus= ContextCompat.checkSelfPermission(CheckFamiliarOrNotService.this, android.Manifest.permission.READ_PHONE_STATE);
        if(permissionStatus== PackageManager.PERMISSION_GRANTED) {
            TelephonyManager mngr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            Constants.DEVICE_ID = mngr.getDeviceId();
            userid = Constants.DEVICE_ID;
        }

        daily_count_HomeMove = sharedPrefs.getInt("daily_count_HomeMove", 0);
        daily_count_HomeNotMove = sharedPrefs.getInt("daily_count_HomeNotMove", 0);
        daily_count_NearHomeMove = sharedPrefs.getInt("daily_count_NearHomeMove", 0);
        daily_count_NearHomeNotMove = sharedPrefs.getInt("daily_count_NearHomeNotMove", 0);
        daily_count_FarawayMove = sharedPrefs.getInt("daily_count_FarawayMove", 0);
        daily_count_FarawayNotMove = sharedPrefs.getInt("daily_count_FarawayNotMove", 0);

        last_hour = sharedPrefs.getInt("last_hour",-99);
        lastTimeSend_today = sharedPrefs.getString("lastTimeSend_today","NA");
        interval_sampled = sharedPrefs.getInt("interval_sampled", 0);
        alarmregistered = sharedPrefs.getBoolean("alarmregistered",false);

        Constants.USER_ID = sharedPrefs.getString("userid","NA");

        Log.d(TAG, "onStartCommand");

        if(!isServiceRunning)
            startService();

//        return START_STICKY;
        return START_REDELIVER_INTENT;
    }


    public void startService(){

        SharedPreferences.Editor editor = getSharedPreferences("isServiceRunning", MODE_PRIVATE).edit();
        editor.putBoolean("isServiceRunning", true);
        editor.commit();

        Log.d(TAG,"startService");

//        isServiceRunning = true;

        runMainThread();

    }

    public void runMainThread(){

        Log.d(TAG,"runMainThread");

        if(scheduledFuture!= null)
            scheduledFuture.cancel(true);

        Log.d(TAG,"scheduledFuture is not null : " + (scheduledFuture != null));

        scheduledFuture = mScheduledExecutorService.scheduleAtFixedRate(
                CheckFamiliarOrNotRunnable,
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

    public static String getTimeString(long time){

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW);
        String currentTimeString = sdf_now.format(time);

        return currentTimeString;
    }

    private void settingIntervalSampleing(long startTimeAfterWalkingSampling){

        Log.d(TAG, "settingIntervalSampleing");

        String yMdformat = today;

        Log.d(TAG, "yMdformat : " + yMdformat);
        interval_sampled_times = new ArrayList<>();
        ArrayList<Long> times = new ArrayList<Long>();
        long now = getCurrentTimeInMillis();
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
            startTime = getSpecialTimeInMillis(yMdformat+" "+sleepingendTime+":00");

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

//                mAlarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);

                //update the latest request_code to be the last request_code.
                sharedPrefs.edit().putInt("last_request_code_"+i, request_code).apply();
                sharedPrefs.edit().putString("sampled_times_"+i, getTimeString(time)).apply();
//                sharedPrefs.edit().apply();
            }

        }

        if (isExternalStorageWritable())
            storeToCSVinterval(today);

    }

    public void cancelAlarmIfExists(Context mContext,int requestCode,Intent intent){
        try {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent,0);
            AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelAlarmAll(Context mContext){
        try {
            Intent intent = new Intent(Constants.Interval_Sample);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,0);
            AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Runnable CheckFamiliarOrNotRunnable = new Runnable() {
        @Override
        public void run() {

            try {

                userid = Constants.DEVICE_ID;

                //variable for link
                participantID = sharedPrefs.getString("userid", "NA");
                groupNum = sharedPrefs.getString("groupNum", "NA");
                weekNum = sharedPrefs.getInt("weekNum", 0);
                dailySurveyNum = sharedPrefs.getInt("dailySurveyNum", 0);
                dailyResponseNum = sharedPrefs.getInt("dailyResponseNum", 0);

                Log.d(TAG, "CheckFamiliarOrNotRunnable");

                Date curDate = new Date(System.currentTimeMillis());
                String dateformat = "yyyy/MM/dd";
                SimpleDateFormat df = new SimpleDateFormat(dateformat);
                today = df.format(curDate);

                String dateformat2 = "dd";
                SimpleDateFormat df2 = new SimpleDateFormat(dateformat2);
                String todayDate = df2.format(curDate);

                Log.d(TAG, "todayDate : " + todayDate);

                //setting third of the day
                String startSleepingTime = sharedPrefs.getString("SleepingStartTime","22:00");
                String endSleepingTime = sharedPrefs.getString("SleepingEndTime","08:00");

                Log.d(TAG,"startSleepingTime : "+ startSleepingTime);
                Log.d(TAG,"endSleepingTime : "+ endSleepingTime);

                long endDay_settingthird = getSpecialTimeInMillis(today+" "+startSleepingTime+":00");
                long startDay_settingthird = getSpecialTimeInMillis(today+" "+endSleepingTime+":00");

                long third_interval = (endDay_settingthird - startDay_settingthird)/3;
                long first_third = startDay_settingthird + third_interval;
                long second_third = startDay_settingthird + third_interval * 2;
                long third_third = startDay_settingthird + third_interval * 3;

                current_hour = getCurrentHour();

                Log.d(TAG, "today is " + today);

                //initialize
                home = 0;
                neighbor = 0;
                outside = 0;
                dist = 0;
                testingserverThenFail = false;

                Log.d(TAG, "userid: " + userid);

                //after a day
                if (!lastTimeSend_today.equals(today)) {

                    //setting the timing for the interval sampling.
//                    if (Long.valueOf(todayDate) % 2 == 1) //run it at odd date
//                        settingIntervalSampleing();
//                    else  //TODO delete the interval sampling. checking the first time day ot not.
//                        ;

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
                    settingIntervalSampleing(-9999);


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

                    //deprecated
                    /*String tofilelastTimeSend_today = lastTimeSend_today;

                    tofilelastTimeSend_today = tofilelastTimeSend_today.replace("/", "-");

                    if (isExternalStorageWritable()) {

                        String sFileName = "LogAt_" + tofilelastTimeSend_today + ".csv";

                        try {
                            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
                            if (!root.exists()) {
                                root.mkdirs();
                            }

                            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH + sFileName, true));

                            List<String[]> data = new ArrayList<String[]>();

                            data.add(new String[]{"", "HomeMove", "HomeNotMove", "NearHomeMove", "NearHomeNotMove", "FarawayMove", "FarawayNotMove"});

                            csv_writer.writeAll(data);

                            csv_writer.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                            e.getMessage();
                        }
                    }
                    daily_count_HomeMove = 0;
                    daily_count_HomeNotMove = 0;
                    daily_count_NearHomeMove = 0;
                    daily_count_NearHomeNotMove = 0;
                    daily_count_FarawayMove = 0;
                    daily_count_FarawayNotMove = 0;

                    sharedPrefs.edit().putInt("daily_count_HomeMove", daily_count_HomeMove).apply();
                    sharedPrefs.edit().putInt("daily_count_HomeNotMove", daily_count_HomeNotMove).apply();
                    sharedPrefs.edit().putInt("daily_count_NearHomeMove", daily_count_NearHomeMove).apply();
                    sharedPrefs.edit().putInt("daily_count_NearHomeNotMove", daily_count_NearHomeNotMove).apply();
                    sharedPrefs.edit().putInt("daily_count_FarawayMove", daily_count_FarawayMove).apply();
                    sharedPrefs.edit().putInt("daily_count_FarawayNotMove", daily_count_FarawayNotMove).apply();

                    interval_sampled = 0;
                    sharedPrefs.edit().putInt("interval_sampled", interval_sampled).apply();

                    countforResponsed = 0;

                }

                Log.d(TAG, "last_hour : " + String.valueOf(last_hour));

                if ((last_hour != current_hour) && isExternalStorageWritable()) {
                    last_hour = current_hour;
                    sharedPrefs.edit().putInt("last_hour", last_hour).apply();

                    storeToCSV(lastTimeSend_today);
                }*/

                }

                //temporaily remove the trigger limit
                if (!checkSleepingTime()) {

                    //get latitude and longitude from LocationStreamGenerator
                    try {

                        Log.d(TAG + " trying to get ", "latitude : " + latitude + " longitude : " + longitude);
                        latitude = LocationStreamGenerator.toCheckFamiliarOrNotLocationDataRecord.getLatitude();
                        longitude = LocationStreamGenerator.toCheckFamiliarOrNotLocationDataRecord.getLongitude();
                        accuracy = LocationStreamGenerator.toCheckFamiliarOrNotLocationDataRecord.getAccuracy();

                        sharedPrefs.edit().putFloat("latestLatitude", latitude).apply();
                        sharedPrefs.edit().putFloat("latestLongitude", longitude).apply();

                    } catch (Exception e) {

                        Log.d(TAG + " before correcting Exception ", "latitude : " + latitude + " longitude : " + longitude);

                        latitude = sharedPrefs.getFloat("latestLatitude", (float) -999.0);
                        longitude = sharedPrefs.getFloat("latestLongitude", (float) -999.0);

                        e.printStackTrace();
                    }

                    Log.d(TAG + " testing server", "latitude : " + latitude + " longitude : " + longitude);

                    StoreToCSV(new Date().getTime(), latitude, longitude, accuracy, userid);

                    Log.d(TAG, "getCurrentTimeInMillis : " + String.valueOf(getCurrentTimeInMillis()));
                    Log.d(TAG, "getCurrentTimeInMillis/1000 : " + String.valueOf((getCurrentTimeInMillis() / 1000)));

                    //check the server
                    String mcog = "http://mcog.asc.ohio-state.edu/apps/pip?" +
                            "lon=" + String.valueOf(longitude) +
                            "&lat=" + String.valueOf(latitude) +
                            "&posaccuracy=" + String.valueOf(accuracy) +
                            "&currentposts=" + String.valueOf(new Date().getTime() / 1000) +
                            "&userid=" + String.valueOf(Constants.USER_ID) +
                            "&tsphone=" + String.valueOf((getCurrentTimeInMillis() / 1000)) +
                            "&lastposupdate=" + String.valueOf(LocationStreamGenerator.lastposupdate / 1000) +
                            "&format=json";

                    Log.d(TAG, "link : " + mcog);

//                    StoreToCSV(new Date().getTime(), "attend to send");

                    try {

                        Log.d(TAG, "going to collect transportation");

                        if (MinukuStreamManager.getInstance().getTransportationModeDataRecord() != null) {
                            transportation = MinukuStreamManager.getInstance().getTransportationModeDataRecord().getConfirmedActivityString();

                            Log.d(TAG, "transportation : " + transportation);

                        }
                    } catch (Exception e) {
                        Log.e(TAG, "No TransportationMode, yet.");
                        e.printStackTrace();
                    }

                    StoreToCSV(new Date().getTime(), transportation, countingTimeForIndoorOutdoor);

                    //walking
                    if(transportation.equals("on_foot")) {
                        //TODO set the condition if the user is at outdoor, then trigger notification and reset the Broadcast Receiver.

                        LocationStreamGenerator.setStartIndoorOutdoor(true);

                        //counting the distance how long in one minute = 60 seconds.
                        countingTimeForIndoorOutdoor++;
//                        sharedPrefs.edit().putInt("walkoutdoor_sampled", walkoutdoor_sampled).apply();
                        sharedPrefs.edit().putInt("countingTimeForIndoorOutdoor", countingTimeForIndoorOutdoor).apply();

                        double dist = 0;

                        if(countingTimeForIndoorOutdoor == 6){
                            ArrayList<LatLng> latLngs = LocationStreamGenerator.locForIndoorOutdoor;

                            //.........................because it will iterate two elements at one iteration.
                            for(int index = 0; index < latLngs.size()-1; index++){
                                LatLng latLng = latLngs.get(index);
                                LatLng latLng2 = latLngs.get(index+1);
                                float[] results = new float[1];
                                Location.distanceBetween(latLng.latitude,latLng.longitude,latLng2.latitude,latLng2.longitude, results);
                                dist += results[0];
                            }

                            countingTimeForIndoorOutdoor = 0;
                            sharedPrefs.edit().putInt("countingTimeForIndoorOutdoor", countingTimeForIndoorOutdoor).apply();
                        }

                        //walking survey
                        if(dist >= 50 && dist <= 150){

                            long currentTime = new Date().getTime();

                            walkoutdoor_sampled = sharedPrefs.getInt("walkoutdoor_sampled", 0);

                            //TODO third of the day condition
                            if (walkoutdoor_sampled < 3 && sameTripPrevent == false
                                    && (currentTime - last_Survey_Time) > One_Hour_In_milliseconds
                                    && (currentTime - last_Walking_Survey_Time) > 2 * One_Hour_In_milliseconds) {

                                Boolean check = true;

                                if (currentTime <= first_third) {
                                    if (walk_first_sampled >= 1)
                                        check = false;
                                } else if (currentTime <= second_third && currentTime > first_third) {
                                    if (walk_second_sampled >= 1)
                                        check = false;
                                } else if (currentTime <= third_third && currentTime > second_third) {
                                    if (walk_third_sampled >= 1)
                                        check = false;
                                }

                                //send notification
                                if (check){
                                    //cancel the random survey if it exists
                                    try {
                                        mNotificationManager.cancel(interval_notifyID);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        android.util.Log.e(TAG, "exception", e);
                                    }

                                    if (currentTime <= first_third) {
                                        walk_first_sampled++;
                                        sharedPrefs.edit().putInt("walk_first_sampled", walk_first_sampled).apply();
                                    } else if (currentTime <= second_third && currentTime > first_third) {
                                        walk_second_sampled++;
                                        sharedPrefs.edit().putInt("walk_second_sampled", walk_second_sampled).apply();
                                    } else if (currentTime <= third_third && currentTime > second_third) {
                                        walk_third_sampled++;
                                        sharedPrefs.edit().putInt("walk_third_sampled", walk_third_sampled).apply();
                                    }

                                    walkoutdoor_sampled++;
                                    sharedPrefs.edit().putInt("walkoutdoor_sampled", walkoutdoor_sampled).apply();

                                    //in order to show it in listlinkOhio.java
                                    addToDB();
                                    notiQualtrics();

                                    //update the last survey time
                                    last_Survey_Time = new Date().getTime();
                                    sharedPrefs.edit().putLong("last_Survey_Time", last_Survey_Time).apply();

                                    last_Walking_Survey_Time = new Date().getTime();
                                    sharedPrefs.edit().putLong("last_Walking_Survey_Time", last_Walking_Survey_Time).apply();

                                    //decrease the needed number for the interval_sample_number
                                    interval_sample_number--;
                                    sharedPrefs.edit().putInt("interval_sample_number", interval_sample_number).apply();


                                    //preventing the same on_foot trip will trigger two notifications.
                                    sameTripPrevent = true;
                                    sharedPrefs.edit().putBoolean("sameTripPrevent", sameTripPrevent).apply();
                                }

                            }

                            //reset the Interval Sampleing
                            if (interval_sampled < 3) {
                                settingIntervalSampleing(new Date().getTime());//TODO input time

                            }else{ //cancel all the interval today if the number of the sample are enough.
                                //TODO might not work
                                cancelAlarmAll(mContext);
                            }
                        }

                        //if not on_foot
                    }else{

                        LocationStreamGenerator.setStartIndoorOutdoor(false);
                        LocationStreamGenerator.locForIndoorOutdoor = new ArrayList<LatLng>();

//                        //dismiss the walking survey
//                        mNotificationManager.cancel(qua_notifyID);

                        //cancel the walking survey if it exists
                        //cancel it when the walking is finished.
                        try{
                            mNotificationManager.cancel(qua_notifyID);
                        }catch (Exception e ){
                            e.printStackTrace();
                            android.util.Log.e(TAG, "exception", e);
                        }

                        sameTripPrevent = false;
                        sharedPrefs.edit().putBoolean("sameTripPrevent", sameTripPrevent).apply();

                    }

                    if(interval_sampled >= 3){
                        //TODO might not work
                        cancelAlarmAll(mContext);
                    }
                }

                StoreToCSV(new Date().getTime(), interval_sampled, walkoutdoor_sampled, walk_first_sampled, walk_second_sampled, walk_third_sampled);


                    //deprecated
/*
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            new HttpReadJsonFromUrlAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mcog).get();
                        else
                            new HttpReadJsonFromUrlAsyncTask().execute(mcog).get();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }


                    JSONObject testobject = new JSONObject();
                    testobject.put("userID", Constants.USER_ID);
                    testobject.put("time", curr);
                    testobject.put("inhomeornot :", inhome_test);

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                            new HttpAsyncPostJsonTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                    testUrl,
                                    testobject.toString(),
                                    "Trip",
                                    curr).get();
                        else
                            new HttpAsyncPostJsonTask().execute(
                                    testUrl,
                                    testobject.toString(),
                                    "Trip",
                                    curr).get();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "Json runnable");


                    if (testingserverThenFail) { //TODO check data on sqllite and server is working or not.


                    } else {
                        //TODO only trigger when they are walking.
                        if (Long.valueOf(todayDate) % 2 == 0 && (!transportation.equals("in_vehicle")) && (daily_count_HomeMove + daily_count_HomeNotMove
                                + daily_count_NearHomeMove + daily_count_NearHomeNotMove
                                + daily_count_FarawayMove + daily_count_FarawayNotMove) < 6) //run it at 偶數天
                            triggerQualtrics();

                    }
*/


                //deprecated
//                if (!transportation.equals("NA")) //transportation != null
//                    showTransportationAndIsHome();

            }catch(Exception e){
                e.printStackTrace();

                throw new RuntimeException(e);
            }
        }
    };

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

    //deprecated
/*

    private class HttpReadJsonFromUrlAsyncTask extends AsyncTask<String, Void, String> {

//        @Override
//        protected void onPreExecute(){
//            Log.d(TAG, "onPreExecute");
//            StoreToCSV(new Date().getTime(), "preparetosend");
//        }

        @Override
        protected String doInBackground(String... params) {

            String result = null;
            String url = params[0];
            try {

                JSONObject json = readJsonFromUrl(url);
                Log.d(TAG, json.toString());

                Log.d(TAG, "inhome : " + json.get("inhome") + " distance : " + json.get("distance"));

                home = Integer.valueOf(json.get("inhome").toString());
                if (json.get("distance").toString().contains("E"))
                    dist = 9999;
                else
                    dist = Float.valueOf(json.get("distance").toString());

                Log.d(TAG, "inhome : " + home + " distance : " + dist);

            } catch (JSONException e) {
                Log.d(TAG, "JSONException");
                e.printStackTrace();
            }catch (IOException e) {
                Log.d(TAG, "IOException");
                testingserverThenFail = true;
                e.printStackTrace();
            }

            return result;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "onPostExecute");
            if(testingserverThenFail){
                //TODO add telephony connectivity AR
                StoreToCSV(new Date().getTime(), "Fail");
                inhome_test = "Fail";
            }else {
                String inhomeornot = null;
                if(home==1)
                    inhomeornot = "home";
                else if(home!=1 && dist<=200)
                    inhomeornot = "near home";
                else if(home!=1 && dist>200)
                    inhomeornot = "faraway";

                inhome_test = inhomeornot;

                //TODO add telephony connectivity AR
                StoreToCSV(new Date().getTime(), inhomeornot);
            }
            Log.d(TAG, "get http post result " + result);
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
            Log.d(TAG, "onPostExecute");
            if(testingserverThenFail) {
                //TODO add telephony connectivity AR
                StoreToCSV(new Date().getTime(), inhome_test, "mLab_ConnectToServerOrNot");
            }else {
                StoreToCSV(new Date().getTime(), inhome_test, "mLab_ConnectToServerOrNot");
            }
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
            //TODO if fail, send the new URL for conn.
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

//            System.setProperty("http.keepAlive", "false");

            conn.setReadTimeout(HTTP_TIMEOUT);
            conn.setConnectTimeout(SOCKET_TIMEOUT);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type","application/json");
            //conn.setRequestProperty("connection", "close");

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

            if (conn!=null)
                conn.disconnect();

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
*/

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

//deprecated
/*
    private void triggerQualtrics(){

        Log.e(TAG,"triggerQualtrics");

        int homeorfaraway=0;
        if(home==1)
            homeorfaraway = 1;
        else if(home!=1 && dist<=200)
            homeorfaraway = 2;
        else if(home!=1 && dist>200)
            homeorfaraway = 3;

        if(transportation != null) { //TODO restrict the count of each condition
            determineByContextQualtrics(homeorfaraway, transportation);
        }
    }

    public void determineByContextQualtrics(int homeorfaraway, String transportation){

        Log.e(TAG,"determineByContextQualtrics");

        String notiText = "Context is not existed."; //by default
        nowtime = getCurrentTimeInMillis();
        int hour = getCurrentHour();
        //Log.d(TAG,"nowtime:"+nowtime);

        if(homeorfaraway==1&&!transportation.equals("static")&&(daily_count_HomeMove + daily_count_HomeNotMove) < 2) {
            notiText = "Trigger for home, moving";

            diffTime_HomeMove = (float)((nowtime - lastTimeSend_HomeMove)/(60*60*1000.0));

            if(diffTime_HomeMove > period_HomeMove) {

                Log.d(TAG, notiText);

                if(hour>=10&&hour<=18) {
                    Random rand = new Random();
                    int num = rand.nextInt(100) + 1;
                    if (num >= 1 && num <= probability_normal_HomeMove) {
                        daily_count_HomeMove++;
                        sharedPrefs.edit().putInt("daily_count_HomeMove", daily_count_HomeMove).apply();
                        addToDB(link);
                        notiQualtrics(notiText);
                    }
                }else {
                    Random rand = new Random();
                    int num = rand.nextInt(100) + 1;
                    if (num >= 1 && num <= probability_high_HomeMove) {
                        daily_count_HomeMove++;
                        sharedPrefs.edit().putInt("daily_count_HomeMove", daily_count_HomeMove).apply();
                        addToDB(link);
                        notiQualtrics(notiText);
                    }

                }
                lastTimeSend_HomeMove = getCurrentTimeInMillis();

            }
        } //daily_count_NearHomeMove + daily_count_NearHomeNotMove + daily_count_FarawayMove + daily_count_FarawayNotMove
        else if(homeorfaraway==1&&transportation.equals("static")&&(daily_count_HomeMove + daily_count_HomeNotMove) < 2) {
            notiText = "Trigger for home, not moving";

            diffTime_HomeNotMove = (float)((nowtime - lastTimeSend_HomeNotMove)/(60*60*1000.0));

            if(diffTime_HomeNotMove > period_HomeNotMove) {

                Log.d(TAG, notiText);
                if(hour>=10&&hour<=18) {
                    Random rand = new Random();
                    int num = rand.nextInt(100) + 1;
                    if (num >= 1 && num <= probability_normal_HomeNotMove) {
                        daily_count_HomeNotMove++;
                        sharedPrefs.edit().putInt("daily_count_HomeNotMove", daily_count_HomeNotMove).apply();
                        addToDB(link);
                        notiQualtrics(notiText);
                    }
                }else{
                    Random rand = new Random();
                    int num = rand.nextInt(100) + 1;
                    if (num >= 1 && num <= probability_normal_HomeNotMove) {
                        daily_count_HomeNotMove++;
                        sharedPrefs.edit().putInt("daily_count_HomeNotMove", daily_count_HomeNotMove).apply();
                        addToDB(link);
                        notiQualtrics(notiText);
                    }

                }
                lastTimeSend_HomeNotMove = getCurrentTimeInMillis();

            }
        }
        else if(homeorfaraway==2&&!transportation.equals("static")&&(daily_count_NearHomeMove + daily_count_NearHomeNotMove) < 2) {
            notiText = "Trigger for near home, moving";

            diffTime_NearHomeMove = (float)((nowtime - lastTimeSend_NearHomeMove)/(60*60*1000.0));

            if(diffTime_NearHomeMove > period_NearHomeMove) {

                Log.d(TAG, notiText);

                if(hour>=10&&hour<=18) {
                    Random rand = new Random();
                    int num = rand.nextInt(100) + 1;
                    if (num >= 1 && num <= probability_high_NearHomeMove) {
                        daily_count_NearHomeMove++;
                        sharedPrefs.edit().putInt("daily_count_NearHomeMove", daily_count_NearHomeMove).apply();
                        addToDB(link);
                        notiQualtrics(notiText);
                    }
                }else{
                    Random rand = new Random();
                    int num = rand.nextInt(100) + 1;
                    if (num >= 1 && num <= probability_normal_NearHomeMove) {
                        daily_count_NearHomeMove++;
                        sharedPrefs.edit().putInt("daily_count_NearHomeMove", daily_count_NearHomeMove).apply();
                        addToDB(link);
                        notiQualtrics(notiText);
                    }
                }
                lastTimeSend_NearHomeMove = getCurrentTimeInMillis();

            }
        }
        else if(homeorfaraway==2&&transportation.equals("static")&&(daily_count_NearHomeMove + daily_count_NearHomeNotMove) < 2) {
            notiText = "Trigger for near home, not moving";

            diffTime_NearHomeNotMove = (float)((nowtime - lastTimeSend_NearHomeNotMove)/(60*60*1000.0));

            if(diffTime_NearHomeNotMove > period_NearHomeNotMove) {

                Log.d(TAG, notiText);

                if(hour>=10&&hour<=18) {
                    Random rand = new Random();
                    int num = rand.nextInt(100) + 1;
                    if (num >= 1 && num <= probability_high_NearHomeNotMove) {
                        daily_count_NearHomeNotMove++;
                        sharedPrefs.edit().putInt("daily_count_NearHomeNotMove", daily_count_NearHomeNotMove).apply();
                        addToDB(link);
                        notiQualtrics(notiText);
                    }
                }else{
                    Random rand = new Random();
                    int num = rand.nextInt(100) + 1;
                    if (num >= 1 && num <= probability_normal_NearHomeNotMove) {
                        daily_count_NearHomeNotMove++;
                        sharedPrefs.edit().putInt("daily_count_NearHomeNotMove", daily_count_NearHomeNotMove).apply();
                        addToDB(link);
                        notiQualtrics(notiText);
                    }
                }
                lastTimeSend_NearHomeNotMove = getCurrentTimeInMillis();

            }
        }
        else if(homeorfaraway==3&&!transportation.equals("static")&&(daily_count_FarawayMove + daily_count_FarawayNotMove) < 2) {
            notiText = "Trigger for faraway, moving";

            diffTime_FarawayMove = (float)((nowtime - lastTimeSend_FarawayMove)/(60*60*1000.0));

            if(diffTime_FarawayMove > period_FarawayMove) {

                Log.d(TAG, notiText);

                Random rand = new Random();
                int num = rand.nextInt(100) + 1;
                if (num >= 1 && num <= probability_normal_FarawayMove) {
                    daily_count_FarawayMove++;
                    sharedPrefs.edit().putInt("daily_count_FarawayMove", daily_count_FarawayMove).apply();
                    addToDB(link);
                    notiQualtrics(notiText);
                }
                lastTimeSend_FarawayMove = getCurrentTimeInMillis();

            }
        }
        else if(homeorfaraway==3&&transportation.equals("static")&&(daily_count_FarawayMove + daily_count_FarawayNotMove) < 2) {
            notiText = "Trigger for faraway, not moving";

            diffTime_FarawayNotMove = (float)((nowtime - lastTimeSend_FarawayNotMove)/(60*60*1000.0));

            if(diffTime_FarawayNotMove > period_FarawayNotMove) {

                Log.d(TAG, notiText);

                Random rand = new Random();
                int num = rand.nextInt(100) + 1;
                if (num >= 1 && num <= probability_normal_FarawayNotMove) {
                    daily_count_FarawayNotMove++;
                    sharedPrefs.edit().putInt("daily_count_FarawayNotMove", daily_count_FarawayNotMove).apply();
                    addToDB(link);
                    notiQualtrics(notiText);
                }
                lastTimeSend_FarawayNotMove = getCurrentTimeInMillis();

            }

        }

        //Log.d(TAG,notiText);



        // need to roll
        // notiQualtrics(notiText);

    }
*/

    //add to DB in order to display it in the linklistOhio.java
    public void addToDB(){
        Log.d(TAG, "addToDB");

        dailyResponseNum++;
        sharedPrefs.edit().putInt("dailyResponseNum", dailyResponseNum).apply();

        /*ConnectivityDataRecord connectivityDataRecord = ConnectivityStreamGenerator.toOtherconnectDataRecord;

        String mob = "";
        if(connectivityDataRecord.getIsMobileConnected()){
            mob = "1";
        }else {
            mob = "0";
        }*/

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
            values.put(DBHelper.clickornot_col, 0); //they can't enter the link by the notification.

//            db.insert(DBHelper.checkFamiliarOrNotLinkList_table, null, values);
            db.insert(DBHelper.surveyLinkList_table, null, values);

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

        String notiText = "You have a new walking survey(Walking)"+"\r\n"
                +"Walking : "+walkoutdoor_sampled+" ; Random : "+interval_sampled;

        Intent resultIntent = new Intent(CheckFamiliarOrNotService.this, linkListohio.class);
        PendingIntent pending = PendingIntent.getActivity(CheckFamiliarOrNotService.this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

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
        //note.flags |= Notification.FLAG_NO_CLEAR;
        //startForeground( 42, note );

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
//        Intent resultIntent = new Intent(CheckFamiliarOrNotService.this, linkListohio.class);
//        PendingIntent pending = PendingIntent.getActivity(CheckFamiliarOrNotService.this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

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

//        SharedPreferences sharedPrefs = getSharedPreferences("edu.umich.si.inteco.minuku_2", MODE_PRIVATE);
        String startSleepingTime = sharedPrefs.getString("SleepingStartTime",null);
        String endSleepingTime = sharedPrefs.getString("SleepingEndTime",null);

        if(startSleepingTime!=null && endSleepingTime!=null) {

            String[] startSleepingTimeArray = startSleepingTime.split(":");
            String[] endSleepingTimeArray = endSleepingTime.split(":");

            Log.d(TAG, "startSleepingTime : " + startSleepingTime + " endSleepingTime : " + endSleepingTime);

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

    public void StoreToCSV(long timestamp, String successOrNot, String filename){

        Log.d(TAG,"StoreToCSV");

        String sFileName = filename+".csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"timestamp","timeString","Latitude","Longitude","Accuracy"});
            String timeString = getTimeString(timestamp);

            TelephonyDataRecord telephonyDataRecord = TelephonyStreamGenerator.toOtherTelephonyDataRecord;
            ConnectivityDataRecord connectivityDataRecord = ConnectivityStreamGenerator.toOtherconnectDataRecord;

            //TODO
            data.add(new String[]{String.valueOf(timestamp), timeString, successOrNot, transportation,
                    "",
                    telephonyDataRecord.getNetworkOperatorName(),
                    telephonyDataRecord.getCallState(),
                    String.valueOf(telephonyDataRecord.getPhoneSignalType()),
                    String.valueOf(telephonyDataRecord.getGsmSignalStrength()),
                    String.valueOf(telephonyDataRecord.getLTESignalStrength_dbm()),
                    String.valueOf(telephonyDataRecord.getCdmaSignalStrenthLevel()),
                    "",
                    connectivityDataRecord.getNetworkType(),
                    String.valueOf(connectivityDataRecord.getIsNetworkAvailable()),
                    String.valueOf(connectivityDataRecord.getIsConnected()),
                    String.valueOf(connectivityDataRecord.getIsWifiAvailable()),
                    String.valueOf(connectivityDataRecord.getIsMobileAvailable()),
                    String.valueOf(connectivityDataRecord.getIsWifiConnected()),
                    String.valueOf(connectivityDataRecord.getIsMobileConnected()),
                    "",
                    String.valueOf(ActivityRecognitionService.toOthermProbableActivities),
                    String.valueOf(ActivityRecognitionService.toOthermMostProbableActivity),
            });

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public void StoreToCSV(long timestamp, String successOrNot, int countingTimeForIndoorOutdoor){

        Log.d(TAG,"StoreToCSV");

        String sFileName = "checkFamiliarTransportation.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"timestamp","timeString","Latitude","Longitude","Accuracy"});
            String timeString = getTimeString(timestamp);

            TelephonyDataRecord telephonyDataRecord = TelephonyStreamGenerator.toOtherTelephonyDataRecord;
            ConnectivityDataRecord connectivityDataRecord = ConnectivityStreamGenerator.toOtherconnectDataRecord;

            data.add(new String[]{String.valueOf(timestamp), timeString, successOrNot, transportation,
                    "",
                    telephonyDataRecord.getNetworkOperatorName(),
                    telephonyDataRecord.getCallState(),
                    String.valueOf(telephonyDataRecord.getPhoneSignalType()),
                    String.valueOf(telephonyDataRecord.getGsmSignalStrength()),
                    String.valueOf(telephonyDataRecord.getLTESignalStrength_dbm()),
                    String.valueOf(telephonyDataRecord.getCdmaSignalStrenthLevel()),
                    "",
                    connectivityDataRecord.getNetworkType(),
                    String.valueOf(connectivityDataRecord.getIsNetworkAvailable()),
                    String.valueOf(connectivityDataRecord.getIsConnected()),
                    String.valueOf(connectivityDataRecord.getIsWifiAvailable()),
                    String.valueOf(connectivityDataRecord.getIsMobileAvailable()),
                    String.valueOf(connectivityDataRecord.getIsWifiConnected()),
                    String.valueOf(connectivityDataRecord.getIsMobileConnected()),
                    "",
                    String.valueOf(ActivityRecognitionService.toOthermProbableActivities),
                    String.valueOf(ActivityRecognitionService.toOthermMostProbableActivity),
            });

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
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

    public void StoreToCSV(long timestamp, double latitude, double longitude, float accuracy, String userid){

        Log.d(TAG,"StoreToCSV");

        String sFileName = "LocationSentToServer.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"timestamp","timeString","Latitude","Longitude","Accuracy"});
            String timeString = getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp),timeString,String.valueOf(latitude),String.valueOf(longitude),String.valueOf(accuracy), userid});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /*@Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        Log.d(TAG, "TASK REMOVED");

//        startService();

        isServiceRunning = false;

        PendingIntent service = PendingIntent.getService(
                getApplicationContext(),
                1001,
                new Intent(getApplicationContext(), CheckFamiliarOrNotService.class),
                PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, service);
    }*/

    public void createCSV(){
        String sFileName = "LocationSentToServer.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            data.add(new String[]{"timestamp","timeString","Latitude","Longitude","Accuracy","UserId"});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void storeToCSVinterval(String todayDate){

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
//                addToCSVFile[index] = getTimeString(interval_sampled_times.get(index));
            }

            addToCSVFile = addToCSV.toArray(addToCSVFile);

            data.add(addToCSVFile);

            /*data.add(new String[]{
                     getTimeString(interval_sampled_times.get(0))
                    ,getTimeString(interval_sampled_times.get(1))
                    ,getTimeString(interval_sampled_times.get(2))
                    ,getTimeString(interval_sampled_times.get(3))
                    ,getTimeString(interval_sampled_times.get(4))
                    ,getTimeString(interval_sampled_times.get(5))
            });*/

            /*data.add(new String[]{"HomeMove",String.valueOf(daily_count_HomeMove)});
            data.add(new String[]{"HomeNotMove",String.valueOf(daily_count_HomeNotMove)});
            data.add(new String[]{"NearHomeMove",String.valueOf(daily_count_NearHomeMove)});
            data.add(new String[]{"NearHomeNotMove",String.valueOf(daily_count_NearHomeNotMove)});
            data.add(new String[]{"FarawayMove",String.valueOf(daily_count_FarawayMove)});
            data.add(new String[]{"FarawayNotMove",String.valueOf(daily_count_FarawayNotMove)});*/

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

    private void storeToCSV(String todayDate){

        String sFileName = "LogAt_"+todayDate+".csv";

        sFileName = sFileName.replace("/","-");

        Log.d(TAG, "sFileName : " + sFileName);

        try {
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);

            Log.d(TAG, "root : " + root);

            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"","HomeMove","HomeNotMove","NearHomeMove","NearHomeNotMove","FarawayMove","FarawayNotMove"});

            data.add(new String[]{String.valueOf(last_hour)+":00"
                    ,String.valueOf(daily_count_HomeMove)
                    ,String.valueOf(daily_count_HomeNotMove)
                    ,String.valueOf(daily_count_NearHomeMove)
                    ,String.valueOf(daily_count_NearHomeNotMove)
                    ,String.valueOf(daily_count_FarawayMove)
                    ,String.valueOf(daily_count_FarawayNotMove)
                });

            /*data.add(new String[]{"HomeMove",String.valueOf(daily_count_HomeMove)});
            data.add(new String[]{"HomeNotMove",String.valueOf(daily_count_HomeNotMove)});
            data.add(new String[]{"NearHomeMove",String.valueOf(daily_count_NearHomeMove)});
            data.add(new String[]{"NearHomeNotMove",String.valueOf(daily_count_NearHomeNotMove)});
            data.add(new String[]{"FarawayMove",String.valueOf(daily_count_FarawayMove)});
            data.add(new String[]{"FarawayNotMove",String.valueOf(daily_count_FarawayNotMove)});*/

            csv_writer.writeAll(data);

            csv_writer.close();

        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    public int generatePendingIntentRequestCode(long time){

        int code = 0;

        if (time-time_base > 1000000000){
            time_base = getCurrentTimeInMillis();
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

    private  boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public long getCurrentTimeInMillis(){
        //get timzone
        TimeZone tz = TimeZone.getDefault();
        java.util.Calendar cal = java.util.Calendar.getInstance(tz);
        long t = cal.getTimeInMillis();
        return t;
    }

    private void intervalQualtrics(){

        String notiText = "You have a new random survey(Random)"+"\r\n"
                +"Walking : "+walkoutdoor_sampled+" ;Random : "+interval_sampled;

        Log.d(TAG,"intervalQualtrics");

        Intent resultIntent = new Intent(CheckFamiliarOrNotService.this, linkListohio.class);
        PendingIntent pending = PendingIntent.getActivity(CheckFamiliarOrNotService.this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

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
        Log.d(TAG, "addToDB");

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
            values.put(DBHelper.clickornot_col, 0); //they can't enter the link by the notification.

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

                long currentTime = new Date().getTime();

                if(interval_sampled < 3 && (currentTime - last_Survey_Time) > One_Hour_In_milliseconds) {
                    //execute the IntervalSample action
                    interval_sampled++;
                    sharedPrefs.edit().putInt("interval_sampled", interval_sampled).apply();

//                    addToDB_interval(link);

                    addToDB();

                    //cancel the walking survey if it exists
                    try{
                        mNotificationManager.cancel(qua_notifyID);
                    }catch (Exception e ){
                        e.printStackTrace();
                        android.util.Log.e(TAG, "exception", e);
                    }

                    intervalQualtrics();

                    //update the last survey time
                    last_Survey_Time = new Date().getTime();
                    sharedPrefs.edit().putLong("last_Survey_Time", last_Survey_Time).apply();

                    //decrease the needed number for the interval_sample_number
                    interval_sample_number--;
                    sharedPrefs.edit().putInt("interval_sample_number", interval_sample_number).apply();

                    //reset the Interval Sampleing
                    if (interval_sampled < 3) {
                        settingIntervalSampleing(new Date().getTime());//TODO input time

                    }else{ //cancel all the interval today if the number of the sample are enough.
                        //TODO might not work
                        cancelAlarmAll(mContext);
                    }
                }

            }

        }
    };

}
