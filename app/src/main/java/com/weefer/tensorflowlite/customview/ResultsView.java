package com.weefer.tensorflowlite.customview;

import com.weefer.tensorflowlite.model.Recognition;

import java.util.List;

public interface ResultsView {
    public void setResults(final List<Recognition> results);
}
