package com.example.home.BequestProto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;

import static com.example.home.BequestProto.PackageDownloader.LOGTAG;

/**
 * Created by HOME on 24-12-2016.
 */

public class JNiActivity extends AsyncTask<Void,Void,String> {

    public Context context;
    public Activity activity;
    private String num;
    private Bitmap outputImageBitmap;


    public JNiActivity(Context _context, Activity _activity) {

        context = _context;
        activity = _activity;

    }


    @Override
    protected String doInBackground(Void... params) {

        String path = "test.jpg";

        Mat input = new Mat();
        Mat output = new Mat();

        File imageFile = new File(Environment.getExternalStorageDirectory(),path);
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

            num = trial(inad,outad);

            return num;
        }
        Log.v(LOGTAG,"Image file does not exists");
        return null;
    }

    @Override
    protected void onPostExecute(String str) {
        TextView textView = (TextView)activity.findViewById(R.id.sample_text);
        textView.setText(num);

        Intent intent = new Intent(context, AnnotationActivity.class);
        intent.putExtra("result",str);
//        intent.putExtra("image",outputImageBitmap);
        context.startActivity(intent);

    }


    public native String trial(long in, long out);

}



