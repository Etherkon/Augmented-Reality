package com.ar.augmentedreality;

import android.hardware.Camera;
import android.util.Log;

public class FaceDetector implements Camera.FaceDetectionListener {

    MainActivity main;

    public FaceDetector(MainActivity main) { this.main = main; }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if (faces.length > 0){
            Log.d("FaceDetection", "face detected: " + faces.length + " Face 1 Location X: " + faces[0].rect.centerX() +
                    "Y: " + faces[0].rect.centerY());
            main.text("Kasvot tunnistettu !");
        }
    }

}
