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
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Data.DataHandler;
import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.MinukuNotificationManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.DataRecord.ConnectivityDataRecord;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku.streamgenerator.ConnectivityStreamGenerator;
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
    public static final int REFRESH_FREQUENCY = 20; //20s, 10000ms
    public static final int BACKGROUND_RECORDING_INITIAL_DELAY = 0;

    private final int URL_TIMEOUT = 5 * 1000;

    private String userid;

    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";
    private static final String PACKAGE_DIRECTORY_PATH_IntervalSamples="/Android/data/edu.ohio.minuku_2/IntervalSamples/";

    private static String today;
    private String lastTimeSend_today;

    private int notifyID = 1;
    private int walk_notifyID = 2;
    private int interval_notifyID = 3; //used to 3
    //qua is equal to interval now

    public static ArrayList<Long> interval_sampled_times;

    private static int interval_sample_number = 6;
    private static int interval_sample_threshold = 2;

    private static int interval_sampled;
    private int walkoutdoor_sampled;
    private int walk_first_sampled;
    private int walk_second_sampled;
    private int walk_third_sampled;

    private boolean sameTripPrevent;

    private long last_Survey_Time;
    private long last_Walking_Survey_Time;

    private long vibrate[] = {0,200,800,500};
    public static long time_base = 0;

    private String transportation;

    private boolean alarmregistered;

    private String participantID, groupNum;
    private int weekNum, dailySurveyNum, dailyResponseNum;

    private final String link = "https://osu.az1.qualtrics.com/jfe/form/SV_6xjrFJF4YwQwuMZ";//"https://osu.az1.qualtrics.com/jfe/form/SV_6xjrFJF4YwQwuMZ";

    private String apikeyfortesting = "TiKLVvkv4m1XTWhmIjCx4A464jBTP0ZE";
    private String testUrl = "https://api.mlab.com/api/1/databases/mobility_study/collections/real_time?apiKey="+apikeyfortesting;

    private final int dist_Lowerbound_Outdoorwalking = 50;
    private final int dist_Upperbound_Outdoorwalking = 150;

    private NotificationManager mNotificationManager;

    private String noti_random = "random";
    private String noti_walk = "walk";

    private static SharedPreferences sharedPrefs;

    private int surveyNum;

    public SurveyTriggerManager(){}

    public SurveyTriggerManager(Context context){

        instance = this;

        mContext = context;

        sharedPrefs = mContext.getSharedPreferences(Constants.sharedPrefString, mContext.MODE_PRIVATE);

        transportation = "NA";

        lastTimeSend_today = sharedPrefs.getString("lastTimeSend_today","NA");

        interval_sampled = sharedPrefs.getInt("interval_sampled", 0);

        alarmregistered = sharedPrefs.getBoolean("alarmregistered",false);

        sameTripPrevent = sharedPrefs.getBoolean("sameTripPrevent", false);

        time_base = ScheduleAndSampleManager.getCurrentTimeInMillis();

        mScheduledExecutorService = Executors.newScheduledThreadPool(REFRESH_FREQUENCY);

        //we set them to avoid the situation that alarm being canceled by Android.
        resetIntervalSampling();

        registerActionAlarmReceiver();

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

        Boolean setAllDaysIntervalSamplingOrNot = sharedPrefs.getBoolean("setAllDaysIntervalSamplingOrNot", true);

        if(setAllDaysIntervalSamplingOrNot) {

            Utils.settingAllDaysIntervalSampling(mContext);
            sharedPrefs.edit().putBoolean("setAllDaysIntervalSamplingOrNot", false).apply();
        }

        lastTimeSend_today = sharedPrefs.getString("lastTimeSend_today","NA");
        interval_sampled = sharedPrefs.getInt("interval_sampled", 0);
        alarmregistered = sharedPrefs.getBoolean("alarmregistered",false);

        Constants.USER_ID = sharedPrefs.getString("userid","NA");
        Constants.downloadedDayInSurvey = sharedPrefs.getInt("downloadedDayInSurvey", Constants.downloadedDayInSurvey);

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

            Log.d(TAG, "[test alarm] reset time back : "+ScheduleAndSampleManager.getTimeString(time));

            //if the time is overtime, skip it.
            if(ScheduleAndSampleManager.getCurrentTimeInMillis() > time)
                continue;

//            int request_code = generatePendingIntentRequestCode(time);

            //create an alarm for a time
            Intent intent = new Intent(Constants.Interval_Sample);
            PendingIntent pi = PendingIntent.getBroadcast(mContext, requestCode, intent, 0);

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(mContext.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);
        }
    }

    public static void settingIntervalSampling(int daysInSurvey, Context context){

        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);

        Date curDate = new Date(System.currentTimeMillis() + daysInSurvey * Constants.MILLISECONDS_PER_DAY);
        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY_Slash);
        String yMdformat = df.format(curDate);

        //Log.d(TAG, "yMdformat : " + yMdformat);
        interval_sampled_times = new ArrayList<>();
        ArrayList<Long> times = new ArrayList<Long>();
        long now = ScheduleAndSampleManager.getCurrentTimeInMillis();
        Random random = new Random(now);
        int sample_period;
        //TODO set the start and end time.
        String sleepingstartTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER); //"22:00"
        String sleepingendTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER); //"08:00"

        Log.d(TAG, "[test alarm] sleepingendTime : " + yMdformat+" "+sleepingendTime+":00");
        Log.d(TAG, "[test alarm] sleepingstartTime : " + yMdformat+" "+sleepingstartTime+":00");

        //if the time havn't been set, don't set the survey time with default.
        if(sleepingstartTime.equals(Constants.NOT_A_NUMBER) || sleepingendTime.equals(Constants.NOT_A_NUMBER)){

            return;
        }

        long surveyEndTime = getSpecialTimeInMillis(yMdformat+" "+sleepingstartTime+":00");
        long surveyStartTime = getSpecialTimeInMillis(yMdformat+" "+sleepingendTime+":00");

        long sub_startTime = surveyStartTime;
        long sub_endTime  = surveyEndTime;
        long min_interval = (surveyEndTime - surveyStartTime)/(interval_sample_number + interval_sample_threshold); //90 * 60 * 1000  (e-s)/10

        //1. first get the entire sampling period

        while(true) {

            //repeatedly find the subsampling period
            for (int i = 0; i < interval_sample_number; i++) {

                //Log.d(TAG, "interval_sample_number : "+i);

                //2. divide the period by the number of sample
                sample_period = (int) (sub_endTime - sub_startTime);

                try {

                    //Log.d(TAG, "sample_period : "+sample_period);
                    //Log.d(TAG, "interval_sample_number : "+interval_sample_number);
                    //Log.d(TAG, "i : "+i);

                    int sub_sample_period = sample_period / (interval_sample_number - i);

                    //Log.d(TAG, "sub_sample_period : "+sub_sample_period);

                    //3. random within the sub sample period
                    long time = random.nextInt(sub_sample_period) + sub_startTime;

                    //Log.d(TAG, "testRandom semi sampling: the " + i + " sampling period is from " + getTimeString(sub_startTime) + " to " + getTimeString(sub_endTime) +
//                        " divided by " + (interval_sample_number-i) + " each period is " + (sub_sample_period/60/1000) + " minutes long, " + " the sampled time within the period is " +
//                        getTimeString(time) );

                    //Log.d(TAG, "testRandom  the sampled time within the period is " + getTimeString(time) );

                    //4. save the sampled time
                    times.add(time);

                    //5. the next startime is the previous sample time + min interval. We do this to ensure that the next sampled time is
                    //not too close to the previous sampled time.
                    sub_startTime = time + min_interval;

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

                if(secondTime - firstTime < Constants.MILLISECONDS_PER_HOUR){

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
            for (int i = 0; i < times.size(); i++) {

                long time = times.get(i);
                int request_code = generatePendingIntentRequestCode(time);
                //create an alarm for a time
                Intent intent = new Intent(Constants.Interval_Sample);

                PendingIntent pi = PendingIntent.getBroadcast(context, request_code, intent, 0);

                //for the reset one
                AlarmManager alarmManager = (AlarmManager)context.getSystemService( context.ALARM_SERVICE );
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);

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
                    new Intent(Constants.Interval_Sample),
                    PendingIntent.FLAG_NO_CREATE) != null);

            if (alarmUp) {

                Log.d(TAG, "[recalling alarms] Alarm is already active");
            }else{

                Log.d(TAG, "[recalling alarms] Alarm is canceled! Recall it back");

                //recall the one which is gone, recall it back.
                Intent intent = new Intent(Constants.Interval_Sample);
                PendingIntent pi = PendingIntent.getBroadcast(mContext, request_code, intent, 0);

                AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(mContext.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi);

                break;
            }
        }
    }

    Runnable RunningForGivingSurveyOrNot = new Runnable() {
        @Override
        public void run() {

            confirmAlarmExist();

            userid = Constants.DEVICE_ID;

            //variable for link
            participantID = sharedPrefs.getString("userid", "NA");
            groupNum = sharedPrefs.getString("groupNum", "NA");
            weekNum = sharedPrefs.getInt("weekNum", 0);
//            dailySurveyNum = sharedPrefs.getInt("dailySurveyNum", 0);
            dailySurveyNum = sharedPrefs.getInt("daysInSurvey", Constants.daysInSurvey);
            dailyResponseNum = sharedPrefs.getInt("dailyResponseNum", 0);

            Date curDate = new Date(System.currentTimeMillis());
            SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY_Slash);
            today = df.format(curDate);

            //get sleep time and get up time from sharedpreference
            String startSleepingTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER);//"22:00"
            String endSleepingTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER);//"08:00"

            //Log.d(TAG,"startSleepingTime : "+ startSleepingTime + " endSleepingTime : "+ endSleepingTime);

            if(!startSleepingTime.equals(Constants.NOT_A_NUMBER) && !endSleepingTime.equals(Constants.NOT_A_NUMBER)) {

                //calculate today's bed time
                long bedTime;
                boolean wakeSleepDateIsSame = sharedPrefs.getBoolean("WakeSleepDateIsSame", false);

                if(wakeSleepDateIsSame){

                    Date tomorDate = new Date(ScheduleAndSampleManager.getCurrentTimeInMillis());
                    String tomorrow = df.format(tomorDate);
                    bedTime = getSpecialTimeInMillis(tomorrow + " " + startSleepingTime + ":00");

                    //updated the last survey when it's time to sleep if it haven't been opened
                    if(bedTime < ScheduleAndSampleManager.getCurrentTimeInMillis()) {

                        Date yesterdayDate = new Date(ScheduleAndSampleManager.getCurrentTimeInMillis() - Constants.MILLISECONDS_PER_DAY);
                        String yesterday = df.format(yesterdayDate);

                        updateLastSurvey(yesterday);
                    }
                }else{

                    bedTime = getSpecialTimeInMillis(today + " " + startSleepingTime + ":00");

                    //updated the last survey when it's time to sleep if it haven't been opened
                    if(bedTime < ScheduleAndSampleManager.getCurrentTimeInMillis()) {

                        updateLastSurvey(today);
                    }
                }

                //Log.d(TAG, "userid: " + userid);

                //after a day
                if (!lastTimeSend_today.equals(today)) {

                    if(Constants.daysInSurvey == -1) {

                        Constants.daysInSurvey = 0;
                    }else {

                        //Prevent the situation that they download at the day which is not the first day of the research.
                        //which means they get the daysInSurvey is x|x>0 instead of -1, just keep it.
                        if(!lastTimeSend_today.equals("NA")){

                            Constants.daysInSurvey++;
                        }
                    }
                    sharedPrefs.edit().putInt("daysInSurvey", Constants.daysInSurvey).apply();

                    Log.d(TAG, "after a day daysInSurvey today : "+ today);
                    Log.d(TAG, "after a day daysInSurvey lastTimeSend_today : "+ lastTimeSend_today);
                    Log.d(TAG, "after a day daysInSurvey : "+ Constants.daysInSurvey);

                    sharedPrefs.edit().putBoolean("MobileReplacePriorPeriodFlag", false).apply();

                    sharedPrefs.edit().putString("mobileMissedCount", "").apply();
                    sharedPrefs.edit().putString("randomMissedCount", "").apply();
                    sharedPrefs.edit().putString("OpenCount", "").apply();

                    walkoutdoor_sampled = 0;
                    sharedPrefs.edit().putInt("walkoutdoor_sampled", walkoutdoor_sampled).apply();

                    walk_first_sampled = 0;
                    sharedPrefs.edit().putInt("walk_first_sampled", walk_first_sampled).apply();
                    walk_second_sampled = 0;
                    sharedPrefs.edit().putInt("walk_second_sampled", walk_second_sampled).apply();
                    walk_third_sampled = 0;
                    sharedPrefs.edit().putInt("walk_third_sampled", walk_third_sampled).apply();

                    //total 6 surveys a day
//                    interval_sample_number = 6;
//                    sharedPrefs.edit().putInt("interval_sample_number", interval_sample_number).apply();

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

                    last_Survey_Time = -999;
                    sharedPrefs.edit().putLong("last_Survey_Time", last_Survey_Time).apply();

                    last_Walking_Survey_Time = -999;
                    sharedPrefs.edit().putLong("last_Walking_Survey_Time", last_Walking_Survey_Time).apply();

                    if (lastTimeSend_today.equals("NA")) {
                        //setting up the parameter for the survey link
                        setUpSurveyLink();
                    }

                    lastTimeSend_today = today;
                    sharedPrefs.edit().putString("lastTimeSend_today", lastTimeSend_today).apply();

                }

                //temporarily remove the trigger limit
                if (!InSleepingTime(startSleepingTime, endSleepingTime)) {

                    //Log.d(TAG, "it's not sleeping time");

                    /** see if the user is walking in the last minute **/
                    long now = ScheduleAndSampleManager.getCurrentTimeInMillis();
                    long lastminute = now - Constants.MILLISECONDS_PER_MINUTE;

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

                        //if the session is on foot and has lasted for one minute
                        if (annotations.size() > 0 && sessionStartTime < lastminute && isWalkingOutdoor(lastminute, now)) {

                            walkoutdoor_sampled = sharedPrefs.getInt("walkoutdoor_sampled", 0);

                            //the user is walking outdoor we need to trigger a survey
                            if (walkoutdoor_sampled < 3 && sameTripPrevent == false) {

                                //judge the period of the day by 6

                                if (isTimeForSurvey(noti_walk)) {

                                    triggerWalkingSurvey();
                                }
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {

                    } catch (NullPointerException e) {

                    }
                }
            }
        }
    };

    private boolean isTimeForSurvey(String noti_type) {

        long now = ScheduleAndSampleManager.getCurrentTimeInMillis();
        boolean isTimeToSendSurvey = false;

        Constants.daysInSurvey = sharedPrefs.getInt("daysInSurvey", Constants.daysInSurvey);
        Constants.downloadedDayInSurvey = sharedPrefs.getInt("downloadedDayInSurvey", Constants.downloadedDayInSurvey);

        Log.d(TAG, "[test alarm] daysInSurvey : "+Constants.daysInSurvey);
        Log.d(TAG, "[test alarm] downloadedDayInSurvey : "+Constants.downloadedDayInSurvey);
        Log.d(TAG, "[test alarm] condition 2 : "+(Constants.daysInSurvey != Constants.downloadedDayInSurvey));
        Log.d(TAG, "[test alarm] condition 3 : "+(Constants.daysInSurvey != -1));

        if(noti_type.equals(noti_walk))
            Log.d(TAG, "[test alarm] noti_walk prepare to judge");

        boolean isPeriodToSurvey = checkPeriod(now, noti_type);

        Log.d(TAG, "[test alarm] checkPeriod : "+isPeriodToSurvey);

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Is the period to trigger survey ? "+ isPeriodToSurvey);

        //check the period haven't triggered the noti or not.
        if (    isPeriodToSurvey
                && Constants.daysInSurvey != Constants.downloadedDayInSurvey
                && Constants.daysInSurvey != -1 ) {

            isTimeToSendSurvey = true;
        }
        return isTimeToSendSurvey;

    }

    private boolean checkPeriod(long triggeredTimestamp, String noti_type){

        int periodNum = getPeriodNum(triggeredTimestamp);

        if(periodNum == -1){

            return false;
        }

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Period Number is "+ periodNum);

        if(periodNum > 1) {

            if (noti_type.equals(noti_walk)) {

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "It is a mobile survey.");

                //get last period
                int lastPeriodNum = periodNum - 1;

                boolean isLastPeriodNotClicked = sharedPrefs.getBoolean("Period"+lastPeriodNum,true);

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "LastPeriod has not clicked "+ isLastPeriodNotClicked + "(P.S. the mobile one might set by clicked for the mechanism.)");

                if(isLastPeriodNotClicked){

                    sharedPrefs.edit().putInt("CurrPeriodNum", lastPeriodNum).apply();

                    sharedPrefs.edit().putBoolean("MobileReplacePriorPeriodFlag", true).apply();

                    Log.d(TAG, "[test new trigger] isLastPeriodNotClicked : "+isLastPeriodNotClicked);

                    return isLastPeriodNotClicked;
                }

                //else get the current one
            }
        }

        //if it's a mobile triggered or have been clicked; true

        boolean isPeriodMobileNotTriggered = sharedPrefs.getBoolean("MobilePeriod" + periodNum, true);

        boolean isPeriodNotClicked = sharedPrefs.getBoolean("Period"+periodNum,true);

        sharedPrefs.edit().putInt("CurrPeriodNum", periodNum).apply();

        Log.d(TAG, "[test new trigger] isPeriodNotClicked : " + isPeriodNotClicked);

        Log.d(TAG, "[test new trigger] isPeriodMobileNotTriggered : " + isPeriodMobileNotTriggered);

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Did period "+ periodNum + " not trigger survey mobile ? " + isPeriodMobileNotTriggered);
        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Did period "+ periodNum + " not trigger survey ? " + isPeriodNotClicked);

        if(isPeriodMobileNotTriggered || isPeriodNotClicked){

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

        long period = sharedPrefs.getLong("PeriodLong", 0);

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
        int TaskDayCount = sharedPrefs.getInt("daysInSurvey", Constants.daysInSurvey);
        TaskDayCount++;

        if ((TaskDayCount - 1) % 7 == 0) { //1, 8, 15
            weekNum++;
        }

        dailySurveyNum = TaskDayCount % 7;
        if (dailySurveyNum == 0)
            dailySurveyNum = 7;

        dailyResponseNum = 0;


//        sharedPrefs.edit().putInt("daysInSurvey", TaskDayCount).apply();
        sharedPrefs.edit().putInt("weekNum", weekNum).apply();
        sharedPrefs.edit().putInt("dailySurveyNum", dailySurveyNum).apply();
        sharedPrefs.edit().putInt("dailyResponseNum", dailyResponseNum).apply();

    }

    private void sendSurveyLink(String noti_type) {

        //cancel the survey if it exists
        //the new notification should replace the old one even if it is not the same id.
        try{
            mNotificationManager.cancel(walk_notifyID);
        }catch (Exception e){
            //e.printStackTrace();
            //Log.d(TAG, "no old walking notification");
//            //Log.e(TAG, "exception", e);
        }

        try{
            mNotificationManager.cancel(interval_notifyID);
        }catch (Exception e){
            //e.printStackTrace();
            //Log.d(TAG, "no old random notification");
//            //Log.e(TAG, "exception", e);
        }

        triggerQualtrics(noti_type);

        boolean mobileReplacePriorPeriodFlag = sharedPrefs.getBoolean("MobileReplacePriorPeriodFlag", false);

        if(mobileReplacePriorPeriodFlag){

            //keep the same link but for the mobile survey

            //recording
            Utils.storeToCSV_IntervalSurveyCreated(ScheduleAndSampleManager.getCurrentTimeInMillis()
                    , surveyNum, "same as the previous", noti_type, mContext);
        }else {

            addSurveyLinkToDB(noti_type);
        }

        sharedPrefs.edit().putBoolean("MobileReplacePriorPeriodFlag", false).apply();

        //after we send out the survey decrease the needed number for the interval_sample_number
//        interval_sample_number--;
//        sharedPrefs.edit().putInt("interval_sample_number", interval_sample_number).apply();

        //update the last survey time
        last_Survey_Time = new Date().getTime();
        sharedPrefs.edit().putLong("last_Survey_Time", last_Survey_Time).apply();
    }

    private void triggerIntervalSurvey() {

        //send survey
        sendSurveyLink(noti_random);

        //after sending survey, update the preference
        interval_sampled++;
        sharedPrefs.edit().putInt("interval_sampled", interval_sampled).apply();

    }

    private void triggerWalkingSurvey() {

        Log.d(TAG, "[test new trigger] noti_walk prepare to judge");

        //update the period mobile triggered record
        int periodNum = sharedPrefs.getInt("CurrPeriodNum", -1);

        //
        sharedPrefs.edit().putBoolean("Period"+periodNum,false).apply();

        sharedPrefs.edit().putBoolean("MobilePeriod" + periodNum, false).apply();

        //send survey
        sendSurveyLink(noti_walk);

        //after sending survey, updatge the preference
        walkoutdoor_sampled++;
        sharedPrefs.edit().putInt("walkoutdoor_sampled", walkoutdoor_sampled).apply();

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

    /*private void settingMissedClickedCount(){
        //TODO to check the missing count and clicked
        long startTime = -9999;
        long endTime = -9999;
        String startTimeString = "";
        String endTimeString = "";

        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        int Year = cal.get(Calendar.YEAR);
        int Month = cal.get(Calendar.MONTH)+1;
        int Day = cal.get(Calendar.DAY_OF_MONTH);

        startTimeString = makingDataFormat(Year, Month, Day);
        endTimeString = makingDataFormat(Year, Month, Day+1);
        startTime = getSpecialTimeInMillis(startTimeString);
        endTime = getSpecialTimeInMillis(endTimeString);

        ArrayList<String> data = new ArrayList<String>();
        data = DataHandler.getSurveyData(startTime, endTime);

        //Log.d(TAG, "SurveyData : "+ data.toString());

        int mobileMissedCount = 0;
        int randomMissedCount = 0;
        int missCount = 0;
        int openCount = 0;

        for(String datapart : data){
            //if the link havn't been opened.
            if(datapart.split(Constants.DELIMITER)[5].equals("0")){
                missCount++;
                if(datapart.split(Constants.DELIMITER)[6].equals(noti_walk))
                    mobileMissedCount++;
                else if(datapart.split(Constants.DELIMITER)[6].equals(noti_random))
                    randomMissedCount++;
            }
            else if(datapart.split(Constants.DELIMITER)[5].equals("1"))
                openCount++;
        }

        String previousMobileMissedCount = sharedPrefs.getString("mobileMissedCount", "");
        String previousRandomMissedCount = sharedPrefs.getString("randomMissedCount", "");
        String previousOpenCount = sharedPrefs.getString("OpenCount", "");

        previousMobileMissedCount += " "+mobileMissedCount;
        previousRandomMissedCount += " "+randomMissedCount;
        previousOpenCount += " "+openCount;

        sharedPrefs.edit().putString("mobileMissedCount", previousMobileMissedCount).apply();
        sharedPrefs.edit().putString("randomMissedCount", previousRandomMissedCount).apply();
        sharedPrefs.edit().putString("OpenCount", previousOpenCount).apply();

    }*/

    private String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }

    public String makingDataFormat(int year,int month,int date){
        String dataformat= "";

        dataformat = addZero(year)+"/"+addZero(month)+"/"+addZero(date)+" "+"00:00:00";
        //Log.d(TAG,"dataformat : " + dataformat);

        return dataformat;
    }

    //add to DB in order to display it in the SurveyActivity.java
    public void addSurveyLinkToDB(String noti_type){
        //Log.d(TAG, "addSurveyLinkToDB");

//        settingMissedClickedCount();

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
            //e.printStackTrace();
            mob = "0";
        }

        String linktoShow = link + "?p="+participantID + "&g=" + groupNum + "&w=" + weekNum + "&d=" + dailySurveyNum + "&r=" + dailyResponseNum + "&m=" + mob;

        ContentValues values = new ContentValues();

        try {

            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            values.put(DBHelper.generateTime_col, new Date().getTime());
            values.put(DBHelper.link_col, linktoShow);
            values.put(DBHelper.surveyType_col, noti_type);
            values.put(DBHelper.openFlag_col, -1); //they can't enter the link by the notification.

            db.insert(DBHelper.surveyLink_table, null, values);
        }
        catch(NullPointerException e){
            //e.printStackTrace();
        }
        finally {

            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection

            //TODO record to the log: IntervalSurveyState.csv
            // +1 is because it will be add after this function but we show the afterPlus one.
            int surveyNum = interval_sampled + walkoutdoor_sampled + 1;

            Utils.storeToCSV_IntervalSurveyCreated(ScheduleAndSampleManager.getCurrentTimeInMillis()
                    , surveyNum, linktoShow, noti_type, mContext);
        }
    }

    public boolean InSleepingTime(String startSleepingTime, String endSleepingTime){

//        String startSleepingTime = sharedPrefs.getString("SleepingStartTime","22:00");
//        String endSleepingTime = sharedPrefs.getString("SleepingEndTime","08:00");

        //Log.d(TAG, "startSleepingTime : " + startSleepingTime + " endSleepingTime : " + endSleepingTime);

        String[] startSleepingTimeArray = startSleepingTime.split(":");
        String[] endSleepingTimeArray = endSleepingTime.split(":");

        //Log.d(TAG, "startSleepingHour : " + startSleepingTimeArray[0] + " startSleepingMin : " + startSleepingTimeArray[1]
//                    + " endSleepingHour : " + endSleepingTimeArray[0] + " endSleepingMin : " + endSleepingTimeArray[1]);

        int startSleepingHour = Integer.valueOf(startSleepingTimeArray[0]);
        int startSleepingMin = Integer.valueOf(startSleepingTimeArray[1]);
        int endSleepingHour = Integer.valueOf(endSleepingTimeArray[0]);
        int endSleepingMin = Integer.valueOf(endSleepingTimeArray[1]);

        //Log.d(TAG, "int startSleepingHour : " + String.valueOf(startSleepingHour) + " startSleepingMin : " + String.valueOf(startSleepingMin)
//                    + " endSleepingHour : " + String.valueOf(endSleepingHour) + " endSleepingMin : " + String.valueOf(endSleepingMin));

        TimeZone tz = TimeZone.getDefault();
        java.util.Calendar cal = java.util.Calendar.getInstance(tz);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        //Log.d(TAG, "hour : " + hour + " min : " + min);

        int startSleepingTimeWholeMin = startSleepingHour * 60 + startSleepingMin;
        int endSleepingTimeWholeMin = endSleepingHour * 60 + endSleepingMin;
        int currentSleepingTimeWholeMin = hour * 60 + min;

        //check the current time is in user's sleeping time or not
        if(currentSleepingTimeWholeMin <= startSleepingTimeWholeMin
                && currentSleepingTimeWholeMin >= endSleepingTimeWholeMin){

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

    private void updateLastSurvey(String day){

        String todayDate = day+" 00:00:00";
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE_Slash);
        long startTime = ScheduleAndSampleManager.getTimeInMillis(todayDate, sdf);
        long endTime = startTime + Constants.MILLISECONDS_PER_DAY;

        ArrayList<String> linkDataToday = DataHandler.getSurveyData(startTime, endTime);

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

        //TODO if the last link(today) haven't been opened, setting it into missed.

        updateLastSurvey(today);

//        String notiText = "You have a new random survey("+noti_type+")"+"\r\n"
//                +"Walking : "+walkoutdoor_sampled+" ;Random : "+interval_sampled;

        String notiText = "You have a new survey available now.";

        Intent resultIntent = new Intent(mContext, SurveyActivity.class);
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
        IntentFilter alarm_filter = new IntentFilter(Constants.Interval_Sample);
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

            Intent intent = new Intent(Constants.Interval_Sample);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,0);
            AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {

        }
    }

    BroadcastReceiver IntervalSampleReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Constants.Interval_Sample)){

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Catch the alarm, going to consider the condition to trigger the notification.");

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Condition: interval_sampled Count < 3 ? "+ (interval_sampled < 3));

                boolean isTimeToSurvey = isTimeForSurvey(noti_random);

                CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Condition: isTimeForSurvey ? "+ isTimeToSurvey);

                Log.d(TAG, "[test alarm] IntervalSampleReceiver isTimeForSurvey : " + isTimeToSurvey);


                if (interval_sampled < 3 && isTimeToSurvey) {

                    CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "Conditions is satisfied, going to trigger the notification.");

                    triggerIntervalSurvey();

                    CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "The interval notification is triggered!!");
                }
            }
        }
    };

}
