package com.vinodapps.likethat.map2memories;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;


import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class BitmapWorker extends AsyncTask<Void, ImgData, String> {
    private Context context;
    final static int target_image_width = 150;
    final static int target_image_height = 150;

    HashMap<LatLng, HashSet<String>> tempimgDataList1 = new HashMap<>();
    HashSet<String> imgPaths = null;


//    Hashtable<Integer, Marker> visibleMarkers = new Hashtable<Integer, Marker>();


    /*public BitmapWorker(String imgNam, double lat, double lan) {
        this.imgName = imgNam;
        this.lat = lat;
        this.lan = lan;
    }*/

    public BitmapWorker(Context c) {
        context = c;


    }

    @Override
    protected String doInBackground(Void... params) {


        Uri uri;
        Cursor cursor;
        int column_index_data = 0;
        int column_index_folder_name;
        List<String> listofAllImages = new ArrayList<>();
        String absolutePathOfImage = null;
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        final int maxMemorySize = (int) Runtime.getRuntime().maxMemory() / 1024;
        final int cacheSize = maxMemorySize / 10;

        /*mImgsCache = new LruCache<String, Bitmap>(maxMemorySize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };*/


        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        }

        //column_index_folder_name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
        Bitmap cropdImag = null;
        LatLng latLng = null;
        try {

            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);
                listofAllImages.add(absolutePathOfImage);
                for (int i = 0; i < listofAllImages.size(); i++) {
                    try {
                        String name = listofAllImages.get(i);
                        ExifInterface exif = new ExifInterface(name);
                        float[] latLong = new float[2];
                        boolean hasLatLong = exif.getLatLong(latLong);
                        if (hasLatLong) {
                            //cropdImag = decodeSampleBitmapFromFile(name);
                            latLng = new LatLng(latLong[0], latLong[1]);
                            if (tempimgDataList1.containsKey(latLng)) {
                                if (tempimgDataList1.containsValue(absolutePathOfImage)) {

                                } else {
                                    imgPaths.add(absolutePathOfImage);
                                }
                            } else {

                                imgPaths = new HashSet<String>();
                                imgPaths.add(absolutePathOfImage);
                                tempimgDataList1.put(latLng, imgPaths);
                            }
                        }
                        else
                        {

                        }
                    } catch (IOException e) {


                        System.out.println("Error" + e.getMessage());
                        if (e instanceof InterruptedIOException) {
                            System.out.println("Invalid image format: " + e.getMessage());
                        } else {
                            System.out.println("Unable to load image: " + e.getMessage());
                        }
                        return null;
                    }

                }
                if (latLng!=null && imgPaths!=null) {
                    ImgData imgData = new ImgData(0, latLng, imgPaths);
                    publishProgress(imgData);
                }
            }
        } catch (Exception e) {
            System.out.println("Error" + e.getMessage());
            return null;


        } finally {
            cursor.close();
        }


        return "";
    }


    @Override
    protected void onProgressUpdate(ImgData... values) {
        super.onProgressUpdate(values);
        ImgData imgData = (ImgData) values[0];
        /*if (MapsActivity2.mainActivityCtx.mMap != null && imgData.getMarker() != null) {
            MapsActivity2.mainActivityCtx.mMap.addMarker(new MarkerOptions().position(new LatLng(imgData.getMarker().latitude, imgData.getMarker().longitude))).setTag(imgData);

        }*/

    }

    private Bitmap decodeSampleBitmapFromFile(String imgName) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        try {

            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imgName, options);
            options.inSampleSize = calculateInSampleSize(options);
            options.inJustDecodeBounds = false;

        } catch (Exception e) {
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
}



