package edu.ohio.minuku_2.controller.Ohio;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.DataHandler;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku_2.R;
import edu.ohio.minuku_2.service.SurveyTriggerService;

/**
 * Created by Lawrence on 2017/9/16.
 */

public class SurveyActivity extends Activity {

    private final static String TAG = "SurveyActivity";

    private Button surveyButton, testButton;
    private TextView totalView, missedView, openedView, lastOpenedView, mobileMissedView, randomMissedView;
    private ListView listview;
    private ArrayList<String> data;

    private int notifyID = 1;

    private int test_notitypeNum = 0;

    private SharedPreferences sharedPrefs;

    private NotificationManager mNotificationManager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surveypage);

        data = new ArrayList<String>();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG,"onResume");

        initlinkListohio();

    }

    private void initlinkListohio(){

        Log.d(TAG,"initlinkListohio");

        sharedPrefs = getSharedPreferences("edu.umich.minuku_2", MODE_PRIVATE);

        /* preparing all the data for this page. */
        long startTime = -9999;
        long endTime = -9999;
        String startTimeString = "";
        String endTimeString = "";

        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        int Year = cal.get(Calendar.YEAR);
        int Month = cal.get(Calendar.MONTH)+1;
        int Day = cal.get(Calendar.DAY_OF_MONTH);

        startTimeString = makingDataFormat(Year, Month, Day);
        endTimeString = makingDataFormat(Year, Month, Day+1);
        startTime = getSpecialTimeInMillis(startTimeString);
        endTime = getSpecialTimeInMillis(endTimeString);

        data = new ArrayList<String>();
        data = DataHandler.getSurveyData(startTime, endTime);

        Log.d(TAG, "SurveyData : "+ data.toString());


        //setting the page view
        surveyButton = (Button) findViewById(R.id.surveyButton);
        surveyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //set opened time.
                //if they click it, set the openFlag to 1.
                String id = data.get(data.size()-1).split(Constants.DELIMITER)[0];
                DataHandler.updateSurveyOpenFlagAndTime(id);

                //we schedule the next interval random survey after the user clicks on the survey
                SurveyTriggerService.setUpNextIntervalSurvey();

                //get the latest link in the surveyLink table.
                String link = data.get(data.size()-1).split(Constants.DELIMITER)[1];

                Log.d(TAG, "the latest link is : "+link);

                Intent resultIntent = new Intent(Intent.ACTION_VIEW);
                resultIntent.setData(Uri.parse(link)); //get the link from adapter

                startActivity(resultIntent);

            }
        });

        //for testing, could deprecate it after we complete all the work
        testButton = (Button) findViewById(R.id.triggerButton);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try{
                    mNotificationManager.cancel(notifyID);
                }catch (Exception e){
                    e.printStackTrace();
                    Log.d(TAG, "no old test notification");
                }

                triggerSurveyByButton();
                addSurveyLinkToDB();

            }
        });

        //parsing the data
        int total = data.size();
        int missCount = 0;
        int openCount = 0;
        String lastOpenedTime;

        if(total == 0){
            surveyButton.setEnabled(false);
        }else{
            surveyButton.setEnabled(true);
        }

        for(String datapart : data){
            //if the link havn't been opened.
            if(datapart.split(Constants.DELIMITER)[5].equals("0"))
                missCount++;
            else if(datapart.split(Constants.DELIMITER)[5].equals("1"))
                openCount++;
        }

        //getting the last opened time
        String latestOpenedSurveyData = DataHandler.getLatestOpenedSurveyData();

        //because the position of the opened time is fourth
        try {

            long opendTime = Long.valueOf(latestOpenedSurveyData.split(Constants.DELIMITER)[3]);

            SimpleDateFormat sdf_Hour_Min = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN_AMPM);

            lastOpenedTime = ScheduleAndSampleManager.getTimeString(opendTime, sdf_Hour_Min);

        }
        //at the very first time of entering this pages, there are no data in the DB.
        catch (ArrayIndexOutOfBoundsException e){
            lastOpenedTime = "NA";
        }

        totalView = (TextView) findViewById(R.id.totalView);
        missedView = (TextView) findViewById(R.id.missedView);
        openedView = (TextView) findViewById(R.id.openedView);
        lastOpenedView = (TextView) findViewById(R.id.last_opened_time_view);

        mobileMissedView = (TextView) findViewById(R.id.mobileMissedView);
        randomMissedView = (TextView) findViewById(R.id.randomMissedView);

        totalView.setText(String.valueOf(total));
        missedView.setText(String.valueOf(missCount));
//        openedView.setText(String.valueOf(openCount));
        lastOpenedView.setText(lastOpenedTime);

        String MobileMissedCount = sharedPrefs.getString("mobileMissedCount", "");
        String RandomMissedCount = sharedPrefs.getString("randomMissedCount", "");
        String OpenCount = sharedPrefs.getString("OpenCount", "");

        mobileMissedView.setText(MobileMissedCount);
        randomMissedView.setText(RandomMissedCount);
        openedView.setText(OpenCount);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            Log.d(TAG, " onKeyDown");

            Intent intent = getIntent();
            Bundle bundle = new Bundle();
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public ArrayList<String> getData(){

        Log.d(TAG, " getData");

        ArrayList<String> data = new ArrayList<String>();

        long startTime = -9999;
        long endTime = -9999;
        String startTimeString = "";
        String endTimeString = "";

        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        int Year = cal.get(Calendar.YEAR);
        int Month = cal.get(Calendar.MONTH)+1;
        int Day = cal.get(Calendar.DAY_OF_MONTH);

        startTimeString = makingDataFormat(Year, Month, Day);
        endTimeString = makingDataFormat(Year, Month, Day+1);
        startTime = getSpecialTimeInMillis(startTimeString);
        endTime = getSpecialTimeInMillis(endTimeString);

        String taskTable = DBHelper.surveyLink_table;

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            Cursor tripCursor = db.rawQuery("SELECT "+ DBHelper.openFlag_col +", "+ DBHelper.link_col +" FROM " + taskTable + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                    +DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ORDER BY "+DBHelper.TIME+" DESC", null);

            Log.d(TAG, "SELECT "+ DBHelper.openFlag_col +", "+ DBHelper.link_col +" FROM " + taskTable + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                    +DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ORDER BY "+DBHelper.TIME+" DESC");

            //get all data from cursor
            int i = 0;
            if(tripCursor.moveToFirst()){
                do{
                    int eachdataInCursor = tripCursor.getInt(0);
                    String link = tripCursor.getString(1);

                    Log.d(TAG, " 0 : "+ eachdataInCursor+ ", 1 : "+ link);

                    data.add(link);
                    Log.d(TAG, " link : "+ link);

                    Log.d(TAG, " tripCursor.moveToFirst()");
                }while(tripCursor.moveToNext());
            }else
                Log.d(TAG, " tripCursor.moveToFirst() else");
            tripCursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return data;
    }

    public String getLinkTime(int position){

        String eachdataInCursor = "NA";

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            Cursor tripCursor = db.rawQuery("SELECT "+ DBHelper.TIME +" FROM " + OhioLinkListAdapter.taskTable + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                    +DBHelper.TIME+" BETWEEN"+" '"+OhioLinkListAdapter.startTime+"' "+"AND"+" '"+OhioLinkListAdapter.endTime+"' ORDER BY "+DBHelper.TIME+" DESC", null);

            Log.d(TAG, "SELECT "+ DBHelper.TIME +" FROM " + OhioLinkListAdapter.taskTable + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                    +DBHelper.TIME+" BETWEEN"+" '"+OhioLinkListAdapter.startTime+"' "+"AND"+" '"+OhioLinkListAdapter.endTime+"' ORDER BY "+DBHelper.TIME+" DESC");

            //get all data from cursor
            if(tripCursor.moveToPosition(position))
                eachdataInCursor = tripCursor.getString(0);
            else
                Log.d(TAG, " tripCursor.moveToPosition() else");
            tripCursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        return eachdataInCursor;
    }

    private long getSpecialTimeInMillis(String givenDateFormat){
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE_Slash);
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

    public String makingDataFormat(int year,int month,int date){
        String dataformat= "";

//        dataformat = addZero(year)+"-"+addZero(month)+"-"+addZero(date)+" "+addZero(hour)+":"+addZero(min)+":00";
        dataformat = addZero(year)+"/"+addZero(month)+"/"+addZero(date)+" "+"00:00:00";
        Log.d(TAG,"dataformat : " + dataformat);

        return dataformat;
    }

    private String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }

    //for Testing
    private void triggerSurveyByButton(){

        //TODO if the last link haven't been opened, setting it into missed.
        String latestLinkData = DataHandler.getLatestSurveyData();
        //if there have data in DB
        if(!latestLinkData.equals("")) {

            String clickOrNot = latestLinkData.split(Constants.DELIMITER)[5];

            String id = latestLinkData.split(Constants.DELIMITER)[0];

            DataHandler.updateSurveyMissTime(id, DBHelper.missedTime_col);

        }

        String notiText = "You have a new random survey(Artifical)";

        Log.d(TAG,"intervalQualtrics");

        Intent resultIntent = new Intent(SurveyActivity.this, SurveyActivity.class);
        PendingIntent pending = PendingIntent.getActivity(SurveyActivity.this, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);//Context.
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle("DMS");
        bigTextStyle.bigText(notiText);

        Notification note = new Notification.Builder(this)
                .setContentTitle(Constants.APP_NAME)
                .setContentText(notiText)
                .setContentIntent(pending)
                .setStyle(bigTextStyle)
                .setSmallIcon(R.drawable.self_reflection)
                .setAutoCancel(true)
                .build();

        // using the same tag and Id causes the new notification to replace an existing one
        mNotificationManager.notify(notifyID, note); //String.valueOf(System.currentTimeMillis()),
        note.flags = Notification.FLAG_AUTO_CANCEL;

    }

    public void addSurveyLinkToDB(){
        Log.d(TAG, "addSurveyLinkToDB");

        settingMissedClickedCount();

        String noti_type;

        test_notitypeNum++;
        if(test_notitypeNum %2 ==0)
            noti_type = "walk";
        else
            noti_type = "random";

        String link = "https://osu.az1.qualtrics.com/jfe/form/SV_6xjrFJF4YwQwuMZ";

        SharedPreferences sharedPrefs = getSharedPreferences("edu.umich.minuku_2", MODE_PRIVATE);
        String participantID = sharedPrefs.getString("userid", "NA");
        String groupNum = sharedPrefs.getString("groupNum", "NA");

        String linktoShow = link + "?p="+participantID + "&g=" + groupNum + "&w=" + "1" + "&d=" + "1" + "&r=" + "1" + "&m=" + "0";

        ContentValues values = new ContentValues();

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            values.put(DBHelper.generateTime_col, new Date().getTime());
            values.put(DBHelper.link_col, linktoShow);
            values.put(DBHelper.surveyType_col, noti_type);
//            values.put(DBHelper.openFlag_col, 0); //they can't enter the link by the notification.

            db.insert(DBHelper.surveyLink_table, null, values);

        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
        finally {
            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection
        }
    }

    private void settingMissedClickedCount(){
        //TODO to check the missing count and clicked
        //setting the
        long startTime = -9999;
        long endTime = -9999;
        String startTimeString = "";
        String endTimeString = "";

        Calendar cal = Calendar.getInstance();
        Date date = new Date();
        cal.setTime(date);
        int Year = cal.get(Calendar.YEAR);
        int Month = cal.get(Calendar.MONTH)+1;
        int Day = cal.get(Calendar.DAY_OF_MONTH);

        startTimeString = makingDataFormat(Year, Month, Day);
        endTimeString = makingDataFormat(Year, Month, Day+1);
        startTime = getSpecialTimeInMillis(startTimeString);
        endTime = getSpecialTimeInMillis(endTimeString);

        ArrayList<String> data = new ArrayList<String>();
        data = DataHandler.getSurveyData(startTime, endTime);

        Log.d(TAG, "SurveyData : "+ data.toString());

        int mobileMissedCount = 0;
        int randomMissedCount = 0;
        int missCount = 0;
        int openCount = 0;

        for(String datapart : data){
            Log.d(TAG, "datapart : " + datapart);
            Log.d(TAG, "datapart [5] : " + datapart.split(Constants.DELIMITER)[5]);
            Log.d(TAG, "datapart [5] == 1 : " + datapart.split(Constants.DELIMITER)[5].equals("1"));

            //if the link havn't been opened.
            if(datapart.split(Constants.DELIMITER)[5].equals("0")){
                missCount++;
                if(datapart.split(Constants.DELIMITER)[6].equals("walk"))
                    mobileMissedCount++;
                else if(datapart.split(Constants.DELIMITER)[6].equals("random"))
                    randomMissedCount++;
            }
            else if(datapart.split(Constants.DELIMITER)[5].equals("1"))
                openCount++;
        }

        String previousMobileMissedCount = sharedPrefs.getString("mobileMissedCount", "");
        String previousRandomMissedCount = sharedPrefs.getString("randomMissedCount", "");
        String previousOpenCount = sharedPrefs.getString("OpenCount", "");

        previousMobileMissedCount += " "+mobileMissedCount;
        previousRandomMissedCount += " "+randomMissedCount;
        previousOpenCount += " "+openCount;

        sharedPrefs.edit().putString("mobileMissedCount", previousMobileMissedCount).apply();
        sharedPrefs.edit().putString("randomMissedCount", previousRandomMissedCount).apply();
        sharedPrefs.edit().putString("OpenCount", previousOpenCount).apply();

    }

}
