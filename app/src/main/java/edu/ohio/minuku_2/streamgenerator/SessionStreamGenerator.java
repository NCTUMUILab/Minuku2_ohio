package edu.ohio.minuku_2.streamgenerator;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.opencsv.CSVWriter;

import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.streamgenerator.AndroidStreamGenerator;
import edu.ohio.minuku_2.dao.SessionDataRecordDAO;
import edu.ohio.minuku_2.model.SessionDataRecord;
import edu.ohio.minuku_2.stream.SessionStream;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.exception.StreamAlreadyExistsException;
import edu.ohio.minukucore.exception.StreamNotFoundException;
import edu.ohio.minukucore.stream.Stream;

/**
 * Created by Lawrence on 2017/12/18.
 */

public class SessionStreamGenerator extends AndroidStreamGenerator<SessionDataRecord> {
    private final String TAG = "SessionStreamGenerator";

    private static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.nctu.minuku_2/";
    private CSVWriter csv_writer = null;

    private SessionStream mStream;
    private SessionDataRecordDAO mDAO;
    private Context mContext;

    public SessionStreamGenerator(Context applicationContext){
        super(applicationContext);
        this.mContext = applicationContext;
        this.mStream = new SessionStream(Constants.LOCATION_QUEUE_SIZE);
        this.mDAO = new SessionDataRecordDAO(applicationContext);

        this.register();
    }

    @Override
    public void register() {
        Log.d(TAG, "Registering with StreamManager.");
        try {
            MinukuStreamManager.getInstance().register(mStream, SessionDataRecord.class, this);
        } catch (StreamNotFoundException streamNotFoundException) {
            Log.e(TAG, "One of the streams on which SessionDataRecord depends in not found.");
        } catch (StreamAlreadyExistsException streamAlreadyExistsException) {
            Log.e(TAG, "Another stream which provides SessionDataRecord is already registered.");
        }
    }

    @Override
    public Stream<SessionDataRecord> generateNewStream() {
        return mStream;
    }

    @Override
    public boolean updateStream() {
        Log.e(TAG, "Update stream called.");

        SessionDataRecord sessionDataRecord = new SessionDataRecord(SessionManager.sessionid_unStatic);

        Log.d(TAG,"sessionid : " + sessionDataRecord.getSessionid());

        StoreToCSV(new Date().getTime(), sessionDataRecord.getSessionid());

        if(sessionDataRecord!=null) {

            mStream.add(sessionDataRecord);

            Log.e(TAG, "Session to be sent to event bus" + sessionDataRecord);

            EventBus.getDefault().post(sessionDataRecord);
            try {
                mDAO.add(sessionDataRecord);
            } catch (DAOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void StoreToCSV(long timestamp, int sessionid){
        Log.d(TAG,"StoreToCSV");

        String sFileName = "Sessionid_Log.csv";

        try{
            File root = new File(Environment.getExternalStorageDirectory() + PACKAGE_DIRECTORY_PATH);
            if (!root.exists()) {
                root.mkdirs();
            }

            Log.d(TAG, "root : " + root);

            csv_writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory()+PACKAGE_DIRECTORY_PATH+sFileName,true));

            List<String[]> data = new ArrayList<String[]>();

            String timeString = getTimeString(timestamp);

            data.add(new String[]{String.valueOf(timestamp), timeString, String.valueOf(sessionid)});

            csv_writer.writeAll(data);

            csv_writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getTimeString(long time){

        SimpleDateFormat sdf_now = new SimpleDateFormat(Constants.DATE_FORMAT_NOW);
        String currentTimeString = sdf_now.format(time);

        return currentTimeString;
    }


    @Override
    public long getUpdateFrequency() {
        return 1;
    }

    @Override
    public void sendStateChangeEvent() {

    }

    @Override
    public void onStreamRegistration() {

    }

    @Override
    public void offer(SessionDataRecord dataRecord) {

    }
}
