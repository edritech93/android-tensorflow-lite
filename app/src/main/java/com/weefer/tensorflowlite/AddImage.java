package com.weefer.tensorflowlite;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;

import static com.weefer.tensorflowlite.DetectorActivity.detector;
import static com.weefer.tensorflowlite.DetectorActivity.resultsUser;

public class AddImage {
    private boolean isLoadImageStorage = true;

    public boolean isLoadImageStorage() {
        return isLoadImageStorage;
    }

    public void addImageStorage(Activity activity) {
        try {
            File imgFile = new File("/sdcard/Download/image_test.jpg");
            if (imgFile.exists()) {
                FaceDetectorOptions faceDetectorOptions =
                        new FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                                .build();
                FaceDetector imageDetector = FaceDetection.getClient(faceDetectorOptions);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bInput = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                float degrees = 270;
                Matrix matrix = new Matrix();
                matrix.setRotate(degrees);
                int TF_OD_API_INPUT_SIZE = 112;
                bInput = Bitmap.createScaledBitmap(bInput, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, false);
                Bitmap bOutput = Bitmap.createBitmap(bInput, 0, 0, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, matrix, false);

                InputImage image = InputImage.fromBitmap(bOutput, 0);
                imageDetector
                        .process(image)
                        .addOnSuccessListener(faces -> {
                            Log.e("faces.size()", String.valueOf(faces.size()));
                            if (faces.size() > 0) {
                                Face face = faces.get(0);
                                final RectF boundingBox = new RectF(face.getBoundingBox());
                                if (boundingBox != null) {
                                    RectF faceBB = new RectF(boundingBox);
                                    Bitmap crop = Bitmap.createBitmap(bOutput,
                                            (int) faceBB.left,
                                            (int) faceBB.top,
                                            (int) faceBB.width(),
                                            (int) faceBB.height());
                                    resultsUser = detector.recognizeImage(crop, true);
                                    detector.register("Yudi", resultsUser.get(0));
                                    ImageView imageView = activity.findViewById(R.id.img_storage);
                                    imageView.setImageBitmap(crop);
                                    isLoadImageStorage = false;
                                }
                            }
                        });
            }
        } catch (Exception e) {
            Log.e("imageDetector", e.toString());
        }
    }
}
