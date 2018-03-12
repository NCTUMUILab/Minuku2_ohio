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
import android.util.Log;

import com.opencsv.CSVWriter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku_2.service.SurveyTriggerService;

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

    public static void settingAllDaysIntervalSampling(Context context){

        //14 is the total of the research days
        int countOfDaysInSurvey = 14 - Constants.daysInSurvey;
        for(int DaysInSurvey = 1; DaysInSurvey <= countOfDaysInSurvey; DaysInSurvey++ ){
            SurveyTriggerService.settingIntervalSampling(DaysInSurvey, context);
        }

        storeToCSV_Interval_Samples_Times_split();
    }

    private static void storeToCSV_Interval_Samples_Times_split(){

//        String sFileName = "Interval_"+todayDate+".csv";
        String sFileName = "Interval_Samples_Times.csv";

        sFileName = sFileName.replace("/","-");

        Log.d(TAG, "sFileName : " + sFileName);

        try {

            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);

            Log.d(TAG, "root : " + root);

            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            data.add(new String[]{"======","======","======","======","======","======","======"});

            csv_writer.writeAll(data);

            csv_writer.close();

        } catch(IOException e) {
            e.printStackTrace();
            android.util.Log.e(TAG, "exception", e);
        } catch (IndexOutOfBoundsException e2){
            e2.printStackTrace();
            android.util.Log.e(TAG, "exception", e2);

        }

    }

    public static void checkinresponseStoreToCSV(long timestamp, Context context){

        String sFileName = "checkInResponse.csv";
        Log.d("checkinresponse", "entering checkinresponseStoreToCSV");

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
            e.printStackTrace();
            android.util.Log.e(TAG, "exception", e);
        }
    }

    public static void checkinresponseStoreToCSV(long timestamp, Context context, String response){

        String sFileName = "checkInResponse.csv";
        Log.d("checkinresponse", "entering checkinresponseStoreToCSV");

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
            e.printStackTrace();
            android.util.Log.e(TAG, "exception", e);
        }
    }

    public static void ServiceDestroying_StoreToCSV_onDestroy(long timestamp, String sFileName){
        Log.d(TAG,"ServiceDestroying_StoreToCSV_onDestroy");

//        String sFileName = "..._.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            String timeString = ScheduleAndSampleManager.getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp), timeString, "Service killed.(onDestroy)"});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void ServiceDestroying_StoreToCSV_onTaskRemoved(long timestamp, String sFileName){
        Log.d(TAG,"ServiceDestroying_StoreToCSV_onTaskRemoved");

//        String sFileName = "..._.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + Constants.PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+Constants.PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            String timeString = ScheduleAndSampleManager.getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp), timeString, "Service killed.(onTaskRemoved)"});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}