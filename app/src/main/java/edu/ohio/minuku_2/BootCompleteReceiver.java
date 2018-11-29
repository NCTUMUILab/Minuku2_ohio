package edu.ohio.minuku_2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
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

}
