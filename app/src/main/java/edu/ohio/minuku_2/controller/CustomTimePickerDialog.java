package edu.ohio.minuku_2.controller;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.NumberPicker;
import android.widget.TimePicker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CustomTimePickerDialog extends TimePickerDialog {

    private final String TAG = "CustomTimePickerDialog";

    private final static int TIME_PICKER_INTERVAL = 30;
    private TimePicker mTimePicker;
    private final OnTimeSetListener mTimeSetListener;

    private int lastSavedHour;
    private int lastSavedMinute;

    public CustomTimePickerDialog(Context context, OnTimeSetListener listener,
                                  int hourOfDay, int minute, boolean is24HourView) {
        super(context, TimePickerDialog.THEME_HOLO_LIGHT, null, hourOfDay,
                minute / TIME_PICKER_INTERVAL, is24HourView);
        mTimeSetListener = listener;

        lastSavedHour = hourOfDay;
        lastSavedMinute = minute / TIME_PICKER_INTERVAL;

        //Log.d(TAG, "initial lastSavedHour : "+ lastSavedHour);
        //Log.d(TAG, "initial lastSavedMinute : "+ lastSavedMinute);

    }

//    @Override
    public void updateTime(int hourOfDay, int minuteOfHour) {
        //Log.d(TAG, "updateTime");

        //Log.d(TAG, "lastSavedHour : "+ lastSavedHour);
        //Log.d(TAG, "lastSavedMinute : "+ lastSavedMinute);

        mTimePicker.setCurrentHour(hourOfDay);
        mTimePicker.setCurrentMinute(minuteOfHour);

        lastSavedHour = hourOfDay;
        lastSavedMinute = minuteOfHour;
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minuteOfHour){
        //Log.d(TAG, "onTimeChanged");

        //Log.d(TAG, "hourOfDay : "+ hourOfDay);
        //Log.d(TAG, "minuteOfHour : "+ minuteOfHour);

        if(hourOfDay!=lastSavedHour && minuteOfHour!= lastSavedMinute) {

            view.setCurrentHour(lastSavedHour);
            // view.setCurrentMinute(minuteOfHour);

            /*//if the lastTime is initialized value.
            if(lastSavedHour == -99){
                updateTime(hourOfDay, minuteOfHour);
            }else{

            }*/
        }else {
            updateTime(hourOfDay, minuteOfHour);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //Log.d(TAG, "onClick");

        switch (which) {
            case BUTTON_POSITIVE:
                if (mTimeSetListener != null) {
                    mTimeSetListener.onTimeSet(mTimePicker, mTimePicker.getCurrentHour(),
                            mTimePicker.getCurrentMinute() * TIME_PICKER_INTERVAL);
                }
                break;
            case BUTTON_NEGATIVE:
                cancel();
                break;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        //Log.d(TAG, "onAttachedToWindow");

        try {
            Class<?> classForid = Class.forName("com.android.internal.R$id");
            Field timePickerField = classForid.getField("timePicker");
            mTimePicker = (TimePicker) findViewById(timePickerField.getInt(null));
            Field field = classForid.getField("minute");

            NumberPicker minuteSpinner = (NumberPicker) mTimePicker
                    .findViewById(field.getInt(null));
            minuteSpinner.setMinValue(0);
            minuteSpinner.setMaxValue((60 / TIME_PICKER_INTERVAL) - 1);
            List<String> displayedValues = new ArrayList<>();
            for (int i = 0; i < 60; i += TIME_PICKER_INTERVAL) {
                displayedValues.add(String.format("%02d", i));
            }
            minuteSpinner.setDisplayedValues(displayedValues
                    .toArray(new String[displayedValues.size()]));
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
}
