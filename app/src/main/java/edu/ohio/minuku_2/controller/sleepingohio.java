package edu.ohio.minuku_2.controller;

import android.app.ActionBar;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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


    public sleepingohio(){}

    public sleepingohio(Context mContext){
        this.mContext = mContext;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settingsleeptimepage);

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
            //Log.e(TAG, "confirm clicked");
            if(sleepStarttime.getText().equals("Please select your sleeping time"))
                Toast.makeText(sleepingohio.this,"Please select your sleeping time!!", Toast.LENGTH_SHORT).show();
            else if(sleepEndtime.getText().equals("Please select your wake up time"))
                Toast.makeText(sleepingohio.this,"Please select your wake up time!!", Toast.LENGTH_SHORT).show();
            else {

                SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN_AMPM);
                long sleepStartTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepStarttime.getText().toString(), sdf);
                long sleepEndTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepEndtime.getText().toString(), sdf);

                SimpleDateFormat sdf2 = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN);
                sleepStartTimeRaw = ScheduleAndSampleManager.getTimeString(sleepStartTimeLong, sdf2);
                sleepEndTimeRaw = ScheduleAndSampleManager.getTimeString(sleepEndTimeLong, sdf2);

                SharedPreferences.Editor editor = getSharedPreferences(Constants.sharedPrefString, MODE_PRIVATE).edit();
                editor.putString("SleepingStartTime", sleepStartTimeRaw);
                editor.putString("SleepingEndTime", sleepEndTimeRaw);
                editor.commit();

                Utils.settingAllDaysIntervalSampling(getApplicationContext());

//                Intent intent = getIntent();
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

//                    sleepEndTimeRaw = hour + ":" + min;

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

//                    sleepStartTimeRaw = hour + ":" + min;

                    sleepEndtime.setText( hour + ":" + min + " " + am_pm);

                }
            }, hour, minute, false).show();
        }
    };

}
