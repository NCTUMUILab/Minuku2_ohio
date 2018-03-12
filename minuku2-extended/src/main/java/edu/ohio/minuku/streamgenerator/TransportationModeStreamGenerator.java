package edu.ohio.minuku.streamgenerator;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.dao.TransportationModeDAO;
import edu.ohio.minuku.manager.MinukuDAOManager;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.model.DataRecord.TransportationModeDataRecord;
import edu.ohio.minuku.service.TransportationModeService;
import edu.ohio.minuku.stream.TransportationModeStream;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.exception.StreamAlreadyExistsException;
import edu.ohio.minukucore.exception.StreamNotFoundException;
import edu.ohio.minukucore.stream.Stream;

/**
 * Created by Lawrence on 2017/5/22.
 */

public class TransportationModeStreamGenerator extends AndroidStreamGenerator<TransportationModeDataRecord> {

    public final String TAG = "TransportationModeStreamGenerator";

    private Context mContext;
    private TransportationModeStream mStream;
    TransportationModeDAO mDAO;

    public ActivityRecognitionStreamGenerator activityRecognitionStreamGenerator;

    private String ConfirmedActvitiyString = "NA";

    public TransportationModeStreamGenerator(Context applicationContext) {
        super(applicationContext);
        this.mContext = applicationContext;
        this.mStream = new TransportationModeStream(Constants.LOCATION_QUEUE_SIZE);
        this.mDAO = MinukuDAOManager.getInstance().getDaoFor(TransportationModeDataRecord.class);

        this.activityRecognitionStreamGenerator = ActivityRecognitionStreamGenerator.getInstance(applicationContext);


        this.register();
    }

    @Override
    public void register() {
        Log.d(TAG, "Registering with StreamManager.");
        try {
            MinukuStreamManager.getInstance().register(mStream, TransportationModeDataRecord.class, this);
        } catch (StreamNotFoundException streamNotFoundException) {
            Log.e(TAG, "One of the streams on which LocationDataRecord depends in not found.");
        } catch (StreamAlreadyExistsException streamAlreadyExistsException) {
            Log.e(TAG, "Another stream which provides LocationDataRecord is already registered.");
        }
    }
    @Override
    public Stream<TransportationModeDataRecord> generateNewStream() {
        return mStream;
    }

    @Override
    public boolean updateStream() {

        Log.d(TAG, "Update stream called.");

        TransportationModeDataRecord transportationModeDataRecord =
                new TransportationModeDataRecord(ConfirmedActvitiyString);

        Log.d(TAG,"updateStream transportationModeDataRecord : " + ConfirmedActvitiyString);

        mStream.add(transportationModeDataRecord);
        Log.d(TAG, "TransportationMode to be sent to event bus" + transportationModeDataRecord);

        MinukuStreamManager.getInstance().setTransportationModeDataRecord(transportationModeDataRecord);

        // also post an event.
        EventBus.getDefault().post(transportationModeDataRecord);
        try {
            mDAO.add(transportationModeDataRecord);
            mDAO.query_counting();

        } catch (DAOException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) { //Sometimes no data is normal
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public long getUpdateFrequency() {
        return 1; //TODO check its efficiency.
    }

    @Override
    public void sendStateChangeEvent() {

    }

    @Override
    public void onStreamRegistration() {

        Intent intent = new Intent(mContext, TransportationModeService.class);
        if(!TransportationModeService.isServiceRunning()){
            Log.d(TAG, "[test alarm] going to start TransportationModeService");
            mContext.startService(intent);
        }

    }

    public void setTransportationModeDataRecord(String getConfirmedActvitiyString){

        ConfirmedActvitiyString = getConfirmedActvitiyString;

        Log.d(TAG, "ConfirmedActvitiyString : " + ConfirmedActvitiyString);

    }

    @Override
    public void offer(TransportationModeDataRecord dataRecord) {

    }

}
