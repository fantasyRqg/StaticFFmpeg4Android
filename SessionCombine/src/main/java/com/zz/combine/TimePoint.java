package com.zz.combine;

/**
 * *Created by rqg on 3/13/17 3:01 PM.
 */

public class TimePoint {
    public static final int TIMEPOINT_BIG = 1;
    public static final int TIMEPOINT_SMALL = 2;

    private long endTimeMills;
    private int type;

    public TimePoint(long endTimeMills, int type) {
        this.endTimeMills = endTimeMills;
        this.type = type;
    }

    public long getEndTimeMills() {
        return endTimeMills;
    }

    public int getType() {
        return type;
    }

    public VideoFragment videoFragment;

    @Override
    public String toString() {
        return "endTimeMills = [" + endTimeMills + "], type = [" + type + "]";
    }
}
