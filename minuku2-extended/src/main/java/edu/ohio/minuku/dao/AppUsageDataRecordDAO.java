package edu.ohio.minuku.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.model.DataRecord.AppUsageDataRecord;
import edu.ohio.minukucore.dao.DAO;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.user.User;

/**
 * Created by Jimmy on 2017/8/8.
 */

public class AppUsageDataRecordDAO implements DAO<AppUsageDataRecord>{

    final private String TAG = "AppUsageDataRecordDAO";
    private DBHelper dBHelper;

    public AppUsageDataRecordDAO(Context applicationContext) {
        dBHelper = DBHelper.getInstance(applicationContext);
    }

    @Override
    public void setDevice(User user, UUID uuid) {

    }

    @Override
    public void add(AppUsageDataRecord entity) throws DAOException {
//        Log.d(TAG, "Adding AppUsage data record.");

        ContentValues values = new ContentValues();

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            values.put(DBHelper.TIME, entity.getCreationTime());
//            values.put(DBHelper.daysInSurvey, entity.getTaskDayCount());
//            values.put(DBHelper.HOUR, entity.getHour());
            values.put(DBHelper.ScreenStatus_col, entity.getScreen_Status());
            values.put(DBHelper.Latest_Used_App_col, entity.getLatestUsedApp());
            values.put(DBHelper.Latest_Foreground_Activity_col, entity.getLatestForegroundActivity());
            values.put(DBHelper.COL_SESSION_ID, entity.getSessionid());

            db.insert(DBHelper.appUsage_table, null, values);
        }
        catch(NullPointerException e){
            //e.printStackTrace();
        }
        finally {
            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection
        }
    }

    public void query_counting(){

    }

    @Override
    public void delete(AppUsageDataRecord entity) throws DAOException {

    }

    @Override
    public Future<List<AppUsageDataRecord>> getAll() throws DAOException {
        return null;
    }

    @Override
    public Future<List<AppUsageDataRecord>> getLast(int N) throws DAOException {
        return null;
    }

    @Override
    public void update(AppUsageDataRecord oldEntity, AppUsageDataRecord newEntity) throws DAOException {

    }
}
