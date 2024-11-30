package com.example.onthelamp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.camera.view.PreviewView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture


import android.net.Uri

import android.widget.Button
import android.widget.ImageView
import androidx.camera.core.*
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale


class NavigationFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    lateinit var captureButton: Button

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_navigation.xml 레이아웃을 사용하여 화면을 구성합니다.
        var view = inflater.inflate(R.layout.fragment_navigation, container, false)
        previewView = view.findViewById(R.id.previewView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = activity as? MainActivity
//        mainActivity?.rightButton?.setOnClickListener{
//            takePhoto()
//        }

        mainActivity?.apply{
            setRightButtonAction {
                takePhoto { savedUri ->
                    if (savedUri != null) {
                        val bundle = Bundle().apply {
                            putString("savedUri", savedUri.toString())
                        }
                        findNavController().navigate(R.id.imageCaptioningFragment, bundle)
                    } else {
                        Toast.makeText(requireContext(), "Photo capture failed.", Toast.LENGTH_SHORT).show()
                    }
                }
//                takePhoto(null)
            }
            setLeftButtonAction {
                materialRadioDialog()
            }
        }

        mainActivity?.setRightButtonColor(R.color.button_green)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun allPermissionsGranted() = (ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED)

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e("NavigationFragment", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto(onPhotoCaptured : ((Uri) -> Unit)? = null) {
        val imageCapture = imageCapture ?: return

        // Create a unique file name for the photo
        val photoFile = File(
            requireContext().externalMediaDirs.firstOrNull(),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("NavigationFragment", "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri: Uri = Uri.fromFile(photoFile)
                    Log.d("NavigationFragment", "Photo capture succeeded: $savedUri")
                    Toast.makeText(requireContext(), "Photo saved: $savedUri", Toast.LENGTH_SHORT).show()
//                    findNavController().navigate(R.id.imageCaptioningFragment)
                    onPhotoCaptured?.invoke(savedUri)
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun materialRadioDialog() {
        val singleItems = arrayOf("점자블록", "볼라드", "음향신호기")
        var checkedItem = 0

        MaterialAlertDialogBuilder(requireContext(),R.style.CustomDialog)
            .setTitle("Choose what you like")
            .setNeutralButton("cancel") { dialog, which ->
                // Respond to neutral button press
            }
            .setPositiveButton("ok") { dialog, which ->
                Toast.makeText(requireContext(), "Oh, you like ${singleItems[checkedItem]}!", Toast.LENGTH_SHORT).show()
            }
            // Single-choice items (initialized with checked item)
            .setSingleChoiceItems(singleItems, checkedItem) { dialog, which ->
                checkedItem = which
            }
            .show()
    }
}