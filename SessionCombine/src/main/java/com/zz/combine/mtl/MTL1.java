package com.zz.combine.mtl;

import com.zz.combine.R;
import com.zz.combine.TimePoint;


/**
 * *Created by rqg on 3/13/17 3:08 PM.
 */

public class MTL1 extends MusicTimelineBase {

    private static final TimePoint mOriginal[] = {
            new TimePoint((long) (1000 * (1.0 + 18.0 / 30.0)), TimePoint.TIMEPOINT_SMALL),
            new TimePoint((long) (1000 * (2.0 + 16.0 / 30.0)), TimePoint.TIMEPOINT_BIG),
            new TimePoint((long) (1000 * (3.0 + 24.0 / 30.0)), TimePoint.TIMEPOINT_SMALL),
            new TimePoint((long) (1000 * (5.0 + 2.0 / 30.0)), TimePoint.TIMEPOINT_BIG),
            new TimePoint((long) (1000 * (6.0 + 11.0 / 30.0)), TimePoint.TIMEPOINT_SMALL),
            new TimePoint((long) (1000 * (7.0 + 17.0 / 30.0)), TimePoint.TIMEPOINT_BIG),
            new TimePoint((long) (1000 * (8.0 + 26.0 / 30.0)), TimePoint.TIMEPOINT_SMALL),
    };

    public MTL1() {
        super();


    }


    @Override
    protected int getMusicRawId() {
        return R.raw.music1;
    }

    @Override
    public TimePoint[] getOriginalTimePoints() {
        return mOriginal;
    }
}
