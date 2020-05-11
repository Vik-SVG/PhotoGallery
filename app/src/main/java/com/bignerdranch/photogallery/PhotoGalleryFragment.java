package com.bignerdranch.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment{

    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private TextView          mCurrentPageText;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int pageFetched = 0;
    private GridLayoutManager mGridManager;
    boolean asyncFetching   = false;
    int     mCurrentPage    = 1;
    int     mMaxPage        = 1;
    int     mItemsPerPage   = 1;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

   /* private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>{

        @Override
        protected List<GalleryItem> doInBackground(Void... params){
            return new FlickrFetch().fetchItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            mItems = items;
            setupAdapter();
        }
    }*/


    public static Fragment newInstance() {
    return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();


        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);

                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mCurrentPageText = (TextView) v.findViewById(R.id.currentPageText);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              //  Context context;
                float columnWidthInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 140, getActivity().getResources().getDisplayMetrics());
                int width = mPhotoRecyclerView.getWidth();
                int columnNumber = Math.round(width / columnWidthInPixels);
                mGridManager = new GridLayoutManager(getActivity(), 3);
                mPhotoRecyclerView.setLayoutManager(mGridManager);
                mPhotoRecyclerView.scrollToPosition(mCurrentPage);
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int lastVisibleItem = mGridManager.findLastVisibleItemPosition();
                int firstVisibleItem = mGridManager.findFirstVisibleItemPosition();
                Log.d(TAG, "Scrolling, First Item: "+ firstVisibleItem + " Last item: " + lastVisibleItem);

                if(!(asyncFetching) && (dy>0)&&(mCurrentPage<mMaxPage)&& (lastVisibleItem>=(mItems.size()-1))){
                    Log.d(TAG, "Fetching more");
                    new FetchItemsTask().execute();
                } else updatePageText(firstVisibleItem);

                //super.onScrolled(recyclerView, dx, dy);
            }
        });

        if(mPhotoRecyclerView.getAdapter() == null)setupAdapter();
      //  mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));//setupAdapter();
        return v;
    }
    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    private  void updatePageText(int pos){
        mCurrentPage = pos / mItemsPerPage+1;
        mCurrentPageText.setText("Page " + mCurrentPage + " of " + mMaxPage);
    }

    private void setupAdapter(){
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        private  ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable (Drawable drawable){
        mItemImageView.setImageDrawable(drawable); }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }


        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            Context context;
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.ic_launcher_background);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>{
        @Override
        protected List<GalleryItem> doInBackground(Void...params){
           asyncFetching = true;
           return new FlickrFetch().fetchItems(pageFetched+1);
        }

        @Override
        protected void onPostExecute(List<GalleryItem>items){
         pageFetched++;
         asyncFetching=false;
         mItems.addAll(items);
         GalleryPage pge = GalleryPage.getGalleryPage();
         mMaxPage = pge.getTotalPages();
         mItemsPerPage = pge.getItemPerPage();

         if(mPhotoRecyclerView.getAdapter()==null)setupAdapter();
         mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            updatePageText(mGridManager.findFirstVisibleItemPosition());
        }
    }

}

