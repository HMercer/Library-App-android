package com.sensorpic.demo.ui.cameraview

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.sensorpic.library.ImageClassifier
import com.sensorpic.library.DetectionListener
import com.sensorpic.library.Recognition
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Audio
import com.otaliastudios.cameraview.filter.Filters
import com.otaliastudios.cameraview.frame.Frame
import com.sensorpic.demo.databinding.CameraViewDemoFragmentBinding
import com.sensorpic.demo.ui.CameraCoverMode
import com.sensorpic.demo.ui.settings.Settings
import com.sensorpic.demo.utils.BitmapSaver
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Sample demonstrating use of the image detection algorithm using the CameraView library
 */
class CameraViewDemoFragment : Fragment() {

    private lateinit var binding: CameraViewDemoFragmentBinding
    private lateinit var imageClassifier: ImageClassifier
    private val args: CameraViewDemoFragmentArgs by navArgs()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = CameraViewDemoFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpCamera()
        setCameraEventListener()
        attachListeners()
        attachImageClassifier()
    }

    override fun onPause() {
        super.onPause()
        binding.camera.close()
    }

    override fun onResume() {
        super.onResume()
        if (!binding.camera.isOpened) { binding.camera.open() }
    }


    /// Private methods

    private fun setUpCamera() {
        binding.camera.audio = Audio.OFF
        binding.camera.setLifecycleOwner(requireActivity())
        binding.camera.addFrameProcessor { frame -> processFrame(frame) }
    }

    private fun processFrame(frame: Frame) {
        imageClassifier.classifyBitmap(frame.frameToBitmap())
//        BitmapSaver.saveToGallery(requireContext(), toClassify, Bitmap.CompressFormat.PNG, "image/png", "Image ${System.currentTimeMillis()}")
    }

    private fun attachListeners() {
        binding.btnCapture.setOnClickListener { binding.camera.takePictureSnapshot() }
    }

    private fun setCameraEventListener() {
        binding.camera.addCameraListener(object : CameraListener() {

            override fun onPictureTaken(result: PictureResult) {
                result.toBitmap { bitmap: Bitmap? ->
                    // to show last captured image on UI
                    binding.imageRecent.setImageBitmap(bitmap)
                }
                try {
                    result.toFile(BitmapSaver.createImageFile()) { file: File? ->
                        BitmapSaver.saveToGallery(requireContext(), file!!.path)
                    }
                } catch (e: IOException) {
                    Timber.d(e, "Failed to save the image in file")
                }
            }

        })
    }

    private fun attachImageClassifier() {
        imageClassifier = ImageClassifier(requireContext(), object : DetectionListener {

            override fun onObjectDetected(recognition: Recognition?, isPortrait: Boolean) {
                onObjectDetected(recognition)
            }

            override fun onObjectNotDetected() { onNoObjectDetected() }

        })
    }

    private fun onNoObjectDetected() {
        binding.camera.filter = Filters.NONE.newInstance()
        binding.cameraBlocker.clearCover()
        binding.cameraBlocker.showNotBlockedSummary()
    }

    private fun onObjectDetected(recognition: Recognition?) {
        val percentage = recognition?.confidence ?: 0f
        val percentage100 = (percentage * 100).toInt()
        val title = recognition?.title ?: ""
        if (percentage >= Settings.getThreshold()) {
            Timber.e("Detected: $title $percentage100")
            binding.cameraBlocker.showBlockedSummary(args.coverMode == CameraCoverMode.Whole.id, title, percentage100)
        } else {
            Timber.i("Detected: $title $percentage100  (low confidence)")
            binding.cameraBlocker.clearCover()
            binding.cameraBlocker.showAlmostBlockedSummary(title, percentage100)
        }
    }

}
