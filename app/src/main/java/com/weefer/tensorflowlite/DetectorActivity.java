package com.weefer.tensorflowlite;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.weefer.tensorflowlite.customview.OverlayView;
import com.weefer.tensorflowlite.env.BorderedText;
import com.weefer.tensorflowlite.env.ImageUtils;
import com.weefer.tensorflowlite.tflite.SimilarityClassifier;
import com.weefer.tensorflowlite.tflite.TFLiteObjectDetectionAPIModel;
import com.weefer.tensorflowlite.tracking.MultiBoxTracker;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class DetectorActivity extends CameraActivity {
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private SimilarityClassifier detector;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private boolean computingDetection = false;
    private boolean addPending = false;

    private long timestamp = 0;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;
    private FaceDetector faceDetector;
    private Bitmap portraitBmp = null;
    private Bitmap faceBmp = null;
    private boolean isLoadImageStorage = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();
        faceDetector = FaceDetection.getClient(options);
    }

    private void addImageStorage() {
        try {
            File imgFile = new File("/sdcard/Download/image_test.jpg");
            if (imgFile.exists() && isLoadImageStorage) {
                FaceDetectorOptions options =
                        new FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                                .build();
                FaceDetector imageDetector = FaceDetection.getClient(options);
                Bitmap bInput = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                float degrees = 270;
                Matrix matrix = new Matrix();
                matrix.setRotate(degrees);
                bInput = Bitmap.createScaledBitmap(bInput, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, true);
                Bitmap bOutput = Bitmap.createBitmap(bInput, 0, 0, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, matrix, true);

                InputImage image = InputImage.fromBitmap(bOutput, 0);
                imageDetector
                        .process(image)
                        .addOnSuccessListener(faces -> {
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

                                    ImageView imgStorage = findViewById(R.id.img_storage);
                                    imgStorage.setImageBitmap(crop);
//                                        final long startTime = SystemClock.uptimeMillis();
//                                        resultsAux = detector.recognizeImage(crop, true);
//                                        long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
//                                        final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
//                                                "0", "Yudi", 0.0f, boundingBox);
//                                        result.setColor(Color.YELLOW);
//                                        result.setLocation(boundingBox);
//                                        result.setExtra(null);
//                                        result.setCrop(crop);
//
//                                        tracker.trackResults(resultsAux, timestamp);
//                                        trackingOverlay.postInvalidate();
//                                        computingDetection = false;
//                                        //adding = false;
//                                        detector.register("User", resultsAux.get(0));
                                    isLoadImageStorage = false;
                                }
                            }
                        });
            }
        } catch (Exception e) {
            Log.e("imageDetector", e.toString());
        }
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        BorderedText borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        } else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888);
        portraitBmp = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);

        addImageStorage();

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(canvas -> {
            tracker.draw(canvas);
            if (isDebug()) {
                tracker.drawDebug(canvas);
            }
        });
        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
        faceDetector
                .process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 0) {
                        updateResults(currTimestamp, new LinkedList<>());
                        return;
                    }
                    runInBackground(() -> {
                        if (!isLoadImageStorage) {
                            onFacesDetected(currTimestamp, faces, addPending);
                            addPending = false;
                        }
                    });
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);
            matrix.postRotate(applyRotation);
        }
        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }
        return matrix;
    }

    private void updateResults(long currTimestamp, final List<SimilarityClassifier.Recognition> mappedRecognitions) {
        tracker.trackResults(mappedRecognitions, currTimestamp);
        trackingOverlay.postInvalidate();
        computingDetection = false;
        if (mappedRecognitions.size() > 0) {
            SimilarityClassifier.Recognition rec = mappedRecognitions.get(0);
            if (rec.getExtra() != null) {
                detector.register("User", rec);
            }
        }
    }

    private void onFacesDetected(long currTimestamp, List<Face> faces, boolean add) {
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<SimilarityClassifier.Recognition> mappedRecognitions =
                new LinkedList<SimilarityClassifier.Recognition>();

        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);
        cv.drawBitmap(rgbFrameBitmap, transform, null);
        final Canvas cvFace = new Canvas(faceBmp);

        for (Face face : faces) {
            final RectF boundingBox = new RectF(face.getBoundingBox());
            if (boundingBox != null) {
                cropToFrameTransform.mapRect(boundingBox);
                RectF faceBB = new RectF(boundingBox);
                transform.mapRect(faceBB);
                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);
                cvFace.drawBitmap(portraitBmp, matrix, null);

                String label = "";
                float confidence = -1f;
                Integer color = Color.BLUE;
                Object extra = null;
                Bitmap crop = null;
                if (add) {
                    crop = Bitmap.createBitmap(portraitBmp,
                            (int) faceBB.left,
                            (int) faceBB.top,
                            (int) faceBB.width(),
                            (int) faceBB.height());
                }
                final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, add);
                if (resultsAux.size() > 0) {
                    SimilarityClassifier.Recognition result = resultsAux.get(0);
                    extra = result.getExtra();
                    float conf = result.getDistance();
                    Log.e("conf-1", String.valueOf(conf));
                    if (conf < 1.0f) {
                        confidence = conf;
                        label = result.getTitle();
                        if (result.getId().equals("0")) {
                            color = Color.GREEN;
                        } else {
                            color = Color.RED;
                        }
                    }
                }
                if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    flip.mapRect(boundingBox);
                }

                final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                        "0", label, confidence, boundingBox);
                result.setColor(color);
                result.setLocation(boundingBox);
                result.setExtra(extra);
                result.setCrop(crop);
                mappedRecognitions.add(result);
            }
        }
        updateResults(currTimestamp, mappedRecognitions);
    }
}
