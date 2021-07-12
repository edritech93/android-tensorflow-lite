package com.weefer.tensorflowlite;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.weefer.tensorflowlite.tflite.SimilarityClassifier;
import com.weefer.tensorflowlite.tflite.TFLiteObjectDetectionAPIModel;

import java.io.File;
import java.io.IOException;

import static com.weefer.tensorflowlite.DetectorActivity.detector;
import static com.weefer.tensorflowlite.DetectorActivity.resultsUser;

public class AddImage {
    private final int TF_OD_API_INPUT_SIZE = 112;
    private Bitmap faceStorage = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);
    private boolean isLoadImageStorage = true;

    public Bitmap getFaceStorage() {
        return faceStorage;
    }

    public boolean isLoadImageStorage() {
        return isLoadImageStorage;
    }

    public void addImageStorage(Activity activity) {
        try {
            File imgFile = new File("/sdcard/Download/image_test.jpg");
            if (imgFile.exists()) {
                isLoadImageStorage = false;
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
//                                    resultsUser = detector.recognizeImage(crop, true);
//                                    detector.register("Yudi", resultsUser.get(0));
                                    ImageView imageView = activity.findViewById(R.id.img_storage);
                                    imageView.setImageBitmap(crop);
                                    faceStorage = crop;
                                    imageDetector.close();

//                                    String label = "";
//                                    float confidence = -1f;
//                                    Integer color = Color.BLUE;
//                                    Object extra = null;
//                                    resultsUser = detector.recognizeImage(crop, true);
//                                    if (resultsUser.size() > 0) {
//                                        SimilarityClassifier.Recognition result = resultsUser.get(0);
//                                        extra = result.getExtra();
//                                        float conf = result.getDistance();
//                                        Log.e("conf-0", String.valueOf(conf));
//                                        if (conf < 1.0f) {
//                                            confidence = conf;
//                                            label = result.getTitle();
//                                            if (result.getId().equals("0")) {
//                                                color = Color.GREEN;
//                                            } else {
//                                                color = Color.RED;
//                                            }
//                                        }
//                                    }
//                                    final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
//                                            "0", label, confidence, boundingBox);
//                                    result.setColor(color);
//                                    result.setLocation(boundingBox);
//                                    result.setExtra(extra);
//                                    result.setCrop(crop);
//
//                                    detector.register("Yudi", result);

//                                    resultsUser = detector.recognizeImageStorage(crop, true);
//
//                                    final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
//                                            "0", "User Haermes", 0.5f, boundingBox);
//                                    SimilarityClassifier.Recognition resultAux = resultsUser.get(0);
//                                    result.setColor(Color.GREEN);
//                                    result.setLocation(boundingBox);
//                                    result.setExtra(resultAux.getExtra());
//                                    result.setCrop(crop);
//                                    detector.register("Yudi", result);
                                }
                            }
                        });
            }
        } catch (Exception e) {
            Log.e("imageDetector", e.toString());
        }
    }
}
