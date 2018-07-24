package edu.ohio.minuku.Utilities;

import android.content.SharedPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.ohio.minuku.config.Constants;

/**
 * Created by Lawrence on 2018/7/1.
 */

public class Utilities {

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static int getPeriodNum(long triggeredTimestamp, SharedPreferences sharedPrefs){

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
}
