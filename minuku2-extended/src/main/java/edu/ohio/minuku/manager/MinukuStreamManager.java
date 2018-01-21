/*
 * Copyright (c) 2016.
 *
 * DReflect and Minuku Libraries by Shriti Raj (shritir@umich.edu) and Neeraj Kumar(neerajk@uci.edu) is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.
 * Based on a work at https://github.com/Shriti-UCI/Minuku-2.
 *
 *
 * You are free to (only if you meet the terms mentioned below) :
 *
 * Share — copy and redistribute the material in any medium or format
 * Adapt — remix, transform, and build upon the material
 *
 * The licensor cannot revoke these freedoms as long as you follow the license terms.
 *
 * Under the following terms:
 *
 * Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
 * NonCommercial — You may not use the material for commercial purposes.
 * ShareAlike — If you remix, transform, or build upon the material, you must distribute your contributions under the same license as the original.
 * No additional restrictions — You may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.
 */

package edu.ohio.minuku.manager;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import edu.ohio.minuku.DBHelper.DBHelper;
import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.model.Annotation;
import edu.ohio.minuku.model.AnnotationSet;
import edu.ohio.minuku.model.DataRecord.ActivityRecognitionDataRecord;
import edu.ohio.minuku.model.DataRecord.LocationDataRecord;
import edu.ohio.minuku.model.DataRecord.TransportationModeDataRecord;
import edu.ohio.minuku.model.MinukuStreamSnapshot;
import edu.ohio.minuku.model.Session;
import edu.ohio.minuku.service.TransportationModeService;
import edu.ohio.minukucore.event.IsDataExpectedEvent;
import edu.ohio.minukucore.event.NoDataChangeEvent;
import edu.ohio.minukucore.event.StateChangeEvent;
import edu.ohio.minukucore.exception.StreamAlreadyExistsException;
import edu.ohio.minukucore.exception.StreamNotFoundException;
import edu.ohio.minukucore.manager.StreamManager;
import edu.ohio.minukucore.model.DataRecord;
import edu.ohio.minukucore.model.StreamSnapshot;
import edu.ohio.minukucore.stream.Stream;
import edu.ohio.minukucore.streamgenerator.StreamGenerator;

/**
 * Created by Neeraj Kumar on 7/17/16.
 *
 * The MinukuStreamManager class implements {@link StreamManager} and runs as a service within
 * the application context. It maintains a list of all the Streams and StreamGenerators registered
 * within the application and is responsible for trigerring the
 * {@link StreamGenerator#updateStream() updateStream} method of the StreamManager class after
 * every {@link StreamGenerator#getUpdateFrequency() updateFrequency}.
 *
 * This depends on a service to call it's updateStreamGenerators method.
 */
public class MinukuStreamManager implements StreamManager {

    private final String TAG = "MinukuStreamManager";

    protected Map<Class, Stream> mStreamMap;
    protected Map<Stream.StreamType, List<Stream<? extends DataRecord>>> mStreamTypeStreamMap;
    protected Map<Class, StreamGenerator> mRegisteredStreamGenerators;

    private ActivityRecognitionDataRecord activityRecognitionDataRecord;
    private TransportationModeDataRecord transportationModeDataRecord;
    private LocationDataRecord locationDataRecord;

    private static int counter = 0;

    private static MinukuStreamManager instance;

    private MinukuStreamManager() throws Exception {
        mStreamMap = new HashMap<>();
        mStreamTypeStreamMap = new HashMap<>();
        mRegisteredStreamGenerators = new HashMap<>();


    }

    public static MinukuStreamManager getInstance() {
        if(MinukuStreamManager.instance == null) {
            try {
                MinukuStreamManager.instance = new MinukuStreamManager();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return MinukuStreamManager.instance;
    }

    public void updateStreamGenerators() {
        for(StreamGenerator streamGenerator: mRegisteredStreamGenerators.values()) {
            Log.d(TAG, "Stream generator : " + streamGenerator.getClass() + " \n" +
                    "Update frequency: " + streamGenerator.getUpdateFrequency() + "\n" +
                    "Counter: " + counter);
            if(streamGenerator.getUpdateFrequency() == -1) {
                continue;
            }
            if(counter % streamGenerator.getUpdateFrequency() == 0) {
                Log.d(TAG, "Calling update stream generator for " + streamGenerator.getClass());
                streamGenerator.updateStream();
            }
        }
        counter++;
    }


    @Override
    public List<Stream> getAllStreams() {
        return new LinkedList<>(mStreamMap.values());
    }

    @Override
    public <T extends DataRecord> void register(Stream s,
                                                Class<T> clazz,
                                                StreamGenerator aStreamGenerator)
            throws StreamNotFoundException, StreamAlreadyExistsException {
        if(mStreamMap.containsKey(clazz)) {
            throw new StreamAlreadyExistsException();
        }
        for(Object dependsOnClass:s.dependsOnDataRecordType()) {
            if(!mStreamMap.containsKey(dependsOnClass)) {
                Log.e(TAG, "Stream not found : " + dependsOnClass.toString());
                throw new StreamNotFoundException();
            }
        }
        mStreamMap.put(clazz, s);
        mRegisteredStreamGenerators.put(clazz, aStreamGenerator);
        aStreamGenerator.onStreamRegistration();
        Log.d(TAG, "Registered a new stream generator for " + clazz);
    }

    @Override
    public void unregister(Stream s, StreamGenerator sg)
            throws StreamNotFoundException {
        Class classType = s.getCurrentValue().getClass();
        if(!mStreamMap.containsKey(classType)) {
            throw new StreamNotFoundException();
        }
        mStreamMap.remove(s);
        mRegisteredStreamGenerators.remove(sg);
    }

    @Override
    public <T extends DataRecord> Stream<T> getStreamFor(Class<T> clazz)
            throws StreamNotFoundException {
        if(mStreamMap.containsKey(clazz)) {
            return mStreamMap.get(clazz);
        } else {
            throw new StreamNotFoundException();
        }
    }

    @Override
    @Subscribe
    public void handleStateChangeEvent(StateChangeEvent aStateChangeEvent) {
        MinukuSituationManager.getInstance().onStateChange(getStreamSnapshot(),
                aStateChangeEvent);
    }

    @Override
    @Subscribe
    public void handleNoDataChangeEvent(NoDataChangeEvent aNoDataChangeEvent) {
        MinukuSituationManager.getInstance().onNoDataChange(getStreamSnapshot(),
                aNoDataChangeEvent);
    }

    @Override
    @Subscribe
    public void handleIsDataExpectedEvent(IsDataExpectedEvent isDataExpectedEvent) {
        MinukuSituationManager.getInstance().onIsDataExpected(getStreamSnapshot(),
                isDataExpectedEvent);
    }

    @Override
    public List<Stream<? extends DataRecord>> getStreams(Stream.StreamType streamType) {
        return mStreamTypeStreamMap.get(streamType);
    }

    @Override
    public <T extends DataRecord> StreamGenerator<T> getStreamGeneratorFor(Class<T> clazz)
            throws StreamNotFoundException {
        if(mRegisteredStreamGenerators.containsKey(clazz)) {
            return mRegisteredStreamGenerators.get(clazz);
        } else {
            throw new StreamNotFoundException();
        }
    }

    private StreamSnapshot getStreamSnapshot() {
        Map<Class<? extends DataRecord>, List<? extends DataRecord>> streamSnapshotData =
                new HashMap<>();
        for(Map.Entry<Class, Stream> entry: mStreamMap.entrySet()) {
            List list = createListOfType(entry.getKey());
            list.add(entry.getValue().getCurrentValue());
            list.add(entry.getValue().getPreviousValue());
            streamSnapshotData.put(entry.getKey(), list);
        }
        return new MinukuStreamSnapshot(streamSnapshotData);
    }

    private static <T extends DataRecord>  List<T> createListOfType(Class<T> type) {
        return new ArrayList<T>();
    }

    //
    public void setActivityRecognitionDataRecord(ActivityRecognitionDataRecord activityRecognitionDataRecord){
        this.activityRecognitionDataRecord = activityRecognitionDataRecord;

    }

    public ActivityRecognitionDataRecord getActivityRecognitionDataRecord(){
        return activityRecognitionDataRecord;
    }

    public void setTransportationModeDataRecord(TransportationModeDataRecord transportationModeDataRecord){

        Log.d(TAG, "test trip incoming transportation: " + transportationModeDataRecord.getConfirmedActivityString());

        Boolean addSessionFlag = false;

        //the first time we see incoming transportation mode data
        if (this.transportationModeDataRecord==null){
            this.transportationModeDataRecord = transportationModeDataRecord;
            Log.d(TAG, "test trip original null updated to " + this.transportationModeDataRecord.getConfirmedActivityString());
        }


        else {

            Log.d(TAG, "test combine. NEW: " + transportationModeDataRecord.getConfirmedActivityString() + " vs OLD:" + this.transportationModeDataRecord.getConfirmedActivityString());


            //checkf if the new activity is different from the previous activity
            if (!this.transportationModeDataRecord.getConfirmedActivityString().equals(transportationModeDataRecord.getConfirmedActivityString())) {


                Log.d(TAG, "test combine test trip: the new acitivty is different from the previous!");


                /**1. first check if the previous activity is a session, if the previous is moving, we should remove the session and call it an end**/
                if(!this.transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NO_TRANSPORTATION)
                        && !this.transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NA)){

                    //remove the session
                    int id = (int) DBHelper.querySessionCount();
                    Log.d(TAG, "test trip: the previous acitivty is movnig, we're going to  remove the session id " + id );
                    SessionManager.getInstance().removeOngoingSessionid(String.valueOf(id));
                    Log.d(TAG, "test trip: the previous acitivty is movnig, we remove the session id " + id );

                    //update the session with end time
                    long endTime = getCurrentTimeInMilli();
                    DBHelper.updateSessionTable(id, endTime);
                    Log.d(TAG, "test trip: the previous acitivty is movnig,after update "  );
                }

                /**2 if the new activity is moving, we will first determine whether this is continuing the previosu activity or a new activity. If it is a continutous one we will not add a new sesssion but let the previous activity in the ongoing **/
                if(!transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NO_TRANSPORTATION)
                        && !transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NA)){


                    /** check if the new actviity should be combine: if the new transportaiotn mode  is the same as the mode of the previous sessison and the time is 5 minuts*/

                    //see if there's existing session
                    long count =  DBHelper.querySessionCount();
                    Log.d(TAG,"[test combine] session count is " + count );


                    //first query session of the previous activity
                    if (count>0){

                        String lastSessionStr = DBHelper.queryLastSession().get(0);

                        //get session and obtain its annotation and endtime
                        String[] sessionCol = lastSessionStr.split(Constants.DELIMITER);
                        String annotationSetStr =  sessionCol[DBHelper.COL_INDEX_SESSION_ANNOTATION_SET];
                        long endTimeOfPreSession = 0;
                        String sessionIdOfPreSession = sessionCol[DBHelper.COL_INDEX_SESSION_ID];

                        if (!sessionCol[DBHelper.COL_INDEX_SESSION_END_TIME].equals("null")){
                            endTimeOfPreSession = Long.parseLong(sessionCol[DBHelper.COL_INDEX_SESSION_END_TIME]);
                        }


                        Log.d(TAG,"[test combine] session " + sessionIdOfPreSession + " with annotation string " + annotationSetStr + " end time " + endTimeOfPreSession );

                        JSONObject annotationSetJSON = null;
                        JSONArray annotateionSetJSONArray = null;
                        AnnotationSet annotationSet = null;

                        try {
                            if (!annotationSetStr.equals("null")){
                                annotationSetJSON = new JSONObject(annotationSetStr);
                                annotateionSetJSONArray = annotationSetJSON.getJSONArray(SessionManager.ANNOTATION_PROPERTIES_ANNOTATION);
//                                Log.d(TAG,"[test combine] annotateionSetJSONArray " + annotateionSetJSONArray.toString() );
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (annotateionSetJSONArray!=null){
                            annotationSet =  SessionManager.toAnnorationSet(annotateionSetJSONArray);
                        }

//                        Log.d(TAG,"[test combine] annotationSet " + annotationSet.toJSONObject().toString() );
                        //get annotaitons that has the transportation mode tag
                        ArrayList<Annotation>  annotations = annotationSet.getAnnotationByContent(transportationModeDataRecord.getConfirmedActivityString());

                        //if the previous session does not have any annotation of which transportation is of the same tag, we should add a new session
                        if (annotations.size()==0){
                            Log.d(TAG,"[test combine] the last session is not the same activity" );
                            addSessionFlag = true;
                        }

                        // the current activity is the same TM with the previous session mode
                        else {
                            Log.d(TAG,"[test combine] we found the last session with the same activity" );
                            //check its interval to see if it's within 5 minutes
                            long now = getCurrentTimeInMilli();

                            if (now - endTimeOfPreSession <= SessionManager.SESSION_MIN_INTERVAL_THRESHOLD_TRANSPORTATION){

                                Log.d(TAG,"[test combine] the current truip is too close from the previous trip, continue the last session! the difference is "
                                 + (now - endTimeOfPreSession)/Constants.MILLISECONDS_PER_MINUTE  + " minutes");

                                //we should put thre last session back
                                SessionManager.getInstance().addOngoingSessionid(sessionIdOfPreSession);

                                //modify the endTime of the previous session to empty (because we extend it!)
                                DBHelper.updateSessionTable(Integer.parseInt(sessionIdOfPreSession), Constants.INVALID_TIME_VALUE);
                                Log.d(TAG, "[test combine] extend the last session " +sessionIdOfPreSession  + ", make the end time of it "  + Constants.INVALID_TIME_VALUE );


                            }
                            //the session is far from the previous one, it should be a new session
                            else {
                                Log.d(TAG,"[test combine] the current truip is far from the previous trip, the difference is "
                                        + (now - endTimeOfPreSession)/Constants.MILLISECONDS_PER_MINUTE  + " minutes");
                                addSessionFlag = true;
                            }
                        }

                    }

                    //there's no session yet, we should just create a new session
                    else {
                        Log.d(TAG,"[test combine] the previous sessuin is the not same transporttion mode, we should create a session " );
                        addSessionFlag = true;

                    }


                    //if we need to add a session
                    if (addSessionFlag){

                        //insert into the session table;
                        int sessionId = (int) count + 1;
                        Session session = new Session(sessionId);
                        session.setStartTime(getCurrentTimeInMilli());
                        Annotation annotation = new Annotation();
                        annotation.setContent(transportationModeDataRecord.getConfirmedActivityString());
                        annotation.addTag(Constants.ANNOTATION_TAG_DETECTED_TRANSPORTATOIN_ACTIVITY);
                        session.addAnnotation(annotation);

                        Log.d(TAG, "[test combine] insert the session is with annotation " +session.getAnnotationsSet().toJSONObject().toString());

                        DBHelper.insertSessionTable(session);
                        //InstanceManager add ongoing session for the new activity
                        SessionManager.getInstance().addOngoingSessionid(String.valueOf(sessionId));
                    }




                }
            }



            //TODO do the Combination here, before the Combination, filter the isTrip col == 0 which menas its not trip


            //TODO filter < 100m trip its isTrip to 0

        }

        this.transportationModeDataRecord = transportationModeDataRecord;
//        Log.d(TAG, "test trip after update::: " + transportationModeDataRecord.getConfirmedActivityString() + "  the original beocomes " + this.transportationModeDataRecord.getConfirmedActivityString());


    }

    public TransportationModeDataRecord getTransportationModeDataRecord(){
        return transportationModeDataRecord;
    }

    public void setLocationDataRecord(LocationDataRecord locationDataRecord){
        this.locationDataRecord = locationDataRecord;
    }

    public LocationDataRecord getLocationDataRecord(){
        return locationDataRecord;
    }

/*
    public void setDataRecord(Class<?> t) {

    }

    public <T extends DataRecord> getDataRecord(){

        return ;
    }
*/


    private static long getCurrentTimeInMilli(){
        //get timzone
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        long t = cal.getTimeInMillis();

        return t;
    }

    /**Generate a formated time string (in the format of "yyyy-MM-dd HH:mm:ss" **/
    private static String getTimeString(long time) {

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW);
        String timeString = sdf_now.format(time);

        return timeString;
    }
}
