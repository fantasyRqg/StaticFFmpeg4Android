package com.zz.combine;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * *Created by rqg on 3/30/17 3:16 PM.
 */

public class ResourceUtils {
    private static final String TAG = "ResourceUtils";

    public static Uri resourceToUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID));
    }

    public static Uri resourceToUri2(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                resID);
    }

    public static boolean rawToFile(Context context, int resId, String outPath) {
        try {
            OutputStream outputStream = new FileOutputStream(outPath);
            final Resources resources = context.getResources();
            final byte[] largeBuffer = new byte[1024 * 4];
            int totalBytes = 0;
            int bytesRead = 0;


            final InputStream inputStream = resources.openRawResource(resId);

            while ((bytesRead = inputStream.read(largeBuffer)) > 0) {
                if (largeBuffer.length == bytesRead) {
                    outputStream.write(largeBuffer);
                } else {
                    final byte[] shortBuffer = new byte[bytesRead];
                    System.arraycopy(largeBuffer, 0, shortBuffer, 0, bytesRead);
                    outputStream.write(shortBuffer);
                }
                totalBytes += bytesRead;
            }
            inputStream.close();

            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            Log.e(TAG, "rawToFile: ", e);
            return false;
        }
        return true;
    }


    public static boolean deleteFileByPath(String mediaPath) {
        File file = new File(mediaPath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }


        return false;
    }
}
