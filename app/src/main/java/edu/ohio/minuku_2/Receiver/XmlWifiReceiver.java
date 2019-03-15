package edu.ohio.minuku_2.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;

/**
 * Created by Lawrence on 2019/2/20.
 */

public class XmlWifiReceiver extends BroadcastReceiver {

    private final String TAG = "XmlWifiReceiver";

    private static boolean firstConnect = true;


    @Override
    public void onReceive(Context context, Intent intent) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (Constants.CONNECTIVITY_CHANGE.equals(intent.getAction())) {

            //activeNetwork may return null if there's no default Internet
            if (activeNetwork != null &&
                    activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {

                Log.d(TAG, "Switch to Wifi.");

                if(firstConnect) {

                    firstConnect = false;
                }
                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_XML_WIFI_PROCESS_ALIVE, "Switch to Wifi.");
            }else {

                firstConnect = true;

                Log.d(TAG, "activeNetwork != null ? "+(activeNetwork != null));
                Log.d(TAG, "Not in Wifi.");

                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_XML_WIFI_PROCESS_ALIVE, "activeNetwork != null ? "+(activeNetwork != null));
                CSVHelper.storeToCSV(CSVHelper.CSV_CHECK_XML_WIFI_PROCESS_ALIVE, "Not in Wifi.");
            }
        }
    }
}
