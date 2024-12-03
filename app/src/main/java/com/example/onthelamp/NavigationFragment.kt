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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale


class NavigationFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    lateinit var captureButton: Button
    private lateinit var objectDetector: TFLiteObjectDetector

    private var job: Job? = null

    var checkAlert: Boolean = false

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

            updateRightButtonText("사진 분석")
        }

        mainActivity?.setRightButtonColor(R.color.button_green)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
            startRepeatingTask()
            objectDetector = TFLiteObjectDetector(requireContext(), "yolov8n_float32.tflite")
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
                    if(onPhotoCaptured!=null){
                        onPhotoCaptured?.invoke(savedUri)
                    }
                    else{
                        objectDetecting(savedUri)
                    }
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        stopRepeatingTask()
        objectDetector.close()
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
//        val singleItems = arrayOf("점자블록", "볼라드", "음향신호기")
//        var checkedItem = 0

        val multiItems = arrayOf("점자블록", "볼라드", "음향신호기")
        val checkedItems = booleanArrayOf(false, false, false, false)

//        MaterialAlertDialogBuilder(requireContext(),R.style.CustomDialog)
//            .setTitle("Choose what you like")
//            .setNeutralButton("cancel") { dialog, which ->
//                // Respond to neutral button press
//            }
//            .setPositiveButton("ok") { dialog, which ->
//                Toast.makeText(requireContext(), "Oh, you like ${singleItems[checkedItem]}!", Toast.LENGTH_SHORT).show()
//            }
//            // Single-choice items (initialized with checked item)
//            .setSingleChoiceItems(singleItems, checkedItem) { dialog, which ->
//                checkedItem = which
//            }
//            .show()
//
        MaterialAlertDialogBuilder(requireContext(),R.style.CustomDialog)
            .setTitle("Choose what you like")
            .setNeutralButton("cancel") { dialog, which ->
                // Respond to neutral button press
            }
            .setPositiveButton("ok") { dialog, which ->
                val checkCnt = checkedItems.count { it }
                Toast.makeText(requireContext(), "you choose $checkCnt items", Toast.LENGTH_SHORT).show()
            }
            // Single-choice items (initialized with checked item)
            .setMultiChoiceItems(multiItems, checkedItems) { dialog, which, checked ->
                checkedItems[which] = checked
            }
            .show()
    }

    private fun startRepeatingTask() {
        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                takePhoto()
                delay(5000) // 5초 대기
            }
        }
    }

    private fun stopRepeatingTask() {
        job?.cancel()
    }

    private fun objectDetecting(savedUri : Uri) {


        // 예제 이미지 로드
//        val imageStream = assets.open("sample_image.jpg")
//        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.image_test)

        val bitmap = uriToBitmap(savedUri)

        // 객체 탐지 실행
        val results = objectDetector.detectObjects(bitmap)

        // 결과 출력
        results.forEach { result ->
            if(!checkAlert){
                if(result.classId==1 || result.classId==2 || result.classId==3 || result.classId==5 || result.classId==7 || result.classId==36){
                    checkAlert=true
                }
            }
            println("Class ID: ${result.classId}, Confidence: ${result.confidence}")
            println("Bounding Box: ${result.boundingBox.joinToString()}")
        }
        if(checkAlert){
            view?.findViewById<View>(R.id.alertView)?.visibility = View.VISIBLE
            checkAlert=false
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {

        val inputStream = requireContext().contentResolver.openInputStream(uri)

        return BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
    }

}