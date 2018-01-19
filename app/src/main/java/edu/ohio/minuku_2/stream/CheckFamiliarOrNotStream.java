package edu.ohio.minuku_2.stream;

import java.util.ArrayList;
import java.util.List;

import edu.ohio.minuku_2.model.CheckFamiliarOrNotDataRecord;
import edu.ohio.minukucore.model.DataRecord;
import edu.ohio.minukucore.stream.AbstractStreamFromDevice;

/**
 * Created by Lawrence on 2017/6/5.
 */

public class CheckFamiliarOrNotStream extends AbstractStreamFromDevice<CheckFamiliarOrNotDataRecord> {
    public CheckFamiliarOrNotStream(int maxSize) {
        super(maxSize);
    }

    @Override
    public List<Class<? extends DataRecord>> dependsOnDataRecordType() {
        return new ArrayList<>();
    }
}
