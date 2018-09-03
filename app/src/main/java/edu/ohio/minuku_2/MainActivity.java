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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.event.DecrementLoadingProcessCountEvent;
import edu.ohio.minuku.event.IncrementLoadingProcessCountEvent;
import edu.ohio.minuku_2.controller.Sleepingohio;
import edu.ohio.minuku_2.controller.SurveyActivity;
import edu.ohio.minuku_2.controller.TripListActivity;
import edu.ohio.minuku_2.service.BackgroundService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private AtomicInteger loadingProcessCount = new AtomicInteger(0);

    public final int REQUEST_ID_MULTIPLE_PERMISSIONS=1;

    private TextView num_6_digit;
    private TextView user_id;
    private TextView sleepingtime;

    private Button ohio_settingSleepTime, ohio_annotate, closeService, tolinkList;
    private String projName = "Ohio";

    private int requestCode_setting = 1;

    private boolean firstTimeToShowDialogOrNot;
    private SharedPreferences sharedPrefs;

    private Intent intentToStartBackground;

    private boolean isTheUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.d(TAG, "Creating Main activity");

        MultiDex.install(this);

        sharedPrefs = getSharedPreferences(Constants.sharedPrefString, MODE_PRIVATE);

        //for testing the over date of the research

        if(Constants.daysInSurvey > Constants.finalday+1){

            setContentView(R.layout.homepage_complete);
            Button finalSurvey = (Button) findViewById(R.id.finalSurvey);
            finalSurvey.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    String url = Constants.FINAL_SURVEY_URL+"?d="+Constants.daysInSurvey+"&p="+Constants.USER_ID;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                }
            });

            user_id = (TextView) findViewById(R.id.userid);
            Constants.USER_ID = sharedPrefs.getString("userid","NA");
            user_id.setText("Confirmation #:" );

            num_6_digit = (TextView) findViewById(R.id.group_num);
            Constants.GROUP_NUM = sharedPrefs.getString("groupNum","NA");
            num_6_digit.setText(Constants.USER_ID);
        } else {

            settingHomepageView();
        }

        intentToStartBackground = new Intent(getBaseContext(), BackgroundService.class);

        isTheUser = sharedPrefs.getBoolean("isTheUser", false);

//        Log.d(TAG, "[test service] isBackgroundServiceRunning : "+BackgroundService.isBackgroundServiceRunning);
//        Log.d(TAG, "[test service] isTheUser : "+isTheUser);

        if(isTheUser && !BackgroundService.isBackgroundServiceRunning){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intentToStartBackground);
            } else {
                startService(intentToStartBackground);
            }
        }

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
        Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.dms_icon);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(getApplicationContext(), SplashScreen.class));
        sendBroadcast(shortcutintent);
    }

    public void getStartDate(){

        //get timzone
        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        int Year = cal.get(Calendar.YEAR);
        int Month = cal.get(Calendar.MONTH)+1;
        int Day = cal.get(Calendar.DAY_OF_MONTH);

        int Hour = cal.get(Calendar.HOUR_OF_DAY);
        int Min = cal.get(Calendar.MINUTE);

        sharedPrefs.edit().putInt("StartYear", Year).apply();
        sharedPrefs.edit().putInt("StartMonth", Month).apply();
        sharedPrefs.edit().putInt("StartDay", Day).apply();

        sharedPrefs.edit().putInt("StartHour", Hour).apply();
        sharedPrefs.edit().putInt("StartMin", Min).apply();

//        Log.d(TAG, "Start Year : " + Year + " Month : " + Month + " Day : " + Day + " daysInSurvey : " + Constants.daysInSurvey);

    }

    private void getDownloadDateInMillisecond(long downloadTime){

        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String downloadDate = ScheduleAndSampleManager.getTimeString(downloadTime, sdf_date);
        long downloadDateTime = ScheduleAndSampleManager.getTimeInMillis(downloadDate, sdf_date);

        Log.d(TAG, "downloadDateTime : "+ ScheduleAndSampleManager.getTimeString(downloadDateTime));

        sharedPrefs.edit().putLong("downloadDateTime", downloadDateTime).apply();
    }

    private void settingHomepageView() {

        setContentView(R.layout.homepage);

        /* alertdialog for checking userid */
        long downloadtime = -999;
        try {

            PackageManager pm = getPackageManager();
            downloadtime = pm.getPackageInfo(Constants.appNameString, 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {

            Log.e(TAG, "Exception", e);
        }

        Log.d(TAG, "downloadtime : " + ScheduleAndSampleManager.getTimeString(downloadtime));

        sharedPrefs.edit().putLong("downloadedTime", downloadtime).apply();

        getDownloadDateInMillisecond(downloadtime);

        sharedPrefs.edit().putBoolean("resetIntervalSurveyFlag", false).apply();

        firstTimeToShowDialogOrNot = sharedPrefs.getBoolean("firstTimeToShowDialogOrNot", true);

        Constants.USER_ID = sharedPrefs.getString("userid", "NA");

        if (firstTimeToShowDialogOrNot) {

            createShortCut(); //on home Screen Desktop
            showdialogforuser();
        } else if (Constants.USER_ID.equals("NA")) {

            showdialogforuser();
        }

        user_id = (TextView) findViewById(R.id.userid);
        Constants.USER_ID = sharedPrefs.getString("userid", "NA");
        user_id.setText("Confirmation #:");

        num_6_digit = (TextView) findViewById(R.id.group_num);
        Constants.GROUP_NUM = sharedPrefs.getString("groupNum", "NA");
        num_6_digit.setText(Constants.USER_ID);

        Constants.daysInSurvey = sharedPrefs.getInt("daysInSurvey", 0);

        //button
        tolinkList = (Button) findViewById(R.id.linkList);

        if (Constants.daysInSurvey > Constants.finalday) {

            tolinkList.setText("Final Survey");
            tolinkList.setOnClickListener(new Button.OnClickListener() {

                @Override
                public void onClick(View view) {

                    DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - to Survey Page");

                    String url = Constants.FINAL_SURVEY_URL;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                }
            });
        }else{

            tolinkList.setOnClickListener(new Button.OnClickListener() {

                @Override
                public void onClick(View view) {

                    DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - to Survey Page");

                    startActivity(new Intent(MainActivity.this, SurveyActivity.class));
                }
            });
        }

        ohio_settingSleepTime = (Button) findViewById(R.id.settingSleepTime);
        ohio_settingSleepTime.setOnClickListener(settingSleepTimeing);

        ohio_annotate = (Button) findViewById(R.id.Annotate);
        ohio_annotate.setOnClickListener(ohio_annotateing);

        closeService = (Button) findViewById(R.id.closeService);
        closeService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                stopService(intentToStartBackground);
            }
        });

        sleepingtime = (TextView) findViewById(R.id.sleepingTime);
        String sleepStartTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER);//"Please select your sleeping time"
        String sleepEndTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER);//"Please select your wake up time"

        if (!sleepStartTime.equals(Constants.NOT_A_NUMBER) && !sleepEndTime.equals(Constants.NOT_A_NUMBER)) {

            String[] sleepStartDetail = sleepStartTime.split(":");

            int sleepStartHour = Integer.valueOf(sleepStartDetail[0]);
            String sleepStartHourStr = "";
            if (sleepStartHour < 12 && sleepStartHour > 0)
                sleepStartHourStr = String.valueOf(sleepStartHour) + ":" + sleepStartDetail[1] + " am";
            else if (sleepStartHour == 12)
                sleepStartHourStr = String.valueOf(sleepStartHour) + ":" + sleepStartDetail[1] + " pm";
            else if (sleepStartHour == 0)
                sleepStartHourStr = "12" + ":" + sleepStartDetail[1] + " am";
            else
                sleepStartHourStr = String.valueOf(sleepStartHour - 12) + ":" + sleepStartDetail[1] + " pm";

            String[] sleepEndDetail = sleepEndTime.split(":");

            int sleepEndHour = Integer.valueOf(sleepEndDetail[0]);
            String sleepEndHourStr = "";
            if (sleepEndHour < 12 && sleepEndHour > 0)
                sleepEndHourStr = String.valueOf(sleepEndHour) + ":" + sleepEndDetail[1] + " am";
            else if (sleepEndHour == 12)
                sleepEndHourStr = String.valueOf(sleepEndHour) + ":" + sleepEndDetail[1] + " pm";
            else if (sleepEndHour == 0)
                sleepEndHourStr = "12" + ":" + sleepEndDetail[1] + " am";
            else
                sleepEndHourStr = String.valueOf(sleepEndHour - 12) + ":" + sleepEndDetail[1] + " pm";

            sleepingtime.setText("Sleep: " + sleepStartHourStr + " to " + sleepEndHourStr);

        } else {

            sleepingtime.setText("Set Sleep Time");
        }

    }

    private void startSettingSleepingTime(){

        Intent intent = new Intent(this, Sleepingohio.class);  //usage
        startActivity(intent);
    }

    public void startpermission(){

        Intent intent1 = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);  //usage
        startActivity(intent1);

        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));	//location
    }

    private void showdialogforuser(){

        firstTimeToShowDialogOrNot = false;

        sharedPrefs = getSharedPreferences(Constants.sharedPrefString, MODE_PRIVATE);
        sharedPrefs.edit().putBoolean("firstTimeToShowDialogOrNot", firstTimeToShowDialogOrNot).apply();

        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        //the parameter for the size of the editText
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT,1.0f);

        final EditText editText_confirmNum = new EditText(MainActivity.this);
        editText_confirmNum.setHint("Confirmation number");
        editText_confirmNum.setGravity(Gravity.CENTER_HORIZONTAL);
        editText_confirmNum.setLayoutParams(params);
        editText_confirmNum.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(6)
        });
        layout.addView(editText_confirmNum);

        final EditText editText_Email = new EditText(MainActivity.this);
        editText_Email.setHint("your Email");
        editText_Email.setGravity(Gravity.CENTER_HORIZONTAL);
        editText_Email.setLayoutParams(params);
        layout.addView(editText_Email);

        user_id = (TextView) findViewById(R.id.userid);
        num_6_digit = (TextView) findViewById(R.id.group_num);


        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Please fill in your confirmation number and Email.")
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

                        String inputID = editText_confirmNum.getText().toString();
//                        Log.d(TAG,"inputID : "+inputID);

                        String inputEmail = editText_Email.getText().toString();
//                        Log.d(TAG,"inputEmail : "+inputEmail);

                        if(Utils.isConfirmNumInvalid(inputID)){
                            Toast.makeText(MainActivity.this,"Error, please try re-entering the number provided",Toast.LENGTH_SHORT).show();
                        }else if(!Utils.isEmailEasyValid(inputEmail)){
                            //TODO email them the error message
                            Toast.makeText(MainActivity.this,"Error, please try re-entering your email",Toast.LENGTH_SHORT).show();
                        }
                        else if(!haveNetworkConnection()){
                            Toast.makeText(MainActivity.this,"Error, please connect to the network",Toast.LENGTH_SHORT).show();
                        }
                        else {

                            sharedPrefs.edit().putString("userid", inputID).apply();
                            Constants.USER_ID = sharedPrefs.getString("userid", "NA");
                            user_id.setText("Confirmation #");

                            sharedPrefs.edit().putString("groupNum", inputID.substring(0, 1)).apply();
                            Constants.GROUP_NUM = sharedPrefs.getString("groupNum", "NA");
                            num_6_digit.setText(Constants.USER_ID);

                            sharedPrefs.edit().putString("Email",inputEmail).apply();
                            Constants.Email = sharedPrefs.getString("Email", "NA");

                            startSettingSleepingTime(); //the appearing order is reversed from the code.

                            startpermission();

                            getStartDate();

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intentToStartBackground);
                            } else {
                                startService(intentToStartBackground);
                            }

                            sendingUserInform();

                            isTheUser = true;
                            sharedPrefs.edit().putBoolean("isTheUser", isTheUser).apply();

                            //Dismiss once everything is OK.
                            dialog.dismiss();
                            MainActivity.this.finish();
                        }
                    }
                });

            }

        });

        mAlertDialog.show();

    }

    private void sendingUserInform(){

//       ex. http://mcog.asc.ohio-state.edu/apps/servicerec?deviceid=375996574474999&email=none@nobody.com&userid=333333
//      deviceid=375996574474999&email=none@nobody.com&userid=3333333
        String link = Constants.checkInUrl + "deviceid=" + Constants.DEVICE_ID + "&email=" + Constants.Email+"&userid="+Constants.USER_ID;
        String userInformInString = null;
        JSONObject userInform = null;

//        Log.d(TAG, "user inform link : "+ link);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                userInformInString = new HttpAsyncGetUserInformFromServer().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        link).get();
            else
                userInformInString = new HttpAsyncGetUserInformFromServer().execute(
                        link).get();

            userInform = new JSONObject(userInformInString);

            //In order to set the survey link
            setDaysInSurveyAndTheDayTodayIs(userInform);

            setDownloadedDaysInSurveyIs(userInform);

            setMidnightStart(userInform);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e){
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }

        Log.d(TAG, "userInform : " + userInform);
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    private void setDaysInSurveyAndTheDayTodayIs(JSONObject userInform){

        try{

            Constants.daysInSurvey = userInform.getInt("daysinsurvey");

            sharedPrefs.edit().putInt("daysInSurvey", Constants.daysInSurvey).apply();

//            Log.d(TAG, "daysInSurvey : "+ Constants.daysInSurvey);
        }catch (JSONException e){
            e.printStackTrace();
        }

        //for testing the over date of the research
//        Constants.daysInSurvey = 15;
//        sharedPrefs.edit().putInt("daysInSurvey", Constants.daysInSurvey).apply();

    }

    private void setDownloadedDaysInSurveyIs(JSONObject userInform){

        try{

            Constants.downloadedDayInSurvey = userInform.getInt("daysinsurvey");

            //set the origin state "-1" to day 0
            if(Constants.downloadedDayInSurvey == -1){

                Constants.downloadedDayInSurvey = 0;
            }

            sharedPrefs.edit().putInt("downloadedDayInSurvey", Constants.downloadedDayInSurvey).apply();

            Log.d(TAG, "downloadedDayInSurvey : "+ Constants.downloadedDayInSurvey);
        }catch (JSONException e){
            e.printStackTrace();
        }

    }

    private void setMidnightStart(JSONObject userInform){

        try{

            long firstcheckin = userInform.getLong("firstcheckin") * Constants.MILLISECONDS_PER_SECOND;

            SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE);

//            Log.d(TAG, "firstcheckin : "+ firstcheckin);
//            Log.d(TAG, "firstcheckin String : "+ ScheduleAndSampleManager.getTimeString(firstcheckin, sdf_now));

            sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);

            long firstcheckinAfteraDay = firstcheckin + Constants.MILLISECONDS_PER_DAY;

//            Log.d(TAG, "firstcheckinAfteraDay : "+ firstcheckinAfteraDay);

            String firstresearchdate = ScheduleAndSampleManager.getTimeString(firstcheckinAfteraDay, sdf_now);

            firstresearchdate = firstresearchdate + " 00:00:00";

//            Log.d(TAG, "firstcheckinAfteraDay String : "+ firstresearchdate);

            long firstresearchday = ScheduleAndSampleManager.getTimeInMillis(firstresearchdate,sdf_now);

            Constants.midnightstart = firstresearchday;

            sharedPrefs.edit().putLong("midnightstart", Constants.midnightstart).apply();

//            Log.d(TAG, "midnightstart : "+ Constants.midnightstart);

            sharedPrefs.edit().putBoolean("resetIntervalSurveyFlag", true).apply();

        }catch (JSONException e){
            e.printStackTrace();
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
                        if(sleepStartHour<=12 && sleepStartHour > 0)
                            sleepStartHourStr = String.valueOf(sleepStartHour)+":"+sleepStartDetail[1]+" am";
                        else if(sleepStartHour==12)
                            sleepStartHourStr = String.valueOf(sleepStartHour)+":"+sleepStartDetail[1]+" pm";
                        else if(sleepStartHour==0)
                            sleepStartHourStr = "12"+":"+sleepStartDetail[1]+" am";
                        else
                            sleepStartHourStr = String.valueOf(sleepStartHour-12)+":"+sleepStartDetail[1]+" pm";

                        String[] sleepEndDetail = sleepEndTime.split(":");

                        int sleepEndHour = Integer.valueOf(sleepEndDetail[0]);
                        String sleepEndHourStr = "";
                        if(sleepEndHour<=12 && sleepEndHour > 0)
                            sleepEndHourStr = String.valueOf(sleepEndHour)+":"+sleepEndDetail[1]+" am";
                        else if(sleepEndHour==12)
                            sleepEndHourStr = String.valueOf(sleepEndHour)+":"+sleepEndDetail[1]+" pm";
                        else if(sleepEndHour==0)
                            sleepEndHourStr = "12"+":"+sleepEndDetail[1]+" am";
                        else
                            sleepEndHourStr = String.valueOf(sleepEndHour-12)+":"+sleepEndDetail[1]+" pm";

                        sleepingtime.setText("Sleep: " + sleepStartHourStr + " to " + sleepEndHourStr);

                        sharedPrefs.edit().putBoolean("resetIntervalSurveyFlag", true).apply();

                        Log.d(TAG, "sleepStartHour : "+sleepStartHour);
                        Log.d(TAG, "sleepEndHour : "+sleepEndHour);

                    }
                    else {

                        sleepingtime.setText("Set Sleep Time");
                    }
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
//            Log.d(TAG,"Date in milli :: " + timeInMilliseconds);
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

    //to view Sleepingohio
    private Button.OnClickListener ohio_annotateing = new Button.OnClickListener() {
        public void onClick(View v) {
//            Log.e(TAG,"recordinglist_ohio clicked");

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - to Trips Page");

            startActivityForResult(new Intent(MainActivity.this, TripListActivity.class), 2);

        }
    };

    //to view Sleepingohio
    private Button.OnClickListener settingSleepTimeing = new Button.OnClickListener() {
        public void onClick(View v) {
//            Log.e(TAG,"Sleepingohio clicked");

            DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - to SleepTime Page");

            startActivityForResult(new Intent(MainActivity.this, Sleepingohio.class), requestCode_setting);

            MainActivity.this.finish();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.findItem(R.id.action_selectdate).setVisible(false);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    private void checkAndRequestPermissions() {

//        Log.e(TAG,"checkingAndRequestingPermissions");

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
//                        Log.d("permission", "[permission test]all permission granted");
                        //permission_ok=1;
                        startServiceWork();
                    } else {
                        Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG).show();
                    }
                }

            }
        }
    }

    private String setDateFormat(int year,int monthOfYear,int dayOfMonth){
        return String.valueOf(year) + "/"
                + String.valueOf(monthOfYear + 1) + "/"
                + String.valueOf(dayOfMonth);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Subscribe
    public void incrementLoadingProcessCount(IncrementLoadingProcessCountEvent event) {
        Integer loadingCount = loadingProcessCount.incrementAndGet();
//        Log.d(TAG, "Incrementing loading processes count: " + loadingCount);
    }

    @Subscribe
    public void decrementLoadingProcessCountEvent(DecrementLoadingProcessCountEvent event) {
        Integer loadingCount = loadingProcessCount.decrementAndGet();
//        Log.d(TAG, "Decrementing loading processes count: " + loadingCount);
    }

    private class HttpAsyncGetUserInformFromServer extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String result=null;

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {

                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
//                    Log.d(TAG, "Response : " + line);
                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
//            Log.d(TAG, "get http post result " + result);

        }

    }


}
