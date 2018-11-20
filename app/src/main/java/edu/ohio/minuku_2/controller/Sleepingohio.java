package edu.ohio.minuku_2.controller;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Locale;

import edu.ohio.minuku.Data.DBHelper;
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

    private String sleepStartTimeRaw, sleepEndTimeRaw, todayDate;

    private SharedPreferences sharedPrefs;

    private long sleepStartTimeLong= -999, sleepEndTimeLong= -999;

    public Sleepingohio(){}

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsleeptimepage);

        sharedPrefs = getSharedPreferences(Constants.sharedPrefString, MODE_PRIVATE);
        mContext = Sleepingohio.this;

        Log.d(TAG, "hasNavBar(this.getResources()) : "+hasNavBar(this.getResources()));

        if(hasNavBar(this.getResources())){

            hideNavigationBar();
        }

        initsettingohio();
    }

    public void onResume(){
        super.onResume();

        if(hasNavBar(this.getResources())){

            hideNavigationBar();
        }
    }

    public void initsettingohio(){

        sleepStarttime = (Button)findViewById(R.id.sleepStarttime);
        sleepStarttime.setOnClickListener(starttimeing);

        sleepEndtime = (Button)findViewById(R.id.sleepEndtime);
        sleepEndtime.setOnClickListener(endtimeing);

        sleepStartTimeLong = sharedPrefs.getLong("sleepStartTimeLong", sleepStartTimeLong);
        sleepEndTimeLong = sharedPrefs.getLong("sleepEndTimeLong", sleepEndTimeLong);

        if((sleepStartTimeLong != Constants.INVALID_IN_LONG) && (sleepEndTimeLong != Constants.INVALID_IN_LONG)){

            SimpleDateFormat sdf_hhmm_a = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN_AMPM, Locale.US);
            sleepStarttime.setText(ScheduleAndSampleManager.getTimeString(sleepStartTimeLong, sdf_hhmm_a));
            sleepEndtime.setText(ScheduleAndSampleManager.getTimeString(sleepEndTimeLong, sdf_hhmm_a));
        }

        confirm = (Button)findViewById(R.id.confirm);
        confirm.setOnClickListener(confirming);


        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.actionbar_new);

        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        todayDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis(), sdf_date);

    }

    public boolean onKeyDown(int keyCode, KeyEvent event)  {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {

            Log.d(TAG, "onKeyDown");

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            Sleepingohio.this.finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {

            hideNavigationBar();
        }
    }

    public boolean hasNavBar(Resources resources) {

        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        return id > 0 && resources.getBoolean(id);
    }

    private void hideNavigationBar() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private long getSleepingRange(long sleepStartTimeCheck, long sleepEndTimeCheck) {

        long sleepingRange = sleepEndTimeCheck - sleepStartTimeCheck;

        if (sleepingRange < 0) {

            sleepingRange += Constants.MILLISECONDS_PER_DAY;

            //TODO check
            sharedPrefs.edit().putBoolean("WakeSleepDateIsSame", false).apply();
        }else {

            sharedPrefs.edit().putBoolean("WakeSleepDateIsSame", true).apply();
        }

        return sleepingRange;
    }

    private Button.OnClickListener confirming = new Button.OnClickListener() {
        public void onClick(View v) {

            Log.d(TAG, "sleepStarttime from button : " + sleepStarttime.getText());
            Log.d(TAG, "sleepEndtime from button : " + sleepEndtime.getText());

            //Get date
//            SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
//            String todayDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis(), sdf_date);
//            String tomorrDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis() + Constants.MILLISECONDS_PER_DAY, sdf_date);

            String sleepStartTimeAMPM = sleepStarttime.getText().toString().split(" ")[1].toUpperCase();
            String sleepEndTimeAMPM = sleepEndtime.getText().toString().split(" ")[1].toUpperCase();

            Log.d(TAG, "sleepStartTimeAMPM : " + sleepStartTimeAMPM);
            Log.d(TAG, "sleepEndTimeAMPM : " + sleepEndTimeAMPM);

            /*if(sleepStartTimeAMPM.equals(sleepEndTimeAMPM)){

                sharedPrefs.edit().putBoolean("WakeSleepDateIsSame", true).apply();
            }else {

                sharedPrefs.edit().putBoolean("WakeSleepDateIsSame", false).apply();
            }*/

            long sleepStartTimeCheck = sleepStartTimeLong, sleepEndTimeCheck = sleepEndTimeLong;

            //TODO replace the code below
            long sleepingRange = getSleepingRange(sleepStartTimeLong, sleepEndTimeLong);

            Log.d(TAG, "sleepStartTimeCheck : " + sleepStartTimeCheck);
            Log.d(TAG, "sleepEndTimeCheck : " + sleepEndTimeCheck);

            if (sleepStarttime.getText().equals("BED TIME")){

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Confirm sleeping time - Fail - Bed Time haven't been set.");

                Toast.makeText(Sleepingohio.this, getResources().getString(R.string.reminder_bedtime_check), Toast.LENGTH_SHORT).show();
            } else if(sleepEndtime.getText().equals("WAKE TIME")) {

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Confirm sleeping time - Fail - Wake up Time haven't been set.");

                Toast.makeText(Sleepingohio.this, getResources().getString(R.string.reminder_wakeuptime_check), Toast.LENGTH_SHORT).show();
            } else if(sleepingRange > Constants.sleepTime_UpperBound * Constants.MILLISECONDS_PER_HOUR || sleepingRange < Constants.sleepTime_LowerBound * Constants.MILLISECONDS_PER_HOUR){

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Confirm sleeping time - Fail - Sleeping Time is not between 3 and 12 hours.");

                Toast.makeText(Sleepingohio.this,"Please check your bed and wake times. Your sleep schedule must be between " +
                        Constants.sleepTime_LowerBound +" and "+Constants.sleepTime_UpperBound+" hours long.", Toast.LENGTH_SHORT).show();
            }
            else {

                Log.d(TAG, "SleepStartTime Long : "+sleepStartTimeLong);
                Log.d(TAG, "SleepEndTime Long : "+sleepEndTimeLong);

                SimpleDateFormat sdf2 = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN);
                sleepStartTimeRaw = ScheduleAndSampleManager.getTimeString(sleepStartTimeLong, sdf2);
                sleepEndTimeRaw = ScheduleAndSampleManager.getTimeString(sleepEndTimeLong, sdf2);

                Log.d(TAG, "SleepingStartTime Raw : "+sleepStartTimeRaw);
                Log.d(TAG, "SleepingEndTime Raw : "+sleepEndTimeRaw);

                //TODO if the hour and min didn't changed, don't reset the overview interval sample times

                String previousSleepingStartTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER);
                String previousSleepingEndTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER);
                Log.d(TAG, "previousSleepingStartTime : "+ previousSleepingStartTime);
                Log.d(TAG, "previousSleepingEndTime : "+ previousSleepingEndTime);

                sharedPrefs.edit().putString("SleepingStartTime", sleepStartTimeRaw).apply();
                sharedPrefs.edit().putString("SleepingEndTime", sleepEndTimeRaw).apply();

                sharedPrefs.edit().putLong("sleepStartTimeLong", sleepStartTimeLong).apply();
                sharedPrefs.edit().putLong("sleepEndTimeLong", sleepEndTimeLong).apply();

                //check if the current time is over the sleep time
                if(ScheduleAndSampleManager.getCurrentTimeInMillis() >= sleepStartTimeLong){

                    sharedPrefs.edit().putLong("nextSleepTime", sleepStartTimeLong + Constants.MILLISECONDS_PER_DAY).apply();
                }else {

                    sharedPrefs.edit().putLong("nextSleepTime", sleepStartTimeLong).apply();
                }


                if(sleepStartTimeRaw.equals(previousSleepingStartTime) && sleepEndTimeRaw.equals(previousSleepingEndTime)){

                    Log.d(TAG, "previousSleepingTime is same as the current one, don't reset the survey times");
                }else {

                    boolean firstTimeEnterSleepTimePage = sharedPrefs.getBoolean("FirstTimeEnterSleepTimePage", false);

                    if(firstTimeEnterSleepTimePage)
                        cancelAlarmsByResetSleepTime();

                    sharedPrefs.edit().putBoolean("FirstTimeEnterSleepTimePage", true).apply();
                    Utils.settingAllDaysIntervalSampling(getApplicationContext());
                }

                Intent intent = new Intent(Sleepingohio.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("SleepingStartTime", sleepStartTimeRaw);
                bundle.putString("SleepingEndTime", sleepEndTimeRaw);
                intent.putExtras(bundle);

                setResult(1, intent);

                DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Confirm sleeping time - success");

                Toast.makeText(Sleepingohio.this, getResources().getString(R.string.reminder_sleeptime_changed), Toast.LENGTH_SHORT).show();

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

            Intent intent = new Intent(Constants.INTERVAL_SAMPLE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent, PendingIntent.FLAG_NO_CREATE);
            AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {

        }
    }

    private int getIndex(Spinner spinner, String myString){

        int index;

        for (int i=0;i<spinner.getCount();i++){

            if (spinner.getItemAtPosition(i).equals(myString)){

                index = i;
                return index;
            }
        }

        return 0;
    }

    private Button.OnClickListener starttimeing = new Button.OnClickListener() {
        public void onClick(View v) {

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Setting bed time");

            final LayoutInflater inflater = LayoutInflater.from(mContext);
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final View layout = inflater.inflate(R.layout.timesetting_dialog,null);

            final Spinner spinner_hour = (Spinner) layout.findViewById(R.id.spinner_hour);
            final Spinner spinner_min = (Spinner) layout.findViewById(R.id.spinner_min);
            final Spinner spinner_ampm = (Spinner) layout.findViewById(R.id.spinner_ampm);

            final String[] hour_options = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
            final String[] min_options = {"00", "30"};
            final String[] ampm_options = {"am", "pm"};

            final ArrayAdapter<String> hourList = new ArrayAdapter<>(mContext,
                    android.R.layout.simple_spinner_dropdown_item,
                    hour_options);

            final ArrayAdapter<String> minList = new ArrayAdapter<>(mContext,
                    android.R.layout.simple_spinner_dropdown_item,
                    min_options);

            final ArrayAdapter<String> ampmList = new ArrayAdapter<>(mContext,
                    android.R.layout.simple_spinner_dropdown_item,
                    ampm_options);

            spinner_hour.setAdapter(hourList);
            spinner_min.setAdapter(minList);
            spinner_ampm.setAdapter(ampmList);

            SimpleDateFormat sdf_hh = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_small);
            SimpleDateFormat sdf_mm = new SimpleDateFormat(Constants.DATE_FORMAT_MIN);
            SimpleDateFormat sdf_a = new SimpleDateFormat(Constants.DATE_FORMAT_AMPM);

            long currentTime = ScheduleAndSampleManager.getCurrentTimeInMillis();

            if(sleepStartTimeLong != Constants.INVALID_IN_LONG)
                currentTime = sleepStartTimeLong;

            String currentHour = ScheduleAndSampleManager.getTimeString(currentTime, sdf_hh);
            String currentMin = ScheduleAndSampleManager.getTimeString(currentTime, sdf_mm);
            String currentAmpm = ScheduleAndSampleManager.getTimeString(currentTime, sdf_a);

            //set the closest time in minutes
            int currentMinInt = Integer.valueOf(currentMin);
            String exactMin;

            //if closer to "30"
            if(currentMinInt > 15 && currentMinInt <= 45)
                exactMin = "30";
            else
                exactMin = "00";

            Log.d(TAG, "currentHour : "+currentHour);
            Log.d(TAG, "currentMin : "+currentMin);
            Log.d(TAG, "exactMin : "+exactMin);
            Log.d(TAG, "currentAmpm : "+currentAmpm);
            Log.d(TAG, "currentAmpm toLowerCase : "+currentAmpm.toLowerCase());

            Log.d(TAG, "getIndex(spinner_hour, exactHour) : "+ getIndex(spinner_hour, currentHour));
            Log.d(TAG, "getIndex(spinner_min, currentMin) : "+ getIndex(spinner_min, exactMin));
            Log.d(TAG, "getIndex(spinner_ampm, currentAmpm) : "+ getIndex(spinner_ampm, currentAmpm.toLowerCase()));

            spinner_hour.setSelection(getIndex(spinner_hour, currentHour));
            spinner_min.setSelection(getIndex(spinner_min, exactMin));
            spinner_ampm.setSelection(getIndex(spinner_ampm, currentAmpm.toLowerCase()));

            builder.setView(layout)
                    .setPositiveButton(R.string.ok, null)
                    .setNegativeButton("cancel", null);

            final AlertDialog mAlertDialog = builder.create();
            mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(final DialogInterface dialogInterface) {

                    Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Confirm bed time");

                            //set spinners with the current time
                            String selectedHour = spinner_hour.getSelectedItem().toString();
                            String selectedMin = spinner_min.getSelectedItem().toString();
                            String selectedAmpm = spinner_ampm.getSelectedItem().toString();

                            String hour=String.valueOf(selectedHour);
                            String min =String.valueOf(selectedMin);
                            String am_pm = selectedAmpm;

                            Log.d(TAG, "starttime raw hour : "+hour);
                            Log.d(TAG, "starttime raw min : "+min);

                            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN_AMPM, Locale.US);
                            long sleepStartTimeToCheck = ScheduleAndSampleManager.getTimeInMillis(todayDate+" "+hour+":"+min+" "+am_pm, sdf);

                            Log.d(TAG, "sleepStartTimeToCheck Long : "+sleepStartTimeToCheck);
                            Log.d(TAG, "sleepStartTimeToCheck String : "+ScheduleAndSampleManager.getTimeString(sleepStartTimeToCheck));

                            if(am_pm.equals("am")){

                                if(hour.equals("00")){

                                    hour = "12";
                                }
                            }

                            long bedLowerBoundTime = getBedLowerBoundTime(todayDate);
                            long bedUpperBoundTime = getBedUpperBoundTime(todayDate);

                            if(sleepStartTimeToCheck > bedUpperBoundTime && sleepStartTimeToCheck < bedLowerBoundTime){

                                Toast.makeText(Sleepingohio.this, getResources().getString(R.string.reminder_bedtime_range_error), Toast.LENGTH_LONG).show();
                            }else {

                                sleepStartTimeLong = sleepStartTimeToCheck;

                                sleepStarttime.setText(hour + ":" + min + " " + am_pm);

                                dialogInterface.dismiss();
                            }
                        }
                    });
                }
            });

            mAlertDialog.show();
        }
    };

    private Button.OnClickListener endtimeing = new Button.OnClickListener() {
        public void onClick(View v) {

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Setting wake up time");

            final LayoutInflater inflater = LayoutInflater.from(mContext);
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final View layout = inflater.inflate(R.layout.timesetting_dialog,null);

            final Spinner spinner_hour = (Spinner) layout.findViewById(R.id.spinner_hour);
            final Spinner spinner_min = (Spinner) layout.findViewById(R.id.spinner_min);
            final Spinner spinner_ampm = (Spinner) layout.findViewById(R.id.spinner_ampm);

            final String[] hour_options = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
            final String[] min_options = {"00", "30"};
            final String[] ampm_options = {"am", "pm"};

            final ArrayAdapter<String> hourList = new ArrayAdapter<>(mContext,
                    android.R.layout.simple_spinner_dropdown_item,
                    hour_options);

            final ArrayAdapter<String> minList = new ArrayAdapter<>(mContext,
                    android.R.layout.simple_spinner_dropdown_item,
                    min_options);

            final ArrayAdapter<String> ampmList = new ArrayAdapter<>(mContext,
                    android.R.layout.simple_spinner_dropdown_item,
                    ampm_options);

            spinner_hour.setAdapter(hourList);
            spinner_min.setAdapter(minList);
            spinner_ampm.setAdapter(ampmList);

            SimpleDateFormat sdf_hh = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_small);
            SimpleDateFormat sdf_mm = new SimpleDateFormat(Constants.DATE_FORMAT_MIN);
            SimpleDateFormat sdf_a = new SimpleDateFormat(Constants.DATE_FORMAT_AMPM);

            long currentTime = ScheduleAndSampleManager.getCurrentTimeInMillis();

            if(sleepEndTimeLong != Constants.INVALID_IN_LONG)
                currentTime = sleepEndTimeLong;

            String currentHour = ScheduleAndSampleManager.getTimeString(currentTime, sdf_hh);
            String currentMin = ScheduleAndSampleManager.getTimeString(currentTime, sdf_mm);
            String currentAmpm = ScheduleAndSampleManager.getTimeString(currentTime, sdf_a);

            //set the closest time in minutes
            int currentMinInt = Integer.valueOf(currentMin);
            String exactMin;

            //if closer to "30"
            if(currentMinInt > 15 && currentMinInt <= 45)
                exactMin = "30";
            else
                exactMin = "00";

            Log.d(TAG, "currentHour : "+currentHour);
            Log.d(TAG, "currentMin : "+currentMin);
            Log.d(TAG, "exactMin : "+exactMin);
            Log.d(TAG, "currentAmpm : "+currentAmpm);
            Log.d(TAG, "currentAmpm toLowerCase : "+currentAmpm.toLowerCase());

            Log.d(TAG, "getIndex(spinner_hour, exactHour) : "+ getIndex(spinner_hour, currentHour));
            Log.d(TAG, "getIndex(spinner_min, currentMin) : "+ getIndex(spinner_min, exactMin));
            Log.d(TAG, "getIndex(spinner_ampm, currentAmpm) : "+ getIndex(spinner_ampm, currentAmpm.toLowerCase()));

            spinner_hour.setSelection(getIndex(spinner_hour, currentHour));
            spinner_min.setSelection(getIndex(spinner_min, exactMin));
            spinner_ampm.setSelection(getIndex(spinner_ampm, currentAmpm.toLowerCase()));

            builder.setView(layout)
                    .setPositiveButton(R.string.ok, null)
                    .setNegativeButton("cancel", null);

            final AlertDialog mAlertDialog = builder.create();
            mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(final DialogInterface dialogInterface) {

                    Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Confirm wake up time");

                            //set spinners with the current time
                            String selectedHour = spinner_hour.getSelectedItem().toString();
                            String selectedMin = spinner_min.getSelectedItem().toString();
                            String selectedAmpm = spinner_ampm.getSelectedItem().toString();

                            String hour = String.valueOf(selectedHour);
                            String min  = String.valueOf(selectedMin);
                            String am_pm = selectedAmpm;

                            Log.d(TAG, "endtime raw hour : "+hour);
                            Log.d(TAG, "endtime raw min : "+min);

                            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN_AMPM, Locale.US);
                            long sleepEndTimeToCheck = ScheduleAndSampleManager.getTimeInMillis(todayDate+" "+hour+":"+min+" "+am_pm, sdf);

                            Log.d(TAG, "SleepEndTime Long : "+ScheduleAndSampleManager.getTimeString(sleepEndTimeToCheck));

                            if(am_pm.equals("am")){

                                if(hour.equals("00")){

                                    hour = "12";
                                }
                            }

                            long wakeUpUpperBoundTime = getWakeUpUpperBoundTime(todayDate);
                            long wakeUpLowerBoundTime = getWakeUpLowerBoundTime(todayDate);
                            Log.d(TAG, "WakeUpUpperBoundTime Long : "+ScheduleAndSampleManager.getTimeString(wakeUpUpperBoundTime));
                            Log.d(TAG, "WakeUpLowerBoundTime Long : "+ScheduleAndSampleManager.getTimeString(wakeUpLowerBoundTime));

                            if(sleepEndTimeToCheck > wakeUpUpperBoundTime || sleepEndTimeToCheck < wakeUpLowerBoundTime){

                                Toast.makeText(Sleepingohio.this, getResources().getString(R.string.reminder_wakeuptime_range_error), Toast.LENGTH_SHORT).show();
                            }else {

                                sleepEndTimeLong = sleepEndTimeToCheck;

                                sleepEndtime.setText(hour + ":" + min + " " + am_pm);

                                dialogInterface.dismiss();
                            }
                        }
                    });
                };
            });

            mAlertDialog.show();
        }
    };

    private long getWakeUpUpperBoundTime(String todayDate){

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN_AMPM, Locale.US);
        long wakeUpUpperBoundTime = ScheduleAndSampleManager.getTimeInMillis(todayDate+" 12:00 pm", sdf);

        return wakeUpUpperBoundTime;
    }

    private long getWakeUpLowerBoundTime(String todayDate){

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN_AMPM, Locale.US);
        long wakeUpLowerBoundTime = ScheduleAndSampleManager.getTimeInMillis(todayDate+" 04:00 am", sdf);

        return wakeUpLowerBoundTime;
    }

    private long getBedUpperBoundTime(String todayDate){

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN_AMPM, Locale.US);
        long wakeUpUpperBoundTime = ScheduleAndSampleManager.getTimeInMillis(todayDate+" 04:00 am", sdf);

        return wakeUpUpperBoundTime;
    }

    private long getBedLowerBoundTime(String todayDate){

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_HOUR_MIN_AMPM, Locale.US);
        long wakeUpLowerBoundTime = ScheduleAndSampleManager.getTimeInMillis(todayDate+" 08:00 pm", sdf);

        return wakeUpLowerBoundTime;
    }



}
