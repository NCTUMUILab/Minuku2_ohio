package edu.ohio.minuku.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.model.DataRecord.LocationNoGoogleDataRecord;
import edu.ohio.minukucore.dao.DAO;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.user.User;

/**
 * Created by Lawrence on 2017/11/20.
 */

public class LocationNoGoogleDataRecordDAO  implements DAO<LocationNoGoogleDataRecord> {

    private String TAG = "LocationNoGoogleDataRecordDAO";
    private DBHelper dBHelper;

    public LocationNoGoogleDataRecordDAO(Context applicationContext){

        dBHelper = DBHelper.getInstance(applicationContext);
    }

    @Override
    public void setDevice(User user, UUID uuid) {

    }

    @Override
    public void add(LocationNoGoogleDataRecord entity) throws DAOException {
        //Log.d(TAG, "Adding location data record.");
        /* * This is old function created by umich.
        Firebase locationListRef = new Firebase(Constants.FIREBASE_URL_LOCATION)
                .child(myUserEmail)
                .child(new SimpleDateFormat("MMddyyyy").format(new Date()).toString());
        locationListRef.push().setValue((LocationDataRecord) entity);*/

        ContentValues values = new ContentValues();

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            values.put(DBHelper.TIME, entity.getCreationTime());
//            values.put(DBHelper.daysInSurvey, entity.getTaskDayCount());
//            values.put(DBHelper.HOUR, entity.getHour());
            values.put(DBHelper.latitude_col, entity.getLatitude());
            values.put(DBHelper.longitude_col, entity.getLongitude());
            values.put(DBHelper.Accuracy_col, entity.getAccuracy());

            db.insert(DBHelper.locationNoGoogle_table, null, values);
        }
        catch(NullPointerException e){
            //e.printStackTrace();
        }
        finally {
            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection
        }
    }

    @Override
    public void delete(LocationNoGoogleDataRecord entity) throws DAOException {

    }

    @Override
    public Future<List<LocationNoGoogleDataRecord>> getAll() throws DAOException {
        return null;
    }

    @Override
    public Future<List<LocationNoGoogleDataRecord>> getLast(int N) throws DAOException {
        return null;
    }

    @Override
    public void update(LocationNoGoogleDataRecord oldEntity, LocationNoGoogleDataRecord newEntity) throws DAOException {

    }
}
