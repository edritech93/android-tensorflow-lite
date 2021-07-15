package com.weefer.tensorflowlite.capture;

public class ObservableBoolean {
    private OnBooleanChangeListener listener;
    private boolean value;

    public void setOnIntegerChangeListener(OnBooleanChangeListener listener) {
        this.listener = listener;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean value) {
        this.value = value;
        if (listener != null) {
            listener.onBooleanChanged(value);
        }
    }
}
