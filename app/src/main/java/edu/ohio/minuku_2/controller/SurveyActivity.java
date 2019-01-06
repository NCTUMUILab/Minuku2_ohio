package edu.ohio.minuku_2.controller;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.Data.DataHandler;
import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Config;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/9/16.
 */

public class SurveyActivity extends Activity {

    private final static String TAG = "SurveyActivity";

    private TextView surveyDayText;

    private Button survey1_Button, survey2_Button, survey3_Button,
            survey4_Button, survey5_Button, survey6_Button;

    private ArrayList<String> surveyDatas = new ArrayList<>();
    private ArrayList<Integer> linkNums = new ArrayList<>();

    private ArrayList<String> buttonState;
    private ArrayList<Button> buttons;

    private SharedPreferences sharedPrefs;

    private final String TEXT_Unavailable = "Unavailable";
    private final String TEXT_Available = " Available ";/*the space is to padding the border*/
    private final String TEXT_COMPLETED = "Activated";
    private final String TEXT_MISSED = "Missed";
    private final String TEXT_ERROR = "Error";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Log.d(TAG, "daysInSurvey : "+ Constants.daysInSurvey);

        if(Config.daysInSurvey == 0 || Config.daysInSurvey == -1) {

            setContentView(R.layout.surveypage_day0);
        }else if(Config.daysInSurvey > Constants.FINALDAY){

            setContentView(R.layout.surveypage_complete);
        }else {
            setContentView(R.layout.surveypage);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(Config.daysInSurvey == 0 || Config.daysInSurvey == -1) {

            setContentView(R.layout.surveypage_day0);
        }else if(Config.daysInSurvey > Constants.FINALDAY){

            setContentView(R.layout.surveypage_complete);
        }else {
            setContentView(R.layout.surveypage);
        }

        if(Config.daysInSurvey <= Constants.FINALDAY && Config.daysInSurvey >= 1)
            initlinkListOhio();
    }

    private void initlinkListOhio(){

        sharedPrefs = getSharedPreferences(Constants.sharedPrefString, MODE_PRIVATE);

        /* get today's data from the DB*/

        long currentTimeInMillis = ScheduleAndSampleManager.getCurrentTimeInMillis();

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String todayDate = ScheduleAndSampleManager.getTimeString(currentTimeInMillis, sdf);

        SimpleDateFormat sdf2 = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_NO_ZONE);
        String startTimeString = todayDate + " 00:00:00";
        long startTime = ScheduleAndSampleManager.getTimeInMillis(startTimeString, sdf2);
        long endTime = startTime + Constants.MILLISECONDS_PER_DAY;

        Log.d(TAG, "[test mobile triggering] startTimeString : "+startTimeString);
        Log.d(TAG, "[test mobile triggering] startTime : "+ScheduleAndSampleManager.getTimeString(startTime));
        Log.d(TAG, "[test mobile triggering] endTime : "+ScheduleAndSampleManager.getTimeString(endTime));

        surveyDatas = new ArrayList<>();
        linkNums = new ArrayList<>();

        //format : id;link_col;generateTime_col;openTime_col;missedTime_col;openFlag_col;surveyType_col
        surveyDatas = DBHelper.querySurveyLinkBetweenTimes(startTime, endTime);

        surveyDayText = (TextView) findViewById(R.id.surveyDayView);
        surveyDayText.setText("Day "+ Config.daysInSurvey+"/"+Constants.FINALDAY);

        Log.d(TAG, "[test mobile triggering] surveyDatas size : "+surveyDatas.size());

        for(String data : surveyDatas){

            String link = data.split(Constants.DELIMITER)[1];

            String getSurveyNumber1 = link.split("n=")[1];
            String n = getSurveyNumber1.split("&m=")[0];

            int linkNum = Integer.valueOf(n);

            linkNums.add(linkNum);

            Log.d(TAG, "[test mobile triggering] link : "+link);
        }

        buttonState = new ArrayList<>();
        buttons = new ArrayList<>();

        survey1_Button = (Button) findViewById(R.id.survey1_button);
        survey2_Button = (Button) findViewById(R.id.survey2_button);
        survey3_Button = (Button) findViewById(R.id.survey3_button);
        survey4_Button = (Button) findViewById(R.id.survey4_button);
        survey5_Button = (Button) findViewById(R.id.survey5_button);
        survey6_Button = (Button) findViewById(R.id.survey6_button);

        buttons.add(survey1_Button);
        buttons.add(survey2_Button);
        buttons.add(survey3_Button);
        buttons.add(survey4_Button);
        buttons.add(survey5_Button);
        buttons.add(survey6_Button);

        setSurveyButtonsWork();

        setSurveyButtonsAvailable(survey1_Button, 1);
        setSurveyButtonsAvailable(survey2_Button, 2);
        setSurveyButtonsAvailable(survey3_Button, 3);
        setSurveyButtonsAvailable(survey4_Button, 4);
        setSurveyButtonsAvailable(survey5_Button, 5);
        setSurveyButtonsAvailable(survey6_Button, 6);

        //check the button's state again
        int latestNotUnava_index = -1;

        //get the newest not unavailable button
        for(int index = buttonState.size() - 1; index >= 0  ; index--){

            String currentState = buttonState.get(index);

            if(!currentState.equals(TEXT_Unavailable)){

                latestNotUnava_index = index;
                break;
            }
        }

        //before the newest not unavailable button, there should not have a unavailable button
        for(int index = 0; index < latestNotUnava_index ; index++){

            String currentState = buttonState.get(index);

            if(currentState.equals(TEXT_Unavailable)){

                Button currentButton = buttons.get(index);
                currentButton.setText(TEXT_ERROR);
                currentButton.setClickable(false);
            }
        }

    }

    private void setSurveyButtonsAvailable(Button survey_Button, int correspondingSize){

        boolean setUnavaliable = true;
        int correspondingIndex = -1;

        Log.d(TAG, "[test mobile triggering] links size : "+linkNums.size());

        if(linkNums.contains(correspondingSize)){

            correspondingIndex = linkNums.lastIndexOf(correspondingSize);
            setUnavaliable = false;
        }

        Log.d(TAG, "[test mobile triggering] setUnavaliable : "+setUnavaliable);

        if(setUnavaliable){

            survey_Button.setBackgroundColor(Color.LTGRAY);
            survey_Button.setTextColor(Color.DKGRAY);

            survey_Button.setText(TEXT_Unavailable);
            survey_Button.setClickable(false);

            buttonState.add(TEXT_Unavailable);
        }else{

            Log.d(TAG, "[test mobile triggering] correspondingIndex : "+correspondingIndex);
            Log.d(TAG, "[test mobile triggering] surveyDatas size : "+surveyDatas.size());

            String surveyData = surveyDatas.get(correspondingIndex);

            String openFlag = surveyData.split(Constants.DELIMITER)[5];

            Log.d(TAG, "[test mobile triggering] surveyData : "+surveyData);
            Log.d(TAG, "[test mobile triggering] openFlag : "+openFlag);

            if(openFlag.equals(Constants.SURVEY_COMPLETE_FLAG)){

                survey_Button.setBackgroundColor(Color.LTGRAY);
                survey_Button.setTextColor(Color.DKGRAY);

                survey_Button.setText(TEXT_COMPLETED);
                survey_Button.setClickable(false);

                buttonState.add(TEXT_COMPLETED);
            }else if(openFlag.equals(Constants.SURVEY_INCOMPLETE_FLAG)){

                survey_Button.setBackgroundColor(Color.LTGRAY);
                survey_Button.setTextColor(Color.DKGRAY);

                survey_Button.setText(TEXT_MISSED);
                survey_Button.setClickable(false);

                buttonState.add(TEXT_MISSED);

                //check the missed is for the mobile or the random

                //if it's random one, check there is a mobile survey in next period.

                //if there is, set to here.
//                setSurveyButtonsAvailable(survey_Button, correspondingSize+1);

            }else if(openFlag.equals(Constants.SURVEY_ERROR_FLAG)){

                survey_Button.setBackgroundColor(Color.LTGRAY);
                survey_Button.setTextColor(Color.DKGRAY);

                survey_Button.setText(TEXT_ERROR);
                survey_Button.setClickable(false);

                buttonState.add(TEXT_ERROR);
            }else{

                survey_Button.setBackgroundColor(Color.RED);
                survey_Button.setTextColor(getResources().getColor(R.color.white));
                survey_Button.setText(TEXT_Available);

                buttonState.add(TEXT_Available);
            }
        }

    }

    private void setSurveyButtonsWork(){

        survey1_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                surveyButtonsWork(1);
            }
        });

        survey2_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                surveyButtonsWork(2);
            }
        });

        survey3_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                surveyButtonsWork(3);
            }
        });

        survey4_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                surveyButtonsWork(4);
            }
        });

        survey5_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                surveyButtonsWork(5);
            }
        });

        survey6_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                surveyButtonsWork(6);
            }
        });
    }

    private void surveyButtonsWork(int buttonNumber){

        DBHelper.insertActionLogTable(ScheduleAndSampleManager.getCurrentTimeInMillis(), "Button - Survey " + buttonNumber);

        //record if the user have clicked the survey button
        sharedPrefs.edit().putBoolean("Period"+buttonNumber,false).apply();

        if(linkNums.contains(buttonNumber)){

            try{

                String surveyData = surveyDatas.get(linkNums.lastIndexOf(buttonNumber));

                //set opened time.
                //if they click it, set the openFlag to 1.
                String id = surveyData.split(Constants.DELIMITER)[0];
                DataHandler.updateSurveyOpenFlagAndTime(id);

                //get the link in the surveyLink table.
                String link = surveyData.split(Constants.DELIMITER)[1];

                //Log.d(TAG, "the "+ buttonNumber +" link is : "+link);

                //check the link is for the mobile or random
                String getSurveyTypeNumber = link.split("&m=")[1];

                int linkNum = Integer.valueOf(getSurveyTypeNumber);

                if(linkNum == 1){

                    int walkoutdoor_sampled = sharedPrefs.getInt("walkoutdoor_sampled", 0);

                    walkoutdoor_sampled++;
                    sharedPrefs.edit().putInt("walkoutdoor_sampled", walkoutdoor_sampled).apply();

                }else {

                    int random_sampled = sharedPrefs.getInt("interval_sampled", 0);

                    random_sampled++;
                    sharedPrefs.edit().putInt("interval_sampled", random_sampled).apply();
                }

                Intent resultIntent = new Intent(Intent.ACTION_VIEW);
                resultIntent.setData(Uri.parse(link)); //get the link from adapter

                startActivity(resultIntent);
            }catch (IndexOutOfBoundsException e){

                Log.d(TAG, "[test mobile triggering] IndexOutOfBoundsException");
            }
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            //Log.d(TAG, " onKeyDown");

            Intent intent = getIntent();
            Bundle bundle = new Bundle();
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

}
