package edu.ohio.minuku_2.controller;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku_2.MainActivity;
import edu.ohio.minuku_2.R;
import edu.ohio.minuku_2.Utils;
//import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/7/5.
 */

public class sleepingohio extends AppCompatActivity {

    final String TAG = "sleepingohio";

    private Context mContext;
    private Button sleepStarttime, sleepEndtime, confirm;
    private final static int TIME_PICKER_INTERVAL = 30;

    private String sleepStartTimeRaw, sleepEndTimeRaw;

    private SharedPreferences sharedPrefs;

    public sleepingohio(){}

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsleeptimepage);

        sharedPrefs = getSharedPreferences(Constants.sharedPrefString, MODE_PRIVATE);
        mContext = getApplicationContext();

        initsettingohio();

    }

    public void initsettingohio(){

        sleepStarttime = (Button)findViewById(R.id.sleepStarttime);
        sleepStarttime.setOnClickListener(starttimeing);

        sleepEndtime = (Button)findViewById(R.id.sleepEndtime);
        sleepEndtime.setOnClickListener(endtimeing);

        confirm = (Button)findViewById(R.id.confirm);
        confirm.setOnClickListener(confirming);

//        getSupportActionBar().setTitle("DMS");
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.actionbar_new);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            sleepingohio.this.finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private Button.OnClickListener confirming = new Button.OnClickListener() {
        public void onClick(View v) {

            Log.d(TAG, "sleepStarttime from button : "+sleepStarttime.getText());
            Log.d(TAG, "sleepEndtime from button : "+sleepEndtime.getText());

            if(sleepStarttime.getText().equals("BED TIME"))
                Toast.makeText(sleepingohio.this,"Please select your bed time", Toast.LENGTH_SHORT).show();
            else if(sleepEndtime.getText().equals("WAKE TIME"))
                Toast.makeText(sleepingohio.this,"Please select your wake time", Toast.LENGTH_SHORT).show();
            else {

                SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN_AMPM); //, Locale.US
                long sleepStartTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepStarttime.getText().toString(), sdf);
                long sleepEndTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepEndtime.getText().toString(), sdf);

                Log.d(TAG, "SleepStartTime Long : "+sleepStartTimeRaw);
                Log.d(TAG, "SleepEndTime Long : "+sleepEndTimeRaw);

                SimpleDateFormat sdf2 = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN);
                sleepStartTimeRaw = ScheduleAndSampleManager.getTimeString(sleepStartTimeLong, sdf2);
                sleepEndTimeRaw = ScheduleAndSampleManager.getTimeString(sleepEndTimeLong, sdf2);

//                Log.d(TAG, "SleepingStartTime Raw : "+sleepStartTimeRaw);
//                Log.d(TAG, "SleepingEndTime Raw : "+sleepEndTimeRaw);

                boolean firstTimeEnterSleepTimePage = sharedPrefs.getBoolean("FirstTimeEnterSleepTimePage", false);

                if(firstTimeEnterSleepTimePage)
                    cancelAlarmsByResetSleepTime();

                sharedPrefs.edit().putBoolean("FirstTimeEnterSleepTimePage", true).apply();

                sharedPrefs.edit().putString("SleepingStartTime", sleepStartTimeRaw).apply();
                sharedPrefs.edit().putString("SleepingEndTime", sleepEndTimeRaw).apply();

                Utils.settingAllDaysIntervalSampling(getApplicationContext());

                Intent intent = new Intent(sleepingohio.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("SleepingStartTime", sleepStartTimeRaw);
                bundle.putString("SleepingEndTime", sleepEndTimeRaw);
                intent.putExtras(bundle);

                setResult(1, intent);

                sleepingohio.this.finish();

                startActivity(intent);
            }
        }
    };

    public void cancelAlarmsByResetSleepTime(){

        //cancel the alarm first
        int alarmTotal = sharedPrefs.getInt("alarmCount", 1);

        long currentTimeInMillis = ScheduleAndSampleManager.getCurrentTimeInMillis();

        SimpleDateFormat sdf_Now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String todayDate = ScheduleAndSampleManager.getTimeString(currentTimeInMillis, sdf_Now);

//        SimpleDateFormat sdf_noZone = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE);
//        String startTimeString = todayDate + " 00:00:00";
        long startTime = ScheduleAndSampleManager.getTimeInMillis(todayDate, sdf_Now);
        long tomorrowStartTime = startTime + Constants.MILLISECONDS_PER_DAY;

        int alarmStartFromTomor = -1;

        //find the day start to cancel the alarm
        for(int alarmCount = 1; alarmCount <= alarmTotal; alarmCount ++) {

            long time = sharedPrefs.getLong("alarm_time_" + alarmCount, -1);

            Log.d(TAG, "[test alarm] check alarm time "+alarmCount+" : "+ScheduleAndSampleManager.getTimeString(time));
            Log.d(TAG, "[test alarm] check tomorrowStartTime : "+ScheduleAndSampleManager.getTimeString(tomorrowStartTime));

            if(time > tomorrowStartTime){

                alarmStartFromTomor = alarmCount;
                break;
            }
        }

        //updated alarm Count
        sharedPrefs.edit().putInt("alarmCount", alarmStartFromTomor-1).apply();

        for(int alarmCount = alarmStartFromTomor; alarmCount <= alarmTotal; alarmCount ++) {

            int request_code = sharedPrefs.getInt("alarm_time_request_code" + alarmCount, -1);


            long time = sharedPrefs.getLong("alarm_time_" + alarmCount, -1);

            Log.d(TAG, "[test alarm] deleting time : "+ScheduleAndSampleManager.getTimeString(time));

            cancelAlarmIfExists(mContext, request_code);
        }
    }

    public static void cancelAlarmIfExists(Context mContext,int requestCode){

        try {

            Intent intent = new Intent(Constants.Interval_Sample);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent, PendingIntent.FLAG_NO_CREATE);
            AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {

        }
    }

    private Button.OnClickListener starttimeing = new Button.OnClickListener() {
        public void onClick(View v) {
            //Log.e(TAG,"sleepStarttime clicked");

            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            new CustomTimePickerDialog(sleepingohio.this, new TimePickerDialog.OnTimeSetListener(){
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                    String hour=String.valueOf(hourOfDay);
                    String min =String.valueOf(minute);
                    String am_pm;

                    if(hourOfDay < 12)
                        am_pm = "am";
                    else
                        am_pm = "pm";

                    if(hourOfDay<10)
                        hour = "0" + String.valueOf(hourOfDay);
                    else if(hourOfDay > 12) {
//                        hour = String.valueOf(hourOfDay - 12);
                        hourOfDay = hourOfDay - 12;

                        if(hourOfDay<10)
                            hour = "0" + String.valueOf(hourOfDay);
                        else
                            hour = String.valueOf(hourOfDay);

                    }

                    if(minute<10)
                        min = "0" + String.valueOf(minute);

                    sleepStarttime.setText( hour + ":" + min + " " + am_pm);

                }

            }, hour, minute, false).show();

        }
    };

    private Button.OnClickListener endtimeing = new Button.OnClickListener() {
        public void onClick(View v) {
            //Log.e(TAG,"sleepEndtime clicked");

            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            new CustomTimePickerDialog(sleepingohio.this, new TimePickerDialog.OnTimeSetListener(){
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                    String hour=String.valueOf(hourOfDay);
                    String min =String.valueOf(minute);
                    String am_pm;

                    if(hourOfDay < 12)
                        am_pm = "am";
                    else
                        am_pm = "pm";

                    if(hourOfDay<10)
                        hour = "0" + String.valueOf(hourOfDay);
                    else if(hourOfDay > 12) {
//                        hour = String.valueOf(hourOfDay - 12);
                        hourOfDay = hourOfDay - 12;

                        if(hourOfDay<10)
                            hour = "0" + String.valueOf(hourOfDay);
                        else
                            hour = String.valueOf(hourOfDay);

                    }
                    if(minute<10)
                        min = "0" + String.valueOf(minute);

                    sleepEndtime.setText( hour + ":" + min + " " + am_pm);

                }
            }, hour, minute, false).show();
        }
    };

}
