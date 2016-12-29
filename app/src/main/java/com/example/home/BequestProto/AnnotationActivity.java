package com.example.home.BequestProto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

/**
 * Created by HOME on 24-12-2016.
 */

public class AnnotationActivity extends AppCompatActivity {

    private static final String LOGTAG = "AnnotationActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_annotation);


        File imageFile = new File(Environment.getExternalStorageDirectory(),"beq.jpg");
        if(imageFile.exists()) {
            Log.v("Annotation", "Image file exists");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap outputImageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            ImageView imageView = (ImageView) findViewById(R.id.sample_image);

            if (imageView != null) {
                Log.v(LOGTAG, "imageview is not empty");
                imageView.setImageBitmap(outputImageBitmap);
                String result = getIntent().getStringExtra("result");
                Log.v(LOGTAG,"result = "+result);
                TextView textView = (TextView) findViewById(R.id.annotation_text);
                String[] parts =result.split("_");
                int resId = getResources().getIdentifier(parts[0], "string", getPackageName());
                textView.setText(getString(resId));

                //textView.setBackground(Drawable.createFromPath(imageFile.getAbsolutePath()));
            } else {
                Log.v(LOGTAG, "imageview is  empty");
            }
        }else
            Log.v(LOGTAG, "imagefile doesn't exists");





    }


 public void loadWordFile(){

    }


}