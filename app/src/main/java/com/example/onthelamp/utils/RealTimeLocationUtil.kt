import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class RealTimeLocationUtil(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    private var isLocationUpdatesStarted = false

    /**
     * 단일 위치 가져오기
     */
    fun requestSingleLocation(
        onLocationReceived: (latitude: Double, longitude: Double) -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (checkLocationPermission()) {
            fetchSingleLocation(onLocationReceived)
        } else {
            onPermissionDenied()
        }
    }

    /**
     * 실시간 위치 업데이트 시작
     */
    fun startRealTimeLocationUpdates(
        onLocationUpdate: (latitude: Double, longitude: Double) -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (checkLocationPermission()) {
            beginRealTimeUpdates(onLocationUpdate)
        } else {
            onPermissionDenied()
        }
    }

    /**
     * 실시간 위치 업데이트 중지
     */
    fun stopRealTimeLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            isLocationUpdatesStarted = false
        }
    }

    /**
     * 권한 체크
     */
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 내부 함수: 단일 위치 요청
     */
    @SuppressLint("MissingPermission")
    private fun fetchSingleLocation(onLocationReceived: (latitude: Double, longitude: Double) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationReceived(location.latitude, location.longitude)
            } else {
                Log.e("RealTimeLocationUtil", "Last location is null, requesting real-time location")
                requestSingleLocationOnce(onLocationReceived)
            }
        }.addOnFailureListener {
            Log.e("RealTimeLocationUtil", "Failed to get last location", it)
            requestSingleLocationOnce(onLocationReceived)
        }
    }

    /**
     * 내부 함수: 실시간 위치 업데이트
     */
    @SuppressLint("MissingPermission")
    private fun beginRealTimeUpdates(onLocationUpdate: (latitude: Double, longitude: Double) -> Unit) {
        if (isLocationUpdatesStarted) return // 이미 시작된 경우 무시
        isLocationUpdatesStarted = true

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L) // 최소 업데이트 주기
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    onLocationUpdate(location.latitude, location.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    /**
     * 내부 함수: 단일 위치 요청 (실시간)
     */
    @SuppressLint("MissingPermission")
    private fun requestSingleLocationOnce(onLocationReceived: (latitude: Double, longitude: Double) -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // 요청 주기 5초
            .setWaitForAccurateLocation(false) // 즉시 위치 반환
            .setMaxUpdates(1) // 한 번만 업데이트
            .build()


        val singleLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    onLocationReceived(location.latitude, location.longitude)
                } else {
                    Log.e("RealTimeLocationUtil", "Single location request failed")
                }
                fusedLocationClient.removeLocationUpdates(this) // 콜백 제거
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            singleLocationCallback,
            Looper.getMainLooper()
        )
    }
}
