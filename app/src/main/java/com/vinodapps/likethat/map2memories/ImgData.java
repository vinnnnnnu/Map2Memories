package com.vinodapps.likethat.map2memories;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashSet;


public class ImgData {
    public ImgData(int id, LatLng marker, HashSet<String> imgPath) {
        this.id = id;
        this.marker = marker;
        this.imgPath = imgPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LatLng getMarker() {
        return marker;
    }
    public HashSet<String> getImgPath() {
        return imgPath;
    }
    int id;
    LatLng marker;
    HashSet<String> imgPath;

    //this is from tryout branch
}









