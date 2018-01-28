package edu.ohio.minuku.manager;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

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

import edu.ohio.minuku.Data.DBHelper;
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
            Log.d(TAG, "test combine test trip original null updated to " + this.transportationModeDataRecord.getConfirmedActivityString());
        }
        else {

            Log.d(TAG, "test combine. NEW: " + transportationModeDataRecord.getConfirmedActivityString() + " vs OLD:" + this.transportationModeDataRecord.getConfirmedActivityString());


            /**
             * 1. checkf if the new activity label is different from the previous activity label. IF it is different, we should do something
             * **/

            if (!this.transportationModeDataRecord.getConfirmedActivityString().equals(transportationModeDataRecord.getConfirmedActivityString())) {

                Log.d(TAG, "test combine test trip: the new acitivty is different from the previous!");

                /** we first see if the this is the first session**/
                int sessionCount =  SessionManager.getNumOfSession();

                //if this is the first time seeing a session and the new transportation is neither static nor NA, we should just insert a session
                if (sessionCount==0
                        && !transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NO_TRANSPORTATION)
                        && !transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NA)){

                    Log.d(TAG, "test combine addSessionFlag = true there's no session in the db");
                    addSessionFlag = true;
                }
                //there's exizstint sessions in the DB
                else if (sessionCount>0){

                    //get the latest session (Which should be the ongoing one)
                    Session lastSession = SessionManager.getLastSession();
                    int sessionIdOfLastSession = lastSession.getId();
                    AnnotationSet annotationSet = lastSession.getAnnotationsSet();
                    long endTimeOfLastSession = lastSession.getEndTime();
                    long startTimeOfLastSession = lastSession.getStartTime();

                    Log.d(TAG, "[test combine] session " + sessionIdOfLastSession + " with annotation string " + annotationSet.toString() + " end time " + endTimeOfLastSession + " startTime " + startTimeOfLastSession);


                    /**
                     * 2. then check if the previous activity is performing a moving activity, if the previous is, the current activity label indicates an end of the previous session
                     * we should remove the current ongoing session from the ongoing session list and add an end time to it
                     * **/

                    if(!this.transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NO_TRANSPORTATION)
                            && !this.transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NA)){


                        //first get the last session id, which is the same as the count of the session in the database (it should
                        Log.d(TAG, "test combine: the previous acitivty is movnig, we're going to remove the current session id " + sessionIdOfLastSession );
                        SessionManager.getInstance().removeOngoingSessionid(String.valueOf(sessionIdOfLastSession));;
                        Log.d(TAG, "test combine: after revmove, now the sesssion manager session list has  " + SessionManager.getInstance().getOngoingSessionList());


                        /**
                         * 3. Then we need to determine whether the session is long enough to be a session. We get the distance of the session to determine its isLongEnoughFlag *
                         * */

                        boolean isSessionLongEnough = SessionManager.isSessionLongEnough(SessionManager.SESSION_LONGENOUGH_THRESHOLD_DISTANCE, sessionIdOfLastSession);

                        //if we end the current session, we should update its time and set a long enough flag
                        long endTime = getCurrentTimeInMilli();
                        lastSession.setEndTime(endTime);
                        lastSession.setLongEnough(isSessionLongEnough);

                        //end the current session
                        SessionManager.endCurSession(lastSession);

                        //debug..
                        String lastSessionStr = DBHelper.queryLastSession().get(0);
                        Log.d(TAG, "test combine: the previous acitivty is movnig,after update the session is: " +  lastSessionStr );

                    }


                    /**
                     * 4 if the new activity is moving, we will first determine whether this is continuing the previosu activity or a new activity. If it is a continutous one we will not add a new sesssion but let the previous activity in the ongoing
                     * **/

                    if(!transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NO_TRANSPORTATION)
                            && !transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NA)) {


                        /** check if the new actviity should be combine: if the new transportaiotn mode  is the same as the mode of the previous sessison and the time is 5 minuts*/

                        boolean shouldCombineWithLastSession = false;
                        long now = getCurrentTimeInMilli();
                        shouldCombineWithLastSession = SessionManager.examineSessionCombinationByActivityAndTime(lastSession, transportationModeDataRecord.getConfirmedActivityString(),now );

                        if (shouldCombineWithLastSession){
                            //modify the endTime of the previous session to empty (because we extend it!). we should also make the notlongenough field to be true.
                            SessionManager.continueLastSession(lastSession);
                        }
                        else {
                            //not continue
                            addSessionFlag = true;
                            Log.d(TAG, "test combine: we shgould not combine but add a new session " );
                        }






                    }


                }

                //if we need to add a session
                if (addSessionFlag && !transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NO_TRANSPORTATION)
                        && !transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NA) ){
                    Log.d(TAG, "[test combine] we should add session " + ((int) sessionCount + 1));

                    //insert into the session table;
                    int sessionId = (int) sessionCount + 1;
                    Session session = new Session(sessionId);
                    session.setStartTime(getCurrentTimeInMilli());
                    Annotation annotation = new Annotation();
                    annotation.setContent(transportationModeDataRecord.getConfirmedActivityString());
                    annotation.addTag(Constants.ANNOTATION_TAG_DETECTED_TRANSPORTATOIN_ACTIVITY);
                    session.addAnnotation(annotation);

                    Log.d(TAG, "[test combine] insert the session is with annotation " +session.getAnnotationsSet().toJSONObject().toString());


                    SessionManager.startNewSession(session);

                }


            }
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

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW_SLASH);
        String timeString = sdf_now.format(time);

        return timeString;
    }
}