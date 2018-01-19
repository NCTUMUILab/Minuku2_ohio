package edu.ohio.minuku.stream;

import java.util.ArrayList;
import java.util.List;

import edu.ohio.minuku.model.DataRecord.ActivityRecognitionDataRecord;
import edu.ohio.minukucore.model.DataRecord;
import edu.ohio.minukucore.stream.AbstractStreamFromDevice;

/**
 * Created by Lawrence on 2017/5/22.
 */

public class ActivityRecognitionStream extends AbstractStreamFromDevice<ActivityRecognitionDataRecord> {

    public ActivityRecognitionStream(int maxSize) {
        super(maxSize);
    }

    @Override
    public List<Class<? extends DataRecord>> dependsOnDataRecordType() {
        return new ArrayList<>();
    }

}
