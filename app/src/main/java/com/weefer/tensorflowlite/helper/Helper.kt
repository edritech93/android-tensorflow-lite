package com.weefer.tensorflowlite.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Environment
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class Helper() {

    // Input image size for FaceNet model.
    private val imgSize = 112

    // Output embedding size
    private val embeddingDim = 128

    // Image Processor for preprocessing input images.
    private val imageTensorProcessor = ImageProcessor.Builder()
        .add(ResizeOp(imgSize, imgSize, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(127.5f, 127.5f))
        .build()

    private fun loadImage() {
        val imgFile =
            File(Environment.getExternalStorageDirectory().absolutePath + "/images/user.png")
        if (imgFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            convertBitmapToBuffer(bitmap);
//            interpreter.runForMultipleInputsOutputs()
        }
    }

    // Resize the given bitmap and convert it to a ByteBuffer
    public fun convertBitmapToBuffer(image: Bitmap): ByteBuffer {
        val imageTensor = imageTensorProcessor.process(TensorImage.fromBitmap(image))
        return imageTensor.buffer
    }

    // Crop the given bitmap with the given rect.
    private fun cropRectFromBitmap(
        source: Bitmap,
        rect: Rect,
        preRotate: Boolean,
        isRearCameraOn: Boolean
    ): Bitmap {
        var width = rect.width()
        var height = rect.height()
        if ((rect.left + width) > source.width) {
            width = source.width - rect.left
        }
        if ((rect.top + height) > source.height) {
            height = source.height - rect.top
        }
        var croppedBitmap = Bitmap.createBitmap(
            if (preRotate) rotateBitmap(source, -90f)!! else source,
            rect.left,
            rect.top,
            width,
            height
        )

        // Add a 180 degrees rotation if the rear camera is on.
        if (isRearCameraOn) {
            croppedBitmap = rotateBitmap(croppedBitmap, 180f)
        }

        // Uncomment the below line if you want to save the input image.
        // Make sure the app has the `WRITE_EXTERNAL_STORAGE` permission.
        //saveBitmap( croppedBitmap , "image")

        return croppedBitmap
    }

    private fun saveBitmap(image: Bitmap, name: String) {
        val fileOutputStream =
            FileOutputStream(File(Environment.getExternalStorageDirectory()!!.absolutePath + "/$name.png"))
        image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, false)
    }
}