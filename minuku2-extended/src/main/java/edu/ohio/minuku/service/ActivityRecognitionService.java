package edu.ohio.minuku.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.model.DataRecord.ActivityRecognitionDataRecord;
import edu.ohio.minuku.streamgenerator.ActivityRecognitionStreamGenerator;
import edu.ohio.minukucore.exception.StreamNotFoundException;

/**
 * Created by Lawrence on 2017/5/22.
 */

public class ActivityRecognitionService extends IntentService {

    private final String TAG = "ActivityRecognitionService";

    private String Latest_mMostProbableActivitytype;
    private DetectedActivity mMostProbableActivity;
    private List<DetectedActivity> mProbableActivities;

    public static DetectedActivity toOthermMostProbableActivity;
    public static List<DetectedActivity> toOthermProbableActivities;

    public static ActivityRecognitionStreamGenerator mActivityRecognitionStreamGenerator;

    private Timer timer;
    private TimerTask timerTask;

    //private static String detectedtime;
    private long detectedtime;

    private Boolean updateOrNot = false;

    private static Context serviceInstance = null;

    public ActivityRecognitionService() {
        super("ActivityRecognitionService");

        serviceInstance = this;

        Log.e(TAG,"ActivityRecognitionService is constructed!");
        //mActivityRecognitionManager = ContextManager.getActivityRecognitionManager();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        /**  move to TriggerManager  **/
        //TODO triggerManager situationManager, triggerManager: replace ModeWork.work. , situationManager: replace ModeWork.condition. 放transportationManager(In Minuku).
        if(ActivityRecognitionResult.hasResult(intent)) {
            try {
                mActivityRecognitionStreamGenerator = (ActivityRecognitionStreamGenerator) MinukuStreamManager.getInstance().getStreamGeneratorFor(ActivityRecognitionDataRecord.class);
            }catch (StreamNotFoundException e){
                e.printStackTrace();
            }
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            mProbableActivities = result.getProbableActivities();
            mMostProbableActivity = result.getMostProbableActivity();
            detectedtime = new Date().getTime(); //TODO might be wrong, be aware for it!!

            Log.d(TAG, "[test ActivityRecognition]" +   mMostProbableActivity.toString());
            try {
                if (mProbableActivities != null && mMostProbableActivity != null)
                    mActivityRecognitionStreamGenerator.setActivitiesandDetectedtime(mProbableActivities, mMostProbableActivity, detectedtime);
            }catch(Exception e){
                e.printStackTrace();
            }

            toOthermMostProbableActivity = mMostProbableActivity;
            toOthermProbableActivities = mProbableActivities;

            //reset the timer
            stoptimer();

            //start counting the time to check how much time do the AR service isn't got anything.
            startTimer();


        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {

            int sec = 0;
            public void run() {

//                Log.d(TAG, String.valueOf(System.currentTimeMillis()));

                sec++;

                //if counting until ten minutes
                if(sec == 10 * 60){

                    try {
                        mActivityRecognitionStreamGenerator = (ActivityRecognitionStreamGenerator) MinukuStreamManager.getInstance().getStreamGeneratorFor(ActivityRecognitionDataRecord.class);
                    }catch (StreamNotFoundException e){
                        e.printStackTrace();
                    }

//                    DetectedActivity detectedActivity = new DetectedActivity(,100);

                    ActivityRecognitionDataRecord activityRecognitionDataRecord
                            = new ActivityRecognitionDataRecord();
                    //update the empty AR to MinukuStreamManager
                    MinukuStreamManager.getInstance().setActivityRecognitionDataRecord(activityRecognitionDataRecord);

                }

            }
        };
    }

    public void startTimer() {

        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask,0,1000);

    }

    public void stoptimer() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public static boolean isServiceRunning() {
        return serviceInstance != null;
    }

    public long getCurrentTimeInMillis(){
        //get timzone
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        long t = cal.getTimeInMillis();
        return t;
    }


}
