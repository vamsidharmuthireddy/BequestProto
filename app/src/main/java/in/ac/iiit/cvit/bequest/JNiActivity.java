package in.ac.iiit.cvit.bequest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;

/**
 * Created by HOME on 24-12-2016.
 */

public class JNiActivity extends AsyncTask<Void,Void,String> {

    private static final String LOGTAG = "JNiActivity";
    public Context context;
    public Activity activity;
    private String num;
    private Bitmap outputImageBitmap;

    private ProgressDialog progressDialog;


    public JNiActivity(Context _context, Activity _activity) {

        context = _context;
        activity = _activity;

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        //activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgress(0);
        progressDialog.setMessage("Getting Information");
        progressDialog.setCancelable(false);
        progressDialog.show();

        //Log.v(LOGTAG, "Progress is " + progressDialog.getProgress());
    }

    @Override
    protected String doInBackground(Void... params) {

        String path = "pic.jpg";

        Mat input = new Mat();
        Mat output = new Mat();

        File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path);
        if(imageFile.exists()) {
            Log.v(LOGTAG,"Image file exists");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            outputImageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            if(outputImageBitmap == null){

                Log.e(LOGTAG,"Path loaded bitmap is null");
            }

            Utils.bitmapToMat(outputImageBitmap,input);
            long inad = input.getNativeObjAddr();
            long outad = output.getNativeObjAddr();

            String imageFileAddress = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            GetMatch(inad, imageFileAddress);
            Log.v(LOGTAG,"Done implementing GetMatch, going to call GeoVerify");
            String treeFileAddress = Environment.getExternalStorageDirectory().getAbsolutePath();
            num = GeoVerify(treeFileAddress);
/*

            try {
                FileOutputStream fos = new FileOutputStream(imageFile);
                int height = outputImageBitmap.getHeight();
                int width = outputImageBitmap.getWidth();
                Log.v(LOGTAG,"width = "+width+" height = "+height);


                if(height > width){
                    //imageView.setRotation(-90.0f);
                    Matrix matrix = new Matrix();

                    matrix.postRotate(-90);
                    //Bitmap random = Bitmap.createBitmap(outputImageBitmap, 0,0,width,height,matrix,true);
                    outputImageBitmap = Bitmap.createBitmap(outputImageBitmap, 0,0,width,height,matrix,true);
                }
                outputImageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(LOGTAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(LOGTAG, "Error accessing file: " + e.getMessage());
            }
*/


            return num;
        }
        Log.v(LOGTAG,"Image file does not exists");
        return null;
    }

    @Override
    protected void onPostExecute(String str) {
        progressDialog.dismiss();
        TextView textView = (TextView) activity.findViewById(R.id.annotation_text);
        if(num != null && !num.equals("") ) {
            String[] parts = num.split("_");
            int resId = context.getResources().getIdentifier(parts[0], "string", context.getPackageName());
            Log.v(LOGTAG, "resId = " + resId);
            if (textView != null) {
                textView.setText(context.getString(resId));
                textView.setVisibility(View.VISIBLE);
                //textView.setText("inliers = "+parts[2]+"\n"+"r_score = "+parts[3]);
            }
        }else{
            if (textView != null) {
                textView.setText("Not able to retrieve information");
                textView.setVisibility(View.VISIBLE);
            }
        }
        Intent intent = new Intent(context, AnnotationActivity.class);
        intent.putExtra("result",num);
//        context.startActivity(intent);

    }


//    public native String trial(long in, long out, String file);

    public native void GetMatch(long in, String file);
    public native String GeoVerify(String fileAddress);

}



