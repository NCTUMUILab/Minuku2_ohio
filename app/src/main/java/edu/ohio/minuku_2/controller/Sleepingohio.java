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

public class Sleepingohio extends AppCompatActivity {

    final String TAG = "Sleepingohio";

    private Context mContext;
    private Button sleepStarttime, sleepEndtime, confirm;
    private final static int TIME_PICKER_INTERVAL = 30;

    private String sleepStartTimeRaw, sleepEndTimeRaw, todayDate;

    private SharedPreferences sharedPrefs;

    private long sleepStartTimeLong= -999, sleepEndTimeLong= -999;
    private String sleepStartTimeHour = "", sleepEndTimeHour = "";
    private String sleepStartTimeMin = "", sleepEndTimeMin = "";

    public Sleepingohio(){}

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

        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        todayDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis(), sdf_date);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            Sleepingohio.this.finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private Button.OnClickListener confirming = new Button.OnClickListener() {
        public void onClick(View v) {

            Log.d(TAG, "sleepStarttime from button : "+sleepStarttime.getText());
            Log.d(TAG, "sleepEndtime from button : "+sleepEndtime.getText());

            //Get date
            SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
            String todayDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis(), sdf_date);
            String tomorrDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis()+Constants.MILLISECONDS_PER_DAY, sdf_date);

            String sleepStartTimeAMPM = sleepStarttime.getText().toString().split(" ")[1];
            String sleepEndTimeAMPM = sleepEndtime.getText().toString().split(" ")[1];

            Log.d(TAG, "sleepStartTimeAMPM : "+sleepStartTimeAMPM);
            Log.d(TAG, "sleepEndTimeAMPM : "+sleepEndTimeAMPM);

            long sleepStartTimeCheck = -999, sleepEndTimeCheck = -999;

            if(!sleepStartTimeAMPM.equals(sleepEndTimeAMPM)){

                //we add the date to get the accurate range
                SimpleDateFormat sdf_ = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN); //, Locale.US
                sleepStartTimeCheck = ScheduleAndSampleManager.getTimeInMillis(todayDate+" "+sleepStartTimeHour+":"+sleepStartTimeMin, sdf_);
                sleepEndTimeCheck = ScheduleAndSampleManager.getTimeInMillis(tomorrDate+" "+sleepEndTimeHour+":"+sleepEndTimeMin, sdf_);

                sharedPrefs.edit().putBoolean("WakeSleepDateIsSame", false).apply();
            }else {

                SimpleDateFormat sdf_ = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN);
                sleepStartTimeCheck = ScheduleAndSampleManager.getTimeInMillis(todayDate+" "+sleepStartTimeHour+":"+sleepStartTimeMin, sdf_);
                sleepEndTimeCheck = ScheduleAndSampleManager.getTimeInMillis(todayDate+" "+sleepEndTimeHour+":"+sleepEndTimeMin, sdf_);

                sharedPrefs.edit().putBoolean("WakeSleepDateIsSame", true).apply();
            }

            Log.d(TAG, "sleepStartTimeCheck : "+sleepStartTimeCheck);
            Log.d(TAG, "sleepEndTimeCheck : "+sleepEndTimeCheck);

            if(sleepStarttime.getText().equals("BED TIME"))
                Toast.makeText(Sleepingohio.this,"Please select your bed time", Toast.LENGTH_SHORT).show();
            else if(sleepEndtime.getText().equals("WAKE TIME"))
                Toast.makeText(Sleepingohio.this,"Please select your wake time", Toast.LENGTH_SHORT).show();
            else if((sleepEndTimeCheck - sleepStartTimeCheck) > 12 * Constants.MILLISECONDS_PER_HOUR ||
                    (sleepEndTimeCheck - sleepStartTimeCheck) < 3 * Constants.MILLISECONDS_PER_HOUR){

                Toast.makeText(Sleepingohio.this,"Please check your bed and wake times. Your sleep schedule must be between 3 and 12 hours long.", Toast.LENGTH_SHORT).show();
            }
            else {

//                SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN_AMPM); //, Locale.US
//                long sleepStartTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepStarttime.getText().toString(), sdf);
//                long sleepEndTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepEndtime.getText().toString(), sdf);

                Log.d(TAG, "SleepStartTime Long : "+sleepStartTimeLong);
                Log.d(TAG, "SleepEndTime Long : "+sleepEndTimeLong);

                SimpleDateFormat sdf2 = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN);
                sleepStartTimeRaw = ScheduleAndSampleManager.getTimeString(sleepStartTimeLong, sdf2);
                sleepEndTimeRaw = ScheduleAndSampleManager.getTimeString(sleepEndTimeLong, sdf2);

                Log.d(TAG, "SleepingStartTime Raw : "+sleepStartTimeRaw);
                Log.d(TAG, "SleepingEndTime Raw : "+sleepEndTimeRaw);

                boolean firstTimeEnterSleepTimePage = sharedPrefs.getBoolean("FirstTimeEnterSleepTimePage", false);

                if(firstTimeEnterSleepTimePage)
                    cancelAlarmsByResetSleepTime();

                sharedPrefs.edit().putBoolean("FirstTimeEnterSleepTimePage", true).apply();

                sharedPrefs.edit().putString("SleepingStartTime", sleepStartTimeRaw).apply();
                sharedPrefs.edit().putString("SleepingEndTime", sleepEndTimeRaw).apply();

                Utils.settingAllDaysIntervalSampling(getApplicationContext());

                Intent intent = new Intent(Sleepingohio.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("SleepingStartTime", sleepStartTimeRaw);
                bundle.putString("SleepingEndTime", sleepEndTimeRaw);
                intent.putExtras(bundle);

                setResult(1, intent);

                Sleepingohio.this.finish();

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
            new CustomTimePickerDialog(Sleepingohio.this, new TimePickerDialog.OnTimeSetListener(){
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

                    if(minute<10)
                        min = "0" + String.valueOf(minute);

                    Log.d(TAG, "starttime raw hour : "+hour);
                    Log.d(TAG, "starttime raw min : "+min);

                    sleepStartTimeHour = hour;
                    sleepStartTimeMin = min;

                    SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN);
                    sleepStartTimeLong = ScheduleAndSampleManager.getTimeInMillis(todayDate+" "+hour+":"+min, sdf);

                    Log.d(TAG, "SleepStartTime Long : "+sleepStartTimeLong);
                    Log.d(TAG, "SleepEndTime Long : "+ScheduleAndSampleManager.getTimeString(sleepStartTimeLong));


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

                    if(am_pm.equals("am")){

                        if(hour.equals("00")){

                            hour = "12";
                        }
                    }

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
            new CustomTimePickerDialog(Sleepingohio.this, new TimePickerDialog.OnTimeSetListener(){
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                    String hour=String.valueOf(hourOfDay);
                    String min =String.valueOf(minute);
                    String am_pm;

                    if(hourOfDay < 12)
                        am_pm = "am";
                    else
                        am_pm = "pm";

                    if(hourOfDay < 10)
                        hour = "0" + String.valueOf(hourOfDay);

                    if(minute < 10)
                        min = "0" + String.valueOf(minute);

                    Log.d(TAG, "endtime raw hour : "+hour);
                    Log.d(TAG, "endtime raw min : "+min);

                    sleepEndTimeHour = hour;
                    sleepEndTimeMin = min;

                    SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN); //, Locale.US
                    sleepEndTimeLong = ScheduleAndSampleManager.getTimeInMillis(todayDate+" "+hour+":"+min, sdf);

                    Log.d(TAG, "SleepEndTime Long : "+sleepEndTimeLong);
                    Log.d(TAG, "SleepEndTime Long : "+ScheduleAndSampleManager.getTimeString(sleepEndTimeLong));


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

                    if(am_pm.equals("am")){

                        if(hour.equals("00")){

                            hour = "12";
                        }
                    }

                    sleepEndtime.setText( hour + ":" + min + " " + am_pm);

                }
            }, hour, minute, false).show();
        }
    };

}
