package edu.ohio.minuku_2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku_2.service.BackgroundService;

/**
 * Created by Lawrence on 2017/7/19.
 */

public class BootCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = "BootCompleteReceiver";
    private static DBHelper dbhelper = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {

            try{

                CSVHelper.storeToCSV(CSVHelper.CSV_REBOOT, ScheduleAndSampleManager.getCurrentTimeString(), "rebooting");

                //check the daysInSurvey
                checkDaysInSurvey(context);

                dbhelper = new DBHelper(context);

                dbhelper.getWritableDatabase();

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "0", "1");
            }finally {

                //here we start the service
                Intent bintent = new Intent(context, BackgroundService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(bintent);
                } else {
                    context.startService(bintent);
                }

            }

        }

    }

    private void checkDaysInSurvey(Context context){

        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);

        long installedTime = sharedPrefs.getLong("downloadedTime", Utils.getDownloadedTime(context));
        long currentTime = ScheduleAndSampleManager.getCurrentTimeInMillis();

        long installedDateTime = Utils.getDateTime(installedTime);
        long currentDateTime = Utils.getDateTime(currentTime);

        CSVHelper.storeToCSV(CSVHelper.CSV_REBOOT, ScheduleAndSampleManager.getCurrentTimeString(), "installedDateTime : "+ScheduleAndSampleManager.getTimeString(installedDateTime));
        CSVHelper.storeToCSV(CSVHelper.CSV_REBOOT, ScheduleAndSampleManager.getCurrentTimeString(), "currentDateTime : "+ScheduleAndSampleManager.getTimeString(currentDateTime));

        long daysInSurveyByTime = (currentDateTime - installedDateTime)/Constants.MILLISECONDS_PER_DAY;

        long daysInSurveyFromSharedPrefs = sharedPrefs.getInt("daysInSurvey", -1);

        CSVHelper.storeToCSV(CSVHelper.CSV_REBOOT, ScheduleAndSampleManager.getCurrentTimeString(), "daysInSurveyByTime : "+daysInSurveyByTime);
        CSVHelper.storeToCSV(CSVHelper.CSV_REBOOT, ScheduleAndSampleManager.getCurrentTimeString(), "daysInSurveyFromSharedPrefs : "+daysInSurveyFromSharedPrefs);

        if(daysInSurveyByTime != daysInSurveyFromSharedPrefs){

            CSVHelper.storeToCSV(CSVHelper.CSV_REBOOT, ScheduleAndSampleManager.getCurrentTimeString(), "daysInSurvey updated with "+daysInSurveyByTime);
        }else {

            CSVHelper.storeToCSV(CSVHelper.CSV_REBOOT, ScheduleAndSampleManager.getCurrentTimeString(), "daysInSurvey is not changed");
        }

        sharedPrefs.edit().putInt("daysInSurvey", (int)daysInSurveyByTime).apply();
        Config.daysInSurvey = (int)daysInSurveyByTime;

    }

}
