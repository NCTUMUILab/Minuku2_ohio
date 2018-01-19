package edu.ohio.minuku.manager;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import edu.ohio.minuku.DBHelper.DBHelper;
import edu.ohio.minuku.R;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.model.DataRecord.LocationDataRecord;
import edu.ohio.minuku.model.Session;

/**
 * Created by Lawrence on 2017/8/11.
 */

public class TripManager {

    private final static String TAG = "TripManager";

    public static int sessionid_unStatic;
    private static int trip_size;
    private long lastSessiontime;
    private long lastSessionIDtime;

    private String sessionid;
    private String transportation;
    private String lasttime_transportation;
    private String lasttime_trip_transportation;

    private ArrayList<LocationDataRecord> LocationToTrip;


    private static Context mContext;

    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";
    private CSVWriter csv_writer = null;
    private static CSVWriter csv_writer2 = null;

    private static TripManager instance;

    private ArrayList<Integer> ongoingSessionidList;

    private SharedPreferences sharedPrefs;
    private static SharedPreferences.Editor editor;

//    private int testing_count;

    public TripManager(Context context) {

        this.mContext = context;

        sharedPrefs = context.getSharedPreferences("edu.umich.minuku_2",Context.MODE_PRIVATE);
        editor = context.getSharedPreferences("edu.umich.minuku_2", Context.MODE_PRIVATE).edit();

        sessionid = "0";
//        sessionid_unStatic = 0;

        ongoingSessionidList = new ArrayList<Integer>();


        sessionid_unStatic = sharedPrefs.getInt("sessionid_unStatic",0);
        trip_size = sharedPrefs.getInt("trip_size",0);

        transportation = "NA";
//        lasttime_transportation = "NA";

        lasttime_transportation = sharedPrefs.getString("","NA");

        lasttime_trip_transportation = sharedPrefs.getString("lasttime_trip_transportation","NA");

        lastSessiontime = sharedPrefs.getLong("lastSessiontime", -1);
//        testing_count = 0;
        lastSessionIDtime = sharedPrefs.getLong("lastSessionIDtime", -1);
    }

    public static TripManager getInstance() {
        if(TripManager.instance == null) {
            try {
//                TripManager.instance = new TripManager();
                Log.d(TAG,"getInstance without mContext.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TripManager.instance;
    }

    public static TripManager getInstance(Context context) {
        if(TripManager.instance == null) {
            try {
                TripManager.instance = new TripManager(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TripManager.instance;
    }

    public ArrayList<Integer> getOngoingSessionidList() {
        return ongoingSessionidList;
    }

    public void setOngoingSessionidList(ArrayList<Integer> ongoingSessionidList) {
        this.ongoingSessionidList = ongoingSessionidList;
    }

    public void addOngoingSessionid(int id) {
        this.ongoingSessionidList.add((id));
    }

    public void removeOngoingSessionid(int id) {
        this.ongoingSessionidList.remove(id);
    }

    public void setTrip(LocationDataRecord entity) {

        Log.d(TAG,"lasttime_transportation "+lasttime_transportation);
        Log.d(TAG,"sessionid_unStatic "+sessionid_unStatic);

        //setting sessionid
        try {

            if (MinukuStreamManager.getInstance().getTransportationModeDataRecord() != null) {
                transportation = MinukuStreamManager.getInstance().getTransportationModeDataRecord().getConfirmedActivityString();

                Log.d(TAG, "transportation : " + transportation);

            }
        } catch (Exception e) {
            Log.e(TAG, "No TransportationMode, yet.");
            e.printStackTrace();
        }

        /*//for testing
        if(testing_count%5 != 0)
            transportation = "on_foot";
        testing_count++;*/

        if (!transportation.equals("NA")) {

            Log.d(TAG, "lastSessionIDtime "+lastSessionIDtime+", new Date().getTime() "+new Date().getTime());
            Log.d(TAG, "lastSessionIDtime - new Date().getTime())/60000 : "+ ((new Date().getTime() - lastSessionIDtime)/60000));

            if (!transportation.equals("static")) {

                //TODO judging the timing of the previous and recent one.
                if (!transportation.equals(lasttime_transportation)) {

                    long minforDiffTrips = 0;

                    if(lasttime_trip_transportation.equals("in_vehicle")){
                        minforDiffTrips = 5;
                    }else{
                        minforDiffTrips = 2;
                    }

                    float dist = 0;
                    float[] results = new float[1];
                    float lastLat = sharedPrefs.getFloat("lastSessionLat", -999);
                    float lastLng = sharedPrefs.getFloat("lastSessionLng", -999);

                    Location.distanceBetween(lastLat, lastLng, entity.getLatitude(), entity.getLongitude(), results);
                    dist = results[0];

//                    if(((lastSessionIDtime - new Date().getTime())/60000 >= minforDiffTrips && dist > 30)|| lastSessionIDtime == -1){
                        //update it
                        sessionid_unStatic++;

                        //TODO update the last trip into not ongoing
                        String lastSessionid = String.valueOf(sessionid_unStatic-1);
                        Log.d(TAG,"lastSessionid : "+lastSessionid);
                        try{
                            SQLiteDatabase db = DBManager.getInstance().openDatabase();
                            Cursor tripCursor = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ lastSessionid + "'"
                                    +"ORDER BY "+DBHelper.TIME+" ASC", null);
                            Log.d(TAG,"SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " =' "+ lastSessionid + "'"
                                    +"ORDER BY "+DBHelper.TIME+" ASC"); //+" ORDER BY "+DBHelper.TIME+" ASC"
                            int rows = tripCursor.getCount();

                            if(rows!=0){
                                tripCursor.moveToFirst();
                                int first_id = tripCursor.getInt(0);
                                tripCursor.moveToLast();
                                int last_id = tripCursor.getInt(0);
                                ContentValues contentValues = new ContentValues();

                                contentValues.put(DBHelper.ongoingOrNot_col, "false");

                                db.update(DBHelper.trip_table, contentValues, "_id >= ? AND _id <= ?" , new String[] {String.valueOf(first_id), String.valueOf(last_id)});
                            }
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                        lastSessionIDtime = new Date().getTime();
                        lasttime_trip_transportation = transportation;
                        sharedPrefs.edit().putString("lasttime_trip_transportation", lasttime_trip_transportation);
                        sharedPrefs.edit().apply();
//                    }

                }

                sessionid = sessionid + ", " + String.valueOf(sessionid_unStatic); // sessionid: "0, 1"

                sharedPrefs.edit().putFloat("lastSessionLat", entity.getLatitude()).apply();
                sharedPrefs.edit().putFloat("lastSessionLng", entity.getLongitude()).apply();
            }

            LocationDataRecord record = new LocationDataRecord(
                    sessionid,
                    entity.getLatitude(),
                    entity.getLongitude(),
                    entity.getAccuracy());

            Log.d(TAG, String.valueOf(record.getID()) + "," +
                    record.getCreationTime() + "," +
                    record.getSessionid() + "," +
                    record.getLatitude() + "," +
                    record.getLongitude() + "," +
                    record.getAccuracy());

            lastSessiontime = record.getCreationTime();
            sharedPrefs.edit().putLong("lastSessiontime", lastSessiontime).apply();
            sharedPrefs.edit().putLong("lastSessionIDtime", lastSessionIDtime).apply();

//            LocationToTrip.add(record);

            // store to DB
            ContentValues values = new ContentValues();

            try {
                SQLiteDatabase db = DBManager.getInstance().openDatabase();

                values.put(DBHelper.TIME, record.getCreationTime());
                values.put(DBHelper.sessionid_col, record.getSessionid());
                values.put(DBHelper.latitude_col, record.getLatitude());
                values.put(DBHelper.longitude_col, record.getLongitude());
                values.put(DBHelper.Accuracy_col, record.getAccuracy());

                int isTrip = -1;
                if(record.getSessionid().equals("0"))
                    isTrip = 0;
                else
                    isTrip = 1;

                values.put(DBHelper.IsTrip_col, isTrip); // 1 = True
                values.put(DBHelper.transportationMode_col, transportation);
                values.put(DBHelper.ongoingOrNot_col, "true");

                StoreToCSV(record.getCreationTime(), record.getSessionid(), record.getLatitude(), record.getLongitude(), record.getAccuracy(), isTrip);

                db.insert(DBHelper.trip_table, null, values);
            } catch (NullPointerException e) {
                e.printStackTrace();
            } finally {
                values.clear();
                DBManager.getInstance().closeDatabase(); // Closing database connection
            }

            sessionid = "0";

            lasttime_transportation = transportation;

            editor.putString("lasttime_transportation",lasttime_transportation);
            editor.putInt("sessionid_unStatic",sessionid_unStatic);

            editor.commit();


        }
    }

    public void StoreToCSV(long timestamp, String sessionid, double latitude, double longitude, float accuracy, int TF){

        Log.d(TAG,"StoreToCSV");

        String sFileName = "TripOnChange.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"timestamp","timeString","Latitude","Longitude","Accuracy"});
            String timeString = getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp), timeString, sessionid, String.valueOf(latitude), String.valueOf(longitude), String.valueOf(accuracy), String.valueOf(TF)});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static String getTimeString(long time){

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW);
        String currentTimeString = sdf_now.format(time);

        return currentTimeString;
    }

    public int getSessionidForTripSize(){
        return sessionid_unStatic;
    }

    public static ArrayList<String> getTripDatafromSQLite() {
        Log.d(TAG, "getTripDatafromSQLite");

        long funcStartTime = new Date().getTime();

        ArrayList<String> times = new ArrayList<String>();

        //setting today date.
        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        int Year = cal.get(Calendar.YEAR);
        int Month = cal.get(Calendar.MONTH)+1;
        int Day = cal.get(Calendar.DAY_OF_MONTH);

        long startTime = -999;
        long endTime = -999;

        startTime = getSpecialTimeInMillis(makingDataFormat(Year, Month, Day));
        endTime = getSpecialTimeInMillis(makingDataFormat(Year, Month, Day+1));

        SQLiteDatabase db = DBManager.getInstance().openDatabase();
        ArrayList<String> res =  DBHelper.querySessionsBetweenTimes(startTime, endTime);

        ArrayList<Session> sessions = new ArrayList<Session>();

        //we start from 1 instead of 0 because the 1st session is the background recording. We will skip it.
        for (int i=0; i<res.size() ; i++) {

            String sessionStr = res.get(i);

            //split each row into columns
            String[] separated = sessionStr.split(Constants.DELIMITER);

            /** get properties of the session **/
            int id = Integer.parseInt(separated[DBHelper.COL_INDEX_SESSION_ID]);
            long sessionStartTime = Long.parseLong(separated[DBHelper.COL_INDEX_SESSION_START_TIME]);

            /** 1. create sessions from the properies obtained **/
            Session session = new Session(id, sessionStartTime, 0);

            /**2. get end time (or time of the last record) of the session**/

            long sessionEndTime = 0;
            //the session could be still ongoing..so we need to check where's endTime
            if (!separated[DBHelper.COL_INDEX_SESSION_END_TIME].equals("null")){
                sessionEndTime = Long.parseLong(separated[DBHelper.COL_INDEX_SESSION_END_TIME]);
            }
            //there 's no end time of the session, we take the time of the last record
            else {
                sessionEndTime = ScheduleAndSampleManager.getCurrentTimeInMillis();
                Log.d(TAG, "[test get session time] testgetdata the last record time is  " + ScheduleAndSampleManager.getTimeString(sessionEndTime));
            }

            //set end time
            session.setEndTime(sessionEndTime);
 /*
           *//** 3. get annotaitons associated with the session **//*
            JSONObject annotationSetJSON = null;
            JSONArray annotateionSetJSONArray = null;
            try {
                annotationSetJSON = new JSONObject(separated[DBHelper.COL_INDEX_SESSION_ANNOTATION_SET]);
                annotateionSetJSONArray = annotationSetJSON.getJSONArray(ANNOTATION_PROPERTIES_ANNOTATION);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, " testBackgroundLogging [testing load session][getSession]  testgetdata id " + id + " startTime " + sessionStartTime + " end time " + sessionEndTime + " contextsource " + session.getContextSourceNames());


            //set annotationset if there is one
            if (annotateionSetJSONArray!=null){
                AnnotationSet annotationSet =  toAnnorationSet(annotateionSetJSONArray);
                session.setAnnotationSet(annotationSet);
            }

*/

            sessions.add(session);
        }

        for(int index = 0 ; index < sessions.size() ; index ++){

            Session session = sessions.get(index);
            int sessionid = session.getId();

            ArrayList<String> result = DBHelper.queryRecordsInSession(DBHelper.location_table, sessionid);

            //notification
            notiQuerySessions();


        }

        //TODO do the Combination here, before the Combination, filter the isTrip col == 0 which menas its not trip
/*
        for(int i=sessionid_unStatic-1;i>=1;i--) {
            try {
//                SQLiteDatabase db = DBManager.getInstance().openDatabase();
                Cursor tripCursor1 = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ i + "'"+ " AND "
                        + DBHelper.TIME + " BETWEEN" + " '" + startTime + "' " + "AND" + " '" + endTime + "' "
                        + "AND " + DBHelper.IsTrip_col + " = 1 " + "ORDER BY " + DBHelper.TIME + " ASC", null);
                Log.d(TAG, "SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ i + "'"+ " AND "
                        + DBHelper.TIME + " BETWEEN" + " '" + startTime + "' " + "AND" + " '" + endTime + "' "
                        + "AND " + DBHelper.IsTrip_col + " = 1 " + "ORDER BY " + DBHelper.TIME + " ASC"); //+" ORDER BY "+DBHelper.TIME+" ASC"
                int rows1 = tripCursor1.getCount();

                Cursor tripCursor2 = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ (i+1) + "'"+ " AND "
                        + DBHelper.TIME + " BETWEEN" + " '" + startTime + "' " + "AND" + " '" + endTime + "' "
                        + "AND " + DBHelper.IsTrip_col + " = 1 " + "ORDER BY " + DBHelper.TIME + " ASC", null);
                Log.d(TAG, "SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ (i+1) + "'"+ " AND "
                        + DBHelper.TIME + " BETWEEN" + " '" + startTime + "' " + "AND" + " '" + endTime + "' "
                        + "AND " + DBHelper.IsTrip_col + " = 1 " + "ORDER BY " + DBHelper.TIME + " ASC"); //+" ORDER BY "+DBHelper.TIME+" ASC"
                int rows2 = tripCursor2.getCount();

                if (rows1 != 0 && rows2 != 0) {
                    tripCursor1.moveToLast();
                    tripCursor2.moveToFirst();

                    long diff = Long.valueOf(tripCursor2.getString(1)) - Long.valueOf(tripCursor1.getString(1));

                    Log.d(TAG, "diff : "+ diff);

                    long minforDiffTrips = 2;
                    long maxforDiffTrips = 10;

                    String lastTransportation = tripCursor1.getString(7);
                    String currentTransportation = tripCursor2.getString(7);

                   *//* if(lastTransportation.equals("in_vehicle")){
                        minforDiffTrips = 5;
                    }else{
                        minforDiffTrips = 4;
                    }*//*

                    String latestsessionid = tripCursor1.getString(2);

                    tripCursor2.moveToFirst();
                    int first_id = tripCursor2.getInt(0);
                    tripCursor2.moveToLast();
                    int last_id = tripCursor2.getInt(0);

                    //if the different times between the trips are < 10 min
                    //and the transportation mode are same
                    if(diff/60000 < maxforDiffTrips && lastTransportation.equals(currentTransportation)){
                        ContentValues contentValues = new ContentValues();

                        contentValues.put(DBHelper.sessionid_col, latestsessionid); //0 = False

                        db.update(DBHelper.trip_table, contentValues, "_id >= ? AND _id <= ?" , new String[] {String.valueOf(first_id), String.valueOf(last_id)});
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */

        //filter < 100m trip its isTrip to 0
        /*
        for(int i=sessionid_unStatic;i>=1;i--){
            try {
                Cursor tripCursor = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ i + "'"
                        +" AND "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' "+"ORDER BY "+DBHelper.TIME+" ASC", null);
                Log.d(TAG,"SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ i + "'"
                        +" AND "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' "+"ORDER BY "+DBHelper.TIME+" ASC"); //+" ORDER BY "+DBHelper.TIME+" ASC"
                int rows = tripCursor.getCount();

                if(rows!=0){

                    float dist = 0;
                    float[] results = new float[1];

                    tripCursor.moveToFirst();
                    float lat = tripCursor.getFloat(3);
                    float lng = tripCursor.getFloat(4);
                    int first_id = tripCursor.getInt(0);

                    //the displacement from start to end <100m trip
                    tripCursor.moveToLast();
                    float lat2 = tripCursor.getFloat(3);
                    float lng2 = tripCursor.getFloat(4);
                    int last_id = tripCursor.getInt(0);

                    Location.distanceBetween(lat,lng,lat2,lng2,results);
                    dist += results[0];

                    //the whole path <100m trip
*//*
                    tripCursor.moveToNext();
                    do{
                        float lat2 = tripCursor.getFloat(3);
                        float lng2 = tripCursor.getFloat(4);
                        Location.distanceBetween(lat,lng,lat2,lng2,results);
                        dist += results[0];
                        lat = lat2;
                        lng = lng2;
                    }while (tripCursor.moveToNext());
*//*

                    //TODO filter < 100m trip
                    Log.d(TAG, "dist : "+ dist);
                    if(dist<100) {
                        //set the IsTrip in table to False
                        ContentValues contentValues = new ContentValues();

                        contentValues.put(DBHelper.IsTrip_col, 0); //0 = False

                        db.update(DBHelper.trip_table, contentValues, "_id >= ? AND _id <= ?" , new String[] {String.valueOf(first_id), String.valueOf(last_id)});
//                        continue;
                    }
                    tripCursor.moveToFirst();
                    String firstTime = tripCursor.getString(1);
//                    String firstTime = getmillisecondToDateWithTime(Long.valueOf(tripCursor.getString(1))); //tripCursor.getString(1);
                    tripCursor.moveToLast();
                    String lastTime = tripCursor.getString(1);
//                    String lastTime = getmillisecondToDateWithTime(Long.valueOf(tripCursor.getString(1))); //tripCursor.getString(1);

                    Log.d(TAG, firstTime+"-"+lastTime);

                }else
                    Log.d(TAG, "rows==0");


            }catch (Exception e){
                e.printStackTrace();
            }

        }*/

        //add filter the isTrip == 0 ones, don't show it.
        /*
        for(int i=sessionid_unStatic;i>=1;i--){
            try {
//                SQLiteDatabase db = DBManager.getInstance().openDatabase();
                Cursor tripCursor = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ i + "'"
                        +" AND "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' "
                        + "AND " + DBHelper.IsTrip_col + " = 1 "+"ORDER BY "+DBHelper.TIME+" ASC", null);
                Log.d(TAG,"SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ i + "'"
                        +" AND "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' "
                        + "AND " + DBHelper.IsTrip_col + " = 1 "+"ORDER BY "+DBHelper.TIME+" ASC"); //+" ORDER BY "+DBHelper.TIME+" ASC"
                int rows = tripCursor.getCount();

                if(rows!=0){
                    tripCursor.moveToFirst();
                    String firstTime = tripCursor.getString(1);
//                    String firstTime = getmillisecondToDateWithTime(Long.valueOf(tripCursor.getString(1))); //tripCursor.getString(1);
                    tripCursor.moveToLast();
                    String lastTime = tripCursor.getString(1);
//                    String lastTime = getmillisecondToDateWithTime(Long.valueOf(tripCursor.getString(1))); //tripCursor.getString(1);

                    Log.d(TAG, firstTime+"-"+lastTime);

                    times.add(firstTime+"-"+lastTime);
                }else{
                    Log.d(TAG, "rows==0");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }*/

        //checking "isTrip" in the csv file
        /*for(int i=sessionid_unStatic;i>=1;i--){
            try {
                SQLiteDatabase db = DBManager.getInstance().openDatabase();
                Cursor tripCursor = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ i + "'"
                        +" AND "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' "+"ORDER BY "+DBHelper.TIME+" ASC", null);
                Log.d(TAG,"SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ i + "'"
                        +" AND "+DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' "+"ORDER BY "+DBHelper.TIME+" ASC"); //+" ORDER BY "+DBHelper.TIME+" ASC"
                int rows = tripCursor.getCount();

                if(rows!=0){
                    do{
                        tripCursor.moveToFirst();

                        StoreToCSV(
                                Long.valueOf(tripCursor.getString(1)),
                                tripCursor.getInt(0),
                                tripCursor.getString(2),
                                tripCursor.getDouble(3),
                                tripCursor.getDouble(4),
                                tripCursor.getFloat(5),
                                tripCursor.getInt(6)
                        );
                    }while (tripCursor.moveToNext());
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }*/

        trip_size = times.size();
        editor.putInt("trip_size",trip_size);

        Log.d(TAG,"getTripDatafromSQLite total time : "+(new Date().getTime() - funcStartTime)/1000);

        return times;
    }

    private static void notiQuerySessions(){

        Log.d(TAG,"notiQuerySessions");

        String notiText = "You have queried the Sessions.";

        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);//Context.
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(notiText);

        Notification note = new Notification.Builder(mContext)
                .setContentTitle(Constants.APP_NAME)
                .setContentText(notiText)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.drawable.self_reflection)
                .setAutoCancel(true)
                .build();

        // using the same tag and Id causes the new notification to replace an existing one
        mNotificationManager.notify(999, note); //String.valueOf(System.currentTimeMillis()),
        note.flags = Notification.FLAG_AUTO_CANCEL;

    }

    public static void StoreToCSV(long timestamp, int id, String sessionid, double latitude, double longitude, float accuracy, int TF){

//        Log.d(TAG,"StoreToCSV");

        String sFileName = "CheckIsTrip.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            csv_writer2 = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

//            data.add(new String[]{"timestamp","timeString","Latitude","Longitude","Accuracy"});
            String timeString = getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp), timeString, sessionid, String.valueOf(latitude), String.valueOf(longitude), String.valueOf(accuracy), String.valueOf(TF)});

            csv_writer2.writeAll(data);

            csv_writer2.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public int getTrip_size(){
        return trip_size;
    }

    public ArrayList<LatLng> getTripLocToDrawOnMap(int position) {
        ArrayList<LatLng> latLngs = new ArrayList<LatLng>();
        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            Cursor tripCursor = db.rawQuery("SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ (position) + "'", null); //cause pos start from 0.
            Log.d(TAG,"SELECT * FROM " + DBHelper.trip_table + " WHERE "+ DBHelper.sessionid_col+ " ='0, "+ (position) + "'");
            int rows = tripCursor.getCount();

            if(rows!=0){
                tripCursor.moveToFirst();
                for(int i=0;i<rows;i++) {
                    Float lat = tripCursor.getFloat(3);
                    Float lng = tripCursor.getFloat(4);

                    Log.d(TAG,"lat"+String.valueOf(lat)+", lng"+String.valueOf(lng));

                    LatLng latLng = new LatLng(lat,lng);

                    latLngs.add(latLng);

                    tripCursor.moveToNext();
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return latLngs;

    }

    private static String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }

    public static long getSpecialTimeInMillis(String givenDateFormat){
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW);
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

    public static String makingDataFormat(int year,int month,int date){
        String dataformat= "";

//        dataformat = addZero(year)+"-"+addZero(month)+"-"+addZero(date)+" "+addZero(hour)+":"+addZero(min)+":00";
        dataformat = addZero(year)+"/"+addZero(month)+"/"+addZero(date)+" "+"00:00:00";
        Log.d(TAG,"dataformat : " + dataformat);

        return dataformat;
    }

    public static String getmillisecondToDateWithTime(long timeStamp){

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH)+1;
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int mhour = calendar.get(Calendar.HOUR_OF_DAY);
        int mMin = calendar.get(Calendar.MINUTE);
        int mSec = calendar.get(Calendar.SECOND);
        int ampm = calendar.get(Calendar.AM_PM);

//        return addZero(mhour)+":"+addZero(mMin)+" "+ampm;
        return addZero(mYear)+"/"+addZero(mMonth)+"/"+addZero(mDay)+" "+addZero(mhour)+":"+addZero(mMin)+":"+addZero(mSec);

    }
}