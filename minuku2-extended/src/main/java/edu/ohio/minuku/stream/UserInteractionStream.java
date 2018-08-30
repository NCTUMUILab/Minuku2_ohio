package edu.ohio.minuku.stream;

import java.util.ArrayList;
import java.util.List;

import edu.ohio.minuku.model.DataRecord.UserInteractionDataRecord;
import edu.ohio.minukucore.model.DataRecord;
import edu.ohio.minukucore.stream.AbstractStreamFromDevice;

/**
 * Created by Lawrence on 2018/8/30.
 */

public class UserInteractionStream extends AbstractStreamFromDevice<UserInteractionDataRecord> {

    public UserInteractionStream(int maxSize) {
        super(maxSize);
    }

    @Override
    public List<Class<? extends DataRecord>> dependsOnDataRecordType() {
        return new ArrayList<>();
    }
}
