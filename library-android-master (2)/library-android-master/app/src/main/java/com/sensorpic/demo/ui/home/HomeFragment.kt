package com.sensorpic.demo.ui.home

import android.Manifest
import android.graphics.ImageFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sensorpic.demo.databinding.HomeFragmentBinding
import com.sensorpic.demo.extensions.getVersionName
import com.sensorpic.demo.ui.CameraCoverMode

class HomeFragment : Fragment() {

    private lateinit var binding: HomeFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = HomeFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        attachListeners()
        displayVersion()
    }

    override fun onResume() {
        super.onResume()
        getPermissions()
    }

    /// Private methods

    private fun getPermissions() {
        Dexter.withActivity(requireActivity())
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .withListener(object : MultiplePermissionsListener {

                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        onRuntimePermissionsChanged(report.areAllPermissionsGranted())
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }

                }).check()
    }

    private fun onRuntimePermissionsChanged(permissionsGranted: Boolean) {
        with(binding) {
            camera2FullScreenBlockingDemoButton.isEnabled = permissionsGranted
            camera2NoBlockingDemoButton.isEnabled = permissionsGranted
            cameraViewDemoFullScreenBlockButton.isEnabled = permissionsGranted
            cameraViewDemoNoBlockButton.isEnabled = permissionsGranted
            permissionsButton.visibility = if (permissionsGranted) View.GONE else View.VISIBLE
        }
    }

    private fun displayVersion() {
        val versionSummary = "v${requireActivity().getVersionName()}"
        binding.version.text = versionSummary
    }

    private fun attachListeners() {
        with (binding) {
            cameraViewDemoNoBlockButton.setOnClickListener { openCameraViewDemoNoBlocking() }
            cameraViewDemoFullScreenBlockButton.setOnClickListener { openCameraViewDemoFullBlocking() }
            camera2NoBlockingDemoButton.setOnClickListener { openCamera2DemoNoBlocking() }
            camera2FullScreenBlockingDemoButton.setOnClickListener { openCamera2DemoFullBlocking() }
            settings.setOnClickListener { openSettings() }
        }
    }

    private fun openCameraViewDemoNoBlocking() {
        val directions = HomeFragmentDirections.actionHomeFragmentToCameraViewFragment()
        directions.coverMode = CameraCoverMode.None.id
        findNavController().navigate(directions)
    }

    private fun openCameraViewDemoFullBlocking() {
        val directions = HomeFragmentDirections.actionHomeFragmentToCameraViewFragment()
        directions.coverMode = CameraCoverMode.Whole.id
        findNavController().navigate(directions)
    }

    private fun openCamera2DemoNoBlocking() {
        val pictureFormat =  ImageFormat.YUV_420_888
        val directions = HomeFragmentDirections.actionHomeFragmentToCamera2Fragment("0", pictureFormat)
        directions.coverMode = CameraCoverMode.None.id
        findNavController().navigate(directions)
    }

    private fun openCamera2DemoFullBlocking() {
        val pictureFormat =  ImageFormat.YUV_420_888
        val directions = HomeFragmentDirections.actionHomeFragmentToCamera2Fragment("0", pictureFormat)
        directions.coverMode = CameraCoverMode.Whole.id
        findNavController().navigate(directions)
    }

    private fun openSettings() {
        val directions = HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
        findNavController().navigate(directions)
    }

}
