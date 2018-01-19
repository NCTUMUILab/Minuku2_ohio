package edu.ohio.minuku_2.controller.Ohio;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import edu.ohio.minuku.DBHelper.DBHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.manager.TripManager;
import edu.ohio.minuku_2.R;

import static edu.ohio.minuku.config.Constants.sharedPrefString;

/**
 * Created by Lawrence on 2017/8/29.
 */

public class OhioListAdapter extends ArrayAdapter<String> {

    private static final String TAG = "OhioListAdapter";
    private Context mContext;
    private ArrayList<String> data;
//    private ArrayList<ArrayList<String>> data;

    public static ArrayList<Integer> dataPos;

    private long startTime = -9999;
    private long endTime = -9999;
    private String startTimeString = "";
    private String endTimeString = "";
    private Boolean status;
    private SharedPreferences sharedPrefs;

    public OhioListAdapter(Context context, int resource, ArrayList<String> locationDataRecords) {
        super(context, resource, locationDataRecords);
        this.mContext = context;
        this.data = locationDataRecords;
        dataPos = new ArrayList<Integer>();
        status = false;
//        decideThePosFontType();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.recordinglistview_ohio, parent, false);

        TextView textView = (TextView) view.findViewById(R.id.ListText);

//        Log.d(TAG, "dataPos outside : " + dataPos.size());

        //only one
        String dataFromSessionId = DBHelper.querySession(Integer.valueOf(data.get(position))).get(0);

        String[] dataFromSessionIds = dataFromSessionId.split(Constants.DELIMITER);

        if(dataFromSessionIds[4]==""){
            String time = "Trip "+(position+1)+": "+dataFromSessionIds[2];
            Log.d(TAG, " time : " + time);
//                textView.setText(Html.fromHtml(time));
            textView.setText(time);
            textView.setTextColor(Color.RED);
            status = true;
        }else {
            String time = "Trip "+(position+1)+": "+dataFromSessionIds[2];
            Log.d(TAG, " time : " + time);
//                textView.setText(Html.fromHtml(time));
            textView.setText(time);
            textView.setTextColor(Color.GRAY);
            dataPos.add(position);
        }

/*
        if(dataPos.size()>0){

            int pos = position; //data.size()-1 -

            Log.d(TAG, "pos : " + pos);
            Log.d(TAG, "dataPos : " + dataPos.size());
            Log.d(TAG, "dataPos : " + dataPos.toString());

            if(dataPos.contains(pos)) {
                String dataFromlocal = data.get(data.size()-pos-1);//pos
                String timedata[] = dataFromlocal.split("-");
                String startTime = timedata[0];
                startTime = TripManager.getmillisecondToDateWithTime(Long.valueOf(startTime));
                startTime = formatconverting(startTime);
                String time = "Trip "+(pos+1)+": "+startTime;
                Log.d(TAG, " time : " + time);
                textView.setText(time);
                textView.setTextColor(Color.GRAY);
            }else{
                String dataFromlocal = data.get(data.size()-pos-1);
                String timedata[] = dataFromlocal.split("-");
                String startTime = timedata[0];
                startTime = TripManager.getmillisecondToDateWithTime(Long.valueOf(startTime));
                startTime = formatconverting(startTime);
//                String time = startTime.replace(startTime, "<b>" + startTime + "</b>");
//                String time = "Trip "+(data.size()-pos)+": "+startTime;
                String time = "Trip "+(pos+1)+": "+startTime;
                Log.d(TAG, " time : " + time);
//                textView.setText(Html.fromHtml(time));
                textView.setText(time);
                textView.setTextColor(Color.RED);
                status = true;
            }

        }else{
            Log.d(TAG, "datapos==0");

            String dataFromlocal = data.get(data.size()-position-1);
            String timedata[] = dataFromlocal.split("-");
            String startTime = timedata[0];
            startTime = TripManager.getmillisecondToDateWithTime(Long.valueOf(startTime));
            startTime = formatconverting(startTime);
//            String time = startTime.replace(startTime, "<b>" + startTime + "</b>");
            String time = "Trip "+(position+1)+": "+startTime;
            Log.d(TAG, " time : " + time);
//            textView.setText(Html.fromHtml(time));
            textView.setText(time);
            textView.setTextColor(Color.RED);
            status = true;
        }
*/


        return view;
    }

    public void decideThePosFontType(){
        Log.d(TAG, " decideThePosFontType");

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

        //get all data in cursor
        ArrayList<String> dataInCursor = new ArrayList<String>();
        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            Cursor tripCursor = db.rawQuery("SELECT "+ DBHelper.Trip_startTime + " , " + DBHelper.lat + " , " + DBHelper.lng +" FROM " + DBHelper.annotate_table + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                    +DBHelper.Trip_startTime+" BETWEEN"+" '"+startTimeString+"' "+"AND"+" '"+endTimeString+"' ORDER BY "+DBHelper.Trip_startTime+" ASC", null);

            Log.d(TAG, "SELECT " + DBHelper.Trip_startTime + " , " + DBHelper.lat + " , " + DBHelper.lng + " FROM " + DBHelper.annotate_table + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                    +DBHelper.Trip_startTime+" BETWEEN"+" '"+startTimeString+"' "+"AND"+" '"+endTimeString+"' ORDER BY "+DBHelper.Trip_startTime+" ASC");

            //get all data from cursor
            if(tripCursor.moveToFirst()){
                do{
                    String eachdataInCursor = tripCursor.getString(0);

                    dataInCursor.add(eachdataInCursor);
                    Log.d(TAG, "eachdataInCursor : "+eachdataInCursor);
                }while(tripCursor.moveToNext());
            }else
                Log.d(TAG, " tripCursor.moveToFirst() else");
            tripCursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        int prepos = 0;
        for(int i = data.size()-1 ; i >= 0;i--){
            String datafromList = data.get(i);
            String timedata[] = datafromList.split("-");
            String startTime = timedata[0];

            startTime = TripManager.getmillisecondToDateWithTime(Long.valueOf(startTime));
            Log.d(TAG,"startTime : "+ startTime);
            Log.d(TAG,"dataInCursor : "+ dataInCursor);
            if(dataInCursor.contains(startTime))
                dataPos.add(prepos);
            prepos++;
        }


        if(dataPos.size()!=data.size()){ // != mean there have some trip haven't been done.
            sharedPrefs = mContext.getSharedPreferences(sharedPrefString, mContext.MODE_PRIVATE);
            status = true;
            sharedPrefs.edit().putBoolean("TripStatus",status).apply();

        }else{
            sharedPrefs = mContext.getSharedPreferences(sharedPrefString, mContext.MODE_PRIVATE);
            status = false;
            sharedPrefs.edit().putBoolean("TripStatus",status).apply();
        }
    }

    //Month-Date-Year Time(12hr am/pm)
    private String formatconverting(String east){
        long convertedTime = getSpecialTimeInMillis(east);

//        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        /*SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");

        sdf.format(convertedTime);

        return  sdf.format(convertedTime);*/
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(convertedTime);

//        int mYear = calendar.get(Calendar.YEAR);
//        int mMonth = calendar.get(Calendar.MONTH)+1;
//        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int mhour = calendar.get(Calendar.HOUR);
        int mMin = calendar.get(Calendar.MINUTE);
//        int mSec = calendar.get(Calendar.SECOND);
        int ampm = calendar.get(Calendar.AM_PM);

        String AMPM = "AM";

        /*if(mhour>12)
            mhour -= 12;*/

        if(ampm == Calendar.AM){
            AMPM = "AM";
        }else{
            AMPM = "PM";
            if(mhour==0)
                mhour = 12;
        }

        return addZero(mhour)+":"+addZero(mMin)+" "+AMPM;
    }

    private long getSpecialTimeInMillis(String givenDateFormat){
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

    /*public String TripManager.getmillisecondToDateWithTime(long timeStamp){

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH)+1;
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int mhour = calendar.get(Calendar.HOUR);
        int mMin = calendar.get(Calendar.MINUTE);
        int mSec = calendar.get(Calendar.SECOND);
        int ampm = calendar.get(Calendar.AM_PM);

        String AMPM = "AM";

        if(ampm == Calendar.AM){
            AMPM = "AM";
        }else{
            AMPM = "PM";
        }
        return addZero(mhour)+":"+addZero(mMin)+" "+AMPM;
//        return addZero(mYear)+"/"+addZero(mMonth)+"/"+addZero(mDay)+" "+addZero(mhour)+":"+addZero(mMin)+":"+addZero(mSec);

    }*/
}
