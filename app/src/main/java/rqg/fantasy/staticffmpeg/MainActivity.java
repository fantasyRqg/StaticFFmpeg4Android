package rqg.fantasy.staticffmpeg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.zz.combine.scale.VideoFragmentScaler;
import com.zz.combine.scale.VideoScaleHelper;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        Button button = (Button) findViewById(R.id.sample_text);
        button.setOnClickListener(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 0);
        }
    }

    @Override
    public void onClick(View v) {
        VideoScaleHelper videoScaleHelper = new VideoScaleHelper();

        final long start = System.currentTimeMillis();
        Log.i(TAG, "onClick:  start scale ");
        videoScaleHelper.scaleVideo("/sdcard/movie_4k.mp4", "/sdcard/scale_out.mp4", new VideoFragmentScaler.OnScaleFinished() {
            @Override
            public void onFinished() {
                Log.i(TAG, "onFinished: " + (System.currentTimeMillis() - start) / 1000f);
            }
        });

    }
}
