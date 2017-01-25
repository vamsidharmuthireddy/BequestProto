package com.example.home.BequestProto;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    LaunchPreferenceManager launchPreferenceManager;

    private static final String LOGTAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4;
    private static final int Click = 5;
    // Used to load the 'native-lib' library on application startup.
    static {
        try {
            System.loadLibrary("native-lib");
            System.loadLibrary("opencv_java");
        System.loadLibrary("nonfree");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("unable to load the opencv  library" + e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (checkPermission()) {
            checkForDownload();

        } else {
            requestPermission();
        }


 /*     Button button = (Button) findViewById(R.id.openCamera);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//Making an intent
                Uri imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"beq.jpg"));
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent,Click);//Calling camera

            }

        });
*/
//        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

 //       Log.v("AFTER_JNI","Done storing the image");

    }


    protected boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
            Button button = (Button) findViewById(R.id.openCamera);
            button.setVisibility(View.INVISIBLE);
            button.setEnabled(false);
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 3);


        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
            }
        }
    }

    protected void onActivityResult(int requestcode, int resultcode,Intent data)//Called after the intent
    {
        if(requestcode==Click && resultcode==RESULT_OK)
        {
            JNiActivity jNiActivity = new JNiActivity(MainActivity.this, MainActivity.this);
            jNiActivity.execute();

        }
    }
    
    public void checkForDownload(){


        launchPreferenceManager = new LaunchPreferenceManager(MainActivity.this);
        Log.v(LOGTAG,  "isDownloaded = "+launchPreferenceManager.isDownloaded());
        final Button button = (Button) findViewById(R.id.openCamera);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//Making an intent
                Uri imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"beq.jpg"));
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent,Click);//Calling camera

            }
        });

        try {

            if (!launchPreferenceManager.isDownloaded()) {
                button.setVisibility(View.INVISIBLE);
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
                new Loader().execute();
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.toString());
        }
        try {

            if (launchPreferenceManager.isDownloaded()) {
                button.setVisibility(View.VISIBLE);
                button.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.toString());
        }





    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkForDownload();
                    //LoadMyData(Environment.getExternalStorageDirectory().getAbsolutePath());
                } else {

                    Log.e("value", "Permission Denied, You cannot use local drive .");

                }
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
                        openApplicationPermissions();
                    } else {
                        //openApplicationPermissions();
                    }
                }
            }
        }
    }


    private void openApplicationPermissions() {
        final Intent intent_permissions = new Intent();
        intent_permissions.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent_permissions.addCategory(Intent.CATEGORY_DEFAULT);
        intent_permissions.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));

        intent_permissions.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent_permissions.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent_permissions.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        MainActivity.this.startActivity(intent_permissions);
    }




    private class Loader extends AsyncTask<Void, Void, Void> {


        private ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(false);
            progressDialog.setProgress(0);
            progressDialog.setMessage("Loading Files");
            progressDialog.setCancelable(false);
            progressDialog.show();

            //Log.v(LOGTAG, "Progress is " + progressDialog.getProgress());
        }
        @Override
        protected Void doInBackground(Void... params) {
            LoadMyData(Environment.getExternalStorageDirectory().getAbsolutePath());
            return null;
        }

        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            progressDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            final Button button = (Button) findViewById(R.id.openCamera);
            button.setVisibility(View.VISIBLE);
            button.setEnabled(true);

        }
    }





    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native void LoadData(String filelocation);

    public native void LoadMyData(String filelocation);

}
