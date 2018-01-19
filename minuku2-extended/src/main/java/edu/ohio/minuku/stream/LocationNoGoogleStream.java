package edu.ohio.minuku.stream;

import java.util.ArrayList;
import java.util.List;

import edu.ohio.minuku.model.DataRecord.LocationNoGoogleDataRecord;
import edu.ohio.minukucore.model.DataRecord;
import edu.ohio.minukucore.stream.AbstractStreamFromDevice;

/**
 * Created by Lawrence on 2017/11/20.
 */

public class LocationNoGoogleStream extends AbstractStreamFromDevice<LocationNoGoogleDataRecord> {

    public LocationNoGoogleStream(int maxSize) {
        super(maxSize);
    }

    @Override
    public List<Class<? extends DataRecord>> dependsOnDataRecordType() {
        return new ArrayList<>();
    }
}
