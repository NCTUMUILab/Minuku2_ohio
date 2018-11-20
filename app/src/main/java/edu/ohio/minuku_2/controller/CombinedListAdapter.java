package edu.ohio.minuku_2.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2018/6/26.
 */

public class CombinedListAdapter extends ArrayAdapter<Session> {

    private Context mContext;
    private ArrayList<Session> mSessions;
    private static LayoutInflater inflater = null;

    public CombinedListAdapter(Context context, int resource, ArrayList<Session> sessions) {

        super(context, resource, sessions);
        this.mContext = context;
        this.mSessions = sessions;
        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;

        if(convertView==null) {

            vi = inflater.inflate(R.layout.combined_list_item, null);
        }

        String sessionTitle;
        String labelStr = SessionManager.SESSION_DISPLAY_NO_ANNOTATION;

        Session session = mSessions.get(position);

        CheckedTextView chkBshow = (CheckedTextView) vi.findViewById(R.id.check1);

        long startTime = session.getStartTime();

        boolean isSessionOngoing = SessionManager.isSessionOngoing(session.getId());
        //Log.d(TAG, "[test show trip] session ongoing flag:  " + isSessionOngoing );

        //format the time string
        SimpleDateFormat sdf_minute = new SimpleDateFormat(Constants.DATE_FORMAT_HOUR_MIN_AMPM);
        String timeLabel = ScheduleAndSampleManager.getTimeString(startTime,sdf_minute);

        //try to the find the transportation mode provided by the user. It is in the annotaiton with ESM tag
        ArrayList<Annotation> annotations = session.getAnnotationsSet().getAnnotationByTag("ESM");

        //{"Entire_session":true,"Tag":["ESM"],"Content":"{\"ans1\":\"Walking outdoors.\",\"ans2\":\"Food\",\"ans3\":\"No\",\"ans4\":\"Right before\"}"}
        if (annotations.size()>0){

            try {

                String content = annotations.get(0).getContent();
                JSONObject contentJSON = new JSONObject(content);
                //Log.d(TAG, "[test show trip] the contentofJSON ESMJSONObject  is " + contentJSON );
                labelStr = contentJSON.getString("ans1");

                labelStr = getLabelFromAns1(labelStr);
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }

        if (isSessionOngoing)
            labelStr = SessionManager.SESSION_DISPLAY_ONGOING;

        //checking the date of the startTime is Today or yesterday.
        SimpleDateFormat sdf_date = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_DAY);
        String StartTimeDate = ScheduleAndSampleManager.getTimeString(startTime,sdf_date);
        String CurrentTimeDate = ScheduleAndSampleManager.getTimeString(ScheduleAndSampleManager.getCurrentTimeInMillis(), sdf_date);

        String today_yesterday;
        if(StartTimeDate.equals(CurrentTimeDate)){
            today_yesterday = "Today";
        }else{
            today_yesterday = "Yesterday";
        }

        sessionTitle = today_yesterday + " " + timeLabel + "    " + labelStr;

        chkBshow.setText(sessionTitle);
        chkBshow.setTextSize(18);

        return vi;
    }

    private String getLabelFromAns1(String labelStr){

        switch (labelStr){
            case "Walking outdoors": //ans1
                return "Walk";
            case "Walking indoors": //ans1
                return "Walk";
            case "Riding a bicycle":
                return "Bike";
            case "Riding a scooter":
                return "Scooter";
            case "Driving (I'm the driver)":
                return "Drive";
            case "Driving (I'm the passenger)":
                return "Drive";
            case "Trip is part of previous trip (COMBINE)":
                return "Combine";
            case "Taking a bus":
                return "Bus";
            case "Trip is incorrect (DELETE)":
                return "Delete";
            case "Other transportation":
                return "Other";
            default:
                return "";
        }
    }
}

