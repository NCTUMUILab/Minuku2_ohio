package edu.ohio.minuku.manager;

import android.app.Notification;
import android.graphics.Color;
import android.os.Build;

import edu.ohio.minuku.R;

/**
 * Created by Lawrence on 2018/3/22.
 */

public class MinukuNotificationManager {

    private final String TAG = "MinukuNotificationManager";

    public static int locationNotificationID = 21;
    public static int appusageNotificationID = 22;

    public static int getNotificationIcon(Notification.Builder notificationBuilder) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            notificationBuilder.setColor(Color.TRANSPARENT);
            return R.drawable.mcog_noticon;

        }
        return R.drawable.dms_icon_noti;
    }


}
