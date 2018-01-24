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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.R;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.AnnotationSet;
import edu.ohio.minuku.model.DataRecord.LocationDataRecord;
import edu.ohio.minuku.model.Session;

/**
 * Created by Lawrence on 2017/8/11.
 */

public class SessionManager {

    private final static String TAG = "SessionManager";

    public static int sessionid_unStatic;
    private static int trip_size;
    private long lastSessiontime;
    private long lastSessionIDtime;

    private String sessionid;
    private String transportation;
    private String lasttime_transportation;
    private String lasttime_trip_transportation;

    public static final String ANNOTATION_PROPERTIES_ANNOTATION = "Annotation";
    public static final String ANNOTATION_PROPERTIES_ID = "Id";
    public static final String ANNOTATION_PROPERTIES_NAME= "Name";
    public static final String ANNOTATION_PROPERTIES_START_TIME = "Start_time";
    public static final String ANNOTATION_PROPERTIES_END_TIME = "End_time";
    public static final String ANNOTATION_PROPERTIES_IS_ENTIRE_SESSION = "Entire_session";
    public static final String ANNOTATION_PROPERTIES_CONTENT = "Content";
    public static final String ANNOTATION_PROPERTIES_TAG = "Tag";

    public static final long SESSION_MIN_INTERVAL_THRESHOLD_TRANSPORTATION = 1 * Constants.MILLISECONDS_PER_MINUTE;
    public static final long SESSION_MIN_DURATION_THRESHOLD_TRANSPORTATION = 1 * Constants.MILLISECONDS_PER_MINUTE;
    public static final long SESSION_MIN_DISTANCE_THRESHOLD_TRANSPORTATION = 100;  // meters;

    public static int SESSION_DISPLAY_RECENCY_THRESHOLD_HOUR = 24;


    private ArrayList<LocationDataRecord> LocationToTrip;

    private static Context mContext;
    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";
    private CSVWriter csv_writer = null;
    private static CSVWriter csv_writer2 = null;

    private static SessionManager instance;

    private static ArrayList<String> mOngoingSessionIdList;

    private SharedPreferences sharedPrefs;
    private static SharedPreferences.Editor editor;

//    private int testing_count;

    public SessionManager(Context context) {

        this.mContext = context;

        sharedPrefs = context.getSharedPreferences("edu.umich.minuku_2",Context.MODE_PRIVATE);
        editor = context.getSharedPreferences("edu.umich.minuku_2", Context.MODE_PRIVATE).edit();

        sessionid = "0";
//        sessionid_unStatic = 0;

        mOngoingSessionIdList = new ArrayList<String>();


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

    public static SessionManager getInstance() {
        if(SessionManager.instance == null) {
            try {
//                SessionManager.instance = new SessionManager();
                Log.d(TAG,"getInstance without mContext.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return SessionManager.instance;
    }

    public static SessionManager getInstance(Context context) {
        if(SessionManager.instance == null) {
            try {
                SessionManager.instance = new SessionManager(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return SessionManager.instance;
    }

    public static boolean isSessionOngoing(int sessionId) {

        Log.d(TAG, " [test show trips] tyring to see if the session is ongoing:" + sessionId);

        for (int i = 0; i< mOngoingSessionIdList.size(); i++){
            //       Log.d(LOG_TAG, " [getCurRecordingSession] looping to " + i + "th session of which the id is " + mCurRecordingSessions.get(i).getId());

            if (mOngoingSessionIdList.get(i).equals(String.valueOf(sessionId))){
                return true;
            }
        }
        Log.d(TAG, " [test show trips] the session is not ongoing:" + sessionId);

        return false;
    }

    public ArrayList<String> getmOngoingSessionIdList() {
        return mOngoingSessionIdList;
    }

    public void setmOngoingSessionIdList(ArrayList<String> mOngoingSessionIdList) {
        mOngoingSessionIdList = mOngoingSessionIdList;
    }

    public void addOngoingSessionid(String id) {
        edu.ohio.minuku.logger.Log.d(TAG, "test replay: adding ongonig session " + id );
        this.mOngoingSessionIdList.add(id);

    }

    public ArrayList<String> getOngoingSessionList () {
        return mOngoingSessionIdList;
    }

    public void removeOngoingSessionid(String id) {
        edu.ohio.minuku.logger.Log.d(TAG, "test replay: inside removeongogint seesion renove " + id );
        this.mOngoingSessionIdList.remove(id);
        edu.ohio.minuku.logger.Log.d(TAG, "test replay: inside removeongogint seesion the ongoiong list is  " + mOngoingSessionIdList.toString() );
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


    /**
     * This function convert Session String retrieved from the DB to Object Session
     * @param sessionStr
     * @return
     */
    private static Session convertStringToSession(String sessionStr) {

        Session session = null;

        //split each row into columns
        String[] separated = sessionStr.split(Constants.DELIMITER);

        /** get properties of the session **/
        int id = Integer.parseInt(separated[DBHelper.COL_INDEX_SESSION_ID]);
        long startTime = Long.parseLong(separated[DBHelper.COL_INDEX_SESSION_START_TIME]);


        /** 1. create sessions from the properies obtained **/
        session = new Session(id, startTime);

        /**2. get end time (or time of the last record) of the sesison**/

        long endTime = 0;
        //the session could be still ongoing..so we need to check where's endTime
        Log.d(TAG, "[test show trip] separated[DBHelper.COL_INDEX_SESSION_END_TIME] " + separated[DBHelper.COL_INDEX_SESSION_END_TIME]);
        if (!separated[DBHelper.COL_INDEX_SESSION_END_TIME].equals("null") && !separated[DBHelper.COL_INDEX_SESSION_END_TIME].equals("")){
            endTime = Long.parseLong(separated[DBHelper.COL_INDEX_SESSION_END_TIME]);
        }
        //there 's no end time of the session, we take the time of the last record
        else {

            //TODO this should be the last record of the session. For now, we just use the current time
            endTime= ScheduleAndSampleManager.getCurrentTimeInMillis();
            Log.d(TAG, "[test show trip] testgetdata the end time is now:  " + ScheduleAndSampleManager.getTimeString(endTime));
        }

        //set end time
        session.setEndTime(endTime);

        /** 3. get annotaitons associated with the session **/
        JSONObject annotationSetJSON = null;
        JSONArray annotateionSetJSONArray = null;
        try {
            if (!separated[DBHelper.COL_INDEX_SESSION_ANNOTATION_SET].equals("null")){
                annotationSetJSON = new JSONObject(separated[DBHelper.COL_INDEX_SESSION_ANNOTATION_SET]);
                annotateionSetJSONArray = annotationSetJSON.getJSONArray(ANNOTATION_PROPERTIES_ANNOTATION);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //set annotationset if there is one
        if (annotateionSetJSONArray!=null){
            AnnotationSet annotationSet =  toAnnorationSet(annotateionSetJSONArray);
            session.setAnnotationSet(annotationSet);
        }



        return session;

    }

    public static Session getSession (int sessionId) {

        ArrayList<String> res =  DBHelper.querySession(sessionId);
        Log.d(TAG, "[test show trip]query session from LocalDB is " + res);
        Session session = null;

        for (int i=0; i<res.size() ; i++) {

            session = convertStringToSession(res.get(i));
            Log.d(TAG, " test show trip  testgetdata id " + session.getId() + " startTime " + session.getStartTime() + " end time " + session.getEndTime() + " annotation " + session.getAnnotationsSet().toJSONObject().toString());

        }


        return session;
    }

    public static ArrayList<Session> getRecentSessions() {

        ArrayList<Session> sessions = new ArrayList<Session>();

        long queryEndTime = ScheduleAndSampleManager.getCurrentTimeInMillis();
        //start time = a specific hours ago
        long queryStartTime = ScheduleAndSampleManager.getCurrentTimeInMillis() - Constants.MILLISECONDS_PER_HOUR * SESSION_DISPLAY_RECENCY_THRESHOLD_HOUR;

        Log.d(TAG, " [gtest show trip] going to query session between " + ScheduleAndSampleManager.getTimeString(queryStartTime) + " and " + ScheduleAndSampleManager.getTimeString(queryEndTime) );


        //query//get sessions between the starTime and endTime
        ArrayList<String> res =  DBHelper.querySessionsBetweenTimes(queryStartTime, queryEndTime);


        Log.d(TAG, "[test show trip] getRecentSessions get res: " +  res);

        //we start from 1 instead of 0 because the 1st session is the background recording. We will skip it.
        for (int i=0; i<res.size() ; i++) {

            Session session = convertStringToSession(res.get(i));
            sessions.add(session);
        }

        return sessions;
    }


    public static ArrayList<String> getRecordsInSession(int sessionId, String tableName) {

        ArrayList<String> resultList = new ArrayList<String>();

        resultList = DBHelper.queryRecordsInSession(tableName, sessionId);
        Log.d(TAG, "[getRecordsInSession] test combine got " + resultList.size() + " of results from queryRecordsInSession");

        return resultList;
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


    public static AnnotationSet toAnnorationSet(JSONArray annotationJSONArray) {

        AnnotationSet annotationSet = new AnnotationSet();
        ArrayList<Annotation> annotations = new ArrayList<Annotation>();

        for (int i=0 ; i<annotationJSONArray.length(); i++){

            JSONObject annotationJSON = null;
            try {
                Annotation annotation = new Annotation();
                annotationJSON = annotationJSONArray.getJSONObject(i);

                String content = annotationJSON.getString(ANNOTATION_PROPERTIES_CONTENT);
                annotation.setContent(content);

                JSONArray tagsJSONArray = annotationJSON.getJSONArray(ANNOTATION_PROPERTIES_TAG);

                for (int j=0; j<tagsJSONArray.length(); j++){

                    String tag = tagsJSONArray.getString(j);
                    annotation.addTag(tag);
                    Log.d(TAG, "[toAnnorationSet] the content is " + content +  " tag " + tag);
                }

                annotations.add(annotation);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        annotationSet.setAnnotations(annotations);

        Log.d(TAG, "[toAnnorationSet] the annotationSet has  " + annotationSet.getAnnotations().size() + " annotations ");
        return annotationSet;

    }


//    private static void notiQuerySessions(){
//
//        Log.d(TAG,"notiQuerySessions");
//
//        String notiText = "You have queried the Sessions.";
//
//        NotificationManager mNotificationManager =
//                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);//Context.
//        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
//        bigTextStyle.setBigContentTitle("DMS");
//        bigTextStyle.bigText(notiText);
//
//        Notification note = new Notification.Builder(mContext)
//                .setContentTitle(Constants.APP_NAME)
//                .setContentText(notiText)
//                .setStyle(bigTextStyle)
//                .setSmallIcon(R.drawable.self_reflection)
//                .setAutoCancel(true)
//                .build();
//
//        // using the same tag and Id causes the new notification to replace an existing one
//        mNotificationManager.notify(999, note); //String.valueOf(System.currentTimeMillis()),
//        note.flags = Notification.FLAG_AUTO_CANCEL;
//
//    }

}