package edu.ohio.minuku_2.model;

import java.util.Date;

import edu.ohio.minukucore.model.DataRecord;

/**
 * Created by Lawrence on 2017/12/18.
 */

public class SessionDataRecord implements DataRecord {
    public long creationTime;
    private int sessionid;

    public SessionDataRecord(int sessionid){
        this.creationTime = new Date().getTime();
        this.sessionid = sessionid;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    public int getSessionid() {
        return sessionid;
    }
}
