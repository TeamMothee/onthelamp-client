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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.onthelamp.utils.TTSHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skt.tmap.TMapPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.PATCH
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

// Retrofit API Interface
interface ReportService {
    @PATCH("api/report")
    suspend fun updateLocation(@Body locationData: LocationData)
}

object RetrofitClient {
    private const val BASE_URL = "http://54.180.202.234:8000/"

    private val client = OkHttpClient.Builder().build()

    val instance: ReportService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReportService::class.java)
    }
}

class NavigationFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    lateinit var captureButton: Button
    private lateinit var objectDetector: TFLiteObjectDetector

    // 이전 방향
    private var lastTurnType: String? = null

    private var job: Job? = null

    var checkAlert: Boolean = false

    private var points: List<TMapPoint> = emptyList()

    private lateinit var realTimeLocationUtil: RealTimeLocationUtil

    private lateinit var ttsHelper: TTSHelper

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

        ttsHelper = TTSHelper(requireContext())

        // 방향 view

        // 최단 거리 points
        val pointsJson = arguments?.getString("points")
        pointsJson?.let {
            val gson = Gson()
            val type = object : TypeToken<List<TMapPoint>>() {}.type
            points = gson.fromJson(it, type)

            // points 데이터 활용
            points.forEach { point ->
                Log.d("NavFragment", "Point: ${point.latitude}, ${point.longitude}")
            }
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
                lifecycleScope.launch {
//                    var latitude = 37.7749
//                    var longitude = -122.4194
                    val (latitude, longitude) = getLocation()
                    Log.d("bbbb","a")

//                    realTimeLocationUtil.requestSingleLocation(
//                        onLocationReceived = {
//                                latitudeT, longitudeT ->
//                            Log.d("bbb", "현재 위치: Lat_t=$latitudeT, Lon_t=$longitudeT")
//                            latitude=latitudeT
//                            longitude=longitudeT
//                        },
//                        onPermissionDenied = {
//                            Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
//                        }
//                    )
                    Log.d("bbbb","현재 위치: Lat=$latitude, Lon=$longitude")
                    updateReportLocation(latitude, longitude)
                }
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
            ttsHelper.speak("차량이 앞에 있습니다. 주의하세요.")
            view?.findViewById<View>(R.id.alertView)?.visibility = View.VISIBLE
            checkAlert=false
        } else{
            view?.findViewById<View>(R.id.alertView)?.visibility = View.GONE
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {

        val inputStream = requireContext().contentResolver.openInputStream(uri)

        return BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
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
        if (points.size < 2) return "종료" // 유효한 포인트가 없으면 종료

        // 현재 위치에서 가장 가까운 포인트의 인덱스를 찾음
        val nextPoint = findNextPoint(currentLat, currentLon, points) ?: return "종료"
        val nextPointIndex = points.indexOf(nextPoint)

        if (nextPointIndex >= points.size - 1) return "종료" // 남은 포인트가 충분하지 않으면 종료

        // 최소 10개 포인트를 확인하도록 범위를 제한
        val rangeEndIndex = minOf(nextPointIndex + 10, points.size - 1) // 최대 10개까지만 확인
        val rangePoints = points.subList(nextPointIndex, rangeEndIndex + 1)

        // 현재 위치에서 첫 번째 포인트까지의 방향
        val currentToNextBearing = calculateBearing(currentLat, currentLon, rangePoints.first().latitude, rangePoints.first().longitude)

        // 다음 10개 포인트의 방향 변화 평균 계산
        val bearings = mutableListOf<Float>()
        for (i in 0 until rangePoints.size - 1) {
            val currentBearing = calculateBearing(
                rangePoints[i].latitude,
                rangePoints[i].longitude,
                rangePoints[i + 1].latitude,
                rangePoints[i + 1].longitude
            )
            bearings.add(currentBearing)
        }

        // 평균 방향 계산
        val averageBearing = bearings.average().toFloat()

        // 방향 변화 계산
        val turnAngle = (averageBearing - currentToNextBearing + 360) % 360

        // 로그 출력
        Log.d("CalculateTurnType", "평균 방향: $averageBearing, 현재 방향: $currentToNextBearing, 변화 각도: $turnAngle")

        return when {
            turnAngle > 60 && turnAngle <= 120 -> "우회전" // 60° ~ 120°: 명확한 우회전
            turnAngle >= 240 && turnAngle < 300 -> "좌회전" // 240° ~ 300°: 명확한 좌회전
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

        if (turnType != lastTurnType) {
            val ttsMessage = when (turnType) {
                "직진" -> "앞으로 직진하세요."
                "좌회전" -> "잠시 후 왼쪽으로 회전하세요."
                "우회전" -> "잠시 후 오른쪽으로 회전하세요."
                else -> null
            }

            ttsMessage?.let {
                ttsHelper.speak(it) // TTS 메시지 출력
            }

            // 마지막 방향 업데이트
            lastTurnType = turnType
        }

        arrowView?.setImageResource(arrowResource) // 화살표 이미지 변경
        Log.d("NavigationFragment", "현재 방향: $turnType")
    }

    suspend fun updateReportLocation(latitude: Double, longitude: Double) {
        val locationData = LocationData(latitude, longitude)

        try {
            RetrofitClient.instance.updateLocation(locationData)
            println("Location updated successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to update location.")
        }
    }

    suspend fun getLocation(): Pair<Double, Double> {
        return suspendCancellableCoroutine { continuation ->
            realTimeLocationUtil.requestSingleLocation(
                onLocationReceived = { latitude, longitude ->
                    Log.d("bbb", "현재 위치: Lat_t=$latitude, Lon_t=$longitude")
                    continuation.resume(Pair(latitude, longitude)) // Resume coroutine with result
                },
                onPermissionDenied = {
                    Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                    continuation.resume(Pair(0.0, 0.0)) // Default values if permission is denied
                }
            )
        }
    }
}