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
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.ohio.minuku.Utilities.FileHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.manager.TripManager;
import edu.ohio.minuku.model.DataRecord.ActivityRecognitionDataRecord;
import edu.ohio.minuku.model.DataRecord.TransportationModeDataRecord;
import edu.ohio.minuku.streamgenerator.TransportationModeStreamGenerator;
import edu.ohio.minuku_2.manager.InstanceManager;
import edu.ohio.minukucore.exception.StreamNotFoundException;

public class BackgroundService extends Service {

    private static final String TAG = "BackgroundService";

    MinukuStreamManager streamManager;
    private ScheduledExecutorService mScheduledExecutorService;


    public BackgroundService() {
        super();
        streamManager = MinukuStreamManager.getInstance();
        mScheduledExecutorService = Executors.newScheduledThreadPool(Constants.STREAM_UPDATE_FREQUENCY);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


//        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
//        alarm.set(
//                AlarmManager.RTC_WAKEUP,     //
//                System.currentTimeMillis() + Constants.PROMPT_SERVICE_REPEAT_MILLISECONDS,
//                PendingIntent.getService(this, 0, new Intent(this, BackgroundService.class), 0)
//        );
/*
        Notification note  = new Notification.Builder(getBaseContext())
                .setContentTitle(Constants.APP_NAME)
                .setContentText(Constants.RUNNING_APP_DECLARATION)
                .setSmallIcon(R.drawable.self_reflection)
                .setAutoCancel(false)
                .build();
        note.flags |= Notification.FLAG_NO_CLEAR;
        startForeground( 42, note );
*/

        if(!InstanceManager.isInitialized()) {
            InstanceManager.getInstance(this);
            TripManager.getInstance(this);
        }


        /**read test file**/
        FileHelper fileHelper = FileHelper.getInstance(getApplicationContext());
        FileHelper.readTestFile();


        /*MinukuDAOManager daoManager = MinukuDAOManager.getInstance();

        LocationDataRecordDAO locationDataRecordDAO = new LocationDataRecordDAO(getApplicationContext());
        daoManager.registerDaoFor(LocationDataRecord.class, locationDataRecordDAO);

        LocationStreamGenerator locationStreamGenerator =
                new LocationStreamGenerator(getApplicationContext());*/

        return START_STICKY_COMPATIBILITY;
        //TODO Keep eye on the service is working or not.
//        return START_STICKY;
    }

    private void runMainThread(){

        mScheduledExecutorService.scheduleAtFixedRate(
                updateStreamManagerRunnable,
                0,
                Constants.STREAM_UPDATE_FREQUENCY,
                TimeUnit.SECONDS);
    }

    Runnable updateStreamManagerRunnable = new Runnable() {
        @Override
        public void run() {

            streamManager.updateStreamGenerators();

        }
    };


    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service. Your state might be lost!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}