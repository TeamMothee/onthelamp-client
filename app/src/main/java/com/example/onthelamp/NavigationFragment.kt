package com.example.onthelamp

import RealTimeLocationUtil
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skt.tmap.TMapPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class NavigationFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    lateinit var captureButton: Button

    private var points: List<TMapPoint> = emptyList()

    private lateinit var realTimeLocationUtil: RealTimeLocationUtil

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

        // 방향 view

        // 최단 거리 points
        val pointsJson = arguments?.getString("points")
        pointsJson?.let {
            val gson = Gson()
            val type = object : TypeToken<List<TMapPoint>>() {}.type
            points = gson.fromJson(it, type)

//            // points 데이터 활용
//            points.forEach { point ->
//                Log.d("NavFragment", "Point: ${point.latitude}, ${point.longitude}")
//            }
        }

        realTimeLocationUtil = RealTimeLocationUtil(requireContext())

        realTimeLocationUtil.startRealTimeLocationUpdates(
            onLocationUpdate = { latitude, longitude ->
                Log.d("NavigationFragment", "현재 위치: Lat=$latitude, Lon=$longitude")
                updateArrowBasedOnTurn(latitude, longitude) // 방향 업데이트
            },
            onPermissionDenied = {
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        )

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
        realTimeLocationUtil.stopRealTimeLocationUpdates()
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

    // 다음 포인트 계산
    private fun findNextPoint(currentLat: Double, currentLon: Double, points: List<TMapPoint>): TMapPoint? {
        val distances = points.map { point ->
            Pair(point, calculateDistance(currentLat, currentLon, point.latitude, point.longitude))
        }
        val sortedPoints = distances.sortedBy { it.second }
        return sortedPoints.getOrNull(1)?.first // 가장 가까운 포인트 다음의 포인트
    }

    // 거리 계산 함수
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371e3 // 지구 반지름 (미터 단위)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    // 방향(각도) 계산
    private fun calculateBearing(currentLat: Double, currentLon: Double, targetLat: Double, targetLon: Double): Float {
        val dLon = Math.toRadians(targetLon - currentLon)
        val y = sin(dLon) * cos(Math.toRadians(targetLat))
        val x = cos(Math.toRadians(currentLat)) * sin(Math.toRadians(targetLat)) -
                sin(Math.toRadians(currentLat)) * cos(Math.toRadians(targetLat)) * Math.cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    private fun calculateTurnType(currentLat: Double, currentLon: Double, points: List<TMapPoint>): String {
        if (points.size < 2) return "종료" // 다음 포인트가 없는 경우 종료

        val nextPoint = findNextPoint(currentLat, currentLon, points) ?: return "종료"
        val nextPointIndex = points.indexOf(nextPoint)

        if (nextPointIndex >= points.size - 1) return "종료" // 더 이상 포인트가 없는 경우 종료

        // 다음 포인트와 그다음 포인트 간의 방향
        val currentToNextBearing = calculateBearing(currentLat, currentLon, nextPoint.latitude, nextPoint.longitude)
        val nextToNextBearing = calculateBearing(
            nextPoint.latitude,
            nextPoint.longitude,
            points[nextPointIndex + 1].latitude,
            points[nextPointIndex + 1].longitude
        )

        // 두 방향의 차이 계산
        val turnAngle = (nextToNextBearing - currentToNextBearing + 360) % 360

        return when {
            turnAngle > 45 && turnAngle <= 135 -> "우회전" // 45° ~ 135°: 우회전
            turnAngle >= 225 && turnAngle < 315 -> "좌회전" // 225° ~ 315°: 좌회전
            else -> "직진" // 나머지 경우: 직진
        }
    }


    private fun updateArrowBasedOnTurn(currentLat: Double, currentLon: Double) {
        val turnType = calculateTurnType(currentLat, currentLon, points)

        val arrowView = view?.findViewById<ImageView>(R.id.arrowView)
        val arrowResource = when (turnType) {
            "직진" -> R.drawable.arrow       // 직진 화살표 이미지
            "좌회전" -> R.drawable.left_arrow // 좌회전 화살표 이미지
            "우회전" -> R.drawable.right_arrow // 우회전 화살표 이미지
            else -> R.drawable.arrow         // 기본값 (직진 화살표)
        }

        arrowView?.setImageResource(arrowResource) // 화살표 이미지 변경
        Log.d("NavigationFragment", "현재 방향: $turnType, 선택된 이미지: $arrowResource")
    }
}