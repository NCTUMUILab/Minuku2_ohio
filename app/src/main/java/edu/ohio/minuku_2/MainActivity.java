/*
 * Copyright (c) 2016.
 *
 * DReflect and Minuku Libraries by Shriti Raj (shritir@umich.edu) and Neeraj Kumar(neerajk@uci.edu) is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.
 * Based on a work at https://github.com/Shriti-UCI/Minuku-2.
 *
 *
 * You are free to (only if you meet the terms mentioned below) :
 *
 * Share — copy and redistribute the material in any medium or format
 * Adapt — remix, transform, and build upon the material
 *
 * The licensor cannot revoke these freedoms as long as you follow the license terms.
 *
 * Under the following terms:
 *
 * Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
 * NonCommercial — You may not use the material for commercial purposes.
 * ShareAlike — If you remix, transform, or build upon the material, you must distribute your contributions under the same license as the original.
 * No additional restrictions — You may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.
 */

package edu.ohio.minuku_2;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.event.DecrementLoadingProcessCountEvent;
import edu.ohio.minuku.event.IncrementLoadingProcessCountEvent;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku_2.controller.Ohio.SurveyActivity;
import edu.ohio.minuku_2.controller.Ohio.recordinglistohio;
import edu.ohio.minuku_2.controller.Ohio.sleepingohio;
import edu.ohio.minuku_2.controller.home;
import edu.ohio.minuku_2.controller.report;
import edu.ohio.minuku_2.service.BackgroundService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    //private TextView compensationMessage;
    private AtomicInteger loadingProcessCount = new AtomicInteger(0);
    private ProgressDialog loadingProgressDialog;

    private int mYear, mMonth, mDay;

    public static String task="PART"; //default is PART
    ArrayList viewList;
    public final int REQUEST_ID_MULTIPLE_PERMISSIONS=1;
    public static View timerview,recordview,deviceIdview;

    public static android.support.design.widget.TabLayout mTabs;
    public static ViewPager mViewPager;

    private TextView device_id;
    private TextView num_6_digit;
    private TextView user_id;
    private TextView sleepingtime;

    private ImageView tripStatus;
    private ImageView surveyStatus;

    private Button ohio_setting, ohio_annotate, startservice, tolinkList;
    private String projName = "Ohio";

    private int requestCode_setting = 1;
    private Bundle requestCode_annotate;

    private boolean firstTimeToShowDialogOrNot;
    private SharedPreferences sharedPrefs;

    private Handler handler;

    private ScheduledExecutorService mScheduledExecutorService;
    public static final int REFRESH_FREQUENCY = 3; //10s, 10000ms
    public static final int BACKGROUND_RECORDING_INITIAL_DELAY = 0;
    //private UserSubmissionStats mUserSubmissionStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Creating Main activity");

        Log.e(TAG,"start");

        //** Please set your project name. **//
        whichView(projName);

        handler = new Handler();

        //TODO moving out
        startService(new Intent(getBaseContext(), BackgroundService.class));
        //startService(new Intent(getBaseContext(), MinukuNotificationManager.class));
        //startService(new Intent(getBaseContext(), DiaryNotificationService.class));  might be useless for us

        EventBus.getDefault().register(this);

        int sdk_int = Build.VERSION.SDK_INT;
        if(sdk_int>=23) {
            checkAndRequestPermissions();
        }else{
            startServiceWork();
        }

    }

    public void createShortCut(){
        Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        shortcutintent.putExtra("duplicate", false);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.dms_icon); //TODO change the icon with the Ohio one.
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(getApplicationContext(), SplashScreen.class));
        sendBroadcast(shortcutintent);
    }


    public static String getTimeString(long time){

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_SLASH);
        String currentTimeString = sdf_now.format(time);

        return currentTimeString;
    }

    public void getStartDate(){
        //get timzone
//        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        int Year = cal.get(Calendar.YEAR);
        int Month = cal.get(Calendar.MONTH)+1;
        int Day = cal.get(Calendar.DAY_OF_MONTH);

        int Hour = cal.get(Calendar.HOUR_OF_DAY);
        int Min = cal.get(Calendar.MINUTE);
        Log.d(TAG, "Year : "+Year+" Month : "+Month+" Day : "+Day+" Hour : "+Hour+" Min : "+Min);

        Constants.TaskDayCount = 0; //increase in checkfamiliarornotservice

//        Day++; //TODO start the task tomorrow.

        sharedPrefs.edit().putInt("StartYear", Year).apply();
        sharedPrefs.edit().putInt("StartMonth", Month).apply();
        sharedPrefs.edit().putInt("StartDay", Day).apply();

        sharedPrefs.edit().putInt("StartHour", Hour).apply();
        sharedPrefs.edit().putInt("StartMin", Min).apply();

        sharedPrefs.edit().putInt("TaskDayCount", Constants.TaskDayCount).apply();

        Log.d(TAG, "Start Year : " + Year + " Month : " + Month + " Day : " + Day + " TaskDayCount : " + Constants.TaskDayCount);

    }



//TODO IInputConnectionWrapper shaking alertdialog

    private void whichView(String projName){
        if(projName.equals("mobilecrowdsourcing")){
            setContentView(R.layout.activity_main);

            final LayoutInflater mInflater = getLayoutInflater().from(this);
            timerview = mInflater.inflate(R.layout.home, null);
            recordview = mInflater.inflate(R.layout.record, null);

            initViewPager(timerview,recordview);

            SettingViewPager();
        }else if(projName.equals("Ohio")){

//            requestCode_setting = new Bundle();
            requestCode_annotate = new Bundle();

            mScheduledExecutorService = Executors.newScheduledThreadPool(REFRESH_FREQUENCY);

            setContentView(R.layout.onlydeviceid); //not "onlydeviceid" actually...

            /* alertdialog for checking userid */
            sharedPrefs = getSharedPreferences("edu.umich.minuku_2", MODE_PRIVATE);

            firstTimeToShowDialogOrNot = sharedPrefs.getBoolean("firstTimeToShowDialogOrNot", true);
            Log.d(TAG,"firstTimeToShowDialogOrNot : "+firstTimeToShowDialogOrNot);

            Constants.USER_ID = sharedPrefs.getString("userid","NA");

            if(firstTimeToShowDialogOrNot) {
                createShortCut(); //on home Screen Desktop
                getStartDate();
                showdialogforuser();
            }else if(Constants.USER_ID.equals("NA")){
                showdialogforuser();
            }

            user_id = (TextView) findViewById(R.id.userid);
            Constants.USER_ID = sharedPrefs.getString("userid","NA");
            user_id.setText("Confirmation #:" );

            num_6_digit = (TextView) findViewById(R.id.group_num);
            Constants.GROUP_NUM =  sharedPrefs.getString("groupNum","NA");
            num_6_digit.setText(Constants.USER_ID); //Constants.GROUP_NUM +

            Constants.TaskDayCount = sharedPrefs.getInt("TaskDayCount",0);

            //button
            tolinkList = (Button) findViewById(R.id.linkList);
            tolinkList.setOnClickListener(new Button.OnClickListener(){

                @Override
                public void onClick(View view) {
//                    startActivity(new Intent(MainActivity.this, SurveyActivity.class));
                    startActivityForResult(new Intent(MainActivity.this, SurveyActivity.class), 2);

                }
            });

            ohio_setting = (Button)findViewById(R.id.Setting);
            ohio_setting.setOnClickListener(ohio_settinging);

            ohio_annotate = (Button)findViewById(R.id.Annotate);
            ohio_annotate.setOnClickListener(ohio_annotateing);

            /*startservice = (Button)findViewById(R.id.service);
            startservice.setOnClickListener(new Button.OnClickListener(){

                @Override
                public void onClick(View v) {

                    //Maybe useless in this project.
//                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));  // 協助工具

                    Intent intent1 = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);  //usage
                    startActivity(intent1);

//                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS); //notification
//                    startActivity(intent);

                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));	//location

                }

            });*/


//            tripStatus = (ImageView)findViewById(R.id.tripstatus);
//            tripStatus.setImageResource(R.drawable.circle_green);
//            ((GradientDrawable) tripStatus.getBackground()).setColor(Color.GREEN);
//
//            surveyStatus = (ImageView)findViewById(R.id.surveystatus);
//            surveyStatus.setImageResource(R.drawable.circle_green);

//            ((GradientDrawable) surveyStatus.getBackground()).setColor(Color.GREEN);

            //setting for the red green light
            boolean tripstatusfromList = sharedPrefs.getBoolean("TripStatus", false);
            boolean surveystatusfromList = sharedPrefs.getBoolean("SurveyStatus", false);

            Log.d(TAG,"tripstatusfromList : " + tripstatusfromList + " , surveystatusfromList : " + surveystatusfromList);

//          tolinkList ohio_annotate

            Drawable drawable_red = getResources().getDrawable(R.drawable.circle_red);
            drawable_red.setBounds(0, 0, (int)(drawable_red.getIntrinsicWidth()*0.5), (int)(drawable_red.getIntrinsicHeight()*0.5));
//            ScaleDrawable sd_red = new ScaleDrawable(drawable_red, 0, 1.0f, 1.0f);

            Drawable drawable_green = getResources().getDrawable(R.drawable.circle_green);
            drawable_green.setBounds(0, 0, (int)(drawable_green.getIntrinsicWidth()*0.5), (int)(drawable_green.getIntrinsicHeight()*0.5));
//            ScaleDrawable sd_green = new ScaleDrawable(drawable_green, 0, 1.0f, 1.0f);

            if(tripstatusfromList)
                ohio_annotate.setCompoundDrawables(null, null, drawable_red, null);
//                tripStatus.setImageResource(R.drawable.circle_red);
            else
                ohio_annotate.setCompoundDrawables(null, null, drawable_green, null);
//                tripStatus.setImageResource(R.drawable.circle_green);

            if(surveystatusfromList)
                tolinkList.setCompoundDrawables(null, null, drawable_red, null);
//                surveyStatus.setImageResource(R.drawable.circle_red);
            else
                tolinkList.setCompoundDrawables(null, null, drawable_green, null);
//                surveyStatus.setImageResource(R.drawable.circle_green);


            sleepingtime = (TextView)findViewById(R.id.sleepingTime);
//            SharedPreferences sharedPrefs = getSharedPreferences("edu.umich.minuku_2", MODE_PRIVATE);
            String sleepStartTime = sharedPrefs.getString("SleepingStartTime","Please select your sleeping time");
            String sleepEndTime = sharedPrefs.getString("SleepingEndTime","Please select your wake up time");

            if(!sleepStartTime.equals("Please select your sleeping time")&&!sleepEndTime.equals("Please select your wake up time")){
                String[] sleepStartDetail = sleepStartTime.split(":");

                int sleepStartHour = Integer.valueOf(sleepStartDetail[0]);
                String sleepStartHourStr = "";
                if(sleepStartHour<12)
                    sleepStartHourStr = String.valueOf(sleepStartHour)+"am";
                else if(sleepStartHour==12)
                    sleepStartHourStr = String.valueOf(sleepStartHour)+"pm";
                else
                    sleepStartHourStr = String.valueOf(sleepStartHour-12)+"pm";

                String[] sleepEndDetail = sleepEndTime.split(":");

                int sleepEndHour = Integer.valueOf(sleepEndDetail[0]);
                String sleepEndHourStr = "";
                if(sleepEndHour<12)
                    sleepEndHourStr = String.valueOf(sleepEndHour)+"am";
                else if(sleepEndHour==12)
                    sleepEndHourStr = String.valueOf(sleepEndHour)+"pm";
                else
                    sleepEndHourStr = String.valueOf(sleepEndHour-12)+"pm";

                sleepingtime.setText("Sleeping time: "+sleepStartHourStr+" to "+sleepEndHourStr);
//                sleepingtime.setText("Sleeping time:"+"\r\n"+sleepStartTime+" to "+sleepEndTime);
            }
            else
                sleepingtime.setText("Set Sleep Time");
            //device_id=(TextView)findViewById(R.id.deviceid);

            //device_id.setText("ID = "+Constant.DEVICE_ID);

            //TODO
            startTimerThread();

//            mScheduledExecutorService.scheduleAtFixedRate(
//                    statusRunnable,
//                    BACKGROUND_RECORDING_INITIAL_DELAY,
//                    REFRESH_FREQUENCY,
//                    TimeUnit.SECONDS);
        }

    }

    private void startTimerThread() {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10 * 60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(statusRunnable);
                }
            }
        };

        new Thread(runnable).start();
    }

    Runnable statusRunnable = new Runnable() {
        @Override
        public void run() {

            Log.d(TAG, "statusRunnable");

            ArrayList<String> data = new ArrayList<String>();
            ArrayList<Integer> dataPos = new ArrayList<Integer>();
            String taskTable = null;
            Calendar cal = Calendar.getInstance();
            Date date = new Date();
            cal.setTime(date);
            int Year = cal.get(Calendar.YEAR);
            int Month = cal.get(Calendar.MONTH) + 1;
            int Day = cal.get(Calendar.DAY_OF_MONTH);

            String startTimeString = makingDataFormat(Year, Month, Day);
            String endTimeString = makingDataFormat(Year, Month, Day + 1);
            long startTime = getSpecialTimeInMillis(startTimeString);
            long endTime = getSpecialTimeInMillis(endTimeString);

            ArrayList<String> results = DBHelper.querySessionsBetweenTimes(startTime, endTime);
            int countForLight = 0;
            for(String result : results){
                String annotation = result.split(Constants.DELIMITER)[4];
                if(!annotation.equals("")){
                    countForLight++;
                }
            }

            Drawable drawable_red = getResources().getDrawable(R.drawable.circle_red);
            drawable_red.setBounds(0, 0, (int)(drawable_red.getIntrinsicWidth()*0.5), (int)(drawable_red.getIntrinsicHeight()*0.5));

            Drawable drawable_green = getResources().getDrawable(R.drawable.circle_green);
            drawable_green.setBounds(0, 0, (int)(drawable_green.getIntrinsicWidth()*0.5), (int)(drawable_green.getIntrinsicHeight()*0.5));


            if(countForLight!=results.size()){
                ohio_annotate.setCompoundDrawables(null, null, drawable_red, null);
            }else {
                ohio_annotate.setCompoundDrawables(null, null, drawable_green, null);
            }

            //get all data in cursor
            //TODO notice !! Convert the function into getRecentSessions
            /*
            data = SessionManager.getTripDatafromSQLite();
            ArrayList<String> dataInCursor = new ArrayList<String>();
            try {
                SQLiteDatabase db = DBManager.getInstance().openDatabase();

                Cursor tripCursor = db.rawQuery("SELECT " + DBHelper.Trip_startTime + " , " + DBHelper.lat + " , " + DBHelper.lng + " FROM " + DBHelper.annotate_table + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                        + DBHelper.Trip_startTime + " BETWEEN" + " '" + startTimeString + "' " + "AND" + " '" + endTimeString + "' ORDER BY " + DBHelper.Trip_startTime + " DESC", null);

                Log.d(TAG, "SELECT " + DBHelper.Trip_startTime + " , " + DBHelper.lat + " , " + DBHelper.lng + " FROM " + DBHelper.annotate_table + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                        + DBHelper.Trip_startTime + " BETWEEN" + " '" + startTimeString + "' " + "AND" + " '" + endTimeString + "' ORDER BY " + DBHelper.Trip_startTime + " DESC");

                //get all data from cursor
                if (tripCursor.moveToFirst()) {
                    do {
                        String eachdataInCursor = tripCursor.getString(0);
                        dataInCursor.add(eachdataInCursor);
                        Log.d(TAG, " tripCursor.moveToFirst()");
                    } while (tripCursor.moveToNext());
                } else
                    Log.d(TAG, " tripCursor.moveToFirst() else");
                tripCursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = 0; i < data.size(); i++) {
                String datafromList = data.get(i);
                String timedata[] = datafromList.split("-");
                String tripstartTime = timedata[0];
                tripstartTime = SessionManager.getmillisecondToDateWithTime(Long.valueOf(tripstartTime));
                if (dataInCursor.contains(tripstartTime))
                    dataPos.add(i);
            }

            Drawable drawable_red = getResources().getDrawable(R.drawable.circle_red);
            drawable_red.setBounds(0, 0, (int)(drawable_red.getIntrinsicWidth()*0.5), (int)(drawable_red.getIntrinsicHeight()*0.5));

            Drawable drawable_green = getResources().getDrawable(R.drawable.circle_green);
            drawable_green.setBounds(0, 0, (int)(drawable_green.getIntrinsicWidth()*0.5), (int)(drawable_green.getIntrinsicHeight()*0.5));

            if (dataPos.size() != data.size()) { // != mean there have some trip haven't been done.
//                tripStatus.setImageResource(R.drawable.circle_red);
                ohio_annotate.setCompoundDrawables(null, null, drawable_red, null);
//                ohio_annotate.setCompoundDrawablesWithIntrinsicBounds( 0, 0, R.drawable.circle_red, 0);
                sharedPrefs.edit().putBoolean("TripStatus", true).apply();
            } else {
//                tripStatus.setImageResource(R.drawable.circle_green);
                ohio_annotate.setCompoundDrawables(null, null, drawable_green, null);
//                ohio_annotate.setCompoundDrawablesWithIntrinsicBounds( 0, 0, R.drawable.circle_green, 0);
                sharedPrefs.edit().putBoolean("TripStatus", false).apply();
            }*/

            dataPos = new ArrayList<Integer>();

            //TODO judge
            /*if (Day % 2 == 0)
                taskTable = DBHelper.checkFamiliarOrNotLinkList_table;
            else
                taskTable = DBHelper.intervalSampleLinkList_table;*/

            taskTable = DBHelper.surveyLink_table;

            int tripCursorNum = -1;

            try {
                SQLiteDatabase db = DBManager.getInstance().openDatabase();

                Cursor tripCursor = db.rawQuery("SELECT " + DBHelper.openFlag_col + ", " + DBHelper.link_col + " FROM " + taskTable + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                        + DBHelper.TIME + " BETWEEN" + " '" + startTime + "' " + "AND" + " '" + endTime + "' ORDER BY " + DBHelper.TIME + " DESC", null);

                Log.d(TAG, "SELECT " + DBHelper.openFlag_col + ", " + DBHelper.link_col + " FROM " + taskTable + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                        + DBHelper.TIME + " BETWEEN" + " '" + startTime + "' " + "AND" + " '" + endTime + "' ORDER BY " + DBHelper.TIME + " DESC");

                tripCursorNum = tripCursor.getCount()-1;
                //get all data from cursor
                int i = 0;
                if (tripCursor.moveToFirst()) {
                    do {
                        int eachdataInCursor = tripCursor.getInt(0);
                        String link = tripCursor.getString(1);

                        Log.d(TAG, " 0 : " + eachdataInCursor + ", 1 : " + link);

                        if (eachdataInCursor == 0) { //0 represent they haven't click into it.
                            dataPos.add(i);
                        }
                        i++;
                        data.add(link);
                        Log.d(TAG, " link : " + link);

                        Log.d(TAG, " tripCursor.moveToFirst()");
                    } while (tripCursor.moveToNext());
                } else
                    Log.d(TAG, " tripCursor.moveToFirst() else");
                tripCursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

//            if (dataPos.contains(tripCursorNum)) {
            if (!dataPos.isEmpty()) {
//                surveyStatus.setImageResource(R.drawable.circle_red);
                tolinkList.setCompoundDrawables(null, null, drawable_red, null);
//                tolinkList.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.circle_red, 0);
                sharedPrefs.edit().putBoolean("SurveyStatus", true).apply();
            } else {
//                surveyStatus.setImageResource(R.drawable.circle_green);
                tolinkList.setCompoundDrawables(null, null, drawable_green, null);
//                tolinkList.setCompoundDrawablesWithIntrinsicBounds( 0, 0, R.drawable.circle_green, 0);
                sharedPrefs.edit().putBoolean("SurveyStatus", false).apply();

            }
        }
    };

    private void startSettingSleepingTime(){
        Intent intent = new Intent(this, sleepingohio.class);  //usage
        startActivity(intent);
    }

    public void startpermission(){
        //Maybe useless in this project.
//                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));  // 協助工具

        Intent intent1 = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);  //usage
        startActivity(intent1);

//                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS); //notification
//                    startActivity(intent);

        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));	//location
    }

    private void showdialogforuser(){

        //** User ID = Confirmation Number

        firstTimeToShowDialogOrNot = false;

        sharedPrefs = getSharedPreferences("edu.umich.minuku_2", MODE_PRIVATE);
        sharedPrefs.edit().putBoolean("firstTimeToShowDialogOrNot", firstTimeToShowDialogOrNot).apply();

        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//        params.setMargins(10, 0, 10, 0);

        final EditText editText = new EditText(MainActivity.this);
        editText.setHint("Confirmation number");
//        editText.setWidth(100);
        editText.setGravity(Gravity.CENTER_HORIZONTAL);
        editText.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(6)
        });
        layout.addView(editText);

//        final EditText editgroup = new EditText(MainActivity.this);
//        editgroup.setHint("");
////        editgroup.setWidth(layout.getWidth());
////        editgroup.setWidth(100);
//        editgroup.setGravity(Gravity.CENTER_HORIZONTAL);
//        editgroup.setFilters(new InputFilter[] {
//                new InputFilter.LengthFilter(1)
//        });
//        layout.addView(editgroup);

        user_id = (TextView) findViewById(R.id.userid);
        num_6_digit = (TextView) findViewById(R.id.group_num);


        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Please fill in your confirmation number.")
                .setView(layout)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false);

        final AlertDialog mAlertDialog = dialog.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener(){

            @Override
            public void onShow(final DialogInterface dialog){
                Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        Log.d(TAG, "editText.getText().toString() : "+ editText.getText().toString());

                        String inputID = editText.getText().toString();
                        Log.d(TAG,"inputID : "+inputID);

//                        Pattern pattern = Pattern.compile("\\d+");

                        if(inputID.matches("") ||
                                !tryParseInt(inputID) ||
                                inputID.length()!=6 ||
                                Integer.valueOf(inputID.substring(0, 1))>4 ||
                                Integer.valueOf(inputID.substring(0, 1))==0){
                            Toast.makeText(MainActivity.this,"Error, please try re-entering the number provided",Toast.LENGTH_SHORT).show();
                        }
                        else {
//                            Log.d(TAG,"confirmation number" + editText.getText().toString().substring(1));

                            Log.d(TAG,"participant ID" + inputID);
                            Log.d(TAG,"group number" + inputID.substring(0, 1));

//                            sharedPrefs.edit().putString("userid", editText.getText().toString().substring(1)).apply();
                            sharedPrefs.edit().putString("userid", inputID).apply();
                            Constants.USER_ID = sharedPrefs.getString("userid","NA");
                            user_id.setText("Confirmation #");
                            sharedPrefs.edit().putString("groupNum", inputID.substring(0, 1)).apply();
                            Constants.GROUP_NUM = sharedPrefs.getString("groupNum","NA");
                            num_6_digit.setText(Constants.USER_ID); //Constants.GROUP_NUM+

                            startSettingSleepingTime(); //the appearing order is reversed from the code.

                            startpermission();


                            //Dismiss once everything is OK.
                            dialog.dismiss();
                        }
                        Log.d(TAG,"setPositiveButton");

                    }
                });

            }

        });

        mAlertDialog.show();

    }

    boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(projName.equals("Ohio")) {
            switch (resultCode) {
                case 1:
                    String sleepStartTime = data.getExtras().getString("SleepingStartTime");
                    String sleepEndTime = data.getExtras().getString("SleepingEndTime");

                    if(!sleepStartTime.equals("Please select your start time")&&!sleepEndTime.equals("Please select your end time")) {
                        String[] sleepStartDetail = sleepStartTime.split(":");

                        int sleepStartHour = Integer.valueOf(sleepStartDetail[0]);
                        String sleepStartHourStr = "";
                        if(sleepStartHour<12)
                            sleepStartHourStr = String.valueOf(sleepStartHour)+"am";
                        else if(sleepStartHour==12)
                            sleepStartHourStr = String.valueOf(sleepStartHour)+"pm";
                        else
                            sleepStartHourStr = String.valueOf(sleepStartHour-12)+"pm";

                        String[] sleepEndDetail = sleepEndTime.split(":");

                        int sleepEndHour = Integer.valueOf(sleepEndDetail[0]);
                        String sleepEndHourStr = "";
                        if(sleepEndHour<12)
                            sleepEndHourStr = String.valueOf(sleepEndHour)+"am";
                        else if(sleepEndHour==12)
                            sleepEndHourStr = String.valueOf(sleepEndHour)+"pm";
                        else
                            sleepEndHourStr = String.valueOf(sleepEndHour-12)+"pm";
//                        sleepingtime.setText("Sleeping time:"+"\r\n"+sleepStartTime+" to "+sleepEndTime);
                        sleepingtime.setText("Sleeping time: " + sleepStartHourStr + " to " + sleepEndHourStr);
                    }
                    else
                        sleepingtime.setText("Set Sleep Time");

                    SharedPreferences.Editor editor_1 = getSharedPreferences("edu.umich.minuku_2", MODE_PRIVATE).edit();
                    editor_1.putString("SleepingStartTime",sleepStartTime);
                    editor_1.commit();
                    SharedPreferences.Editor editor_2 = getSharedPreferences("edu.umich.minuku_2", MODE_PRIVATE).edit();
                    editor_2.putString("SleepingEndTime",sleepEndTime);
                    editor_2.commit();

                    break;
            }
            switch (requestCode){
                case 2:
                    Log.d(TAG," case 2 ");
                    boolean tripstatusfromList = sharedPrefs.getBoolean("TripStatus", false);
                    boolean surveystatusfromList = sharedPrefs.getBoolean("SurveyStatus", false);

                    Log.d(TAG,"tripstatusfromList : " + tripstatusfromList + " , surveystatusfromList : " + surveystatusfromList);

                    Drawable drawable_red = getResources().getDrawable(R.drawable.circle_red);
                    drawable_red.setBounds(0, 0, (int)(drawable_red.getIntrinsicWidth()*0.5), (int)(drawable_red.getIntrinsicHeight()*0.5));
//            ScaleDrawable sd_red = new ScaleDrawable(drawable_red, 0, 30f, 30f);

                    Drawable drawable_green = getResources().getDrawable(R.drawable.circle_green);
                    drawable_green.setBounds(0, 0, (int)(drawable_green.getIntrinsicWidth()*0.5), (int)(drawable_green.getIntrinsicHeight()*0.5));
//            ScaleDrawable sd_green = new ScaleDrawable(drawable_green, 0, 30f, 30f);

                    if(tripstatusfromList)
                        ohio_annotate.setCompoundDrawables(null, null, drawable_red, null);
//                        tripStatus.setImageResource(R.drawable.circle_red);
                    else
                        ohio_annotate.setCompoundDrawables(null, null, drawable_green, null);
//                        tripStatus.setImageResource(R.drawable.circle_green);

                    if(surveystatusfromList)
                        tolinkList.setCompoundDrawables(null, null, drawable_red, null);
//                        surveyStatus.setImageResource(R.drawable.circle_red);
                    else
                        tolinkList.setCompoundDrawables(null, null, drawable_green, null);
//                        surveyStatus.setImageResource(R.drawable.circle_green);

                    break;
            }
        }
    }

    private long getSpecialTimeInMillis(String givenDateFormat){
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_SLASH);
        long timeInMilliseconds = 0;
        try {
            Date mDate = sdf.parse(givenDateFormat);
            timeInMilliseconds = mDate.getTime();
            Log.d(TAG,"Date in milli :: " + timeInMilliseconds);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timeInMilliseconds;
    }

    private String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }

    public String makingDataFormat(int year,int month,int date){
        String dataformat= "";

//        dataformat = addZero(year)+"-"+addZero(month)+"-"+addZero(date)+" "+addZero(hour)+":"+addZero(min)+":00";
        dataformat = addZero(year)+"/"+addZero(month)+"/"+addZero(date)+" "+"00:00:00";
        Log.d(TAG,"dataformat : " + dataformat);

        return dataformat;
    }

    //to view sleepingohio
    private Button.OnClickListener ohio_annotateing = new Button.OnClickListener() {
        public void onClick(View v) {
            Log.e(TAG,"recordinglist_ohio clicked");

//            startActivity(new Intent(MainActivity.this, recordinglistohio.class));
            startActivityForResult(new Intent(MainActivity.this, recordinglistohio.class), 2);

        }
    };

    //to view sleepingohio
    private Button.OnClickListener ohio_settinging = new Button.OnClickListener() {
        public void onClick(View v) {
            Log.e(TAG,"sleepingohio clicked");

            startActivityForResult(new Intent(MainActivity.this, sleepingohio.class),requestCode_setting);
//            startActivity(new Intent(MainActivity.this, sleepingohio.class));

        }
    };

    //public for update
    public void initViewPager(View timerview, View recordview){
        mTabs = (android.support.design.widget.TabLayout) findViewById(R.id.tablayout);
        mTabs.addTab(mTabs.newTab().setText("計時"));
        mTabs.addTab(mTabs.newTab().setText("紀錄"));

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        timerview.setTag(Constant.home_tag);


    }

    protected void showToast(String aText) {
        Toast.makeText(this, aText, Toast.LENGTH_SHORT).show();
    }

    public void improveMenu(boolean bool){
        Constant.tabpos = bool;
        ActivityCompat.invalidateOptionsMenu(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.findItem(R.id.action_selectdate).setVisible(false);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if(projName.equals("Ohio")) //f***ing hardcode
            menu.findItem(R.id.action_report).setVisible(false);

        if(Constant.tabpos)
            menu.findItem(R.id.action_selectdate).setVisible(true);
        else
            menu.findItem(R.id.action_selectdate).setVisible(false);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_report:
                startActivity(new Intent(MainActivity.this, report.class));
                return true;
            case R.id.action_selectdate:
                final Calendar c = Calendar.getInstance();
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);
                new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        String format = setDateFormat(year,month,day);
                        //startdate.setText(format);
                    }

                }, mYear,mMonth, mDay).show();
                return true;
        }
        return true;
    }

    private void checkAndRequestPermissions() {

        Log.e(TAG,"checkingAndRequestingPermissions");

        int permissionReadExternalStorage = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionWriteExternalStorage = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        int permissionFineLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionCoarseLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int permissionStatus= ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE);

        List<String> listPermissionsNeeded = new ArrayList<>();


        if (permissionReadExternalStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permissionWriteExternalStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (permissionFineLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (permissionCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_PHONE_STATE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
        }else{
            startServiceWork();
        }

    }


    public void getDeviceid(){

        TelephonyManager mngr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        int permissionStatus= ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE);
        if(permissionStatus==PackageManager.PERMISSION_GRANTED){
            Constants.DEVICE_ID = mngr.getDeviceId();

            Log.e(TAG,"DEVICE_ID"+Constants.DEVICE_ID+" : "+mngr.getDeviceId());

        }
    }

    public void startServiceWork(){

        getDeviceid();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();

                // Initialize the map with both permissions
                perms.put(android.Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                //perms.put(Manifest.permission.SYSTEM_ALERT_WINDOW, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.BODY_SENSORS, PackageManager.PERMISSION_GRANTED);



                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && perms.get(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && perms.get(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(android.Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED){
                        android.util.Log.d("permission", "[permission test]all permission granted");
                        //permission_ok=1;
                        startServiceWork();
                    } else {
                        Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG).show();
                    }
                }


                //TODO after they permit all the permission, then start the work.
//                startService(new Intent(getBaseContext(), BackgroundService.class));
//                startTimerThread();

            }
        }
    }

    private String setDateFormat(int year,int monthOfYear,int dayOfMonth){
        return String.valueOf(year) + "/"
                + String.valueOf(monthOfYear + 1) + "/"
                + String.valueOf(dayOfMonth);
    }

    public void SettingViewPager() {
        viewList = new ArrayList<View>();
        viewList.add(timerview);
        viewList.add(recordview);

        mViewPager.setAdapter(new TimerOrRecordPagerAdapter(viewList, this));

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs));
        //TODO date button now can show on menu when switch to recordview, but need to determine where to place the date textview(default is today's date).
        mTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                if(!Constant.tabpos)
                    //show date on menu
                    Constant.tabpos = true;
                else
                    //hide date on menu
                    Constant.tabpos = false;
                invalidateOptionsMenu();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mTabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(MainActivity.mViewPager));
    }
/*
    private void showSettingsScreen() {
        //showToast("Clicked settings");
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
    }
*/
    @Override
    protected void onStart() {
        super.onStart();
    }

/*
    @Subscribe
    public void assertEligibilityAndPopulateCompensationMessage(
            UserSubmissionStats userSubmissionStats) {
        Log.d(TAG, "Attempting to update compesnation message");
        if(userSubmissionStats != null && isEligibleForReward(userSubmissionStats)) {
            Log.d(TAG, "populating the compensation message");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    compensationMessage.setText("You are now eligible for today's reward!");
                    compensationMessage.setVisibility(View.VISIBLE);
                    compensationMessage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onCheckCreditPressed(v);
                        }
                    });

                }});
        } else {
                compensationMessage.setText("");
                compensationMessage.setVisibility(View.INVISIBLE);
        }
    }
*/

    @Subscribe
    public void incrementLoadingProcessCount(IncrementLoadingProcessCountEvent event) {
        Integer loadingCount = loadingProcessCount.incrementAndGet();
        Log.d(TAG, "Incrementing loading processes count: " + loadingCount);
    }

    @Subscribe
    public void decrementLoadingProcessCountEvent(DecrementLoadingProcessCountEvent event) {
        Integer loadingCount = loadingProcessCount.decrementAndGet();
        Log.d(TAG, "Decrementing loading processes count: " + loadingCount);
        //maybeRemoveProgressDialog(loadingCount);
    }
    // because of loadingProgressDialog
/*
    private void maybeRemoveProgressDialog(Integer loadingCount) {
        if(loadingCount <= 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingProgressDialog.hide();
                }
            });
        }
    }
*/
    /*
    @Subscribe
    public boolean isEligibleForReward(UserSubmissionStats userSubmissionStats) {
        return getRewardRelevantSubmissionCount(userSubmissionStats) >= ApplicationConstants.MIN_REPORTS_TO_GET_REWARD;
    }

    public void onCheckCreditPressed(View view) {
        Intent displayCreditIntent = new Intent(MainActivity.this, DisplayCreditActivity.class);
        startActivity(displayCreditIntent);
    }*/


    public class TimerOrRecordPagerAdapter extends PagerAdapter {
        private List<View> mListViews;
        private Context mContext;

        public TimerOrRecordPagerAdapter(){};

        public TimerOrRecordPagerAdapter(List<View> mListViews,Context mContext) {
            this.mListViews = mListViews;
            this.mContext = mContext;
        }

        @Override
        public int getCount() {
            return mListViews.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Item " + (position + 1);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = mListViews.get(position);
            switch (position){
                case 0: //timer
                    home mhome = new home(mContext);
                    mhome.inithome(timerview);

                    break;
                case 1: //report

                    break;
            }


            container.addView(view);

            return view;
        }

        /*
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }*/

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }


    }
}
