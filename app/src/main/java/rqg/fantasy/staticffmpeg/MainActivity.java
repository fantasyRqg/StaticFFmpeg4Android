package rqg.fantasy.staticffmpeg;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.zz.combine.SSNative;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);

        SSNative.resampleVideo("adfaadfa");
    }

}
