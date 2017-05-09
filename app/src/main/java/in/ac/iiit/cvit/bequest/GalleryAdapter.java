package in.ac.iiit.cvit.bequest;


import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by HOME on 07-05-2017.
 */


public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.DataObjectHolder> {

    private Activity activity;
    private Context context;
    private ArrayList<String> ImageNamesList = new ArrayList<String>();
    private int imageWidth;

    private ImageView galleryImage;

    private static final String LOGTAG = "GalleryAdapter";

    public class DataObjectHolder extends RecyclerView.ViewHolder {

        private ImageView galleryImage;

        public DataObjectHolder(View view) {
            super(view);
            this.galleryImage = (ImageView) view.findViewById(R.id.gallery_image);
        }
    }

    public GalleryAdapter(Context _context, Activity _activity, ArrayList<String> filePaths, int imageWidth) {
        this.context = _context;
        this.activity = _activity;
        this.ImageNamesList = filePaths;
        this.imageWidth = imageWidth;
    }


    @Override
    public GalleryAdapter.DataObjectHolder onCreateViewHolder(ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_gallery_image, parent, false);
        DataObjectHolder dataObjectHolder = new DataObjectHolder(view);
        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(GalleryAdapter.DataObjectHolder viewHolder, final int position) {


        ViewPropertyAnimation.Animator animationObject = new ViewPropertyAnimation.Animator() {
            @Override
            public void animate(View view) {
                // if it's a custom view class, cast it here
                // then find subviews and do the animations
                // here, we just use the entire view for the fade animation
                view.setAlpha(0f);

                ObjectAnimator fadeAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
                fadeAnim.setDuration(200);
                fadeAnim.start();
            }
        };


        File file = new File(ImageNamesList.get(position));

        Uri uri = Uri.fromFile(file);
        viewHolder.galleryImage.setLayoutParams(new GridView.LayoutParams(imageWidth, imageWidth));
        viewHolder.galleryImage.setBackgroundColor(context.getResources().getColor(R.color.colorBlack));
        //Log.v(LOGTAG, "file name " + file.getAbsolutePath());
        //Log.v(LOGTAG, "uri " + uri.toString());
        Glide.with(context)
                .load(file)
                .asBitmap()
//                .placeholder(R.drawable.monument)
                .centerCrop()
//                .crossFade(300)
                .animate(animationObject)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(viewHolder.galleryImage);
/*
        .into(new GlideDrawableImageViewTarget(viewHolder.galleryImage) {
            @Override public void onResourceReady(GlideDrawable resource,
                                                  GlideAnimation<? super GlideDrawable> animation) {
                super.onResourceReady(resource, new GlideAnimator(context).getGlideAnimation(animation));
            }
        }
*/

        //viewHolder.galleryImage.setImageURI(uri);

        final int _position = position;
        final ArrayList<String> ff = new ArrayList<String>(ImageNamesList);
        viewHolder.galleryImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(LOGTAG, "onClick");

            }
        });


        viewHolder.galleryImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.v(LOGTAG, "onLongClick");
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    v.startDragAndDrop(data, shadowBuilder, v, 0);
                } else {
                    v.startDrag(data, shadowBuilder, v, 0);
                }

                return true;
            }
        });


        viewHolder.galleryImage.setOnDragListener(new View.OnDragListener() {
            Drawable enterShape = activity.getResources().getDrawable(R.drawable.monument);
            Drawable historyDrawer;

            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        Log.v(LOGTAG, "ACTION_DRAG_STARTED");
                        break;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        historyDrawer = v.getBackground();
                        v.setBackground(enterShape);
                        Log.v(LOGTAG, "ACTION_DRAG_ENTERED");
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                        Log.v(LOGTAG, "ACTION_DRAG_EXITED");
                        v.setBackground(historyDrawer);
                        break;
                    case DragEvent.ACTION_DROP:
                        Log.v(LOGTAG, "ACTION_DROP");
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        Log.v(LOGTAG, "ACTION_DRAG_ENDED");
                        break;
                }
                return false;
            }
        });


    }

    @Override
    public int getItemCount() {
        return this.ImageNamesList.size();
    }


}