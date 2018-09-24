package edu.ohio.minuku.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.model.DataRecord.TelephonyDataRecord;
import edu.ohio.minukucore.dao.DAO;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.user.User;

/**
 * Created by Lawrence on 2017/9/19.
 */

public class TelephonyDataRecordDAO implements DAO<TelephonyDataRecord> {

    private DBHelper dBHelper;


    public TelephonyDataRecordDAO(Context applicationContext){

        dBHelper = DBHelper.getInstance(applicationContext);

    }

    @Override
    public void setDevice(User user, UUID uuid) {

    }

    @Override
    public void add(TelephonyDataRecord entity) throws DAOException {
        ContentValues values = new ContentValues();

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            values.put(DBHelper.TIME, entity.getCreationTime());
            //values.put(DBHelper.daysInSurvey, entity.getTaskDayCount());
            //values.put(DBHelper.HOUR, entity.getHour());
            values.put(DBHelper.NetworkOperatorName_col, entity.getNetworkOperatorName());
            values.put(DBHelper.CallState_col, entity.getCallState());
            values.put(DBHelper.PhoneSignalType_col, entity.getPhoneSignalType());
            values.put(DBHelper.GsmSignalStrength_col, entity.getGsmSignalStrength());
            values.put(DBHelper.LTESignalStrength_col, entity.getLTESignalStrength());
            values.put(DBHelper.CdmaSignalStrengthLevel_col, entity.getCdmaSignalStrengthLevel());
            values.put(DBHelper.COL_SESSION_ID, entity.getSessionid());

            //db.insert(DBHelper.telephony_table, null, values);
            db.insertWithOnConflict(DBHelper.telephony_table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            //Log.d(TAG, "ADD"+DBHelper.id+"");
        } catch (NullPointerException e) {
            //e.printStackTrace();
        } finally {
            values.clear();
            // Closing database connection
            DBManager.getInstance().closeDatabase();
        }
    }

    @Override
    public void delete(TelephonyDataRecord entity) throws DAOException {

    }

    @Override
    public Future<List<TelephonyDataRecord>> getAll() throws DAOException {
        return null;
    }

    @Override
    public Future<List<TelephonyDataRecord>> getLast(int N) throws DAOException {
        return null;
    }

    @Override
    public void update(TelephonyDataRecord oldEntity, TelephonyDataRecord newEntity) throws DAOException {

    }
}
