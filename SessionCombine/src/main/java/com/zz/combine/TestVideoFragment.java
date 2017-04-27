package com.zz.combine;

import java.io.File;
import java.io.IOException;

/**
 * *Created by rqg on 3/31/17 2:32 PM.
 */

public class TestVideoFragment extends VideoFragment {
    private String mVideoName;

    private boolean mFileExist = false;


    public TestVideoFragment(String name, long duration) throws IOException {
        super(null);
        mVideoDurationMills = duration;
        mVideoName = name;
    }


    public void checkSetVideoFile(String path) {
        File file = new File(path);

        if (file.isFile() && file.exists()) {
            mFileExist = true;
            mVideoPath = path;
        }
    }


    public VideoFragment toVideoFragment() throws IOException {
        prepare();

        return this;
    }


}
