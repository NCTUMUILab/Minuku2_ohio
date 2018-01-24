package edu.ohio.minuku_2.controller.Ohio;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import edu.ohio.minuku.Utilities.ScheduleAndSampleManager;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku_2.R;

/**
 * Created by Lawrence on 2017/8/29.
 */

public class OhioListAdapter extends ArrayAdapter<Session> {

    private static final String TAG = "OhioListAdapter";
    private Context mContext;
    private ArrayList<Session> data;
//    private ArrayList<ArrayList<String>> data;

    public static ArrayList<Integer> dataPos;

    private long startTime = -9999;
    private long endTime = -9999;
    private String startTimeString = "";
    private String endTimeString = "";

    private SharedPreferences sharedPrefs;

    public OhioListAdapter(Context context, int resource, ArrayList<Session> sessions) {

        super(context, resource, sessions);
        this.mContext = context;
        this.data = sessions;
        dataPos = new ArrayList<Integer>();
//        decideThePosFontType();
    }

    @Override
    public View getView(int sessionPos, View convertView, ViewGroup parent) {
//        Log.d(TAG,"test show trip result get view start"  );
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.recordinglistview_ohio, parent, false);

        TextView textView = (TextView) view.findViewById(R.id.ListText);

        Session session = getItem(sessionPos);

//        Log.d(TAG,"test show trip: " + session.getId() );

        /**
         * setting the style of the trip title
         */

        //if there's not annotation, put the text in red

        if (session!=null){

            String content;
            String sessionTitle = null;
            String timeLabel=null;
            String labelText = "No Label Yet";
            String noteText = "No Content Yet";

            //show the text of the session with annotation
            timeLabel = ScheduleAndSampleManager.getTimeString(session.getStartTime());
            sessionTitle = session.getId()+ ":" + "Trip "+(sessionPos+1)+": "+ timeLabel;

//            Log.d(TAG,  " test show trip session not null time, setting title " + sessionTitle);

            //if there's annotaiton in the session
            if (session.getAnnotationsSet()!=null && session.getAnnotationsSet().getAnnotations()!=null) {
                //get annotation
                for (int j=0; j<session.getAnnotationsSet().getAnnotations().size(); j++) {

                    Annotation annotation = session.getAnnotationsSet().getAnnotations().get(j);
                    content = annotation.getContent();

                    //the annotation is the label
                    if (annotation.getTags().contains("Label")){
                        labelText = content;
                    }
                    //the annotation is the note
                    if (annotation.getTags().contains("Note")){
                        noteText = content;
                    }
                }

                sessionTitle += " : " + labelText;
                Log.d(TAG,  " test show trip session not null time, adding annotation to title, and become " + sessionTitle);

                //if they've edited, put the text in green
                textView.setTextColor(Color.RED);

            }

            //if there's no annotation, put the text in red
            else {
                textView.setTextColor(Color.DKGRAY);
            }

            //set the title of the view
            textView.setText(sessionTitle);


            Log.d(TAG, "[test show trip] the session id is " + session.getId() + " start Time is " + session.getStartTime() + " session title " + textView.getText().toString() );
            // My layout has only one TextView


        }

        return view;
    }

}
