package edu.ohio.minuku_2.controller;

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

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/9/17.
 */

public class OhioLinkListAdapter extends ArrayAdapter<String> {

    private final String TAG = "OhioLinkListAdapter";

    private Context mContext;
    public static ArrayList<String> data;
    public static String taskTable;
    public static ArrayList<Integer> dataPos;
    public static long startTime = -9999;
    public static long endTime = -9999;
    private String startTimeString = "";
    private String endTimeString = "";
    private Boolean status;
    private SharedPreferences sharedPrefs;

    public OhioLinkListAdapter(Context context, int resource, ArrayList<String> dataRecords) {
        super(context, resource, dataRecords);

        this.mContext = context;
        dataPos = new ArrayList<Integer>();
        data = new ArrayList<String>();
        taskTable = null;
        status = false;
        decideThePosFontType();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //Log.d(TAG, "getView");

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.recordinglistview_ohio, parent, false);

        TextView textView = (TextView) view.findViewById(R.id.ListText);

        //Log.d(TAG, "dataPos outside : " + dataPos.size());

        if(data.size()>0 && dataPos.size()>0){

            int pos = position; //data.size()-1 -

            //Log.d(TAG, "pos : " + pos);

            //Log.d(TAG, "dataPos : " + dataPos.size());

            if(!dataPos.contains(pos)) {
                String dataFromlocal = data.get(pos);
                //Log.d(TAG, " dataFromlocal : " + dataFromlocal);
                String survey = "Survey "+(pos+1);
                textView.setText(survey);
                textView.setTextColor(Color.GRAY);
                view.setClickable(false);
            }else{
                String dataFromlocal = data.get(pos);
//                String link = dataFromlocal.replace(dataFromlocal, "<b>" + dataFromlocal + "</b>");
                String link = dataFromlocal;
                //Log.d(TAG, " link : " + link);
//                textView.setText(Html.fromHtml(link));
                String survey = "Survey "+(pos+1);
                textView.setText(survey);
                textView.setTextColor(Color.RED);
//                if(data.size()==(pos+1)){ //when the newest link haven't been done.
            }

            /*if(dataPos.contains(0)){
                sharedPrefs = mContext.getSharedPreferences(sharedPrefString, mContext.MODE_PRIVATE);
                status = true;
                sharedPrefs.edit().putBoolean("SurveyStatus",status).apply();
            }else{
                sharedPrefs = mContext.getSharedPreferences(sharedPrefString, mContext.MODE_PRIVATE);
                status = false;
                sharedPrefs.edit().putBoolean("SurveyStatus",status).apply();
            }*/

        }else{
            String dataFromlocal = data.get(position);
//            String link = dataFromlocal.replace(dataFromlocal, "<b>" + dataFromlocal + "</b>");
            String link = dataFromlocal;
            //Log.d(TAG, " link : " + link);
//            textView.setText(Html.fromHtml(link));
            String survey = "Survey "+(position+1);
            textView.setText(survey);
            textView.setTextColor(Color.GRAY);
        }


        return view;
    }

    public void decideThePosFontType(){
        //Log.d(TAG, " decideThePosFontType");

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

        taskTable = null;

        taskTable = DBHelper.surveyLink_table;

        //get all data in cursor
        ArrayList<Integer> dataInCursor = new ArrayList<Integer>();
        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            Cursor tripCursor = db.rawQuery("SELECT "+ DBHelper.openFlag_col +", "+ DBHelper.link_col +" FROM " + taskTable + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
                    +DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ORDER BY "+DBHelper.TIME+" DESC", null);

            //Log.d(TAG, "SELECT "+ DBHelper.openFlag_col +", "+ DBHelper.link_col +" FROM " + taskTable + " WHERE " //+ DBHelper.Trip_id + " ='" + position + "'" +" AND "
//                    +DBHelper.TIME+" BETWEEN"+" '"+startTime+"' "+"AND"+" '"+endTime+"' ORDER BY "+DBHelper.TIME+" DESC");

            //get all data from cursor
            int i = 0;
            if(tripCursor.moveToFirst()){
                do{
                    int eachdataInCursor = tripCursor.getInt(0);
                    String link = tripCursor.getString(1);

                    //Log.d(TAG, " 0 : "+ eachdataInCursor+ ", 1 : "+ link);

                    if(eachdataInCursor==0){ //0 represent they haven't click into it.
                        dataPos.add(i);
                    }
                    i++;
                    data.add(link);
                    //Log.d(TAG, " link : "+ link);

                    //Log.d(TAG, " tripCursor.moveToFirst()");
                }while(tripCursor.moveToNext());
            }else
                //Log.d(TAG, " tripCursor.moveToFirst() else");
            tripCursor.close();
        }catch (Exception e){
            //e.printStackTrace();
        }

        /*for(int i = 0; i < data.size();i++){
            String datafromList = data.get(i);
            String timedata[] = datafromList.split("-");
            String startTime = timedata[0];
            if(dataInCursor.contains(startTime))
                dataPos.add(i);
        }*/
    }

    private long getSpecialTimeInMillis(String givenDateFormat){
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_SLASH);
        long timeInMilliseconds = 0;
        try {
            Date mDate = sdf.parse(givenDateFormat);
            timeInMilliseconds = mDate.getTime();
            //Log.d(TAG,"Date in milli :: " + timeInMilliseconds);
        } catch (ParseException e) {
            //e.printStackTrace();
        }
        return timeInMilliseconds;
    }

    public String makingDataFormat(int year,int month,int date){
        String dataformat= "";

//        dataformat = addZero(year)+"-"+addZero(month)+"-"+addZero(date)+" "+addZero(hour)+":"+addZero(min)+":00";
        dataformat = addZero(year)+"/"+addZero(month)+"/"+addZero(date)+" "+"00:00:00";
        //Log.d(TAG,"dataformat : " + dataformat);

        return dataformat;
    }

    private String addZero(int date){
        if(date<10)
            return String.valueOf("0"+date);
        else
            return String.valueOf(date);
    }
}
