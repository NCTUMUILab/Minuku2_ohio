package edu.ohio.minuku_2.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import edu.ohio.minuku.DBHelper.DBHelper;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku_2.model.SessionDataRecord;
import edu.ohio.minukucore.dao.DAO;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.user.User;

/**
 * Created by Lawrence on 2017/12/18.
 */

public class SessionDataRecordDAO implements DAO<SessionDataRecord> {
    private final String TAG = "SessionDataRecordDAO";

    private DBHelper dBHelper;
    private Context mContext;

    public SessionDataRecordDAO(Context applicationContext){
        this.mContext = applicationContext;
        dBHelper = DBHelper.getInstance(applicationContext);
    }

    @Override
    public void setDevice(User user, UUID uuid) {

    }

    @Override
    public void add(SessionDataRecord entity) throws DAOException {
        Log.d(TAG, "Adding Session data record.");

        ContentValues values = new ContentValues();
        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();
            values.put(DBHelper.TIME, entity.getCreationTime());
            values.put(DBHelper.sessionid_col, entity.getSessionid());

            db.insert(DBHelper.session_table, null, values);

        } catch (NullPointerException e) {
            e.printStackTrace();
        } finally {
            values.clear();
            DBManager.getInstance().closeDatabase();
        }
    }

    @Override
    public void delete(SessionDataRecord entity) throws DAOException {

    }

    @Override
    public Future<List<SessionDataRecord>> getAll() throws DAOException {
        return null;
    }

    @Override
    public Future<List<SessionDataRecord>> getLast(int N) throws DAOException {
        return null;
    }

    @Override
    public void update(SessionDataRecord oldEntity, SessionDataRecord newEntity) throws DAOException {

    }

}
