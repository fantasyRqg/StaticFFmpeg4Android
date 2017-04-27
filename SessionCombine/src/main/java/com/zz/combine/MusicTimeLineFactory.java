package com.zz.combine;

import com.zz.combine.mtl.MTL1;
import com.zz.combine.mtl.MTL2;
import com.zz.combine.mtl.MusicTimelineBase;

/**
 * *Created by rqg on 3/20/17 3:02 PM.
 */

public class MusicTimeLineFactory {
    private static final String TAG = "MusicTimeLineFactory";

    public static MusicTimelineBase randomGetMTL() {
        if (System.currentTimeMillis() % 2 == 0) {
            return new MTL2();
        } else {
            return new MTL1();
        }

    }
}
