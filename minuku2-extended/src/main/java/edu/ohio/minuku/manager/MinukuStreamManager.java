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


        if (this.transportationModeDataRecord==null){

            this.transportationModeDataRecord = transportationModeDataRecord;
        }

        //TODO do the Combination here, before the Combination, filter the isTrip col == 0 which means its not trip
        else {

            //detecting stop of the trip
            //if last transportation is moving
            if(!this.transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NO_TRANSPORTATION)
                    && !this.transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NA)) {

                //different from the last transportation, stop the ongoing session.
                if (!this.transportationModeDataRecord.getConfirmedActivityString().equals(transportationModeDataRecord.getConfirmedActivityString())) {
                    int id = (int) DBHelper.querySessionCount();
                    Log.d(TAG, "testgetdata query session from LocalDB is " + id);

                    TripManager.getInstance().removeOngoingSessionid(id);
                }
            }

            //TODO do the Combination here, before the Combination, filter the isTrip col == 0 which menas its not trip


            //TODO filter < 100m trip its isTrip to 0


            this.transportationModeDataRecord = transportationModeDataRecord;

            //detecting start of the trip
            //ongoing session, detecting new travel activity
            //add ongoing session if the travel activity is not static
            if(!transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NO_TRANSPORTATION)
                    && !transportationModeDataRecord.getConfirmedActivityString().equals(TransportationModeService.TRANSPORTATION_MODE_NAME_NA)){

                //insert into the session table
                long count =  DBHelper.querySessionCount();
                Log.d(TAG, "testgetdata query session from LocalDB is " + count);

                int id = (int) count + 1;

                Session session = new Session(id);
                session.setStartTime(getCurrentTimeInMilli());

                DBHelper.insertSessionTable(session);

                //InstanceManager add ongoing session for the new activity
                TripManager.getInstance().addOngoingSessionid(id);

            }

        }


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
