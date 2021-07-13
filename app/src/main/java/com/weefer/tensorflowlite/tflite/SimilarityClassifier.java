package com.weefer.tensorflowlite.tflite;

import android.graphics.Bitmap;

import com.weefer.tensorflowlite.model.Recognition;

import java.util.List;

public interface SimilarityClassifier {

    void register(String name, Recognition recognition);

    List<Recognition> recognizeImage(Bitmap bitmap, boolean getExtra);

    void enableStatLogging(final boolean debug);

    String getStatString();

    void close();
}
