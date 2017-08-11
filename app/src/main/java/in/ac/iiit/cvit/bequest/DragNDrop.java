package in.ac.iiit.cvit.bequest;

import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DragNDrop#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DragNDrop extends Fragment {


    public static int NUM_OF_COLUMNS = 6;

    // Gridview image padding
    public static int GRID_PADDING = 2; // in dp
    private int columnWidth;
    private static final String LOGTAG = "DragNDrop";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DragNDrop.
     */
    // TODO: Rename and change types and number of parameters
    public static DragNDrop newInstance() {
        return new DragNDrop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_drag_ndrop, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        setGridView();
    }

    private void setGridView() {

        //GRID_PADDING in pixels
        GRID_PADDING = (int) getResources().getDimension(R.dimen.recycler_item_margin);

//columnWidth in pixels
        //since we are not using entire screen, recalculate the width
        columnWidth = (int) ((getScreenWidth() - ((NUM_OF_COLUMNS + 1) * GRID_PADDING)) / NUM_OF_COLUMNS);

        ImageView questionMarkImage = (ImageView) getActivity().findViewById(R.id.question_mark);
//        questionMarkImage.getLayoutParams().width = columnWidth;
        questionMarkImage.getLayoutParams().height = columnWidth;

        Animation anim = new MyAnimation(questionMarkImage, 100);
        anim.setDuration(5000);
        anim.setRepeatCount(Animation.INFINITE);
//        anim.setRepeatMode(Animation.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.setStartTime(0);
        questionMarkImage.startAnimation(anim);



        RecyclerView dragNdropGridView = (RecyclerView) getActivity().findViewById(R.id.recyclerview_gallery);
        dragNdropGridView.setHasFixedSize(true);
        //RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),NUM_OF_COLUMNS);
        RecyclerView.LayoutManager layoutManager =
                new PreLoadingGridLayoutManager(getActivity().getApplicationContext(), NUM_OF_COLUMNS);
        layoutManager.setItemPrefetchEnabled(true);
        new PreLoadingGridLayoutManager(getActivity().getApplicationContext(), NUM_OF_COLUMNS).setPages(5);
        layoutManager.setMeasurementCacheEnabled(true);
        dragNdropGridView.setLayoutManager(layoutManager);
        dragNdropGridView.isDrawingCacheEnabled();
        dragNdropGridView.addItemDecoration(new MarginDecoration(getActivity(), NUM_OF_COLUMNS, GRID_PADDING, true));
        dragNdropGridView.setHasFixedSize(true);
        dragNdropGridView.setVerticalScrollBarEnabled(true);
        dragNdropGridView.setBackgroundColor(getResources().getColor(R.color.colorWhite));

        ArrayList<String> ImageNamesList = new ArrayList<String>();
//
//        for (int i = 0; i < 20; i++) {
//            ImageNamesList.add(Environment.getExternalStorageDirectory().getAbsolutePath() + "/pic.jpg");
//        }
//

        String dragNdropFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                + getString(R.string.basepackagename) + File.separator + getString(R.string.dragNdropfolder);

        File dragNdropFolder = new File(dragNdropFolderPath);

        File imageFiles[] = dragNdropFolder.listFiles();
        File newImageFiles[] = new File[2 * imageFiles.length];

        int last_image_position = ((imageFiles.length / 4) * 6) + imageFiles.length % 4 - 1;
        int last_adapter_position = ((imageFiles.length / 4) * 6) + (6 - 1);
        int numImages = 0;
        String imageName;
        int adapter_position = 0;
        while (adapter_position < last_adapter_position) {
            if (adapter_position % 6 == 5 || adapter_position % 6 == 4 || adapter_position > last_image_position) {
                newImageFiles[adapter_position] = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                ImageNamesList.add("");

                Log.v(LOGTAG, imageFiles.length + " " + adapter_position + " " + last_image_position + " " + last_adapter_position + " " + newImageFiles[adapter_position].getAbsolutePath());
                adapter_position = adapter_position + 1;
            } else {
                newImageFiles[adapter_position] = imageFiles[numImages];
                ImageNamesList.add(imageFiles[numImages].getAbsolutePath());

                Log.v(LOGTAG, imageFiles.length + " " + adapter_position + " " + last_image_position + " " + last_adapter_position + " " + newImageFiles[adapter_position].getAbsolutePath());
                numImages = numImages + 1;
                adapter_position = adapter_position + 1;

            }

        }

//
//        for (int i=0; i<newImageFiles.length; i++){
//            ImageNamesList.add(newImageFiles[i].getAbsolutePath());
//        }

        DragNDropGalleryAdapter dragNDropGalleryAdapter = new DragNDropGalleryAdapter(getActivity(), getActivity(), ImageNamesList, last_image_position, columnWidth);
        dragNdropGridView.setAdapter(dragNDropGalleryAdapter);

        CardView cardView = (CardView) getActivity().findViewById(R.id.card_interest_point);
        ImageView dropArea = (ImageView) cardView.findViewById(R.id.query_drop_area);
//        ImageView dropArea = (ImageView) getActivity().findViewById(R.id.query_drop_area);
        dropArea.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                final int action = event.getAction();

                // Handles each of the expected events
                switch (action) {
                    case DragEvent.ACTION_DROP:

                        ClipData.Item item = event.getClipData().getItemAt(0);

                        // Gets the text data from the item.
                        String dragData = item.getText().toString();

                        if (dragData.equals("Holla!")) {
                            Toast.makeText(getActivity(), "Dragged data is " + dragData, Toast.LENGTH_LONG).show();
                            Log.v(LOGTAG, "ACTION_DROP in TextView");

                            return true;
                        }


                }
                return false;
            }
        });


    }


    /*
 * getting screen width
 */
    public int getScreenWidth() {
        int columnWidth;
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        final Point point = new Point();
        try {
            display.getSize(point);
        } catch (java.lang.NoSuchMethodError ignore) { // Older device
            point.x = display.getWidth();
            point.y = display.getHeight();
        }
        columnWidth = point.x;
        return columnWidth;
    }


    public class MarginDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;


        public MarginDecoration(Context context, int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            //all the values here are pixels

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }


            Log.v(LOGTAG, "position= " + position + " top = " + outRect.top + " right = " + outRect.right + " bottom = " + outRect.bottom
                    + " left = " + outRect.left + " spanCount = " + spanCount + " spacing = " + spacing);

        }
    }


    public class PreLoadingGridLayoutManager extends GridLayoutManager {
        private int mPages = 1;
        private OrientationHelper mOrientationHelper;

        public PreLoadingGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        public PreLoadingGridLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        public PreLoadingGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
            super(context, spanCount, orientation, reverseLayout);
        }

        @Override
        public void setOrientation(final int orientation) {
            super.setOrientation(orientation);
            mOrientationHelper = null;
        }

        /**
         * Set the number of pages of layout that will be preloaded off-screen,
         * a page being a pixel measure equivalent to the on-screen size of the
         * recycler view.
         *
         * @param pages the number of pages; can be {@code 0} to disable preloading
         */
        public void setPages(final int pages) {
            this.mPages = pages;
        }

        @Override
        protected int getExtraLayoutSpace(final RecyclerView.State state) {
            if (mOrientationHelper == null) {
                mOrientationHelper = OrientationHelper.createOrientationHelper(this, getOrientation());
            }
            return mOrientationHelper.getTotalSpace() * mPages;
        }
    }


}
