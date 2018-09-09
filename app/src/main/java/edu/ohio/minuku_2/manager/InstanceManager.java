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

package edu.ohio.minuku_2.manager;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Utilities.FileHelper;
import edu.ohio.minuku.dao.ActivityRecognitionDataRecordDAO;
import edu.ohio.minuku.dao.AppUsageDataRecordDAO;
import edu.ohio.minuku.dao.BatteryDataRecordDAO;
import edu.ohio.minuku.dao.ConnectivityDataRecordDAO;
import edu.ohio.minuku.dao.LocationDataRecordDAO;
import edu.ohio.minuku.dao.RingerDataRecordDAO;
import edu.ohio.minuku.dao.TelephonyDataRecordDAO;
import edu.ohio.minuku.dao.TransportationModeDAO;
import edu.ohio.minuku.dao.UserInteractionDataRecordDAO;
import edu.ohio.minuku.dao.UserSubmissionStatsDAO;
import edu.ohio.minuku.event.DecrementLoadingProcessCountEvent;
import edu.ohio.minuku.event.IncrementLoadingProcessCountEvent;
import edu.ohio.minuku.manager.MinukuDAOManager;
import edu.ohio.minuku.manager.MinukuSituationManager;
import edu.ohio.minuku.model.DataRecord.ActivityRecognitionDataRecord;
import edu.ohio.minuku.model.DataRecord.AppUsageDataRecord;
import edu.ohio.minuku.model.DataRecord.BatteryDataRecord;
import edu.ohio.minuku.model.DataRecord.ConnectivityDataRecord;
import edu.ohio.minuku.model.DataRecord.LocationDataRecord;
import edu.ohio.minuku.model.DataRecord.RingerDataRecord;
import edu.ohio.minuku.model.DataRecord.TelephonyDataRecord;
import edu.ohio.minuku.model.DataRecord.TransportationModeDataRecord;
import edu.ohio.minuku.model.DataRecord.UserInteractionDataRecord;
import edu.ohio.minuku.model.UserSubmissionStats;
import edu.ohio.minuku.streamgenerator.ActivityRecognitionStreamGenerator;
import edu.ohio.minuku.streamgenerator.AppUsageStreamGenerator;
import edu.ohio.minuku.streamgenerator.BatteryStreamGenerator;
import edu.ohio.minuku.streamgenerator.ConnectivityStreamGenerator;
import edu.ohio.minuku.streamgenerator.LocationStreamGenerator;
import edu.ohio.minuku.streamgenerator.RingerStreamGenerator;
import edu.ohio.minuku.streamgenerator.TelephonyStreamGenerator;
import edu.ohio.minuku.streamgenerator.TransportationModeStreamGenerator;
import edu.ohio.minuku.streamgenerator.UserInteractionStreamGenerator;

/**
 * Created by neerajkumar on 8/28/16.
 */
public class InstanceManager {
    private static InstanceManager instance = null;
    private Context mApplicationContext = null;
    private static Context mContext = null;
    private static Intent mintent;
    private UserSubmissionStats mUserSubmissionStats = null;
    private static String LOG_TAG = "InstanceManager";

    private InstanceManager(Context applicationContext) {
        this.mApplicationContext = applicationContext;
        initialize();
    }

    public static InstanceManager getInstance(Context applicationContext) {
        if (instance == null) {
            instance = new InstanceManager(applicationContext);
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    private Context getApplicationContext() {
        return mApplicationContext;
    }

    public static void setContexttoActivityRecognitionservice(Context context) {
        mContext = context;
    }

    private void initialize() {
        // Add all initialization code here.
        // DAO initialization stuff

        DBHelper dBHelper = new DBHelper(getApplicationContext());

        FileHelper fileHelper = new FileHelper(getApplicationContext());

        MinukuDAOManager daoManager = MinukuDAOManager.getInstance();
        //For location
        LocationDataRecordDAO locationDataRecordDAO = new LocationDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(LocationDataRecord.class, locationDataRecordDAO);

        //TODO build new DAO here.
        ActivityRecognitionDataRecordDAO activityRecognitionDataRecordDAO = new ActivityRecognitionDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(ActivityRecognitionDataRecord.class, activityRecognitionDataRecordDAO);

        TransportationModeDAO transportationModeDAO = new TransportationModeDAO(getApplicationContext());
        daoManager.registerDaoFor(TransportationModeDataRecord.class, transportationModeDAO);

        ConnectivityDataRecordDAO connectivityDataRecordDAO = new ConnectivityDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(ConnectivityDataRecord.class, connectivityDataRecordDAO);

        BatteryDataRecordDAO batteryDataRecordDAO = new BatteryDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(BatteryDataRecord.class, batteryDataRecordDAO);

        RingerDataRecordDAO ringerDataRecordDAO = new RingerDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(RingerDataRecord.class, ringerDataRecordDAO);

        AppUsageDataRecordDAO appUsageDataRecordDAO = new AppUsageDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(AppUsageDataRecord.class, appUsageDataRecordDAO);

        TelephonyDataRecordDAO telephonyDataRecordDAO = new TelephonyDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(TelephonyDataRecord.class, telephonyDataRecordDAO);

        UserInteractionDataRecordDAO userInteractionDataRecordDAO = new UserInteractionDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(UserInteractionDataRecord.class, userInteractionDataRecordDAO);


        // Create corresponding stream generators. Only to be created once in Main Activity
        //creating a new stream registers it with the stream manager

        LocationStreamGenerator locationStreamGenerator =
                new LocationStreamGenerator(getApplicationContext());

        ActivityRecognitionStreamGenerator activityRecognitionStreamGenerator =
                new ActivityRecognitionStreamGenerator(getApplicationContext());

        TransportationModeStreamGenerator transportationModeStreamGenerator =
                new TransportationModeStreamGenerator(getApplicationContext());

        ConnectivityStreamGenerator connectivityStreamGenerator =
                new ConnectivityStreamGenerator(getApplicationContext());

        BatteryStreamGenerator batteryStreamGenerator =
                new BatteryStreamGenerator(getApplicationContext());

        RingerStreamGenerator ringerStreamGenerator =
                new RingerStreamGenerator(getApplicationContext());

        AppUsageStreamGenerator appUsageStreamGenerator =
                new AppUsageStreamGenerator(getApplicationContext());

        TelephonyStreamGenerator telephonyStreamGenerator =
                new TelephonyStreamGenerator(getApplicationContext());

        UserInteractionStreamGenerator userInteractionStreamGenerator =
                new UserInteractionStreamGenerator(getApplicationContext());


        // All situations must be registered AFTER the stream generators are registers.
        MinukuSituationManager situationManager = MinukuSituationManager.getInstance();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Future<UserSubmissionStats> submissionStatsFuture = ((UserSubmissionStatsDAO)
                        MinukuDAOManager.getInstance().getDaoFor(UserSubmissionStats.class)).get();
                EventBus.getDefault().post(new IncrementLoadingProcessCountEvent());
                while (!submissionStatsFuture.isDone()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
                //
                try {
                    //Log.d(LOG_TAG, "initialize: getting mUserSubmissionStats from future ");
                     mUserSubmissionStats = submissionStatsFuture.get();
                    //date check - ensuring that every day we have a new instance of submission
                    // stats. Needs to be tested

                    if(!areDatesEqual((new Date().getTime()), mUserSubmissionStats.getCreationTime())
                            || mUserSubmissionStats==null) {
                        if(mUserSubmissionStats == null)
                            //Log.d(LOG_TAG, "initialize: userSubmissionStats is null");
                        //Log.d(LOG_TAG, "initialize: userSubmissionStats is either null or we have a new date." +
//                                "Creating new userSubmissionStats object");
                        mUserSubmissionStats = new UserSubmissionStats();

                    }
                    EventBus.getDefault().post(mUserSubmissionStats);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    //Log.d(LOG_TAG, "initialize: Creating mUserSubmissionStats");
                    //gotUserStatsFromDatabase(null);
                } catch (ExecutionException e) {
                    //e.printStackTrace();
                    //Log.d(LOG_TAG, "initialize: Creating mUserSubmissionStats");
                    //gotUserStatsFromDatabase(null);
                } finally {
                    EventBus.getDefault().post(new DecrementLoadingProcessCountEvent());
                }
            }
        });

    }

    protected boolean areDatesEqual(long currentTime, long previousTime) {
        //Log.d(LOG_TAG, "Checking if the both dates are the same");

        Calendar currentDate = Calendar.getInstance();
        Calendar previousDate = Calendar.getInstance();

        currentDate.setTimeInMillis(currentTime);
        previousDate.setTimeInMillis(previousTime);
        //Log.d(LOG_TAG, "Current Year:" + currentDate.get(Calendar.YEAR) + " Previous Year:" + previousDate.get(Calendar.YEAR));
        //Log.d(LOG_TAG, "Current Day:" + currentDate.get(Calendar.DAY_OF_YEAR) + " Previous Day:" + previousDate.get(Calendar.DAY_OF_YEAR));
        //Log.d(LOG_TAG, "Current Month:" + currentDate.get(Calendar.MONTH) + " Previous Month:" + previousDate.get(Calendar.MONTH));

        boolean sameDay = (currentDate.get(Calendar.YEAR) == previousDate.get(Calendar.YEAR)) &&
                (currentDate.get(Calendar.DAY_OF_YEAR) == previousDate.get(Calendar.DAY_OF_YEAR)) &&
                (currentDate.get(Calendar.MONTH) == previousDate.get(Calendar.MONTH));

//        if(sameDay)
            //Log.d(LOG_TAG, "it is the same day, should not create a new object");
//        else
            //Log.d(LOG_TAG, "it is not the same day - a new day, should create a new object");
        return sameDay;
    }



}
