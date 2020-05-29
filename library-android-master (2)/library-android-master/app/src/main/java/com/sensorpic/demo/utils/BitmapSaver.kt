package com.sensorpic.demo.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat

/**
 * Writes a Bitmap object to the device storage / photo gallery
 */
object BitmapSaver {

    // TODO Extend to work on both Android 10+ AND prior Android versions

    @Throws(IOException::class)
    fun saveToGallery(context: Context, bitmap: Bitmap,
                   format: CompressFormat, mimeType: String,
                   displayName: String) {
        val relativeLocation = Environment.DIRECTORY_PICTURES
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
        val resolver = context.contentResolver
        var stream: OutputStream? = null
        var uri: Uri? = null
        try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = resolver.insert(contentUri, contentValues)
            if (uri == null) {
                throw IOException("Failed to create new MediaStore record.")
            }
            stream = resolver.openOutputStream(uri)
            if (stream == null) {
                throw IOException("Failed to get output stream.")
            }
            if (!bitmap.compress(format, 95, stream)) {
                throw IOException("Failed to save bitmap.")
            }
        } catch (e: IOException) {
            if (uri != null) { // Don't leave an orphan entry in the MediaStore
                resolver.delete(uri, null, null)
            }
            throw e
        } finally {
            stream?.close()
        }
    }

    @Throws(IOException::class)
    fun createImageFile(): File { // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(System.currentTimeMillis())
        val storageDir = File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile(
                timeStamp,  /* prefix */
                ".jpeg",  /* suffix */
                storageDir /* directory */
        )
    }

    fun saveToGallery(context: Context, photoPath: String) {
        val galleryIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val file = File(photoPath)
        val pictureUri = Uri.fromFile(file)
        galleryIntent.data = pictureUri
        context.sendBroadcast(galleryIntent)
    }

}
