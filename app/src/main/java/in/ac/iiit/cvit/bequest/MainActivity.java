package in.ac.iiit.cvit.bequest;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    LaunchPreferenceManager launchPreferenceManager;

    private static final String LOGTAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4;
    private static final int Click = 5;
    private static final int PERMISSIONS_REQUEST_CAMERA = 6;
    public static Menu menu;
    public static MenuItem menuItem;
    private int totalPermissions = 0;
    private boolean storageRequested = false;
    private boolean cameraRequested = false;

    private Toolbar toolbar;
    private CardView toolbarCard;
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

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        toolbarCard = (CardView) findViewById(R.id.toolbar_card);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.WHITE);
        setSupportActionBar(toolbar);

        checkAllPermissions();


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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu mMenu) {
        menu = mMenu;
        menuItem = menu.findItem(R.id.openCamera);
//        menuItem.setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.openCamera) {
//All the permissions are set. Call camera on button click
                /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//Making an intent
                Uri imageUri = Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"beq.jpg"));
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                takePictureIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent,Click);//Calling camera
                }*/

            Intent takePicture = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(takePicture);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkAllPermissions() {
        //Setting Camera permissions
        if (checkCameraPermission()) {
            cameraRequested = true;
            Log.v(LOGTAG, "MainActivity has Location permission");
        } else {
            Log.v(LOGTAG, "MainActivity Requesting Location permission");
            requestCameraPermission();
        }
        //Setting Storage permissions
        if (checkStoragePermission()) {
            storageRequested = true;
            Log.v(LOGTAG, "MainActivity has storage permission");
            checkForDownload();
        } else {
            Log.v(LOGTAG, "MainActivity Requesting storage permission");
            requestStoragePermission();
        }
    }

    /**
     * Checking if read/write permissions are set or not
     *
     * @return
     */
    protected boolean checkStoragePermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean checkCameraPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }


    protected void requestStoragePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            //Toast.makeText(this, getString(R.string.storage_permission_request), Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
            /*ImageButton button = (ImageButton) toolbarCard.findViewById(R.id.openCamera);
            button.setVisibility(View.INVISIBLE);
            button.setEnabled(false);*/
            if (menuItem != null) {
                menuItem.setVisible(false);
            }
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            Log.v(LOGTAG, "requestStoragePermission if");

        } else {
            Log.v(LOGTAG, "requestStoragePermission else");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    protected void requestCameraPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            //toast to be shown while requesting permissions
            //Toast.makeText(this, getString(R.string.gps_permission_request), Toast.LENGTH_LONG).show();
            Log.v(LOGTAG, "requestCameraPermission if");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);

        } else {
            Log.v(LOGTAG, "requestCameraPermission else");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            }
        }
    }


    protected void onActivityResult(int requestcode, int resultcode,Intent data)//Called after the intent
    {
        super.onActivityResult(requestcode, resultcode,data);
        if(requestcode==Click && resultcode==RESULT_OK) {

            /*Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get(android.provider.MediaStore.EXTRA_OUTPUT);
            if(imageBitmap==null){
                Log.v(LOGTAG,"image is null");
            }else{
                Log.v(LOGTAG,"image is not null");
            }
*/

            //Picture has been clicked and saved to storage.
            //Now perform search
            JNiActivity jNiActivity = new JNiActivity(MainActivity.this, MainActivity.this);
            jNiActivity.execute();

        }
    }
    
    public void checkForDownload(){


        launchPreferenceManager = new LaunchPreferenceManager(MainActivity.this);
        Log.v(LOGTAG,  "isDownloaded = "+launchPreferenceManager.isDownloaded());

        /*final ImageButton button = (ImageButton) toolbarCard.findViewById(R.id.openCamera);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //All the permissions are set. Call camera on button click
                *//*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//Making an intent
                Uri imageUri = Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"beq.jpg"));
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                takePictureIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent,Click);//Calling camera
                }*//*

                Intent takePicture = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(takePicture);


            }
        });
*/

        if (menu != null) {
            MenuItem openCamera = menu.findItem(R.id.openCamera);
        }


        try {

            if (!launchPreferenceManager.isDownloaded()) {
                /*button.setVisibility(View.INVISIBLE);
                button.setEnabled(false);*/
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
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

                getFragmentManager().beginTransaction()
                        .replace(R.id.main_container, DragNDrop.newInstance())
                        .commit();
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.toString());
        }
        try {

            if (launchPreferenceManager.isDownloaded()) {
                /*button.setVisibility(View.VISIBLE);
                button.setEnabled(true);*/
                if (menuItem != null) {
                    menuItem.setVisible(true);
                }
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
                storageRequested = true;
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    totalPermissions = totalPermissions + 1;
                    checkForDownload();
                    //LoadMyData(Environment.getExternalStorageDirectory().getAbsolutePath());
                } else {

                    Log.v("value", "Permission Denied, You cannot use local drive .");
                    totalPermissions = totalPermissions - 1;
                }
                break;

            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                storageRequested = true;
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(LOGTAG, "MainActivity has WRITE storage permissions");
                    totalPermissions = totalPermissions + 1;
                } else {
                    Log.v(LOGTAG, "MainActivity does not have WRITE storage permissions");
                    totalPermissions = totalPermissions - 1;
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        //Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
                        //openApplicationPermissions();
                    } else {
                        //openApplicationPermissions();
                    }
                }
                break;

            case PERMISSIONS_REQUEST_CAMERA:
                cameraRequested = true;
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(LOGTAG, "MainActivity has WRITE storage permissions");
                    totalPermissions = totalPermissions + 1;
                } else {
                    Log.v(LOGTAG, "MainActivity does not have WRITE storage permissions");
                    totalPermissions = totalPermissions - 1;
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
                        //Log.v(LOGTAG,"4 if");
                        //openApplicationPermissions();
                    } else {
                        //Log.v(LOGTAG,"4 else");
                        //openApplicationPermissions();
                    }
                }
                break;


        }

        Log.v(LOGTAG, "totalPermissions = " + totalPermissions + " storageRequested = " + storageRequested + " cameraRequested = " + cameraRequested);
        if (totalPermissions <= 0 & storageRequested & cameraRequested) {
            //Log.v(LOGTAG, "5");
            Log.v(LOGTAG, "openApplicationPermissions");
            openApplicationPermissions();
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
/*            final ImageButton button = (ImageButton) toolbarCard.findViewById(R.id.openCamera);
            button.setVisibility(View.VISIBLE);
            button.setEnabled(true);*/

            if (menuItem != null) {
                menuItem.setVisible(true);
            }

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
