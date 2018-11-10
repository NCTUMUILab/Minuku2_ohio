package edu.ohio.minuku_2.manager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Data.DataHandler;
import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.RandomNumber;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.MinukuNotificationManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku.streamgenerator.TransportationModeStreamGenerator;
import edu.ohio.minuku_2.Utils;
import edu.ohio.minuku_2.controller.SurveyActivity;

/**
 * Created by Lawrence on 2018/3/15.
 */

public class SurveyTriggerManager {

    private static final String TAG = "SurveyTriggerManager";

    private static SurveyTriggerManager instance;
    private static Context mContext;

    private ScheduledExecutorService mScheduledExecutorService;

    private int alarmCheckCount = 0;

    public static final int REFRESH_FREQUENCY = 20; //20s, 10000ms
    public static final int BACKGROUND_RECORDING_INITIAL_DELAY = 0;

    private static String today;
    private String lastTimeSend_today;

    private int walk_notifyID = 2;
    private int interval_notifyID = 3;

    public static ArrayList<Long> interval_sampled_times;
    public static List<String> interval_sampled_times_bound;

    private static int interval_sample_number = 6;

    private static int interval_sampled;
    private int walkoutdoor_sampled;

    private boolean sameTripPrevent;

    private long last_Survey_Time;
    private long last_Walking_Survey_Time;

    public static long time_base = 0;

    private boolean alarmregistered;

    private int dailySurveyNum, dailyResponseNum;

    private final String LINK_NCTU = "https://nctucommunication.qualtrics.com/jfe/form/SV_aWcHPkmdagnfgDb";
    private final String LINK_OHIO = "https://osu.az1.qualtrics.com/jfe/form/SV_6xjrFJF4YwQwuMZ";
    private final String LINK = LINK_OHIO;

    private final String SURVEY_TEXT = "You have a new survey available now.";

    private final int dist_Lowerbound_Outdoorwalking = 50;
    private final int dist_Upperbound_Outdoorwalking = 150;

    private NotificationManager mNotificationManager;

    private String noti_random = "random";
    private String noti_walk = "walk";

    private static SharedPreferences sharedPrefs;


    public SurveyTriggerManager(Context context){

        instance = this;

        mContext = context;

        sharedPrefs = mContext.getSharedPreferences(Constants.sharedPrefString, mContext.MODE_PRIVATE);

        lastTimeSend_today = sharedPrefs.getString("lastTimeSend_today","NA");

        interval_sampled = sharedPrefs.getInt("interval_sampled", 0);

        alarmregistered = sharedPrefs.getBoolean("alarmregistered",false);

        sameTripPrevent = sharedPrefs.getBoolean("sameTripPrevent", false);

        time_base = ScheduleAndSampleManager.getCurrentTimeInMillis();

        mScheduledExecutorService = Executors.newScheduledThreadPool(REFRESH_FREQUENCY);


        registerActionAlarmReceiver();

        CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "Going to reset IntervalSamples.");

        //we set them to avoid the situation that alarm being canceled by Android.
        resetIntervalSampling();

        CSVHelper.storeToCSV(CSVHelper.CSV_RUNNABLE_CHECK, "Reset IntervalSamples, complete.");

        initialize();
    }

    public static SurveyTriggerManager getInstance(Context context) {

        if(SurveyTriggerManager.instance == null) {
            try {
                SurveyTriggerManager.instance = new SurveyTriggerManager(context);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        return SurveyTriggerManager.instance;
    }

    private void initialize() {

        lastTimeSend_today = sharedPrefs.getString("lastTimeSend_today","NA");
        interval_sampled = sharedPrefs.getInt("interval_sampled", 0);
        alarmregistered = sharedPrefs.getBoolean("alarmregistered",false);

        Config.USER_ID = sharedPrefs.getString("userid","NA");
        Config.downloadedDayInSurvey = sharedPrefs.getInt("downloadedDayInSurvey", Config.downloadedDayInSurvey);

        runMainThread();
    }

    public void runMainThread(){

        mScheduledExecutorService.scheduleAtFixedRate(
                RunningForGivingSurveyOrNot,
                BACKGROUND_RECORDING_INITIAL_DELAY,
                REFRESH_FREQUENCY,
                TimeUnit.SECONDS);
    }

    private static long getSpecialTimeInMillis(String givenDateFormat){

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

    private void resetIntervalSampling(){

        //cancel the times which are set.
        cancelAlarmAll(mContext);

        int alarmCount = sharedPrefs.getInt("alarmCount", 1);

        for (int eachAlarm = 1; eachAlarm <= alarmCount; eachAlarm++) {

            long time = sharedPrefs.getLong("alarm_time_" + eachAlarm,0);
            int requestCode = sharedPrefs.getInt("alarm_time_request_code" + eachAlarm, -1);

            //if the time is overtime, skip it.
            if(ScheduleAndSampleManager.getCurrentTimeInMillis() > time)
                continue;

            Log.d(TAG, "[test alarm] reset time back : "+ScheduleAndSampleManager.getTimeString(time));

            CSVHelper.storeToCSV(CSVHelper.CSV_RESET_INTERVALSAMPLES_CHECK, ScheduleAndSampleManager.getTimeString(time));

//            int request_code = generatePendingIntentRequestCode(time);

            //create an alarm for a time
            Intent intent = new Intent(Constants.INTERVAL_SAMPLE);
            PendingIntent pi = PendingIntent.getBroadcast(mContext, requestCode, intent, 0);

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(mContext.ALARM_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
            }else{

                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);
            }
        }

        CSVHelper.storeToCSV(CSVHelper.CSV_RESET_INTERVALSAMPLES_CHECK, "-------------Divider------------");

    }

    public static void settingIntervalSampling(int daysInSurvey, Context context){

        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);

        Date curDate = new Date(System.currentTimeMillis() + daysInSurvey * Constants.MILLISECONDS_PER_DAY);
        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY_Slash);
        String yMdformat = df.format(curDate);

        Date tomorDate = new Date(System.currentTimeMillis() + Constants.MILLISECONDS_PER_DAY + daysInSurvey * Constants.MILLISECONDS_PER_DAY);
        String tomorformat = df.format(tomorDate);


        interval_sampled_times = new ArrayList<>();
        interval_sampled_times_bound = new ArrayList<>();
        ArrayList<Long> times = new ArrayList<>();
        ArrayList<String> time_bounds = new ArrayList<>();

        int sample_period;

        String sleepingstartTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER);
        String sleepingendTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER);


        boolean wakeSleepDateIsSame = sharedPrefs.getBoolean("WakeSleepDateIsSame", false);

        //if the time havn't been set, don't set the survey time with default.
        if(sleepingstartTime.equals(Constants.NOT_A_NUMBER) || sleepingendTime.equals(Constants.NOT_A_NUMBER)){

            return;
        }

        long surveyEndTime = -999;
        long surveyStartTime = getSpecialTimeInMillis(yMdformat+" "+sleepingendTime+":00");

        Log.d(TAG, "[test alarm] surveyStartTime : " + yMdformat+" "+sleepingendTime+":00");

        Log.d(TAG, "[test alarm] wakeSleepDateIsSame : "+wakeSleepDateIsSame);

        //if the bed time date is same as the start time implies that the survey time would cross the midnight
        if(wakeSleepDateIsSame) {

            surveyEndTime = getSpecialTimeInMillis(tomorformat+" "+"00:00:00");
            Log.d(TAG, "[test alarm] sleepingstartTime : " + tomorformat + " " + "00:00:00");
        }else{

            surveyEndTime = getSpecialTimeInMillis(yMdformat+" "+sleepingstartTime+":00");
            Log.d(TAG, "[test alarm] sleepingstartTime : " + yMdformat + " " + sleepingstartTime + ":00");
        }

        long sub_startTime = surveyStartTime;
        long sub_endTime  = surveyEndTime;
//        long min_interval = (surveyEndTime - surveyStartTime)/(interval_sample_number + interval_sample_threshold); //90 * 60 * 1000  (e-s)/10

        long min_interval = (surveyEndTime - surveyStartTime)/interval_sample_number;

        //1. first get the entire sampling period

        while(true) {

            //repeatedly find the subsampling period
            for (int i = 0; i < interval_sample_number; i++) {

                //Log.d(TAG, "interval_sample_number : "+i);

                //2. divide the period by the number of sample
//                sample_period = (int) (sub_endTime - sub_startTime);

                //sample the period from the start time to before 30 mins of the end time
                sample_period = (int) (min_interval - Constants.MILLISECONDS_PER_HOUR);

                try {

                    //Log.d(TAG, "sample_period : "+sample_period);
                    //Log.d(TAG, "interval_sample_number : "+interval_sample_number);
                    //Log.d(TAG, "i : "+i);

//                    int sub_sample_period = sample_period / (interval_sample_number - i);
                    int sub_sample_period = sample_period;

                    //Log.d(TAG, "sub_sample_period : "+sub_sample_period);

                    //3. random within the sub sample period
//                    long time = random.nextInt(sub_sample_period) + sub_startTime;
                    long time = new RandomNumber().random(sub_sample_period) + sub_startTime;
                    Log.d(TAG, "[test alarm] sub_sample_period random number : "+(time - sub_startTime));

                    //Log.d(TAG, "testRandom semi sampling: the " + i + " sampling period is from " + getTimeString(sub_startTime) + " to " + getTimeString(sub_endTime) +
//                        " divided by " + (interval_sample_number-i) + " each period is " + (sub_sample_period/60/1000) + " minutes long, " + " the sampled time within the period is " +
//                        getTimeString(time) );

                    //Log.d(TAG, "testRandom  the sampled time within the period is " + getTimeString(time) );

                    //4. save the sampled time
                    times.add(time);

                    //5. the next startime is the previous sample time + min interval. We do this to ensure that the next sampled time is
                    //not too close to the previous sampled time.
//                    sub_startTime = time + min_interval;
                    sub_startTime += min_interval;

                    time_bounds.add(ScheduleAndSampleManager.getTimeString(sub_startTime));

                    //Log.d(TAG, "testRandom semi sampling: the new start time is " + getTimeString(sub_startTime));

                    //6. if the next start time is later than the overall end time, stop the sampling.
                    if (sub_startTime >= sub_endTime)
                        break;

                } catch (Exception e) {
                    //e.printStackTrace();
                    //Log.e(TAG, "exception", e);
                }

            }

            boolean havingAnHourInterval = true;

            //check there are an hour interval between them
            for(int time_index = 1; time_index < times.size() ;time_index++){

                long firstTime = times.get(time_index-1);
                long secondTime = times.get(time_index);

                Log.d(TAG, "[test alarm] time_index : " + time_index);
                Log.d(TAG, "[test alarm] firstTime : "+ ScheduleAndSampleManager.getTimeString(firstTime));
                Log.d(TAG, "[test alarm] secondTime : "+ ScheduleAndSampleManager.getTimeString(secondTime));
                Log.d(TAG, "[test alarm] secondTime - firstTime : "+ (secondTime - firstTime));
                Log.d(TAG, "[test alarm] MILLISECONDS_PER_HOUR : " + Constants.MILLISECONDS_PER_HOUR);

                Log.d(TAG, "[test alarm] is over an hour ? " +((secondTime - firstTime) < Constants.MILLISECONDS_PER_HOUR));

                if((secondTime - firstTime) < Constants.MILLISECONDS_PER_HOUR){

                    havingAnHourInterval = false;
                    break;
                }
            }

            if(havingAnHourInterval){
                break;
            }

        }

        //setting them to the alarmManager.
        if (times!=null) {

            interval_sampled_times = times;
            interval_sampled_times_bound = time_bounds;
            for (int i = 0; i < times.size(); i++) {

                long time = times.get(i);
                int request_code = generatePendingIntentRequestCode(time);
                //create an alarm for a time
                Intent intent = new Intent(Constants.INTERVAL_SAMPLE);

                PendingIntent pi = PendingIntent.getBroadcast(context, request_code, intent, 0);

                //for the reset one
                AlarmManager alarmManager = (AlarmManager)context.getSystemService( context.ALARM_SERVICE );

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
                }else{

                    alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);
                }

                //update the latest request_code to be the last request_code.
                sharedPrefs.edit().putInt("last_request_code_"+i, request_code).apply();
                sharedPrefs.edit().putString("sampled_times_"+i, ScheduleAndSampleManager.getTimeString(time)).apply();

                //daysInSurvey
                int alarmCount = sharedPrefs.getInt("alarmCount", 0);
                alarmCount = alarmCount + 1;
                sharedPrefs.edit().putInt("alarmCount", alarmCount).apply();
                sharedPrefs.edit().putLong("alarm_time_"+alarmCount, time).apply();
                Log.d(TAG, "[test alarm] Alarm time "+alarmCount+" : " + ScheduleAndSampleManager.getTimeString(time));

                sharedPrefs.edit().putInt("alarm_time_request_code"+alarmCount, request_code).apply();
            }
        }
    }

    private void checkSurveyLinkOvertime(){

        try {

            String latestSurveyLinkData = DBHelper.queryLatestSurveyLink();
            String latestSurveyLink_Time = latestSurveyLinkData.split(Constants.DELIMITER)[DBHelper.COL_INDEX_GENERATE_TIME];

            DBHelper.updateOverTimeSurvey(Long.valueOf(latestSurveyLink_Time));
        }catch (Exception e){
            Log.e(TAG, "exception", e);
        }
    }

    private void confirmAlarmExist(){

        int alarmTotal = sharedPrefs.getInt("alarmCount", 0);

        long currentTimeInMillis = ScheduleAndSampleManager.getCurrentTimeInMillis();

        //find the next alarm time
        int alarmNext = -1;

        //find the day start to cancel the alarm
        for(int alarmCount = 1; alarmCount <= alarmTotal; alarmCount ++) {

            long time = sharedPrefs.getLong("alarm_time_" + alarmCount, -1);

            if(time > currentTimeInMillis){

                alarmNext = alarmCount;
                break;
            }
        }

        for(int alarmCount = alarmNext; alarmCount <= alarmTotal; alarmCount ++) {

            int request_code = sharedPrefs.getInt("alarm_time_request_code" + alarmCount, -1);

            long time = sharedPrefs.getLong("alarm_time_" + alarmCount, -1);

//            Log.d(TAG, "[test alarm] deleting time : "+ScheduleAndSampleManager.getTimeString(time));

            boolean alarmUp = (PendingIntent.getBroadcast(mContext, request_code,
                    new Intent(Constants.INTERVAL_SAMPLE),
                    PendingIntent.FLAG_NO_CREATE) != null);

            if (alarmUp) {

//                Log.d(TAG, "[recalling alarms] Alarm is already active");
            }else{

//                Log.d(TAG, "[recalling alarms] Alarm is canceled! Recall it back");

                //recall the one which is gone, recall it back.
                Intent intent = new Intent(Constants.INTERVAL_SAMPLE);
                PendingIntent pi = PendingIntent.getBroadcast(mContext, request_code, intent, 0);

                AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(mContext.ALARM_SERVICE);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
                }else{

                    alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);
                }

                break;
            }
        }
    }

    private void checkAlarmAndSurveylink(){

        //30: 10 mins, 60: 20 mins
        if(alarmCheckCount == 60) {

            Log.d(TAG, "confirmAlarmExist");

            confirmAlarmExist();
            alarmCheckCount = 0;

            Log.d(TAG, "checking if there is a LINK unavailable and missed");

            checkSurveyLinkOvertime();

        }else {

            alarmCheckCount++;
        }
    }

    Runnable RunningForGivingSurveyOrNot = new Runnable() {
        @Override
        public void run() {

            checkAlarmAndSurveylink();

            //variable for LINK
            Config.USER_ID = sharedPrefs.getString("userid", Constants.NOT_A_NUMBER);
            Config.GROUP_NUM = sharedPrefs.getString("groupNum", Constants.NOT_A_NUMBER);
            Config.Email = sharedPrefs.getString("Email", Constants.NOT_A_NUMBER);

            dailySurveyNum = sharedPrefs.getInt("daysInSurvey", Config.daysInSurvey);
            dailyResponseNum = sharedPrefs.getInt("dailyResponseNum", 0);

            Date curDate = new Date(System.currentTimeMillis());
            SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY_Slash);
            today = df.format(curDate);

            //get sleep time and get up time from sharedpreference
            String startSleepingTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER);
            String endSleepingTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER);

//            Log.d(TAG,"startSleepingTime : "+ ScheduleAndSampleManager.getTimeString(Long.valueOf(startSleepingTime)) + " endSleepingTime : "+ ScheduleAndSampleManager.getTimeString(Long.valueOf(endSleepingTime)));

            if(!startSleepingTime.equals(Constants.NOT_A_NUMBER) && !endSleepingTime.equals(Constants.NOT_A_NUMBER)) {

                //after a day, switch the survey day after the bed time
                long sleepStartTimeLong = sharedPrefs.getLong("sleepStartTimeLong", Constants.INVALID_IN_LONG);
                Log.d(TAG, "sleepStartTimeLong String : "+ ScheduleAndSampleManager.getTimeString(sleepStartTimeLong));

                long nextSleepTime = sharedPrefs.getLong("nextSleepTime", sleepStartTimeLong + Constants.MILLISECONDS_PER_DAY);
                Log.d(TAG, "nextSleepTime String : "+ ScheduleAndSampleManager.getTimeString(nextSleepTime));

                Log.d(TAG, "[test sleep time issue] is over next Sleep Time ? "+(ScheduleAndSampleManager.getCurrentTimeInMillis() >= nextSleepTime));
                Log.d(TAG, "[test sleep time issue] is lastTimeSend_today NA ? "+(lastTimeSend_today.equals(Constants.NOT_A_NUMBER)));

                //is over a day
//                if (ScheduleAndSampleManager.getCurrentTimeInMillis() >= nextSleepTime || lastTimeSend_today.equals(Constants.NOT_A_NUMBER)) {
                if(!lastTimeSend_today.equals(today)){

                    Log.d(TAG, "[test sleep time issue] over a day !!");

                    updateLastSurvey();

                    //-1 is for the survey day got from the server should be set to 0
                    if(Config.daysInSurvey == -1) {

                        Config.daysInSurvey = 0;
                    }else {

                        //prevent the situation that they download at the day which is not the first day of the research.
                        //which means they get the daysInSurvey is x|x>0 instead of -1, just keep it.
                        if(!lastTimeSend_today.equals(Constants.NOT_A_NUMBER)){

                            Config.daysInSurvey++;
                        }
                    }

                    Log.d(TAG, "[test sleep time issue] daysInSurvey : "+ Config.daysInSurvey);

                    sharedPrefs.edit().putInt("daysInSurvey", Config.daysInSurvey).apply();

                    //make sure that the survey day is corresponding to the date
                    SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
                    String surveyDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis(), sdf_date);

                    DBHelper.insertSurveyDayWithDateTable(Config.daysInSurvey, surveyDate);

                    Log.d(TAG, "after a day daysInSurvey today : "+ today);
                    Log.d(TAG, "after a day daysInSurvey lastTimeSend_today : "+ lastTimeSend_today);
                    Log.d(TAG, "after a day daysInSurvey : "+ Config.daysInSurvey);

                    sharedPrefs.edit().putString("mobileMissedCount", "").apply();
                    sharedPrefs.edit().putString("randomMissedCount", "").apply();
                    sharedPrefs.edit().putString("OpenCount", "").apply();

                    walkoutdoor_sampled = 0;
                    sharedPrefs.edit().putInt("walkoutdoor_sampled", walkoutdoor_sampled).apply();

                    //set period to default
                    for(int p=1 ; p<=6 ; p++) {
                        sharedPrefs.edit().putBoolean("Period" + p, true).apply();
                    }

                    //set period if mobile triggered
                    for(int p=1 ; p<=6 ; p++) {
                        sharedPrefs.edit().putBoolean("MobilePeriod" + p, true).apply();
                    }

                    interval_sampled = 0;
                    sharedPrefs.edit().putInt("interval_sampled", interval_sampled).apply();

                    last_Survey_Time = Constants.INVALID_IN_LONG;
                    sharedPrefs.edit().putLong("last_Survey_Time", last_Survey_Time).apply();

                    last_Walking_Survey_Time = Constants.INVALID_IN_LONG;
                    sharedPrefs.edit().putLong("last_Walking_Survey_Time", last_Walking_Survey_Time).apply();

                    if (lastTimeSend_today.equals(Constants.NOT_A_NUMBER)) {
                        //setting up the parameter for the survey LINK
                        setUpSurveyLink();
                    }else{

                        sharedPrefs.edit().putLong("nextSleepTime", nextSleepTime + Constants.MILLISECONDS_PER_DAY).apply();
                    }

                    lastTimeSend_today = today;
                    sharedPrefs.edit().putString("lastTimeSend_today", lastTimeSend_today).apply();
                }

                checkIsWalkingSurvey(startSleepingTime, endSleepingTime);
            }
        }
    };

    private void checkIsWalkingSurvey(String startSleepingTime, String endSleepingTime){

        boolean isSleepingTime = InSleepingTime(startSleepingTime, endSleepingTime);

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "[test mobile triggering] is sleeping time ? "+ !isSleepingTime);

        //temporarily remove the trigger limit
        if (!isSleepingTime) {

            //Log.d(TAG, "it's not sleeping time");

            /** see if the user is walking in the last minute **/
            long now = ScheduleAndSampleManager.getCurrentTimeInMillis();
            long lastMinute = now - Constants.MILLISECONDS_PER_MINUTE;

            //1. we get walking session
            //try : prevent the situation that the session hasn't created
            try {

                int sessionId = SessionManager.getOngoingSessionIdList().get(0);
                Session session = SessionManager.getSession(sessionId);

                //Log.d(TAG, "sessionId : " + sessionId);

                //see if the session is walking session
                ArrayList<Annotation> annotations = session.getAnnotationsSet().getAnnotationByContent(TransportationModeStreamGenerator.TRANSPORTATION_MODE_NAME_ON_FOOT);

                //session startTime
                long sessionStartTime = session.getStartTime();

                Log.d(TAG, "[test mobile triggering] annotations size > 0 ? " + (annotations.size() > 0));
                Log.d(TAG, "[test mobile triggering] sessionStartTime < lastminute ? " + (sessionStartTime < lastMinute));
                Log.d(TAG, "[test mobile triggering] isWalkingOutdoor ? " + isWalkingOutdoor(lastMinute, now));

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "annotations size > 0 ? "+ (annotations.size() > 0));
                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "sessionStartTime < lastminute ? "+ (sessionStartTime < lastMinute));
                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "isWalkingOutdoor ? "+ isWalkingOutdoor(lastMinute, now));

                //if the session is on foot and has lasted for one minute
                if (annotations.size() > 0 && sessionStartTime < lastMinute && isWalkingOutdoor(lastMinute, now)) {

                    walkoutdoor_sampled = sharedPrefs.getInt("walkoutdoor_sampled", 0);

                    CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "before checking the time, sameTripPrevent : "+ sameTripPrevent);
                    CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Condition: walkoutdoor_sampled Count : "+ walkoutdoor_sampled);
                    CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Condition: walkoutdoor_sampled Count < 3 ? "+ (walkoutdoor_sampled < 3));

                    Log.d(TAG, "[test mobile triggering] walkoutdoor_sampled < 3 ? " + (walkoutdoor_sampled < 3));
                    Log.d(TAG, "[test mobile triggering] sameTripPrevent == false ? " + (sameTripPrevent == false));

                    //the user is walking outdoor we need to trigger a survey
                    if (walkoutdoor_sampled < 3 && sameTripPrevent == false) {

                        //judge the period of the day by 6
                        boolean isTimeForSurvey = isTimeForSurvey(noti_walk);

                        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Condition: isTimeForSurvey ? "+ isTimeForSurvey);
                        Log.d(TAG, "[test mobile triggering] isTimeForSurvey(noti_walk) ? " + isTimeForSurvey);

                        if (isTimeForSurvey) {

                            CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Conditions is satisfied, going to trigger the notification.");

                            triggerWalkingSurvey();

                            CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "The mobile notification is triggered!!");
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {

            } catch (NullPointerException e) {

            }
        }
    }

    private boolean isTimeForSurvey(String noti_type) {

        long now = ScheduleAndSampleManager.getCurrentTimeInMillis();
        boolean isTimeToSendSurvey = false;

        Config.daysInSurvey = sharedPrefs.getInt("daysInSurvey", Config.daysInSurvey);
        Config.downloadedDayInSurvey = sharedPrefs.getInt("downloadedDayInSurvey", Config.downloadedDayInSurvey);

        Log.d(TAG, "[test mobile triggering] daysInSurvey : " + Config.daysInSurvey);
        Log.d(TAG, "[test mobile triggering] downloadedDayInSurvey : " + Config.downloadedDayInSurvey);
        Log.d(TAG, "[test mobile triggering] condition 2 : " + (Config.daysInSurvey != Config.downloadedDayInSurvey));
        Log.d(TAG, "[test mobile triggering] condition 3 : " + (Config.daysInSurvey != -1));

        boolean isPeriodToSurvey = checkPeriod(now, noti_type);

        Log.d(TAG, "[test mobile triggering] checkPeriod : "+isPeriodToSurvey);

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Going to check ? "+ noti_type);
        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Is the period to trigger survey ? "+ isPeriodToSurvey);
        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Is the daysInSurvey equal to downloadedDayInSurvey ? "+ (Config.daysInSurvey != Config.downloadedDayInSurvey));
        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Is the daysInSurvey equal to -1 ? "+ (Config.daysInSurvey != -1));
        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Is the daysInSurvey equal to 0 ? "+ (Config.daysInSurvey != 0));

        //check the period haven't triggered the noti or not.
        if (    isPeriodToSurvey
                && Config.daysInSurvey != Config.downloadedDayInSurvey
                && Config.daysInSurvey != -1
                && Config.daysInSurvey != 0
                && Config.daysInSurvey <= Constants.FINALDAY
                ) {

            isTimeToSendSurvey = true;

            CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "isTimeToSendSurvey ? "+ isTimeToSendSurvey);

            return isTimeToSendSurvey;
        }

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "isTimeToSendSurvey ? "+ isTimeToSendSurvey);

        return isTimeToSendSurvey;
    }

    private boolean checkPeriod(long triggeredTimestamp, String noti_type){

        int periodNum = getPeriodNum(triggeredTimestamp);

        Log.d(TAG, "periodNum : " + periodNum);

        if(periodNum == -1){

            return false;
        }

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Period Number is "+ periodNum);

        //if it's a mobile triggered or have been clicked; true

        boolean isPeriodMobileNotTriggered = sharedPrefs.getBoolean("MobilePeriod" + periodNum, true);

        boolean isPeriodNotClicked = sharedPrefs.getBoolean("Period"+periodNum,true);

        sharedPrefs.edit().putInt("CurrPeriodNum", periodNum).apply();

        Log.d(TAG, "[test new trigger] isPeriodNotClicked : " + isPeriodNotClicked);

        Log.d(TAG, "[test new trigger] isPeriodMobileNotTriggered : " + isPeriodMobileNotTriggered);

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Did period "+ periodNum + " not trigger mobile survey ? " + isPeriodMobileNotTriggered);
        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Did period "+ periodNum + " not trigger survey ? " + isPeriodNotClicked);

        if(isPeriodMobileNotTriggered && isPeriodNotClicked){

            return true;
        }

        //else if the period has been mobile triggered or has been clicked

        return false;
    }

    private int getPeriodNum(long triggeredTimestamp){

        String sleepingstartTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER); //"22:00"
        String sleepingendTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER);

        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String date = ScheduleAndSampleManager.getTimeString(new Date().getTime(), sdf_date);

        String sleepstartTimeWithDate = date + " " + sleepingstartTime + ":00";
        String sleepingendTimeWithDate = date + " " + sleepingendTime + ":00";

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE);
        long sleepStartTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepstartTimeWithDate, sdf);
        long sleepEndTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepingendTimeWithDate, sdf);

        Log.d(TAG, "sleepstartTimeWithDate : " + sleepstartTimeWithDate);
        Log.d(TAG, "sleepingendTimeWithDate : " + sleepingendTimeWithDate);

        long period = sharedPrefs.getLong("PeriodLong", 0);

        Log.d(TAG, "sleepingendTimeWithDate period 1 : " + ScheduleAndSampleManager.getTimeString(sleepEndTimeLong +period));
        Log.d(TAG, "sleepingendTimeWithDate period 2 : " + ScheduleAndSampleManager.getTimeString(sleepEndTimeLong +2*period));
        Log.d(TAG, "sleepingendTimeWithDate period 3 : " + ScheduleAndSampleManager.getTimeString(sleepEndTimeLong +3*period));
        Log.d(TAG, "sleepingendTimeWithDate period 4 : " + ScheduleAndSampleManager.getTimeString(sleepEndTimeLong +4*period));
        Log.d(TAG, "sleepingendTimeWithDate period 5 : " + ScheduleAndSampleManager.getTimeString(sleepEndTimeLong +5*period));
        Log.d(TAG, "sleepingendTimeWithDate period 6 : " + ScheduleAndSampleManager.getTimeString(sleepEndTimeLong +6*period));

        if(triggeredTimestamp >= sleepEndTimeLong && triggeredTimestamp <= (sleepEndTimeLong +period)){

            return 1;
        }else if(triggeredTimestamp > (sleepEndTimeLong + period) && triggeredTimestamp <= (sleepEndTimeLong + 2*period)){

            return 2;
        }else if(triggeredTimestamp > (sleepEndTimeLong + 2*period) && triggeredTimestamp <= (sleepEndTimeLong + 3*period)){

            return 3;
        }else if(triggeredTimestamp > (sleepEndTimeLong + 3*period) && triggeredTimestamp <= (sleepEndTimeLong + 4*period)){

            return 4;
        }else if(triggeredTimestamp > (sleepEndTimeLong + 4*period) && triggeredTimestamp <= (sleepEndTimeLong + 5*period)){

            return 5;
        }else if(triggeredTimestamp > (sleepEndTimeLong + 5*period) && triggeredTimestamp <= (sleepEndTimeLong + 6*period)){

            return 6;
        }

        return -1;
    }

    private void setUpSurveyLink() {
        //Log.d(TAG, "setUpSurveyLink");

        //Counting the day of the experiments
        int TaskDayCount = sharedPrefs.getInt("daysInSurvey", Config.daysInSurvey);
        TaskDayCount++;

        dailySurveyNum = TaskDayCount % 7;

        dailyResponseNum = 0;

        sharedPrefs.edit().putInt("dailySurveyNum", dailySurveyNum).apply();
        sharedPrefs.edit().putInt("dailyResponseNum", dailyResponseNum).apply();

    }

    private void sendSurveyLink(String noti_type) {

        Log.d(TAG, "[test mobile triggering] sendSurveyLink");

        //cancel the survey if it exists
        //the new notification should replace the old one even if it is not the same id.
        try{
            mNotificationManager.cancel(walk_notifyID);
        }catch (Exception e){
        }

        try{
            mNotificationManager.cancel(interval_notifyID);
        }catch (Exception e){
        }

        triggerQualtrics(noti_type);

        addSurveyLinkToDB(noti_type);

        //update the last survey time
        last_Survey_Time = new Date().getTime();
        sharedPrefs.edit().putLong("last_Survey_Time", last_Survey_Time).apply();
    }

    private void triggerIntervalSurvey() {

        //send survey
        sendSurveyLink(noti_random);
    }

    private void triggerWalkingSurvey() {

        Log.d(TAG, "[test mobile triggering] noti_walk prepare to judge ");

        //update the period mobile triggered record
        int periodNum = sharedPrefs.getInt("CurrPeriodNum", -1);

        sharedPrefs.edit().putBoolean("Period"+periodNum,false).apply();

        sharedPrefs.edit().putBoolean("MobilePeriod" + periodNum, false).apply();

        //send survey
        sendSurveyLink(noti_walk);

        last_Walking_Survey_Time = new Date().getTime();
        sharedPrefs.edit().putLong("last_Walking_Survey_Time", last_Walking_Survey_Time).apply();

        //preventing the same on_foot trip will trigger two notifications.
        sameTripPrevent = true;
        sharedPrefs.edit().putBoolean("sameTripPrevent", sameTripPrevent).apply();
    }

    public boolean isWalkingOutdoor(long walkingStarTime, long walkingEndtime) {

        //Log.d(TAG, "isWalkingOutdoor");

        //get locations in the last minute
        ArrayList<LatLng> latlngs = getLocationRecordInLastMinute(walkingStarTime, walkingEndtime);

        //get distance from the last minute
        double dist = calculateTotalDistanceOfPath(latlngs);

        //Log.d(TAG, "dist : "+ dist);

        //50 < dist < 150 means walking outdoor
        if(dist >= dist_Lowerbound_Outdoorwalking && dist <= dist_Upperbound_Outdoorwalking){
            //Log.d(TAG, "isWalkingOutdoor true");

            return true;
        }
        else {
            //Log.d(TAG, "isWalkingOutdoor false");

            return false;
        }
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

        Log.d(TAG, "[test mobile triggering] distance : "+ dist);

        return dist;

    }

    private ArrayList<LatLng> getLocationRecordInLastMinute(long lastminute, long now) {

        ArrayList<LatLng> LatLngs = new ArrayList<LatLng>();

        //Log.d(TAG, "[test sampling] getLocationRecordInLastMinute ");

        //get data from the database
        ArrayList<String> data = DBHelper.queryRecordsBetweenTimes(DBHelper.STREAM_TYPE_LOCATION, lastminute, now);

        //Log.d(TAG, "[test sampling] getLocationRecordInLastMinute get data:" + data.size() + "rows");

        for (int i = 0; i < data.size(); i++) {

            String[] record = data.get(i).split(Constants.DELIMITER);
            double lat = Double.parseDouble(record[2]);
            double lng = Double.parseDouble(record[3]);

            LatLngs.add(new LatLng(lat, lng));

        }

        return LatLngs;
    }

    private String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }

    //add to DB in order to display it in the SurveyActivity.java
    public void addSurveyLinkToDB(String noti_type){

        //Log.d(TAG, "addSurveyLinkToDB");

        dailyResponseNum++;
        sharedPrefs.edit().putInt("dailyResponseNum", dailyResponseNum).apply();

        String mob;
        if(noti_type.equals(noti_walk)){

            mob = "1";
        }else{

            mob = "0";
        }

        //before the place it was dailySurveyNum
        int daysInSurvey = sharedPrefs.getInt("daysInSurvey", Config.daysInSurvey);

        int periodNum = getPeriodNum(ScheduleAndSampleManager.getCurrentTimeInMillis());

        String linktoShow = LINK + "?p="+Config.USER_ID + "&g=" + Config.GROUP_NUM + "&d=" + daysInSurvey + "&n=" + periodNum + "&m=" + mob;

        ContentValues values = new ContentValues();

        try{

            DBHelper.updateSurveyBydn(linktoShow, noti_type, daysInSurvey, periodNum);
        } finally {

            values.clear();
            DBManager.getInstance().closeDatabase();

            //TODO deprecated, +1 is because it will be add after this function but we show the afterPlus one.
//            int surveyNum = interval_sampled + walkoutdoor_sampled + 1;

            int surveyNum = periodNum;

            Utils.storeToCSV_IntervalSurveyCreated(ScheduleAndSampleManager.getCurrentTimeInMillis()
                    , daysInSurvey, surveyNum, linktoShow, noti_type, mContext);
        }
    }

    public boolean InSleepingTime(String startSleepingTime, String endSleepingTime){

        boolean wakeSleepDateIsSame = sharedPrefs.getBoolean("WakeSleepDateIsSame", false);

        Date curDate = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY_Slash);
        String yMdformat = df.format(curDate);

        Date tomorDate = new Date(System.currentTimeMillis() + Constants.MILLISECONDS_PER_DAY);
        String tomorformat = df.format(tomorDate);

        long surveyEndTime = -999;
        long surveyStartTime = getSpecialTimeInMillis(yMdformat+" "+endSleepingTime+":00");

        Log.d(TAG, "[test mobile triggering] surveyStartTime : " + yMdformat+" "+endSleepingTime+":00");

//        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "[test mobile triggering] surveyStartTime : "+ yMdformat+" "+endSleepingTime+":00");

        Log.d(TAG, "wakeSleepDateIsSame : "+wakeSleepDateIsSame);

        if(wakeSleepDateIsSame) {

            surveyEndTime = getSpecialTimeInMillis(tomorformat+" "+startSleepingTime+":00");
            Log.d(TAG, "[test mobile triggering] sleepingstartTime : " + tomorformat + " " + startSleepingTime + ":00");
//            CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "[test mobile triggering] sleepingstartTime : "+ tomorformat+" "+startSleepingTime+":00");
        }else{

            surveyEndTime = getSpecialTimeInMillis(yMdformat+" "+startSleepingTime+":00");
            Log.d(TAG, "[test mobile triggering] sleepingstartTime : " + yMdformat + " " + startSleepingTime + ":00");
//            CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "[test mobile triggering] sleepingstartTime : "+ yMdformat+" "+startSleepingTime+":00");
        }

        long currentTimeLong = ScheduleAndSampleManager.getCurrentTimeInMillis();

        //check the current time is in user's sleeping time or not
        if(currentTimeLong <= surveyEndTime
                && currentTimeLong >= surveyStartTime){

            return false;
        }else{

            return true;
        }
    }

    public static int generatePendingIntentRequestCode(long time){

        int code = 0;

        if (time-time_base > 1000000000){
            time_base = ScheduleAndSampleManager.getCurrentTimeInMillis();
        }

        return (int) (time-time_base);
    }

    private void updateLastSurvey(){

        //update the sameTripPrevent
        sameTripPrevent = false;
        sharedPrefs.edit().putBoolean("sameTripPrevent", sameTripPrevent).apply();

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "after check the time, sameTripPrevent : "+ sameTripPrevent);

//        ArrayList<String> linkDataToday = DataHandler.getSurveyData(startTime, endTime);
        ArrayList<String> linkDataToday = DBHelper.querySurveyLinks();

        //if there have data in DB
        if(!linkDataToday.isEmpty() || linkDataToday == null) {

            String openedFlag = linkDataToday.get(linkDataToday.size()-1).split(Constants.DELIMITER)[5];

            //if still not opened, set it missed
            if(openedFlag.equals("-1")) {

                String id = linkDataToday.get(linkDataToday.size() - 1).split(Constants.DELIMITER)[0];

                DataHandler.updateSurveyMissTime(id, DBHelper.missedTime_col);
            }
        }
    }

    private void triggerQualtrics(String noti_type){

        Log.d(TAG, "[test mobile triggering] triggerQualtrics");

        updateLastSurvey();

        String notiText = SURVEY_TEXT;

        Intent resultIntent = new Intent(mContext, SurveyActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(mContext, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);//Context.
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(notiText);


        Notification.Builder noti = new Notification.Builder(mContext)
                .setContentTitle(Constants.APP_FULL_NAME)
                .setContentText(notiText)
                .setContentIntent(pending)
                .setStyle(bigTextStyle)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            noti.setChannelId(Constants.SURVEY_CHANNEL_ID);
        }

        Notification note = noti.setSmallIcon(MinukuNotificationManager.getNotificationIcon(noti)).build();

        // using the same tag and Id causes the new notification to replace an existing one
        if(noti_type.equals(noti_walk)){
            mNotificationManager.notify(walk_notifyID, note);
        }else {
            mNotificationManager.notify(interval_notifyID, note);
        }

        note.flags = Notification.FLAG_AUTO_CANCEL;
    }

    public void registerActionAlarmReceiver(){

        //Log.d(TAG, "registerActionAlarmReceiver");
        alarmregistered = true;

        sharedPrefs.edit().putBoolean("alarmregistered", alarmregistered);
        sharedPrefs.edit().commit();

        //register action alarm
        IntentFilter alarm_filter = new IntentFilter(Constants.INTERVAL_SAMPLE);
        mContext.registerReceiver(IntervalSampleReceiver, alarm_filter);
    }

    public void unregisterActionAlarmReceiver(){
        mContext.unregisterReceiver(IntervalSampleReceiver);

        alarmregistered = false;

        sharedPrefs.edit().putBoolean("alarmregistered", alarmregistered);
        sharedPrefs.edit().commit();
    }

    public static void cancelAlarmAll(Context mContext){

        try {

            Intent intent = new Intent(Constants.INTERVAL_SAMPLE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,0);
            AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {

        }
    }

    BroadcastReceiver IntervalSampleReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Constants.INTERVAL_SAMPLE)){

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Catch the alarm, going to consider the condition to trigger the notification.");

                interval_sampled = sharedPrefs.getInt("interval_sampled", 0);

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Condition: interval_sampled Count : "+ interval_sampled);

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Condition: interval_sampled Count < 3 ? "+ (interval_sampled < 3));

                boolean isTimeToSurvey = isTimeForSurvey(noti_random);

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Condition: isTimeForSurvey ? "+ isTimeToSurvey);

                Log.d(TAG, "[test alarm] IntervalSampleReceiver isTimeForSurvey : " + isTimeToSurvey);


                if (interval_sampled < 6 && isTimeToSurvey) {

                    CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Conditions is satisfied, going to trigger the notification.");

                    triggerIntervalSurvey();

                    CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "The interval notification is triggered!!");
                }
            }
        }
    };

}
