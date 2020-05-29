package com.sensorpic.demo.ui.cameraview

import android.graphics.*
import com.otaliastudios.cameraview.frame.Frame
import java.io.ByteArrayOutputStream

fun Frame.frameToBitmap(): Bitmap {
    val out = ByteArrayOutputStream()
    val yuvImage = YuvImage(
            data,
            ImageFormat.NV21,
            size.width,
            size.height,
            null
    )
    val rectangle = Rect(0, 0, size.width, size.height)
    yuvImage.compressToJpeg(rectangle, 90, out)
    val imageBytes = out.toByteArray()
    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    if (rotation != 0) {
        val matrix = Matrix()
        matrix.setRotate(rotation.toFloat())
        val temp = bitmap
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        temp.recycle()
    }
    return bitmap
}
