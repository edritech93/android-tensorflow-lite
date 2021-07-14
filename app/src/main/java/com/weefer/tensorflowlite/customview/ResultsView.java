package com.weefer.tensorflowlite.customview;

import com.weefer.tensorflowlite.recognition.ModelFace;

import java.util.List;

public interface ResultsView {
    public void setResults(final List<ModelFace> results);
}
