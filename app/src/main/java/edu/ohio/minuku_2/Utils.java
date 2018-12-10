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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.opencsv.CSVWriter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ohio.minuku.Data.DataHandler;
import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku_2.manager.SurveyTriggerManager;

/**
 * Created by neera_000 on 3/26/2016.
 *
 * modified by shriti 07/22/2016
 */
public class Utils {

    public static String TAG = "Utils";

    public static CSVWriter csv_writer = null;

    /**
     * Given an Email Id as string, encodes it so that it can go into the firebase object without
     * any issues.
     * @param unencodedEmail
     * @return
     */
    public static final String encodeEmail(String unencodedEmail) {
        if (unencodedEmail == null) return null;
        return unencodedEmail.replace(".", ",");
    }

    /**
     * method is used for checking valid Email id format.
     * @param email
     * @return boolean true for valid false for invalid
     */
    public static boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static boolean isEmailEasyValid(String email) {
        return email.contains("@");
    }

    public static long getDownloadedTime(Context context){

        long downloadtime = -999;
        try {

            PackageManager pm = context.getPackageManager();
            downloadtime = pm.getPackageInfo(Constants.appNameString, 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {

            Log.e(TAG, "Exception", e);
        }

        return downloadtime;
    }

    public static long getDateTime(long time){

        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String date = ScheduleAndSampleManager.getTimeString(time, sdf_date);
        long dateTime = ScheduleAndSampleManager.getTimeInMillis(date, sdf_date);

        return dateTime;
    }

    public static void getDeviceid(Context context){

        TelephonyManager mngr = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        int permissionStatus= ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE);
        if(permissionStatus== PackageManager.PERMISSION_GRANTED){
            Config.DEVICE_ID = mngr.getDeviceId();

//            Log.e(TAG,"DEVICE_ID"+Constants.DEVICE_ID+" : "+mngr.getDeviceId());
        }
    }

    public static long getDateTimeInMillis(long time){

        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String date = ScheduleAndSampleManager.getTimeString(time, sdf_date);
        long dateTime = ScheduleAndSampleManager.getTimeInMillis(date, sdf_date);

        return dateTime;
    }

    public static boolean isConfirmNumInvalid(String inputID) {
        boolean isInvalid = inputID.matches("") ||
                !tryParseInt(inputID) ||
                inputID.length()!=6 ||
                Integer.valueOf(inputID.substring(0, 1))>3;

        return isInvalid;
    }

    public static boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static long changeToCurrentDate(long time){

        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String timeDate = ScheduleAndSampleManager.getTimeString(time, sdf_date);
        String currentDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis(), sdf_date);

        if(!timeDate.equals(currentDate)){

            SimpleDateFormat sdf_HHmmss = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN_SECOND);
            String timeHHmmss = ScheduleAndSampleManager.getTimeString(time, sdf_HHmmss);

            time = ScheduleAndSampleManager.getTimeInMillis(currentDate + " " + timeHHmmss, new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE));
        }

        return time;
    }

    public static void settingAllDaysIntervalSampling(Context context){

        //14 is the total of the research days
        int countOfDaysInSurvey = Constants.FINALDAY - Config.daysInSurvey;
        for(int daysInSurvey = 1; daysInSurvey <= countOfDaysInSurvey; daysInSurvey++ ){

//            Log.d(TAG, "[test alarm] DaysInSurvey : "+DaysInSurvey);

            List<String> comment = new ArrayList<>();

            comment.clear();
            comment.add("Trigger times");
            CSVHelper.storeToCSV(CSVHelper.CSV_Interval_Samples_Times, comment);

            SurveyTriggerManager.settingIntervalSampling(daysInSurvey, context);

            storeToCSV_IntervalSamplesTimes();

            comment.clear();
            comment.add("Times Boundary");
            CSVHelper.storeToCSV(CSVHelper.CSV_Interval_Samples_Times, comment);

            CSVHelper.storeToCSV(CSVHelper.CSV_Interval_Samples_Times, SurveyTriggerManager.interval_sampled_times_bound);

            comment.clear();
            comment.add("");
            CSVHelper.storeToCSV(CSVHelper.CSV_Interval_Samples_Times, comment);
        }

        storeToCSV_Interval_Samples_Times_split();

        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);

        sharedPrefs.edit().putBoolean("IsSleepTimeSet", true).apply();
    }

    public static void setDefaultSleepTime(Context context){

        //set the time as default time which is from 12:00am to 08:00am
        SimpleDateFormat sdf_today_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String todayDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis(), sdf_today_date);

        String sleepTimeByDefault = todayDate + " 00:00:00";
        String wakeupTimeByDefault = todayDate + " 08:00:00";

        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);

        sharedPrefs.edit().putBoolean("WakeSleepDateIsSame", true).apply();

        long sleepStartTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepTimeByDefault, new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE));
        long sleepEndTimeLong = ScheduleAndSampleManager.getTimeInMillis(wakeupTimeByDefault, new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE));
        Log.d(TAG, "SleepStartTime Long : "+sleepStartTimeLong);
        Log.d(TAG, "SleepEndTime Long : "+sleepEndTimeLong);

        long sleepingRange = sleepEndTimeLong - sleepStartTimeLong;
        long period = (Constants.MILLISECONDS_PER_DAY - sleepingRange) / 6;
        Log.d(TAG, "period : "+period);
        Log.d(TAG, "period secs : "+period / Constants.MILLISECONDS_PER_SECOND);

        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "period : "+period);
        CSVHelper.storeToCSV(CSVHelper.CSV_ALARM_CHECK, "period secs : "+period / Constants.MILLISECONDS_PER_SECOND);

        sharedPrefs.edit().putLong("PeriodLong", period).apply();

        SimpleDateFormat sdf2 = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN);
        String sleepStartTimeRaw = ScheduleAndSampleManager.getTimeString(sleepStartTimeLong, sdf2);
        String sleepEndTimeRaw = ScheduleAndSampleManager.getTimeString(sleepEndTimeLong, sdf2);
        Log.d(TAG, "SleepingStartTime Raw : "+sleepStartTimeRaw);
        Log.d(TAG, "SleepingEndTime Raw : "+sleepEndTimeRaw);

        boolean firstTimeEnterSleepTimePage = sharedPrefs.getBoolean("FirstTimeEnterSleepTimePage", false);
        if(firstTimeEnterSleepTimePage)
            cancelAlarmsByResetSleepTime(context, sharedPrefs);
        sharedPrefs.edit().putBoolean("FirstTimeEnterSleepTimePage", true).apply();

        sharedPrefs.edit().putString("SleepingStartTime", sleepStartTimeRaw).apply();
        sharedPrefs.edit().putString("SleepingEndTime", sleepEndTimeRaw).apply();

        //for easy to maintain
        sharedPrefs.edit().putLong("sleepStartTimeLong", sleepStartTimeLong).apply();
        sharedPrefs.edit().putLong("sleepEndTimeLong", sleepEndTimeLong).apply();

        //check if the current time is over the sleep time
        if(ScheduleAndSampleManager.getCurrentTimeInMillis() >= sleepStartTimeLong){

            sharedPrefs.edit().putLong("nextSleepTime", sleepStartTimeLong + Constants.MILLISECONDS_PER_DAY).apply();
        }else {

            sharedPrefs.edit().putLong("nextSleepTime", sleepStartTimeLong).apply();
        }

        Utils.settingAllDaysIntervalSampling(context);
    }

    public static void cancelAlarmsByResetSleepTime(Context context, SharedPreferences sharedPrefs){

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

            cancelAlarmIfExists(context, request_code);
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

    public static void storeToCSV_IntervalSamplesTimes(){

        String sFileName = "Interval_Samples_Times.csv";

        sFileName = sFileName.replace("/","-");

        try {

            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);

            //Log.d(TAG, "root : " + root);

            if (!root.exists()) {
                root.mkdirs();
            }

            //Log.d(TAG, "interval_sampled_times : "+interval_sampled_times);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            String[] addToCSVFile = new String[]{};
            List<String> addToCSV = new ArrayList<String>();

            for(int index=0 ; index < SurveyTriggerManager.interval_sampled_times.size() ; index++){

                addToCSV.add(ScheduleAndSampleManager.getTimeString(SurveyTriggerManager.interval_sampled_times.get(index)));
            }

            addToCSVFile = addToCSV.toArray(addToCSVFile);

            data.add(addToCSVFile);

            csv_writer.writeAll(data);

            csv_writer.close();

        } catch(IOException e) {

        } catch (IndexOutOfBoundsException e){

        }

    }


    public static void storeToCSV_IntervalSurveyCreated(long triggeredTimestamp, int daysInSurvey, int surveyNum, String surveyLink, String noti_type, Context context){

        String sFileName = CSVHelper.CSV_IntervalSurveyState;

        //Log.d(TAG, "sFileName : " + sFileName);

        try {

            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);

            //Log.d(TAG, "root : " + root);

            if (!root.exists()) {
                root.mkdirs();
            }

            //get overall data
            ArrayList<String> resultInArray = DataHandler.getSurveys();

            int openCount = 0;

            float rate;

            for(int index = 0; index < resultInArray.size();index++){

                String openOrNot = resultInArray.get(index).split(Constants.DELIMITER)[5];

//                Log.d(TAG, "[test show link] open flag : "+ openOrNot);

                if(openOrNot.equals("1"))
                    openCount++;
            }

//            Log.d(TAG, "[test show link] openCount : "+ openCount + " resultInArray size : "+resultInArray.size());

            rate = (float)openCount/resultInArray.size() * 100;

            String rateString = String.format("%.2f", rate);


            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);
            Boolean startSurveying = sharedPrefs.getBoolean("startSurveying", true);

            if(startSurveying) {

                List<String[]> title = new ArrayList<String[]>();

                title.add(new String[]{"triggeredTime", "Day #(d)", "Survey #(n)", "clicked", "clickedTime","Survey link", "Noti Type", "Overall rate"});

                csv_writer.writeAll(title);

                sharedPrefs.edit().putBoolean("startSurveying", false).apply();
            }

            String triggeredTimeString = ScheduleAndSampleManager.getTimeString(triggeredTimestamp);

            String periodNum = getPeriodNumForNoti(context, triggeredTimestamp);

            List<String[]> data = new ArrayList<String[]>();

            data.add(new String[]{triggeredTimeString, "Day "+daysInSurvey,"Survey "+surveyNum, "",  "", surveyLink, noti_type, rateString+"%"});

            csv_writer.writeAll(data);

            csv_writer.close();

        } catch(IOException e) {
            //e.printStackTrace();
            //Log.e(TAG, "exception", e);
        } catch (IndexOutOfBoundsException e2){
            //e2.printStackTrace();
            //Log.e(TAG, "exception", e2);
        }
    }

    public static String getPeriodNumForNoti(Context context, long triggeredTimestamp){

        String periodName = "Period";

        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);
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

            return periodName + " 1";
        }else if(triggeredTimestamp > (sleepEndTimeLong + period) && triggeredTimestamp <= (sleepEndTimeLong + 2*period)){

            return periodName + " 2";
        }else if(triggeredTimestamp > (sleepEndTimeLong + 2*period) && triggeredTimestamp <= (sleepEndTimeLong + 3*period)){

            return periodName + " 3";
        }else if(triggeredTimestamp > (sleepEndTimeLong + 3*period) && triggeredTimestamp <= (sleepEndTimeLong + 4*period)){

            return periodName + " 4";
        }else if(triggeredTimestamp > (sleepEndTimeLong + 4*period) && triggeredTimestamp <= (sleepEndTimeLong + 5*period)){

            return periodName + " 5";
        }else if(triggeredTimestamp > (sleepEndTimeLong + 5*period) && triggeredTimestamp <= (sleepEndTimeLong + 6*period)){

            return periodName + " 6";
        }else{

            return "Wrong ";
        }
    }

    private static void storeToCSV_Interval_Samples_Times_split(){

//        String sFileName = "Interval_"+todayDate+".csv";
        String sFileName = "Interval_Samples_Times.csv";

        sFileName = sFileName.replace("/","-");

        //Log.d(TAG, "sFileName : " + sFileName);

        try {

            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);

            //Log.d(TAG, "root : " + root);

            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            data.add(new String[]{"======","======","======","======","======","======","======"});

            csv_writer.writeAll(data);

            csv_writer.close();

        } catch(IOException e) {
            //e.printStackTrace();
            //Log.e(TAG, "exception", e);
        } catch (IndexOutOfBoundsException e2){
//            e2.printStackTrace();
            //Log.e(TAG, "exception", e2);

        }

    }

    public static void checkinresponseStoreToCSV(long timestamp, Context context){

        String sFileName = "checkInResponse.csv";
        //Log.d("checkinresponse", "entering checkinresponseStoreToCSV");

        try{
            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            SharedPreferences sharedPrefs = context.getSharedPreferences("edu.umich.minuku_2", context.MODE_PRIVATE);
            Boolean startCheckInResponseOrNot = sharedPrefs.getBoolean("startCheckInResponseOrNot", true);

            if(startCheckInResponseOrNot) {
                List<String[]> title = new ArrayList<String[]>();

                title.add(new String[]{"timestamp", "timeString",
                        "deviceid", "email", "userid", "firstcheckin", "currentcheckin", "firstcheckinstr", "currentcheckinstr",
                        "daysinsurvey", "midnightstart", "surveyrunninghours", "startmidnightstr" });

                csv_writer.writeAll(title);

                sharedPrefs.edit().putBoolean("startCheckInResponseOrNot", false).apply();

            }

            List<String[]> data = new ArrayList<String[]>();

            String timeString = ScheduleAndSampleManager.getTimeString(timestamp);

            String message = "going to get the userinform";
            //write transportation mode
            data.add(new String[]{String.valueOf(timestamp), timeString, message});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (Exception e){
            //e.printStackTrace();
            //Log.e(TAG, "exception", e);
        }
    }

    public static void checkinresponseStoreToCSV(long timestamp, Context context, String response){

        String sFileName = "checkInResponse.csv";
        //Log.d("checkinresponse", "entering checkinresponseStoreToCSV");

        try{
            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            SharedPreferences sharedPrefs = context.getSharedPreferences("edu.umich.minuku_2", context.MODE_PRIVATE);
            Boolean startCheckInResponseOrNot = sharedPrefs.getBoolean("startCheckInResponseOrNot", true);

            if(startCheckInResponseOrNot) {
                List<String[]> title = new ArrayList<String[]>();

                title.add(new String[]{"timestamp", "timeString",
                        "deviceid", "email", "userid", "firstcheckin", "currentcheckin", "firstcheckinstr", "currentcheckinstr",
                        "daysinsurvey", "midnightstart", "surveyrunninghours", "startmidnightstr" });

                csv_writer.writeAll(title);

                sharedPrefs.edit().putBoolean("startCheckInResponseOrNot", false).apply();

            }

            List<String[]> data = new ArrayList<String[]>();

            String timeString = ScheduleAndSampleManager.getTimeString(timestamp);

            JSONObject responseInJson = new JSONObject(response);

            String deviceid = responseInJson.getString("deviceid");
            String email = responseInJson.getString("email");
            String userid = responseInJson.getString("userid");
            String firstcheckin = responseInJson.getString("firstcheckin");
            String currentcheckin = responseInJson.getString("currentcheckin");
            String firstcheckinstr = responseInJson.getString("firstcheckinstr");
            String currentcheckinstr = responseInJson.getString("currentcheckinstr");
            String daysinsurvey = responseInJson.getString("daysinsurvey");
            String midnightstart = responseInJson.getString("midnightstart");
            String surveyrunninghours = responseInJson.getString("surveyrunninghours");
            String startmidnightstr = responseInJson.getString("startmidnightstr");

            //write transportation mode
            data.add(new String[]{String.valueOf(timestamp), timeString,
                    deviceid, email, userid, firstcheckin, currentcheckin, firstcheckinstr, currentcheckinstr,
                    daysinsurvey, midnightstart, surveyrunninghours, startmidnightstr });

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (Exception e){
            //e.printStackTrace();
            //Log.e(TAG, "exception", e);
        }
    }

    public static void ServiceDestroying_StoreToCSV_onDestroy(long timestamp, String sFileName){
        //Log.d(TAG,"ServiceDestroying_StoreToCSV_onDestroy");

//        String sFileName = "..._.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            //Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            String timeString = ScheduleAndSampleManager.getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp), timeString, "Service killed.(onDestroy)"});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            //e.printStackTrace();
        }catch (Exception e){
            //e.printStackTrace();
        }
    }

    public static void ServiceDestroying_StoreToCSV_onTaskRemoved(long timestamp, String sFileName){
        //Log.d(TAG,"ServiceDestroying_StoreToCSV_onTaskRemoved");

//        String sFileName = "..._.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            //Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            String timeString = ScheduleAndSampleManager.getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp), timeString, "Service killed.(onTaskRemoved)"});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            //e.printStackTrace();
        }catch (Exception e){
            //e.printStackTrace();
        }
    }


}