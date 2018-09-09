package edu.ohio.minuku_2.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku_2.service.BackgroundService;

/**
 * Created by Lawrence on 2018/8/17.
 */
public class RestarterBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "RestarterBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        boolean isOverFinalDay = (Config.daysInSurvey > Constants.FINALDAY);

        if (intent.getAction().equals(Constants.CHECK_SERVICE_ACTION) && !isOverFinalDay) {

            Log.d(TAG, "the RestarterBroadcastReceiver is going to start the BackgroundService");

            Intent intentToStartBackground = new Intent(context, BackgroundService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intentToStartBackground);
            } else {
                context.startService(intentToStartBackground);
            }
        }
    }
}
