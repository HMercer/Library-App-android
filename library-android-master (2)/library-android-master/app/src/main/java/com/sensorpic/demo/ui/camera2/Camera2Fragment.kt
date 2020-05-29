package com.sensorpic.demo.ui.camera2

/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * With alterations and additions.
 */

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.nfc.FormatException
import android.os.*
import android.renderscript.RenderScript
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sensorpic.demo.databinding.Camera2FragmentBinding
import com.sensorpic.demo.ui.CameraCoverMode
import com.sensorpic.demo.ui.settings.Settings
import com.sensorpic.library.ImageClassifier
import com.sensorpic.library.DetectionListener
import com.sensorpic.library.Recognition
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Sample demonstrating use of the image detection algorithm using Google Android Camera2 API
 */
class Camera2Fragment : Fragment() {

    companion object {

        /** Maximum number of images that will be held in the reader's buffer */
        private const val IMAGE_BUFFER_SIZE: Int = 2

    }

    private val args: Camera2FragmentArgs by navArgs()

    private lateinit var binding: Camera2FragmentBinding

    private lateinit var imageClassifier: ImageClassifier

    private val detectionListener = object : DetectionListener {

        override fun onObjectDetected(recognition: Recognition?, isPortrait: Boolean) {
            onObjectDetected(recognition)
        }

        override fun onObjectNotDetected() { onNoObjectDetected() }

    }

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(args.cameraId)
    }

    /** Readers used as buffers for camera still shots */
    private lateinit var imageReader: ImageReader

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    /** [HandlerThread] where all buffer reading operations run */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /** [Handler] corresponding to [imageReaderThread] */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var camera: CameraDevice

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    private lateinit var session: CameraCaptureSession

    /** Live data listener for changes in the device orientation relative to the camera */
    private lateinit var relativeOrientation: OrientationLiveData

    private lateinit var renderScript: RenderScript

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = Camera2FragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageClassifier = ImageClassifier(requireActivity(), detectionListener)
        renderScript = RenderScript.create(context)
        binding.cameraCover.clearCover()
        attachViewFinderSurfaceListener(view)
        // Used to rotate the output media to match device orientation
        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner, Observer { orientation ->
                Timber.d("Orientation changed: $orientation")
            })
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            camera.close()
        } catch (exc: Throwable) {
            Timber.e(exc, "Error closing camera")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
        imageReaderThread.quitSafely()
    }

    /// Private methods

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating capture request
     * - Sets up the still image capture listeners
     */
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        // Open the selected camera
        camera = openCamera(cameraManager, args.cameraId, cameraHandler)

        val pixelFormat = args.pixelFormat

        // Initialize an image reader which will be used to capture still photos
        val size = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                .getOutputSizes(pixelFormat).maxBy { it.height * it.width }!!

        checkForLegacyCamera()

        imageReader = ImageReader.newInstance(size.width, size.height, pixelFormat, IMAGE_BUFFER_SIZE)

//        if (latestBitmap == null) {
//            createNewBitmap(size.width, size.height)
//            imageProcessor = ImageProcessor(requireContext(), latestBitmap!!)
//        }
//        if (latestBitmap == null) { createNewBitmap(size.width, size.height) }

        // Creates list of Surfaces where the camera will output frames
        val targets = listOf(binding.viewFinder.holder.surface, imageReader.surface)

        // Start a capture session using our open camera and list of Surfaces where frames will go
        session = createCaptureSession(camera, targets, cameraHandler)

        val captureRequest = camera.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(binding.viewFinder.holder.surface)
            addTarget(imageReader.surface)
        }

        // This will keep sending the capture request as frequently as possible until the
        // session is torn down or session.stopRepeating() is called
        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

        imageReader.setOnImageAvailableListener({ reader ->
            val image: Image? = reader.acquireNextImage()
            if (image != null) {
                if (image.format != ImageFormat.YUV_420_888) throw FormatException("Incompatible format. Must be YUV 420 888.")
                imageClassifier.processFrameImageIfNotAlreadyProcessing(image)
            }
        }, imageReaderHandler)
    }

    private fun checkForLegacyCamera() {
        if (!isHardwareLevelSupported(characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)) {
            Toast.makeText(requireContext(), "Legacy camera hardware not supported", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun isHardwareLevelSupported(c: CameraCharacteristics, requiredLevel: Int): Boolean {
        val sortedHwLevels = intArrayOf(
                 CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
        val deviceLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
        if (requiredLevel == deviceLevel) {
            return true
        }
        for (sortedLevel in sortedHwLevels) {
            if (sortedLevel == requiredLevel) {
                return true
            } else if (sortedLevel == deviceLevel) {
                return false
            }
        }
        return false // Should never reach here
    }
    private fun attachViewFinderSurfaceListener(view: View) {
        binding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {

                // Selects appropriate preview size and configures view finder
                val previewSize = getPreviewOutputSize(
                        binding.viewFinder.display, characteristics, SurfaceHolder::class.java)
                Timber.d("View finder size: ${binding.viewFinder.width} x ${binding.viewFinder.height}")
                Timber.d("Selected preview size: $previewSize")
                binding.viewFinder.holder.setFixedSize(previewSize.width, previewSize.height)
                binding.viewFinder.setAspectRatio(previewSize.width, previewSize.height)
                // To ensure that size is set, initialize camera in the view's thread
                view.post { initializeCamera() }
            }
        })
    }

    private fun onNoObjectDetected() {
        Timber.i("Detected: No")
        binding.cameraCover.clearCover()
        binding.cameraCover.showNotBlockedSummary()
    }

    private fun onObjectDetected(recognition: Recognition?) {
        val percentage = recognition?.confidence ?: 0f
        val percentage100 = (percentage * 100).toInt()
        val title = recognition?.title ?: ""
        if (percentage >= Settings.getThreshold()) {
            Timber.e("Detected: $title $percentage100")
            binding.cameraCover.showBlockedSummary(args.coverMode == CameraCoverMode.Whole.id, title, percentage100)
        } else {
            Timber.i("Detected: $title $percentage100  (low confidence)")
            binding.cameraCover.showAlmostBlockedSummary(title, percentage100)
        }
    }

    /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
            manager: CameraManager,
            cameraId: String,
            handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                Timber.w("Camera $cameraId has been disconnected")
                requireActivity().finish()
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Timber.e(exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    private suspend fun createCaptureSession(
            device: CameraDevice,
            targets: List<Surface>,
            handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) {
                cont.resume(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Timber.e(exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

}
