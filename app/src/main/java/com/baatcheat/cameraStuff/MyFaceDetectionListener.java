package com.baatcheat.cameraStuff;

import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

public class MyFaceDetectionListener implements Camera.FaceDetectionListener {
public static Rect visibleRect=new Rect(0,0,0,0);
    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if (faces.length > 0){
            Log.d("FaceDetection", "face detected: "+ faces.length +
                    " Face 1 Location X: " + faces[0].rect.centerX() +
                    "Y: " + faces[0].rect.centerY()+"" +
                    "rect"+faces[0].rect );
            visibleRect=faces[0].rect;


        //drawing a bounding box::

// set Camera parameters


        }
        else{
            visibleRect=new Rect(0,0,0,0);
        }
    }
}