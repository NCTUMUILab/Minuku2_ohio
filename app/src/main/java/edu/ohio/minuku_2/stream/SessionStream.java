package edu.ohio.minuku_2.stream;

import java.util.ArrayList;
import java.util.List;

import edu.ohio.minuku_2.model.SessionDataRecord;
import edu.ohio.minukucore.model.DataRecord;
import edu.ohio.minukucore.stream.AbstractStreamFromDevice;

/**
 * Created by Lawrence on 2017/12/18.
 */

public class SessionStream extends AbstractStreamFromDevice<SessionDataRecord> {
    public SessionStream(int maxSize) {
        super(maxSize);
    }

    @Override
    public List<Class<? extends DataRecord>> dependsOnDataRecordType() {
        return new ArrayList<>();
    }

}
