package com.vinodapps.likethat.map2memories;

import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MapsActivity2 extends FragmentActivity implements OnMapReadyCallback {
    public GoogleMap mMap;
    public static MapsActivity2 mainActivityCtx2;
    List<Bitmap> bitmapList;
    final static int target_image_width = 150;
    final static int target_image_height = 150;
    public ViewPager pager;
    public static int count;
    HashMap<LatLng, HashSet<String>> tempimgDataList1 = new HashMap<>();
    HashSet<String> imgPaths = null;
    Thread thread;
    Handler handler;
    FrameLayout progressBarHolder;
    ProgressBar progressBar;
    Handler observerHandler;
    ImageObserver imageObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityCtx2 = this;
        setContentView(R.layout.activity_maps2);
        pager = (ViewPager) findViewById(R.id.myviewpager);
        ActivityCompat.requestPermissions(MapsActivity2.this,
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        this.bitmapList = new ArrayList<>();
        progressBarHolder = (FrameLayout) findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.VISIBLE);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(0);
        observerHandler = new Handler();
        imageObserver = new ImageObserver(observerHandler);
        this.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, imageObserver);

    }

    public static class ImageFragmentPagerAdapter extends FragmentPagerAdapter {
        public ImageFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return MapsActivity2.mainActivityCtx2.bitmapList.size();
        }

        @Override
        public Fragment getItem(int position) {
            //SwipeFragment fragment = new SwipeFragment();
            return SwipeFragment.newInstance(position);
        }
    }

    public static class SwipeFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View swipeView = inflater.inflate(R.layout.swipefragment, container, false);
            ImageView imageView = (ImageView) swipeView.findViewById(R.id.imageView);
            Bundle bundle = getArguments();
            int position = bundle.getInt("position");
            imageView.setImageBitmap(MapsActivity2.mainActivityCtx2.bitmapList.get(position));
            return swipeView;
        }

        static SwipeFragment newInstance(int position) {
            SwipeFragment swipeFragment = new SwipeFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            swipeFragment.setArguments(bundle);
            return swipeFragment;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    thread = new Thread(new MyThread());
                    thread.start();
                }
                else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MapsActivity2.this, "Permission denied to read your External storage", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ImgData imgData = (ImgData) msg.obj;
                if (mMap != null && imgData.getMarker() != null) {
                    mMap.addMarker(new MarkerOptions().position(new LatLng(imgData.getMarker().latitude, imgData.getMarker().longitude))).setTag(imgData);
                }
                int value = msg.arg1;
                if (value > 0) {
                    progressBar.setProgress(value);
                }
            }
        };
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                                          @Override
                                          public boolean onMarkerClick(final Marker marker) {
                                              if (marker.getPosition() != null) {
                                                  bitmapList.clear();
                                                  try {
                                                      ImgData imgData = (ImgData) marker.getTag();
                                                      HashSet<String> imgPaths = imgData.getImgPath();
                                                      for (String eachPath : imgPaths) {
                                                          Bitmap cropdImag = decodeSampleBitmapFromFile(eachPath);
                                                          bitmapList.add(cropdImag);
                                                      }
                                                  }
                                                  catch (Exception e) {
                                                  }
                                              }
                                              ImgsForCarousel();
                                              return true;
                                          }
                                      }
        );

    }

    public void ImgsForCarousel() {
        ImageFragmentPagerAdapter imageFragmentPagerAdapter = new ImageFragmentPagerAdapter(getSupportFragmentManager());
        pager.setVisibility(View.VISIBLE);
        pager.setAdapter(imageFragmentPagerAdapter);
    }


    private Bitmap decodeSampleBitmapFromFile(String imgName) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        try {

            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imgName, options);
            options.inSampleSize = calculateInSampleSize(options);
            options.inJustDecodeBounds = false;

        }
        catch (Exception e) {
            System.out.println("Error" + e.getMessage());
        }
        return BitmapFactory.decodeFile(imgName, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > target_image_height || width > target_image_width) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= target_image_height && (halfWidth / inSampleSize) >= target_image_width) {
                inSampleSize *= 2;
            }

        }
        return inSampleSize;
    }

    class MyThread implements Runnable {

        @Override
        public void run() {

            Uri uri;
            Cursor cursor;
            int column_index_data = 0;
            List<String> listofAllImages = new ArrayList<>();
            String absolutePathOfImage = null;
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
            cursor = mainActivityCtx2.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            }
            LatLng latLng = null;
            try {
                while (cursor.moveToNext()) {
                    absolutePathOfImage = cursor.getString(column_index_data);
                    listofAllImages.add(absolutePathOfImage);
                    try {
                        String name = absolutePathOfImage;
                        ExifInterface exif = new ExifInterface(name);
                        float[] latLong = new float[2];
                        boolean hasLatLong = exif.getLatLong(latLong);
                        if (hasLatLong) {
                            latLng = new LatLng(latLong[0], latLong[1]);
                            if (tempimgDataList1.containsKey(latLng)) {
                                if (tempimgDataList1.get(latLng).contains(absolutePathOfImage)) {
                                }
                                else {
                                    imgPaths.add(absolutePathOfImage);
                                }
                            }
                            else {
                                imgPaths = new HashSet<String>();
                                imgPaths.add(absolutePathOfImage);
                                tempimgDataList1.put(latLng, imgPaths);
                            }

                            if (latLng != null && imgPaths != null) {
                                ImgData imgData = new ImgData(0, latLng, imgPaths);
                                Message message = new Message();
                                message.obj = imgData;
                                message.arg1 = imgPaths.size();
                                handler.sendMessage(message);
                                Thread.sleep(1000);
                            }
                        }
                    }
                    catch (IOException e) {
                        System.out.println("Error" + e.getMessage());
                        if (e instanceof InterruptedIOException) {
                            System.out.println("Invalid image format: " + e.getMessage());
                        }
                        else {
                            System.out.println("Unable to load image: " + e.getMessage());
                        }

                    }
                    if (cursor.isLast()) {
                        progressBarHolder.setVisibility(View.INVISIBLE);
                    }
                }
            }
            catch (Exception e) {
                if (tempimgDataList1.size() == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MapsActivity2.this, "No memories, please click some photos with location enabled!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                System.out.println("Error" + e.getMessage());
            }
            finally {
                cursor.close();
            }
        }
    }

    class ImageObserver extends ContentObserver {
        public ImageObserver(Handler handler) {
            super(handler);
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }
        @Override
        public void onChange(boolean selfChange, Uri uri) {

            thread = new Thread(new MyThread());
            thread.start();
        }
    }
    @Override
    protected void onDestroy() {
        this.getContentResolver().unregisterContentObserver(imageObserver);
        super.onDestroy();
    }
}
