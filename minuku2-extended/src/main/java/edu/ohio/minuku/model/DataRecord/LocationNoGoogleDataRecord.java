package edu.ohio.minuku.model.DataRecord;

import java.util.Date;

import edu.ohio.minukucore.model.DataRecord;

/**
 * Created by Lawrence on 2017/11/20.
 */

public class LocationNoGoogleDataRecord implements DataRecord {

    private long creationTime;

    private float latitude;
    private float longitude;
    private float Accuracy;

    public LocationNoGoogleDataRecord(float latitude, float longitude, float Accuracy) {
        this.creationTime = new Date().getTime();
        this.latitude = latitude;
        this.longitude = longitude;
        this.Accuracy = Accuracy;

    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getAccuracy(){
        return Accuracy;
    }

}
