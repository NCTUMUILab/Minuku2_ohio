package edu.ohio.minuku_2.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import edu.ohio.minuku.logger.Log;

/**
 * Created by Lawrence on 2018/2/26.
 */

public class DateChangedReceiver extends BroadcastReceiver {

    private final String TAG = "DateChangedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_DATE_CHANGED)) {

            Log.d(TAG, "Date Changed");
//            SurveyTriggerService.settingIntervalSampling(-9999);
        }

    }
}
