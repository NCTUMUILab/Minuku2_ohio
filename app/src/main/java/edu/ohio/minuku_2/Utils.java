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
import android.content.SharedPreferences;
import android.os.Environment;

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
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
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

    public static boolean isConfirmNumInvalid(String inputID) {
        boolean isInvalid = inputID.matches("") ||
                !tryParseInt(inputID) ||
                inputID.length()!=6 ||
                Integer.valueOf(inputID.substring(0, 1))>4 ||
                Integer.valueOf(inputID.substring(0, 1))==0;

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


    public static void settingAllDaysIntervalSampling(Context context){

        //14 is the total of the research days
        int countOfDaysInSurvey = 14 - Constants.daysInSurvey;
        for(int DaysInSurvey = 1; DaysInSurvey <= countOfDaysInSurvey; DaysInSurvey++ ){

//            Log.d(TAG, "[test alarm] DaysInSurvey : "+DaysInSurvey);

            SurveyTriggerManager.settingIntervalSampling(DaysInSurvey, context);

            storeToCSV_IntervalSamplesTimes();
        }

        storeToCSV_Interval_Samples_Times_split();

        SharedPreferences sharedPrefs = context.getSharedPreferences(Constants.sharedPrefString, context.MODE_PRIVATE);
        String sleepingstartTime = sharedPrefs.getString("SleepingStartTime", Constants.NOT_A_NUMBER); //"22:00"
        String sleepingendTime = sharedPrefs.getString("SleepingEndTime", Constants.NOT_A_NUMBER);

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN);
        long startTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepingstartTime, sdf);
        long endTimeLong = ScheduleAndSampleManager.getTimeInMillis(sleepingendTime, sdf);

//        Log.d(TAG, "startTimeLong : "+startTimeLong+"| sleepingstartTime : "+sleepingstartTime);
//        Log.d(TAG, "endTimeLong : "+endTimeLong+"| sleepingendTime : "+sleepingendTime);

        boolean wakeSleepDateIsSame = sharedPrefs.getBoolean("WakeSleepDateIsSame", false);

        long period;

        if(wakeSleepDateIsSame){

            //TODO should changed 6 into the total surveys count in each day
            //TODO change the sharedPrefs into one day one PeriodLong
            period = (startTimeLong-endTimeLong + Constants.MILLISECONDS_PER_DAY)/6;
        }else {

            period = (startTimeLong-endTimeLong)/6;
        }

//        Log.d(TAG, "period : "+period);

        sharedPrefs.edit().putLong("PeriodLong", period).apply();

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

        } catch (IndexOutOfBoundsException e2){

        }

    }


    public static void storeToCSV_IntervalSurveyCreated(long triggeredTimestamp, int surveyNum, String surveyLink, String noti_type,Context context){

        String sFileName = "IntervalSurveyState.csv";

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

                title.add(new String[]{"triggeredTime", "Survey #", "clicked", "clickedTime","Survey link", "Noti Type","Period Number", "Overall rate"});

                csv_writer.writeAll(title);

                sharedPrefs.edit().putBoolean("startSurveying", false).apply();

            }

            String triggeredTimeString = ScheduleAndSampleManager.getTimeString(triggeredTimestamp);

            String periodNum = getPeriodNumForNoti(context, triggeredTimestamp);

            List<String[]> data = new ArrayList<String[]>();

            data.add(new String[]{triggeredTimeString, "Survey "+surveyNum, "",  "", surveyLink, noti_type, periodNum, rateString+"%"});

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

    public static void ServiceDestroying_StoreToCSV_onLowMemory(long timestamp, String sFileName){
        //Log.d(TAG,"ServiceDestroying_StoreToCSV_onLowMemory");

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

            data.add(new String[]{String.valueOf(timestamp), timeString, "Service killed.(onLowMemory)"});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            //e.printStackTrace();
        }catch (Exception e){
            //e.printStackTrace();
        }
    }

}