package com.example.home.BequestProto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

/**
 * Created by HOME on 24-12-2016.
 */

public class AnnotationActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_annotation);

        File imageFile = new File(Environment.getExternalStorageDirectory(),"golden_eagle.jpg");
        if(imageFile.exists()) {
            Log.v("Annotation", "Image file exists");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap outputImageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            ImageView imageView = (ImageView) findViewById(R.id.sample_image);

            if (imageView != null) {
                Log.v("Annotation", "imageview is not empty");
                imageView.setImageBitmap(outputImageBitmap);
            } else {
                Log.v("Annotation", "imageview is  empty");
            }
        }else
            Log.v("Annotation", "imagefile doesn't exists");

    }
}
