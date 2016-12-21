package com.example.home.BequestProto;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.example.home.BequestProto.PackageDownloader.LOGTAG;

public class MainActivity extends AppCompatActivity {

    LaunchPreferenceManager launchPreferenceManager;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        launchPreferenceManager = new LaunchPreferenceManager(MainActivity.this);
        Button button = (Button) findViewById(R.id.openCamera);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);

            }
        });

        try {

            if (!launchPreferenceManager.isDownloaded()) {
                button.setEnabled(false);
                //From second time whenever this app is opened, this activity is shown
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Do you want to download ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            // do something when the button is clicked
                            public void onClick(DialogInterface arg0, int arg1) {
                                new PackageDownloader(MainActivity.this, MainActivity.this).execute("hello");
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            // do something when the button is clicked
                            public void onClick(DialogInterface arg0, int arg1) {
                                //onBackPressed();
                            }
                        })
                        .show();

            } else {
                button.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.toString());
        }


    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
