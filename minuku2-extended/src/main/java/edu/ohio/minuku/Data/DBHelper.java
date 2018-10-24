package edu.ohio.minuku.Data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import edu.ohio.minuku.Utilities.CSVHelper;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.Utilities.Utilities;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.AnnotationSet;
import edu.ohio.minuku.model.Session;


/**
 * Created by Lawrence on 2017/6/5.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper instance = null;

    public static final String id = "_id";
    public static final String TAG = "DBHelper";

    public static final String home_col = "home";
    public static final String neighbor_col = "neighbor";
    public static final String outside_col = "outside";
    public static final String homeorfaraway = "homeorfaraway";
    public static final String staticornot = "staticornot";
    public static final String DEVICE = "device_id";
    public static final String USERID = "user_id";
    public static final String TIME = "time"; //timeToSQLite

//    public static final String daysInSurvey = "daysInSurvey";
//    public static final String HOUR = "hour";

    //link list
    public static final String link_col = "link";
    public static final String generateTime_col = "generateTime";
    public static final String openTime_col = "openTime";
    public static final String missedTime_col = "missedTime";
    public static final String openFlag_col = "openFlag";
    public static final String surveyType_col = "surveyType";
    public static final String sentOrNot_col = "sentOrNot";
    public static final int COL_INDEX_ID = 0;
    public static final int COL_INDEX_LINK = 1;
    public static final int COL_INDEX_GENERATE_TIME = 2;

    //Location and Trip
    public static final String latitude_col = "latitude";
    public static final String longitude_col = "longitude";
    public static final String Accuracy_col = "Accuracy";
    public static final String Altitude_col = "Altitude";
    public static final String Speed_col = "Speed";
    public static final String Bearing_col = "Bearing";
    public static final String Provider_col = "Provider";

    //ActivityRecognition
    public static final String MostProbableActivity_col = "MostProbableActivity";
    public static final String ProbableActivities_col = "ProbableActivities";
    public static final String DetectedTime_col = "DetectedTime";

    public static final String trip_col = "Trip";

    //Transportation
    public static final String confirmTransportation_col = "Transportation";

    //ActionLog
    public static final String action_col = "Action";
    public static final String userUnlock_col = "UserUnlock";

    //ringer
    public static final String RingerMode_col = "RingerMode";
    public static final String AudioMode_col = "AudioMode";
    public static final String StreamVolumeMusic_col = "StreamVolumeMusic";
    public static final String StreamVolumeNotification_col = "StreamVolumeNotification";
    public static final String StreamVolumeRing_col = "StreamVolumeRing";
    public static final String StreamVolumeVoicecall_col = "StreamVolumeVoicecall";
    public static final String StreamVolumeSystem_col = "StreamVolumeSystem";

    //battery
    public static final String BatteryLevel_col = "BatteryLevel";
    public static final String BatteryPercentage_col = "BatteryPercentage";
    public static final String BatteryChargingState_col = "BatteryChargingState";
    public static final String isCharging_col = "isCharging";

    //connectivity
    public static final String NetworkType_col = "NetworkType";
    public static final String IsNetworkAvailable_col = "IsNetworkAvailable";
    public static final String IsConnected_col = "IsConnected";
    public static final String IsWifiAvailable_col = "IsWifiAvailable";
    public static final String IsMobileAvailable_col = "IsMobileAvailable";
    public static final String IsWifiConnected_col = "IsWifiConnected";
    public static final String IsMobileConnected_col = "IsMobileConnected";

    //telephony
    public static final String NetworkOperatorName_col = "NetworkOperatorName";
    public static final String CallState_col = "CallState";
    public static final String PhoneSignalType_col = "PhoneSignalType";
    public static final String GsmSignalStrength_col = "GsmSignalStrength";
    public static final String LTESignalStrength_col = "LTESignalStrength";
    //public static final String CdmaSignalStrength_col = "CdmaSignalStrength";
    public static final String CdmaSignalStrengthLevel_col = "CdmaSignalStrengthLevel";

    //AppUsage
    public static final String ScreenStatus_col = "ScreenStatus";
    public static final String Latest_Used_App_col = "Latest_Used_App";
    public static final String Latest_Foreground_Activity_col = "Latest_Foreground_Activity";

    //UserInteraction
    public static final String Present_col = "Present";
    public static final String Unlock_col = "Unlock";
    public static final String Background_col = "Background";
    public static final String Foreground_col = "Foreground";

    //SurveydayWithDate
    public static final String surveyDay_col = "SurveyDay";
    public static final String surveyDate_col = "SurveyDate";

    //records
    public static final String COL_DATA = "data";
    public static final int COL_INDEX_RECORD_ID = 0;
    public static final int COL_INDEX_RECORD_SESSION_ID = 1;
    public static final int COL_INDEX_RECORD_DATA = 2;
    public static final int COL_INDEX_RECORD_TIMESTAMP_STRING = 3;
    public static final int COL_INDEX_RECORD_TIMESTAMP_LONG = 4;

    //session
    public static final String COL_ID = "_id";
    public static final String COL_SESSION_MODIFIED_FLAG = "session_modified_flag";
    public static final String COL_SESSION_START_TIME = "session_start_time";
    public static final String COL_SESSION_END_TIME = "session_end_time";
    public static final String COL_SESSION_LONG_ENOUGH_FLAG = "session_long_enough";
    public static final String COL_SESSION_ID = "session_id";
    public static final String COL_TIMESTAMP_STRING = "timestamp_string";
    public static final String COL_SESSION_ANNOTATION_SET = "session_annotation_set";
    public static final String COL_SESSION_SENTORNOT_FLAG = "sentOrNot";
    public static final String COL_SESSION_COMBINEDORNOT_FLAG = "combinedOrNot";
    public static final String COL_SESSION_PERIODNUMBER_FLAG = "periodnumber";
    public static final String COL_SESSION_SURVEYDAY_FLAG = "surveyDay";
    public static final int COL_INDEX_SESSION_ID = 0;
    public static final int COL_INDEX_SESSION_TIMESTAMP_STRING = 1;
    public static final int COL_INDEX_SESSION_START_TIME = 2;
    public static final int COL_INDEX_SESSION_END_TIME= 3;
    public static final int COL_INDEX_SESSION_ANNOTATION_SET= 4;
    public static final int COL_INDEX_SESSION_MODIFIED_FLAG= 5;
    public static final int COL_INDEX_SESSION_LONG_ENOUGH_FLAG= 6;
    public static final int COL_INDEX_SESSION_SENTORNOT_FLAG = 7;
    public static final int COL_INDEX_SESSION_COMBINEDORNOT_FLAG = 8;
    public static final int COL_INDEX_SESSION_PERIODNUMBER_FLAG = 9;
    public static final int COL_INDEX_SESSION_SURVEYDAY_FLAG = 10;

    //table name
    public static final String surveydayWithDate_table = "SurveydayWithDate";
    public static final String surveyLink_table = "SurveyLinkList";
    public static final String STREAM_TYPE_LOCATION = "Location";
    public static final String activityRecognition_table = "ActivityRecognition";
    public static final String transportationMode_table = "TransportationMode";
    public static final String actionLog_table = "ActionLog";
    public static final String userInteraction_table = "UserInteraction";
    public static final String trip_table = "Trip";
    public static final String telephony_table = "Telephony";
    public static final String ringer_table = "Ringer";
    public static final String battery_table = "Battery";
    public static final String connectivity_table = "Connectivity";
    public static final String appUsage_table = "AppUsage";
    public static final String SESSION_TABLE_NAME = "Session_Table";


    public static final String ORDER_TYPE_DESC = "DESC";
    public static final String ORDER_TYPE_ASC = "ASC";

    public static Context mContext;

    private static ArrayList<String> mTableNames;


    public static final String DATABASE_NAME = "MySQLite.db";
    public static int DATABASE_VERSION = 1;

    private SQLiteDatabase db;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mContext = context;

        initiateDBManager();

    }

    public static DBHelper getInstance(Context applicationContext) {
        if (instance == null) {
            instance = new DBHelper(applicationContext);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        createSurveyDayWithDateTable(db);
        createSurveyLinkTable(db);

        createSessionTable(db);
        createUserInteractionTable(db);
        createActionLogTable(db);

        createTransportationModeTable(db);
        createARTable(db);
        createLocationTable(db);
        createTelephonyTable(db);
        createRingerTable(db);
        createBatteryTable(db);
        createConnectivityTable(db);
        createAppUsageTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    //recall it after the phone restarted.
    public void initiateDBManager() {
        DBManager.initializeInstance(this);
    }

    public void createSurveyDayWithDateTable(SQLiteDatabase db){
        //Log.d(TAG,"create SurveyDayWithDate table");

        String cmd = "CREATE TABLE " +
                surveydayWithDate_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                surveyDay_col+" INTEGER, " +
                surveyDate_col+" TEXT " +
                ");";

        db.execSQL(cmd);
    }

    public void createSurveyLinkTable(SQLiteDatabase db){
       //Log.d(TAG,"create SurveyLink table");

        String cmd = "CREATE TABLE " +
                surveyLink_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                link_col+" TEXT," +
                generateTime_col+" INTEGER NOT NULL," +
                openTime_col+" INTEGER," +
                missedTime_col+" INTEGER," +
                openFlag_col +" INTEGER," +
                surveyType_col +" TEXT, " +
                sentOrNot_col + " INTEGER " +
                ");";

        db.execSQL(cmd);

    }

    public void createTelephonyTable(SQLiteDatabase db){

       //Log.d(TAG,"create telephony table");

        String cmd = "CREATE TABLE " +
                telephony_table + "(" +
                id + "ID integer PRIMARY KEY AUTOINCREMENT," +
                TIME + " TEXT NOT NULL," +
                NetworkOperatorName_col + " TEXT," +
                CallState_col + " INT," +
                PhoneSignalType_col + " INT," +
                GsmSignalStrength_col + " INT," +
                LTESignalStrength_col + " INT," +
                CdmaSignalStrengthLevel_col + " INT," +
                COL_SESSION_ID + " TEXT" +
                ");";

        db.execSQL(cmd);
    }

    public void createTransportationModeTable(SQLiteDatabase db){
       //Log.d(TAG,"create TransportationMode table");

        String cmd = "CREATE TABLE " +
                transportationMode_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                TIME + " TEXT NOT NULL," +
                confirmTransportation_col+" TEXT, " +
                COL_SESSION_ID + " TEXT " +
                ");";

        db.execSQL(cmd);

    }

    public void createUserInteractionTable(SQLiteDatabase db){
        Log.d(TAG,"create UserInteraction table");

        String cmd = "CREATE TABLE " +
                userInteraction_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                TIME + " TEXT NOT NULL, " +
                Present_col+" TEXT, " +
                Unlock_col+" TEXT, " +
                Background_col+" TEXT, " +
                Foreground_col+" TEXT " +
                ");";

        db.execSQL(cmd);
    }

    public void createActionLogTable(SQLiteDatabase db){

        //TODO add a col for inserting the user interaction into the ActionLog table
        //TODO might need to make action log insert a "" blank into the table to consistent the table size with sessionid

        String cmd = "CREATE TABLE " +
                actionLog_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                TIME + " TEXT NOT NULL," +
                action_col+" TEXT, " +
                userUnlock_col + " TEXT, "+
                COL_SESSION_ID + " TEXT" +
                ");";

        db.execSQL(cmd);

    }

    public void createAppUsageTable(SQLiteDatabase db){
       //Log.d(TAG,"create AppUsage table");

        String cmd = "CREATE TABLE " +
                appUsage_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
//                daysInSurvey+" TEXT NOT NULL,"+
//                HOUR+" TEXT NOT NULL,"+
                TIME + " TEXT NOT NULL," +
                ScreenStatus_col+" TEXT," +
                Latest_Used_App_col+" TEXT," +
                Latest_Foreground_Activity_col+" TEXT," +
                COL_SESSION_ID + " TEXT" +
                ");";

        db.execSQL(cmd);
    }

    public void createConnectivityTable(SQLiteDatabase db){
       //Log.d(TAG,"create Connectivity table");

        String cmd = "CREATE TABLE " +
                connectivity_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                TIME + " TEXT NOT NULL," +
                NetworkType_col+" TEXT," +
                IsNetworkAvailable_col+" BOOLEAN," +
                IsConnected_col+" BOOLEAN," +
                IsWifiAvailable_col+" BOOLEAN," +
                IsMobileAvailable_col+" BOOLEAN," +
                IsWifiConnected_col+" BOOLEAN," +
                IsMobileConnected_col+" BOOLEAN," +
                COL_SESSION_ID + " TEXT" +
                ");";

        db.execSQL(cmd);
    }

    public void createBatteryTable(SQLiteDatabase db){
       //Log.d(TAG,"create Battery table");

        String cmd = "CREATE TABLE " +
                battery_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                TIME + " TEXT NOT NULL," +
                BatteryLevel_col+" INTEGER," +
                BatteryPercentage_col+" FLOAT," +
                BatteryChargingState_col+" TEXT," +
                isCharging_col+" BOOLEAN," +
                COL_SESSION_ID + " TEXT" +
                ");";

        db.execSQL(cmd);
    }

    public void createRingerTable(SQLiteDatabase db){
       //Log.d(TAG,"create Ringer table");

        String cmd = "CREATE TABLE " +
                ringer_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                TIME + " TEXT NOT NULL," +
                RingerMode_col+" TEXT," +
                AudioMode_col+" TEXT," +
                StreamVolumeMusic_col+" INTEGER," +
                StreamVolumeNotification_col+" INTEGER," +
                StreamVolumeRing_col+" INTEGER," +
                StreamVolumeVoicecall_col+" INTEGER," +
                StreamVolumeSystem_col+" INTEGER," +
                COL_SESSION_ID + " TEXT" +
                ");";

        db.execSQL(cmd);
    }

    public void createARTable(SQLiteDatabase db){
       //Log.d(TAG,"create AR table");

        String cmd = "CREATE TABLE " +
                activityRecognition_table + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                TIME + " TEXT NOT NULL," +
                MostProbableActivity_col+" TEXT, " +
                ProbableActivities_col +" TEXT, " +
                DetectedTime_col + " TEXT, "+
                COL_SESSION_ID + " TEXT" +
                ");";

        db.execSQL(cmd);

    }

    public void createLocationTable(SQLiteDatabase db){
       //Log.d(TAG,"create location table");

        String cmd = "CREATE TABLE " +
                STREAM_TYPE_LOCATION + "(" +
                id+" INTEGER PRIMARY KEY NOT NULL, " +
                TIME + " TEXT NOT NULL," +
                latitude_col+" TEXT,"+
                longitude_col +" TEXT, " +
                Accuracy_col + " FLOAT, " +
                Altitude_col +" FLOAT," +
                Speed_col +" FLOAT," +
                Bearing_col +" FLOAT," +
                Provider_col +" TEXT, " +
                COL_SESSION_ID + " TEXT" +
                ");";

        db.execSQL(cmd);

    }

    public void createSessionTable(SQLiteDatabase db){

       //Log.d(TAG, "createSessionTable");

        String cmd = "CREATE TABLE" + " " +
                SESSION_TABLE_NAME + " ( "+
                COL_ID + " " + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TIMESTAMP_STRING + " TEXT NOT NULL, " +
                COL_SESSION_START_TIME + " INTEGER NOT NULL, " +
                COL_SESSION_END_TIME + " INTEGER, " +
                COL_SESSION_ANNOTATION_SET + " TEXT, " +
                COL_SESSION_MODIFIED_FLAG + " INTEGER, " +
                COL_SESSION_LONG_ENOUGH_FLAG+ " INTEGER, " +
                COL_SESSION_SENTORNOT_FLAG + " INTEGER, " +
                COL_SESSION_COMBINEDORNOT_FLAG + " INTEGER, " +
                COL_SESSION_PERIODNUMBER_FLAG + " INTEGER, " +
                COL_SESSION_SURVEYDAY_FLAG + " INTEGER "+
                ");";

        db.execSQL(cmd);
    }

    public static long insertSurveyDayWithDateTable(int surveyDay, String surveyDate){

        long rowId = 0;

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(DBHelper.surveyDay_col, surveyDay);
            values.put(DBHelper.surveyDate_col, surveyDate);

            rowId = db.insert(DBHelper.surveydayWithDate_table, null, values);
        }
        catch(NullPointerException e){
            //e.printStackTrace();
            rowId = -1;
        }

        return rowId;
    }

    public static ArrayList<String> querySurveyDayWithDate(String date){

        Log.d(TAG, "[test show trip] query SurveyDayWithDate in DBHelper with date : " + date);

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            String sql = "SELECT *"  +" FROM " + surveydayWithDate_table +
                    " where " + surveyDate_col + " LIKE " + " '" + date + "' ";

            //Log.d(TAG, "[test show trip querySession] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

        //Log.d(TAG, "[test show trip] the session is " +rows);

        return rows;
    }

    public static long insertActionLogTable(long createdTime, String action){

        long rowId = 0;

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(DBHelper.TIME, createdTime);
            values.put(DBHelper.action_col, action);
            values.put(DBHelper.userUnlock_col, "");

            int session_id = 0;

            int countOfOngoingSession = SessionManager.getInstance().getOngoingSessionIdList().size();

            //if there exists an ongoing session
            if (countOfOngoingSession>0){
                session_id = SessionManager.getInstance().getOngoingSessionIdList().get(0);
            }

            values.put(DBHelper.COL_SESSION_ID, String.valueOf(session_id));

            rowId = db.insert(DBHelper.actionLog_table, null, values);
        }
        catch(NullPointerException e){
            //e.printStackTrace();
            rowId = -1;
        }

        return rowId;
    }

    public static long insertActionLogTable(long createdTime, String userpresent, String userunlock){

        long rowId = 0;

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(DBHelper.TIME, createdTime);
            values.put(DBHelper.action_col, userpresent);
            values.put(DBHelper.userUnlock_col, userunlock);

            int session_id = 0;

            int countOfOngoingSession = SessionManager.getInstance().getOngoingSessionIdList().size();

            //if there exists an ongoing session
            if (countOfOngoingSession>0){
                session_id = SessionManager.getInstance().getOngoingSessionIdList().get(0);
            }

            values.put(DBHelper.COL_SESSION_ID, String.valueOf(session_id));

            rowId = db.insert(DBHelper.actionLog_table, null, values);
        }
        catch(NullPointerException e){
            //e.printStackTrace();
            rowId = -1;
        }

        return rowId;
    }

    public static long insertSessionTable(Session session){

       //Log.d(TAG, "test trip put session " + session.getId() + " to table " + SESSION_TABLE_NAME);

        long rowId = 0;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

//            values.put(COL_TASK_ID, session.getTaskId());
            values.put(COL_TIMESTAMP_STRING, ScheduleAndSampleManager.getTimeString(session.getStartTime()));
            values.put(COL_SESSION_START_TIME, session.getStartTime());

            if(session.getEndTime() != 0){

                values.put(COL_SESSION_END_TIME, session.getEndTime());
            }

            values.put(COL_SESSION_ANNOTATION_SET, session.getAnnotationsSet().toJSONObject().toString());
            values.put(COL_SESSION_LONG_ENOUGH_FLAG, session.isLongEnough());
            values.put(COL_SESSION_SENTORNOT_FLAG, session.getIsSent());
            values.put(COL_SESSION_COMBINEDORNOT_FLAG, session.getIsCombined());
            values.put(COL_SESSION_PERIODNUMBER_FLAG, session.getPeriodNum());
            values.put(COL_SESSION_SURVEYDAY_FLAG, session.getSurveyDay());

            rowId = db.insert(SESSION_TABLE_NAME, null, values);

            Log.d(TAG, "[show split trip] Session is long enough ? "+session.isLongEnough());
            Log.d(TAG, "[show split trip] Session is Sent ? "+session.getIsSent());
            Log.d(TAG, "[show split trip] Session is Combined ? "+session.getIsCombined());
            Log.d(TAG, "[show split trip] Session's starttime ? "+session.getStartTime());
            Log.d(TAG, "[show split trip] Session is endtime ? "+session.getEndTime());

        }catch(Exception e){

            Log.e(TAG, "[show split trip] exception", e);
            rowId = -1;

            CSVHelper.storeToCSV(CSVHelper.CSV_SESSION_CONCAT_CHECK, Utilities.getStackTrace(e));
        }

        DBManager.getInstance().closeDatabase();

        return rowId;
    }

    public static ArrayList<String> querySession(int sessionId){

		Log.d(TAG, "[test show trip] query session in DBHelper with session id " + sessionId);

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            String sql = "SELECT *"  +" FROM " + SESSION_TABLE_NAME +
                    //condition with session id
                    " where " + COL_ID + " = " + sessionId + " and " +
                    COL_SESSION_LONG_ENOUGH_FLAG + " = 1"; //only long enough trip

           //Log.d(TAG, "[test show trip querySession] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

       //Log.d(TAG, "[test show trip] the session is " +rows);

        return rows;

    }


    public static ArrayList<String> querySessionsBetweenTimes(long startTime, long endTime, String order) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + SESSION_TABLE_NAME +

//                    " where " + COL_SESSION_START_TIME + " > " + startTime + " and " +
//                    COL_SESSION_START_TIME + " < " + endTime +
                    " order by " + COL_SESSION_START_TIME + " " + order;

            //Log.d(TAG, "[test show trip querySessionsBetweenTimes] test order the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){
            //e.printStackTrace();
        }


        return rows;

    }


    public static ArrayList<String> querySessionsBetweenTimes(long startTime, long endTime) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + SESSION_TABLE_NAME +
                    " where " + COL_SESSION_LONG_ENOUGH_FLAG + " = 1" +
                    " and " + COL_SESSION_START_TIME + " > " + startTime + " and " +
                    COL_SESSION_START_TIME + " < " + endTime +
                    " order by " + COL_SESSION_START_TIME + " DESC ";

            //Log.d(TAG, "test combine [querySessionsBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){

                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();
//           //Log.d(TAG,"cursor.getCount : "+cursor.getCount());

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

        return rows;

    }

    public static ArrayList<String> queryNotBeenCombinedSessionsBetweenTimes(long startTime, long endTime) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + SESSION_TABLE_NAME +
                    " where " + COL_SESSION_LONG_ENOUGH_FLAG + " = 1" +
                    " and " + COL_SESSION_START_TIME + " > " + startTime + " and " +
                    COL_SESSION_START_TIME + " < " + endTime + " and " +
                    COL_SESSION_COMBINEDORNOT_FLAG + " <> " + Constants.SESSION_IS_COMBINED_FLAG +
                    " order by " + COL_SESSION_START_TIME + " DESC ";

            //Log.d(TAG, "test combine [querySessionsBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){

                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();
//           //Log.d(TAG,"cursor.getCount : "+cursor.getCount());

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

        return rows;

    }

    public static ArrayList<String> querySessionsBetweenTimes(long startTime, long endTime, int sessionid) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + SESSION_TABLE_NAME +
                    " where " + COL_SESSION_LONG_ENOUGH_FLAG + " = 1" + " and " +
                    COL_SESSION_START_TIME + " > " + startTime + " and " +
                    COL_SESSION_START_TIME + " < " + endTime + " and " +
                    COL_ID + " <> " + sessionid +
                    " order by " + COL_SESSION_START_TIME + " DESC ";

            //Log.d(TAG, "test combine [querySessionsBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){

                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();
//           //Log.d(TAG,"cursor.getCount : "+cursor.getCount());

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }


        return rows;

    }

    public static ArrayList<String> queryNotBeenCombinedSessionsBetweenTimes(long startTime, long endTime, int sessionid) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + SESSION_TABLE_NAME +
                    " where " + COL_SESSION_LONG_ENOUGH_FLAG + " = 1" + " and " +
                    COL_SESSION_START_TIME + " > " + startTime + " and " +
                    COL_SESSION_START_TIME + " < " + endTime + " and " +
                    COL_SESSION_END_TIME + " IS NOT NULL "  + " and " +
                    COL_ID + " <> " + sessionid + " and " +
                    COL_SESSION_COMBINEDORNOT_FLAG + " <> " + Constants.SESSION_IS_COMBINED_FLAG + " and " +
                    COL_SESSION_SENTORNOT_FLAG + " = " + Constants.SESSION_SHOULDNT_BEEN_SENT_FLAG +
                    " order by " + COL_SESSION_START_TIME + " DESC ";

            //Log.d(TAG, "test combine [querySessionsBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){

                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();
//           //Log.d(TAG,"cursor.getCount : "+cursor.getCount());

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }


        return rows;

    }


    //query task table
    public static ArrayList<String> queryModifiedSessions (){

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + SESSION_TABLE_NAME + " where " +
                    COL_SESSION_MODIFIED_FLAG + " = 1";

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }


        return rows;

    }


    //query task table
    public static ArrayList<String> querySessions (){

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + DBHelper.SESSION_TABLE_NAME;

           //Log.d(TAG, "[queryLastRecord] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }
       //Log.d(TAG, "[test show trip] the sessions are" + " " +rows);

        return rows;
    }

    //get the number of existing session
    public static long querySessionCount(){


        long count = 0;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT * "  +" FROM " + SESSION_TABLE_NAME ;
            Cursor cursor = db.rawQuery(sql, null);
            count = cursor.getCount();

            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }
        return count;

    }

    public static ArrayList<String> querySessions(long time24HrAgo){

        Log.d(TAG, "[test show trip] querySessions");

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + DBHelper.SESSION_TABLE_NAME +
                    " WHERE " + DBHelper.COL_SESSION_START_TIME + " < " + time24HrAgo +
                    " AND "+ DBHelper.COL_SESSION_SENTORNOT_FLAG + " <> " + Constants.SESSION_IS_ALREADY_SENT_FLAG +
                    " order by " + DBHelper.COL_SESSION_START_TIME + " " + "ASC";

            Log.d(TAG, "[queryLastRecord] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

            Log.e(TAG, "exception", e);
        }

        Log.d(TAG, "[test show trip] the sessions are" + " " +rows);

        return rows;
    }

    public static ArrayList<String> queryUnSentSessions(){

        Log.d(TAG, "[test show trip] queryUnSentSessions");

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + DBHelper.SESSION_TABLE_NAME +
                    " WHERE " + DBHelper.COL_SESSION_SENTORNOT_FLAG + " = " + Constants.SESSION_SHOULD_BE_SENT_FLAG +
                    " order by " + COL_SESSION_START_TIME + " " + "ASC";

            Log.d(TAG, "[queryLastRecord] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

            Log.e(TAG, "exception", e);
        }

        Log.d(TAG, "[test show trip] the sessions are" + " " +rows);

        return rows;
    }

    public static ArrayList<String> queryLastRecord(String table_name, int sessionId) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " where " + COL_SESSION_ID + " = " + sessionId +
                    " order by " + COL_ID + " DESC LIMIT 1";
            ;


  //         //Log.d(TAG, "[queryLastRecord] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
               //Log.d(TAG, "[queryLastRecord] get result row " +curRow);

                rows.add(curRow);
            }
            cursor.close();


            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }


        return rows;
    }

    public static ArrayList<String> queryLastSession() {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + SESSION_TABLE_NAME  +
                    " order by " + COL_ID + " DESC LIMIT 1";
            ;

 //          //Log.d(TAG, "[test combine queryLastSession] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
               //Log.d(TAG, "[test combine queryLastRecord] get result row " +curRow);

                rows.add(curRow);
            }
            cursor.close();


            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }


        return rows;

    }


    public static ArrayList<String> queryRecordsBetweenTimes(String table_name, long startTime, long endTime) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " where " +  TIME + " > " + startTime + " and " +
                    TIME + " < " + endTime  +
                    " order by " + TIME;

           //Log.d(TAG, "[test sampling] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
//                   //Log.d(TAG, "[queryRecordsInSession][testgetdata] column " + i + " content: " + cursor.getString(i));
                    curRow += cursor.getString(i)+ Constants.DELIMITER;

                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();


        }catch (Exception e){

        }


       //Log.d(TAG, "[test sampling] the rsult is " +rows);
        return rows;


    }

    public static ArrayList<String> queryRecordsBetweenTimes(String table_name, long startTime, long endTime, LatLng latLng) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " where " +  TIME + " > " + startTime + " and " +
                    TIME + " < " + endTime  +" and " +
                    latitude_col + " = " + latLng.latitude  +" and " +
                    longitude_col + " = " + latLng.longitude  +
                    " order by " + TIME;

            //Log.d(TAG, "[test sampling] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
//                   //Log.d(TAG, "[queryRecordsInSession][testgetdata] column " + i + " content: " + cursor.getString(i));
                    curRow += cursor.getString(i)+ Constants.DELIMITER;

                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();


        }catch (Exception e){

        }


        //Log.d(TAG, "[test sampling] the rsult is " +rows);
        return rows;
    }


    public static ArrayList<String> queryRecordsInSession(String table_name, int sessionId, long startTime, long endTime) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " where " + COL_SESSION_ID + " = " + sessionId + " and " +
                    TIME + " > " + startTime + " and " +
                    TIME + " < " + endTime  +
                    " order by " + TIME;

//           //Log.d(TAG, "[queryRecordsInSession][testgetdata] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
//                   //Log.d(TAG, "[queryRecordsInSession][testgetdata] column " + i + " content: " + cursor.getString(i));
                    curRow += cursor.getString(i)+ Constants.DELIMITER;

                }
//               //Log.d(TAG, "[queryRecordsInSession][testgetdata] get result row " +curRow);

                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();


        }catch (Exception e){

        }


        return rows;

    }

    public static ArrayList<String> queryRecordsInSession(String table_name, int sessionId) {

        ArrayList<String> rows = new ArrayList<String>();

        String querySessionidInDelimiters = "( '"+Constants.SESSION_DELIMITER + "' || RTRIM("+COL_SESSION_ID+") || '" + Constants.SESSION_DELIMITER+"' )"
                +"LIKE ('%" +Constants.SESSION_DELIMITER + "' || " + sessionId + " || '" + Constants.SESSION_DELIMITER + "%')";

        String querySessionid = COL_SESSION_ID + " = " + sessionId;

        //Log.d(TAG, "[test show trip] queryRecordsInSession ");
        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " WHERE " +
//                    querySessionid+
//                    " or " +
                    querySessionidInDelimiters +
                    " order by " + TIME;


           Log.d(TAG, "[test show trip] the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
//           //Log.d(TAG, "[test show trip] columnCount " +columnCount);
            while(cursor.moveToNext()){
//               //Log.d(TAG, "[test show trip] cursor" +cursor.getCount());
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
//               //Log.d(TAG, "[test show trip] get result row " +curRow);

                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

            Log.e(TAG, "[test show trip] Exception ", e);
        }


        return rows;

    }

    public static void updateRecordsInSession(String table_name, int currentSessionId, int newSessionid) {

//        String where = COL_SESSION_ID + " = " + currentSessionId;

        //get the exact session id in the delimiters
        String querySessionidInDelimiters = "( '"+Constants.SESSION_DELIMITER + "' || RTRIM("+COL_SESSION_ID+") || '" + Constants.SESSION_DELIMITER+"' )"
                +"LIKE ('%" +Constants.SESSION_DELIMITER + "' || " + currentSessionId + " || '" + Constants.SESSION_DELIMITER + "%')";

        String querySessionid = COL_SESSION_ID + " = " + currentSessionId;

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(COL_SESSION_ID, newSessionid);

            db.update(table_name, values, querySessionidInDelimiters
//                            + " or " + querySessionidBetweenSpaceAndDelimiter
//                            + " or " + querySessionid
                    , null);

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

            Log.e(TAG, "SessionConcat exception", e);

            CSVHelper.storeToCSV(CSVHelper.CSV_SESSION_CONCAT_CHECK, Utilities.getStackTrace(e));
        }
    }

    public static void updateRecordsInSession(String table_name, long splittingTime, int currentSessionId, int newSessionid) {

//        String where = COL_SESSION_ID + " = " + currentSessionId;

        //get the exact session id in the delimiters
        String querySessionidInDelimiters = "( '"+Constants.SESSION_DELIMITER + "' || RTRIM("+COL_SESSION_ID+") || '" + Constants.SESSION_DELIMITER+"' )"
                +"LIKE ('%" +Constants.SESSION_DELIMITER + "' || " + currentSessionId + " || '" + Constants.SESSION_DELIMITER + "%')";

        String querySessionid = COL_SESSION_ID + " = " + currentSessionId;

        String afterSplitting = TIME + " > " + splittingTime;
        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(COL_SESSION_ID, newSessionid);

            db.update(table_name, values, querySessionidInDelimiters
//                            + " or " + querySessionidBetweenSpaceAndDelimiter
//                            + " or " + querySessionid
                    +" and " + afterSplitting
                    , null);

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

            Log.e(TAG, "SessionConcat exception", e);

            CSVHelper.storeToCSV(CSVHelper.CSV_SESSION_CONCAT_CHECK, Utilities.getStackTrace(e));
        }
    }

    public static void updateRecordsInSessionConcat(String table_name, int currentSessionId, int newSessionid) {

        //get the exact session id in the delimiters
//        String where = " WHERE ( '"+Constants.DELIMITER + "' + RTRIM("+COL_SESSION_ID+") + '" + Constants.DELIMITER+"' )"
//                +" LIKE ('%" +Constants.DELIMITER + "' + " + currentSessionId + " + '" + Constants.DELIMITER + "%')";

        String querySessionidInDelimiters = "( '"+Constants.SESSION_DELIMITER + "' || RTRIM("+COL_SESSION_ID+") || '" + Constants.SESSION_DELIMITER+"' )"
                +"LIKE ('%" +Constants.SESSION_DELIMITER + "' || " + currentSessionId + " || '" + Constants.SESSION_DELIMITER + "%')";

        String querySessionid = COL_SESSION_ID + " = " + currentSessionId;

        String update = "Update "+table_name;
        String set = "Set "+COL_SESSION_ID+" = "+COL_SESSION_ID+" || '"+ Constants.SESSION_DELIMITER + "' || "+newSessionid;

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();

//            ContentValues values = new ContentValues();
//
//            values.put(COL_SESSION_ID, currentSessionId+Constants.DELIMITER+newSessionid);
//
//            db.update(table_name, values, where, null);
//
//            DBManager.getInstance().closeDatabase();

            String cmd = update + " " + set + " "+
                    " WHERE " + querySessionidInDelimiters
//                    + " OR " + querySessionidBetweenSpaceAndDelimiter
//                    + " OR " + querySessionid
                    ;

            db.execSQL(cmd);

        }catch (Exception e){

            Log.e(TAG, "SessionConcat exception", e);

            CSVHelper.storeToCSV(CSVHelper.CSV_SESSION_CONCAT_CHECK, Utilities.getStackTrace(e));
        }
    }

    public static void updateRecordsInSessionConcat(String table_name, long splittingTime, int currentSessionId, int newSessionid) {

        //get the exact session id in the delimiters
//        String where = " WHERE ( '"+Constants.DELIMITER + "' + RTRIM("+COL_SESSION_ID+") + '" + Constants.DELIMITER+"' )"
//                +" LIKE ('%" +Constants.DELIMITER + "' + " + currentSessionId + " + '" + Constants.DELIMITER + "%')";

        String querySessionidInDelimiters = "( '"+Constants.SESSION_DELIMITER + "' || RTRIM("+COL_SESSION_ID+") || '" + Constants.SESSION_DELIMITER+"' )"
                +"LIKE ('%" +Constants.SESSION_DELIMITER + "' || " + currentSessionId + " || '" + Constants.SESSION_DELIMITER + "%')";

        String querySessionid = COL_SESSION_ID + " = " + currentSessionId;

        String afterSplittingTime = TIME + " > " + splittingTime;

        String update = "Update "+table_name;
        String set = "Set "+COL_SESSION_ID+" = "+COL_SESSION_ID+" || '"+ Constants.SESSION_DELIMITER + "' || "+newSessionid;

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();

//            ContentValues values = new ContentValues();
//
//            values.put(COL_SESSION_ID, currentSessionId+Constants.DELIMITER+newSessionid);
//
//            db.update(table_name, values, where, null);
//
//            DBManager.getInstance().closeDatabase();

            String cmd = update + " " + set + " "+
                    " WHERE " + querySessionidInDelimiters
//                    + " OR " + querySessionidBetweenSpaceAndDelimiter
//                    + " OR " + querySessionid
                    + " AND " + afterSplittingTime
                    ;

            db.execSQL(cmd);

        }catch (Exception e){

            Log.e(TAG, "SessionConcat exception", e);

            CSVHelper.storeToCSV(CSVHelper.CSV_SESSION_CONCAT_CHECK, Utilities.getStackTrace(e));
        }
    }

    /**
     * this is called usally when we want to end a session.
     * @param session_id
     * @param endTime
     * @param sessionLongEnoughFlag
     */
    public static void updateSessionTable(int session_id, long endTime, boolean sessionLongEnoughFlag){

        String where = COL_ID + " = " +  session_id;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(COL_SESSION_END_TIME, endTime);
            values.put(COL_SESSION_LONG_ENOUGH_FLAG, sessionLongEnoughFlag);

            db.update(SESSION_TABLE_NAME, values, where, null);

            DBManager.getInstance().closeDatabase();

//            Log.d(TAG, "test combine: completing updating end time for sesssion" + id );

        }catch(Exception e){

            Log.e(TAG, "exception", e);
            CSVHelper.storeToCSV(CSVHelper.CSV_SESSION_CONCAT_CHECK, Utilities.getStackTrace(e));

        }

    }

    public static void updateSessionTable(int sessionId, int toBeSent){

        String where = COL_ID + " = " +  sessionId;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(COL_SESSION_SENTORNOT_FLAG, toBeSent);

            db.update(SESSION_TABLE_NAME, values, where, null);

            DBManager.getInstance().closeDatabase();

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static void updateSessionTableToCombined(int sessionId, int isCombined){

        String where = COL_ID + " = " +  sessionId;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(COL_SESSION_COMBINEDORNOT_FLAG, isCombined);

            db.update(SESSION_TABLE_NAME, values, where, null);

            DBManager.getInstance().closeDatabase();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void updateSessionTable(int sessionId, int toBeSent, int isCombined){

        String where = COL_ID + " = " +  sessionId;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(COL_SESSION_SENTORNOT_FLAG, toBeSent);
            values.put(COL_SESSION_COMBINEDORNOT_FLAG, isCombined);

            db.update(SESSION_TABLE_NAME, values, where, null);

            DBManager.getInstance().closeDatabase();

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static void deleteSessionTable(int sessionId){

        String where = COL_ID + " = " +  sessionId;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            db.delete(SESSION_TABLE_NAME, where, null);

            DBManager.getInstance().closeDatabase();

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static void updateSessionTable(int sessionId, long startTime , long endTime){

        String where = COL_ID + " = " +  sessionId;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(COL_SESSION_START_TIME, startTime);
            values.put(COL_SESSION_END_TIME, endTime);

            db.update(SESSION_TABLE_NAME, values, where, null);

            DBManager.getInstance().closeDatabase();

        }catch(Exception e){

        }
    }

    public static void updateSessionTable(int sessionId, long endTime, AnnotationSet annotationSet){

        String where = COL_ID + " = " +  sessionId;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            values.put(COL_SESSION_END_TIME, endTime);
            //beacuse only one data(annotation) exist.
            values.put(COL_SESSION_ANNOTATION_SET, annotationSet.toString());
            values.put(COL_SESSION_SENTORNOT_FLAG, Constants.SESSION_SHOULD_BE_SENT_FLAG);

            db.update(SESSION_TABLE_NAME, values, where, null);

            DBManager.getInstance().closeDatabase();

        }catch(Exception e){
            //e.printStackTrace();
        }

    }

    public static ArrayList<String> querySurveyLinkBetweenTimes(long startTime, long endTime) {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + surveyLink_table +
                    " where " + generateTime_col + " > " + startTime + " and " +
                    generateTime_col + " < " + endTime +
                    " order by " + generateTime_col;

           //Log.d(TAG, "[querySurveyLinkBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

        return rows;

    }

    public static ArrayList<String> querySurveyLinks() {

        ArrayList<String> rows = new ArrayList<String>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + surveyLink_table + " order by " + generateTime_col;

            //Log.d(TAG, "[querySurveyLinkBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

        return rows;

    }

    public static String queryLatestSurveyLink() {

        String result = "";

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + surveyLink_table +
                    " order by " + generateTime_col;

           //Log.d(TAG, "[querySurveyLinkBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);
            /*int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }*/

            cursor.moveToLast();
            int columnCount = cursor.getColumnCount();

            for (int i=0; i<columnCount; i++){
                result += cursor.getString(i)+ Constants.DELIMITER;
            }

            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

        return result;

    }

    public static String queryLatestOpenedSurveyLink() {

        String result = "";

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + surveyLink_table + " where " + openFlag_col + " = " + 1 +
                    " order by " + generateTime_col;

           //Log.d(TAG, "[querySurveyLinkBetweenTimes] the query statement is " +sql);

            Cursor cursor = db.rawQuery(sql, null);

            cursor.moveToLast();
            int columnCount = cursor.getColumnCount();

            for (int i=0; i<columnCount; i++){
                result += cursor.getString(i)+ Constants.DELIMITER;
            }

            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

        return result;

    }

    public static ArrayList<String> getSurveysAreOpenedOrMissed(){

        ArrayList<String> resultInArray = new ArrayList<>();

        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT "+openFlag_col +" FROM " + surveyLink_table + " order by " + generateTime_col;
            Cursor cursor = db.rawQuery(sql, null);

            cursor.moveToFirst();
            int rowCount = cursor.getCount();

            for (int i=0; i<rowCount; i++){

                String value = cursor.getString(0);

                resultInArray.add(value);
            }

            cursor.close();
        }catch(Exception e){

        }

        DBManager.getInstance().closeDatabase();

        return resultInArray;
    }

    public static void updateSurveyMissTime(String _id, String colName){

//        Log.d(TAG, "[test show link] updateSurveyMissTime check");

        String where = id + " = " +  _id;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            //missedTime_col
            values.put(colName, ScheduleAndSampleManager.getCurrentTimeInMillis());
            values.put(openFlag_col, 0);
            values.put(sentOrNot_col, Constants.SURVEYLINK_SHOULD_BE_SENT_FLAG);

            db.update(surveyLink_table, values, where, null);

        }catch(Exception e){
            //e.printStackTrace();
        }

        DBManager.getInstance().closeDatabase();

    }

    public static void updateSurveyOpenTime(String _id){

//        Log.d(TAG, "[test show link] updateSurveyOpenTime check");

        String where = id + " = " +  _id;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            //missedTime_col
            values.put(openTime_col, ScheduleAndSampleManager.getCurrentTimeInMillis());
            values.put(openFlag_col, 1);
            values.put(sentOrNot_col, Constants.SURVEYLINK_SHOULD_BE_SENT_FLAG);

            db.update(surveyLink_table, values, where, null);

        }catch(Exception e){

        }

        DBManager.getInstance().closeDatabase();

    }

    public static void updateOverTimeSurvey(long time){

//        Log.d(TAG, "[test show link] updateSurveyOpenTime check");

        String where = generateTime_col + " < " +  time + " and " + openFlag_col + " = " + (-1);

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            //missedTime_col
            values.put(openFlag_col, 2);
            values.put(sentOrNot_col, Constants.SURVEYLINK_SHOULD_BE_SENT_FLAG);

            db.update(surveyLink_table, values, where, null);

        }catch(Exception e){

        }

        DBManager.getInstance().closeDatabase();

    }

    public static void updateSurveyToAlreadyBeenSent(int _id){

//        Log.d(TAG, "[test show link] updateSurveyOpenTime check");

        String where = id + " = " +  _id;

        try{
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            ContentValues values = new ContentValues();

            //missedTime_col
            values.put(sentOrNot_col, Constants.SURVEYLINK_IS_ALREADY_SENT_FLAG);

            db.update(surveyLink_table, values, where, null);

        }catch(Exception e){

        }

        DBManager.getInstance().closeDatabase();

    }

    public static ArrayList<String> queryRecords(String table_name, long startTime, long endTime) {

        ArrayList<String> rows = new ArrayList<String>();

       //Log.d(TAG, "[test show trip] queryRecordsInSession ");
        try{

            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            String sql = "SELECT *"  +" FROM " + table_name  +
                    " where "+ TIME + " > " + startTime +
                    " and " + TIME + " < " + endTime  +
                    " order by " + TIME;

           //Log.d(TAG, "the query statement is " +sql);

            //execute the query
            Cursor cursor = db.rawQuery(sql, null);
            int columnCount = cursor.getColumnCount();
            while(cursor.moveToNext()){
                String curRow = "";
                for (int i=0; i<columnCount; i++){
                    curRow += cursor.getString(i)+ Constants.DELIMITER;
                }
                rows.add(curRow);
            }
            cursor.close();

            DBManager.getInstance().closeDatabase();

        }catch (Exception e){

        }

        return rows;

    }
}
