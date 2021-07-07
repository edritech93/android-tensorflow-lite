package com.weefer.tensorflowlite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public class AddPerson {
    public Bitmap add()   {
        File imgFile = new File("/sdcard/Download/image_test.jpg");

        if(imgFile.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            return bitmap;
        } else {
            return null;
        }
    }
}
