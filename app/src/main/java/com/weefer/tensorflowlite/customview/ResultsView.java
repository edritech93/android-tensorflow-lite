package com.weefer.tensorflowlite.customview;

import com.weefer.tensorflowlite.tflite.SimilarityClassifier;

import java.util.List;

public interface ResultsView {
    public void setResults(final List<SimilarityClassifier.Recognition> results);
}
