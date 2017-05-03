package in.ac.iiit.cvit.bequest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
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


        File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "beq.jpg");
        Uri imageUri = Uri.fromFile(imageFile);
        int orientation = 0, rotation = 0;





        if(imageFile.exists()) {

            Log.v("Annotation", "Image file exists");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap outputImageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            int height = outputImageBitmap.getHeight();
            int width = outputImageBitmap.getWidth();
            Log.v(LOGTAG, "width = " + width + " height = " + height);
            rotation = getWindowManager().getDefaultDisplay().getRotation();

            Log.v(LOGTAG, "Built in rotation = " + rotation + " " + Surface.ROTATION_0 + " " + Surface.ROTATION_90 + " " + Surface.ROTATION_180 + " " + Surface.ROTATION_270);
            Camera.CameraInfo info;

            if (rotation == Surface.ROTATION_0) {
            }
            ImageView imageView = (ImageView) findViewById(R.id.sample_image);
            Matrix matrix = new Matrix();
            if (height > width) {
                //imageView.setRotation(-90.0f);
                matrix.postRotate(-90, width / 2, height / 2);


//                outputImageBitmap = Bitmap.createBitmap(outputImageBitmap, 0,0,width,height,matrix,true);
                imageView.setScaleType(ImageView.ScaleType.MATRIX);

            } else {
                //matrix.postRotate(0,height/2,width/2);
            }
            imageView.setBackgroundColor(getResources().getColor(R.color.colorAccent));

            if (imageView != null) {
                Log.v(LOGTAG, "imageview is not empty");
                imageView.setImageBitmap(outputImageBitmap);

                imageView.setImageMatrix(matrix);
                String result = getIntent().getStringExtra("result");
                Log.v(LOGTAG,"result = "+result);
                TextView textView = (TextView) findViewById(R.id.annotation_text);
                if(result != null && !result.equals("") ) {
                    String[] parts = result.split("_");
                    int resId = getResources().getIdentifier(parts[0], "string", getPackageName());
                    textView.setText(getString(resId));
                    //textView.setText(parts[2]+"\n"+parts[3]+"\n"+getString(resId));
                    Log.v("inliers","m_score = "+parts[2]+"r_score"+parts[3]);
                }else{
                    textView.setText("Not able to retrieve information");
                }
                //textView.setBackground(Drawable.createFromPath(imageFile.getAbsolutePath()));
            } else {
                Log.v(LOGTAG, "imageview is  empty");
            }
        }else
            Log.v(LOGTAG, "imagefile doesn't exists");


    }


    private void setViews() {

    }


 public void loadWordFile(){

    }


}