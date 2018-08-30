package edu.ohio.minuku.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import edu.ohio.minuku.Data.DBHelper;
import edu.ohio.minuku.manager.DBManager;
import edu.ohio.minuku.model.DataRecord.UserInteractionDataRecord;
import edu.ohio.minukucore.dao.DAO;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.user.User;

/**
 * Created by Lawrence on 2018/8/30.
 */

public class UserInteractionDataRecordDAO implements DAO<UserInteractionDataRecord> {
    private String TAG = "UserInteractionDataRecordDAO";

    private Context mContext;

    public UserInteractionDataRecordDAO(Context applicationContext) {
        this.mContext = applicationContext;

    }

    @Override
    public void setDevice(User user, UUID uuid) {

    }

    @Override
    public void add(UserInteractionDataRecord entity) throws DAOException {

        Log.d(TAG, "Adding userInteraction data record.");

        ContentValues values = new ContentValues();

        try {
            SQLiteDatabase db = DBManager.getInstance().openDatabase();

            values.put(DBHelper.TIME, entity.getCreationTime());
            values.put(DBHelper.Present_col, entity.getPresent());
            values.put(DBHelper.Unlock_col, entity.getUnlock());
            values.put(DBHelper.Background_col, entity.getBackground());
            values.put(DBHelper.Foreground_col, entity.getForeground());

            db.insert(DBHelper.userInteraction_table, null, values);
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
        finally {
            values.clear();
            DBManager.getInstance().closeDatabase(); // Closing database connection
        }
    }

    @Override
    public void delete(UserInteractionDataRecord entity) throws DAOException {

    }

    @Override
    public Future<List<UserInteractionDataRecord>> getAll() throws DAOException {
        return null;
    }

    @Override
    public Future<List<UserInteractionDataRecord>> getLast(int N) throws DAOException {
        return null;
    }

    @Override
    public void update(UserInteractionDataRecord oldEntity, UserInteractionDataRecord newEntity) throws DAOException {

    }
}
