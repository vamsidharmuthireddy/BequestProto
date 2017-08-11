package in.ac.iiit.cvit.bequest;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by HOME on 07-05-2017.
 */


public class DragNDropGalleryAdapter extends RecyclerView.Adapter<DragNDropGalleryAdapter.DataObjectHolder> {

    private Activity activity;
    private Context context;
    private ArrayList<String> ImageNamesList = new ArrayList<String>();
    private int last_image_position;
    private int imageWidth;


    private ImageView galleryImage;

    private static final String LOGTAG = "DragNDropGalleryAdapter";

    public class DataObjectHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener, View.OnDragListener {

        private ImageView galleryImage;

        public DataObjectHolder(View view) {
            super(view);
            this.galleryImage = (ImageView) view.findViewById(R.id.gallery_image);

            final int position = getAdapterPosition();
            if (position % 6 == 5 || position % 6 == 4 || position > last_image_position) {
                galleryImage.setImageBitmap(null);
                galleryImage.setOnTouchListener(null);
                galleryImage.setOnClickListener(null);
                galleryImage.setOnLongClickListener(null);

            } else {
                galleryImage.buildDrawingCache(true);

                galleryImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.v(LOGTAG, "onClick position = " + getAdapterPosition());

                    }
                });

                galleryImage.setOnLongClickListener(this);
                galleryImage.setOnDragListener(this);


            }

        }


        @Override
        public boolean onLongClick(View view) {
            int __position = getAdapterPosition();
            Log.v(LOGTAG, "onLongClick position = " + getAdapterPosition());
            if (__position % 6 == 5 || __position % 6 == 4 || __position > last_image_position) {
            } else {
                ImageView questionMarkImage = (ImageView) activity.findViewById(R.id.question_mark);
                Animation anim = questionMarkImage.getAnimation();
                Log.v(LOGTAG, anim + "");
                if (anim == null) {
                    anim = new MyAnimation(questionMarkImage, 100);
                    anim.setDuration(5000);
                    anim.setRepeatCount(Animation.INFINITE);
                    anim.setInterpolator(new LinearInterpolator());
                    anim.setStartTime(0);
                    questionMarkImage.startAnimation(anim);
                    questionMarkImage.setVisibility(View.VISIBLE);
                }

                CoordinatorLayout dropAreaContainer = (CoordinatorLayout) activity.findViewById(R.id.drop_area_container);
                dropAreaContainer.setVisibility(View.INVISIBLE);

                ClipData data = ClipData.newPlainText("Dragdata", ImageNamesList.get(__position) + "__" + __position);
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    view.startDragAndDrop(data, shadowBuilder, view, 0);
                } else {
                    view.startDrag(data, shadowBuilder, view, 0);
                }
            }
            return true;
        }

        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            int __position = getAdapterPosition();


            final View.OnLongClickListener onLongClickListener = this;


            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
//                        Log.v(LOGTAG, "ACTION_DRAG_STARTED");

                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
//                                historyDrawer = v.getBackground();
//                                v.setBackground(enterShape);
                    //Log.v(LOGTAG, "ACTION_DRAG_ENTERED");
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    //Log.v(LOGTAG,v.getX()+" "+v.getY());
                case DragEvent.ACTION_DRAG_EXITED:
                    //Log.v(LOGTAG, "ACTION_DRAG_EXITED");
//                                v.setBackground(historyDrawer);
                    break;
                case DragEvent.ACTION_DROP:

                    // Gets the item containing the dragged data
                    ClipData.Item item = dragEvent.getClipData().getItemAt(0);

                    // Gets the text data from the item.
                    String data = item.getText().toString();
                    String selected_image_address = data.split("__")[0];
                    int dragPosition = Integer.parseInt(data.split("__")[1]);
                    //CharSequence dragData = item.getText().toString();

                    // Displays a message containing the dragged data.
                    Toast.makeText(context, "Dragged data from " + dragPosition + " to " + __position, Toast.LENGTH_LONG).show();
                    Log.v(LOGTAG, "ACTION_DROP " + dragPosition + " to " + __position + " " + selected_image_address);
                    if (__position % 6 == 5 || __position % 6 == 4) {


                        String imageName = selected_image_address.substring(
                                selected_image_address.lastIndexOf(File.separator) + 1,
                                selected_image_address.lastIndexOf("."));
                        Log.v(LOGTAG, imageName);
                        int resIdInterestPoint = context.getResources().getIdentifier(imageName, "string", context.getPackageName());

                        CardView interestPointCard = (CardView) activity.findViewById(R.id.card_interest_point);
                        final ImageView resultImageView = (ImageView) interestPointCard.findViewById(R.id.query_drop_area);
                        TextView resultTextView = (TextView) interestPointCard.findViewById(R.id.cardview_text);
                        resultTextView.setText(context.getString(resIdInterestPoint));

//                            ImageView resultImageView = (ImageView) activity.findViewById(R.id.query_drop_area);
                        File file = new File(selected_image_address);

                        Glide.with(context)
                                .load(file)
                                .asBitmap()
                                .centerCrop()
                                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                                .into(resultImageView);
                        resultImageView.invalidate();


                        final ImageView questionMarkImage = (ImageView) activity.findViewById(R.id.question_mark);
                        questionMarkImage.clearAnimation();
                        ObjectAnimator flip = ObjectAnimator.ofFloat(questionMarkImage, "rotationY", 0f, 360f);
                        flip.setDuration(1500);
                        flip.setRepeatCount(2);
//                        flip.start();
                        flip.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animator) {
                                Log.v(LOGTAG, "Flip started");
                            }

                            @Override
                            public void onAnimationEnd(Animator animator) {
                                Log.v(LOGTAG, "Flip Ended");
                                questionMarkImage.setVisibility(View.INVISIBLE);
                                questionMarkImage.invalidate();

                                resultImageView.setVisibility(View.VISIBLE);

                                CoordinatorLayout dropAreaContainer = (CoordinatorLayout) activity.findViewById(R.id.drop_area_container);
                                dropAreaContainer.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animator) {

                            }
                        });

                        questionMarkImage.setVisibility(View.INVISIBLE);
                        questionMarkImage.invalidate();

                        resultImageView.setVisibility(View.VISIBLE);

                        CoordinatorLayout dropAreaContainer = (CoordinatorLayout) activity.findViewById(R.id.drop_area_container);
                        dropAreaContainer.setVisibility(View.VISIBLE);
                    }


                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    //Log.v(LOGTAG, "ACTION_DRAG_ENDED");
                    break;
            }
            return true;

        }

    }

    public DragNDropGalleryAdapter(Context _context, Activity _activity, ArrayList<String> filePaths, int last_image_position, int imageWidth) {
        this.context = _context;
        this.activity = _activity;
        this.ImageNamesList = filePaths;
        this.last_image_position = last_image_position;
        this.imageWidth = imageWidth;
    }


    @Override
    public DragNDropGalleryAdapter.DataObjectHolder onCreateViewHolder(ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_gallery_image, parent, false);
        DataObjectHolder dataObjectHolder = new DataObjectHolder(view);
        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(final DragNDropGalleryAdapter.DataObjectHolder viewHolder, final int position) {

        Log.v(LOGTAG, "onBindViewHolder position = " + position);
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
        viewHolder.galleryImage.setBackgroundColor(context.getResources().getColor(R.color.colorWhite));
        viewHolder.galleryImage.setTag(R.string.position, ImageNamesList.get(position) + "__" + position);


        final int _position = position;
        final ArrayList<String> ff = new ArrayList<String>(ImageNamesList);

        if (position % 6 == 5 || position % 6 == 4 || position > last_image_position) {
            viewHolder.galleryImage.setImageBitmap(null);
            viewHolder.galleryImage.setOnTouchListener(null);
            viewHolder.galleryImage.setOnClickListener(null);
            viewHolder.galleryImage.setOnLongClickListener(null);

        } else {
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
        }


//
//        viewHolder.galleryImage.setOnDragListener(new View.OnDragListener() {
//            Drawable enterShape = activity.getResources().getDrawable(R.drawable.monument);
//            Drawable historyDrawer;
//
//            @Override
//            public boolean onDrag(View v, DragEvent event) {
//                switch (event.getAction()) {
//                    case DragEvent.ACTION_DRAG_STARTED:
////                        Log.v(LOGTAG, "ACTION_DRAG_STARTED");
//
//                        break;
//                    case DragEvent.ACTION_DRAG_ENTERED:
//                        historyDrawer = v.getBackground();
//                        v.setBackground(enterShape);
//                        //Log.v(LOGTAG, "ACTION_DRAG_ENTERED");
//                        break;
//                    case DragEvent.ACTION_DRAG_LOCATION:
//                        //Log.v(LOGTAG,v.getX()+" "+v.getY());
//                    case DragEvent.ACTION_DRAG_EXITED:
//                        //Log.v(LOGTAG, "ACTION_DRAG_EXITED");
//                        v.setBackground(historyDrawer);
//                        break;
//                    case DragEvent.ACTION_DROP:
//                        // Gets the item containing the dragged data
//
//                        ClipData.Item item = event.getClipData().getItemAt(0);
//
//                        // Gets the text data from the item.
//                        String data = item.getText().toString();
//                        String selected_image_address = data.split("__")[0];
//                        int dragPosition = Integer.parseInt(data.split("__")[1]);
//                        //CharSequence dragData = item.getText().toString();
//
//                        // Displays a message containing the dragged data.
//                        Toast.makeText(context, "Dragged data from " + dragPosition + " to " + _position, Toast.LENGTH_LONG).show();
//                        Log.v(LOGTAG, "ACTION_DROP " + dragPosition + " to " + _position + " " + selected_image_address);
//                        if (position % 6 == 5 || position % 6 == 4) {
//
//                            String imageName = selected_image_address.substring(
//                                    selected_image_address.lastIndexOf(File.separator) + 1,
//                                    selected_image_address.lastIndexOf("."));
//                            Log.v(LOGTAG, imageName);
//                            int resIdInterestPoint = context.getResources().getIdentifier(imageName, "string", context.getPackageName());
//
//                            CardView interestPointCard = (CardView) activity.findViewById(R.id.card_interest_point);
//                            final ImageView resultImageView = (ImageView) interestPointCard.findViewById(R.id.query_drop_area);
//                            TextView resultTextView = (TextView) interestPointCard.findViewById(R.id.cardview_text);
//                            resultTextView.setText(context.getString(resIdInterestPoint));
//
////                            ImageView resultImageView = (ImageView) activity.findViewById(R.id.query_drop_area);
//                            File file = new File(selected_image_address);
//
//                            Glide.with(context)
//                                    .load(file)
//                                    .asBitmap()
//                                    .centerCrop()
//                                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
//                                    .into(resultImageView);
//                            resultImageView.invalidate();
//
//
//                            final ImageView questionMarkImage = (ImageView) activity.findViewById(R.id.question_mark);
//                            questionMarkImage.clearAnimation();
//                            ObjectAnimator flip = ObjectAnimator.ofFloat(questionMarkImage, "rotationY", 0f, 360f);
//                            flip.setDuration(1500);
//                            flip.setRepeatCount(2);
//                            flip.start();
//                            flip.addListener(new Animator.AnimatorListener() {
//                                @Override
//                                public void onAnimationStart(Animator animator) {
//
//                                }
//
//                                @Override
//                                public void onAnimationEnd(Animator animator) {
//
//                                    questionMarkImage.setVisibility(View.INVISIBLE);
//                                    questionMarkImage.invalidate();
//
//                                    resultImageView.setVisibility(View.VISIBLE);
//
//                                    CoordinatorLayout dropAreaContainer = (CoordinatorLayout) activity.findViewById(R.id.drop_area_container);
//                                    dropAreaContainer.setVisibility(View.VISIBLE);
//                                }
//
//                                @Override
//                                public void onAnimationCancel(Animator animator) {
//
//                                }
//
//                                @Override
//                                public void onAnimationRepeat(Animator animator) {
//
//                                }
//                            });
//
//
//
//                        } else {
//
//                        }
//
//
//                        break;
//                    case DragEvent.ACTION_DRAG_ENDED:
//                        //Log.v(LOGTAG, "ACTION_DRAG_ENDED");
//                        break;
//                }
//                return true;
//            }
//
//        });


    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public void getItem(int position) {

    }

    @Override
    public int getItemCount() {
        return this.ImageNamesList.size();
    }

}