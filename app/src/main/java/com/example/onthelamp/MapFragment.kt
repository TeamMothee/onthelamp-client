package com.example.onthelamp

import RealTimeLocationUtil
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onthelamp.data.model.PedestrianRouteRequest
import com.example.onthelamp.data.model.PedestrianRouteResponse
import com.example.onthelamp.utils.SpeechRecognizerHelper
import com.example.onthelamp.utils.TTSHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapPolyLine
import retrofit2.Call
import java.net.URLEncoder

class MapFragment : Fragment(), OnMicButtonClickListener {
    private lateinit var tMapView: TMapView
    private val apiService = TMapRetrofitClient.getInstance().create(TMapService::class.java)
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var realTimeLocationUtil: RealTimeLocationUtil

    private var routePoints: List<TMapPoint> = emptyList()

    private lateinit var ttsHelper: TTSHelper
    private lateinit var speechRecognizerHelper: SpeechRecognizerHelper

    // 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_mapFragment_to_mainFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_map.xml 레이아웃을 설정
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // TMapView 동적 추가
        val tmapViewContainer = view.findViewById<FrameLayout>(R.id.tmapViewContainer)
        tMapView = TMapView(requireContext())
        tMapView.setSKTMapApiKey(BuildConfig.TMAP_API_KEY)
        tmapViewContainer.addView(tMapView)

        ttsHelper = TTSHelper(requireContext())
        // 전달된 데이터 받기
        val selectedPOI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("selectedPOI", POI::class.java)
        } else {
            arguments?.getParcelable("selectedPOI")
        }

        realTimeLocationUtil = RealTimeLocationUtil(requireContext())

        //TODO:  현재 위치 (임의 설정) GPS로 변경 필요

        fetchSingleLocation(onLocationReceived = { latitude, longitude ->
            Log.d("MapFragment", "Current Location: Lat=$latitude, Lon=$longitude")
            selectedPOI?.let {
                promptUserToSelectOption()

                Log.d("MapFragment", "Received POI: ${it.name}, Lat: ${it.frontLat}, Lon: ${it.frontLon}")

                // TextView에 POI 이름 설정
                val startInput = view.findViewById<TextView>(R.id.startInput)
                startInput.text = it.name


                // TextView 클릭 시 MainFragment로 이동
                startInput.setOnClickListener {
                    findNavController().navigate(R.id.action_mapFragment_to_mainFragment)
                }


                if (latitude != null && longitude != null) {
                    // RadioGroup 버튼 클릭 처리
                    val riskRadioGroup = view.findViewById<RadioGroup>(R.id.riskRadioGroup)
                    riskRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                        when (checkedId) {
                            R.id.highRisk -> { // "최단" 버튼
                                Log.d("MapFragment", "최단 경로 버튼 선택됨")
                                searchRoute(longitude, latitude, it.frontLon!!, it.frontLat!!)
                            }
                            R.id.lowRisk -> { // "안전" 버튼
                                Log.d("MapFragment", "안전 경로 버튼 선택됨 (아직 구현되지 않음)")
                                // TODO: 안전 경로 탐색 로직 추가 예정
                            }
                        }
                    }
                    // 기본적으로 "안전" 경로 버튼 동작 수행
                    // TODO: api 연결
                    searchRoute(longitude, latitude, it.frontLon!!, it.frontLat!!)
                }
            }
        })

        // MainActivity의 setRightButtonAction 호출
        (requireActivity() as? MainActivity)?.setRightButtonAction {
            navigateToNavFragment()
        }


        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsHelper.destroy()
    }

    private fun calculateRoute(isSafeRoute: Boolean) {
        val selectedPOI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("selectedPOI", POI::class.java)
        } else {
            arguments?.getParcelable("selectedPOI")
        }

        fetchSingleLocation { latitude, longitude ->
            if (selectedPOI != null) {
                if (isSafeRoute) {
                    Log.d("MapFragment", "안전 경로 계산 시작")
                    // 안전 경로 계산 로직 추가
                    // TODO: 안전 경로 API 연결 필요
                    searchRoute(longitude, latitude, selectedPOI.frontLon!!, selectedPOI.frontLat!!)
                } else {
                    Log.d("MapFragment", "최단 경로 계산 시작")
                    searchRoute(longitude, latitude, selectedPOI.frontLon!!, selectedPOI.frontLat!!)
                }

                // 안내 시작 버튼과 동일한 동작
                navigateToNavFragment()
            } else {
                Toast.makeText(requireContext(), "목적지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun promptUserToSelectOption() {
        ttsHelper.speak("경로를 찾았습니다, 안전 경로 또는 최단 경로를 선택하세요.")

        // STT로 안전, 최단 선택
        speechRecognizerHelper = SpeechRecognizerHelper(requireContext()) { recognizedText ->
            when {
                recognizedText.contains("안전") -> {
//                    ttsHelper.speak("안전 경로를 선택하셨습니다. 안내를 시작합니다.")
                    ttsHelper.speakWithCallback("안전 경로를 선택하셨습니다. 안내를 시작합니다."){
                        calculateRoute(isSafeRoute = true)
                    }
                }
                recognizedText.contains("최단") -> {
//                    ttsHelper.speak("최단 경로를 선택하셨습니다. 안내를 시작합니다.")
                    ttsHelper.speakWithCallback("최단 경로를 선택하셨습니다. 안내를 시작합니다."){
                        calculateRoute(isSafeRoute = false)
                    }
                }
                else -> {
                    ttsHelper.speak("잘못된 선택입니다. 안전 경로 또는 최단 경로 중에서 선택해주세요.")
                }
            }
        }
    }

    // 음성 퍼미션 체크
    private fun checkRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 퍼미션 요청
    private fun requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestSTTPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ActivityResultLauncher for Permission Request
    private val requestSTTPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(requireContext(), "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
                speechRecognizerHelper.startListening()
            } else {
                Toast.makeText(requireContext(), "권한이 거부되었습니다. STT를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onMicButtonPressed() {
        // 퍼미션 체크 후 STT 시작
        if (checkRecordAudioPermission()) {
            speechRecognizerHelper.startListening()
        } else {
            requestRecordAudioPermission()
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            context.micButtonClickListener = this
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (requireActivity() is MainActivity) {
            (requireActivity() as MainActivity).micButtonClickListener = null
        }
    }


    private fun navigateToNavFragment() {
        if (routePoints.isNotEmpty()) {
//             points를 JSON으로 변환
            val gson = Gson()
            val pointsJson = gson.toJson(routePoints)

            // NavFragment로 데이터 전달
            val action = MapFragmentDirections.actionMapFragmentToNavigationFragment(pointsJson)
            findNavController().navigate(action)
        } else {
            Toast.makeText(requireContext(), "경로가 없습니다. 경로를 계산해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchSingleLocation( onLocationReceived: (latitude: Double, longitude: Double) -> Unit) {
        realTimeLocationUtil.requestSingleLocation(
            onLocationReceived = { latitude, longitude ->
                Log.d("MainActivity", "Single Location: Lat=$latitude, Lon=$longitude")
                onLocationReceived(latitude, longitude)
            }
            ,
            onPermissionDenied = {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        )
    }


    private fun searchRoute(startX: Double, startY: Double, endX: Double, endY: Double) {
        val startName = URLEncoder.encode("출발지", "UTF-8")
        val endName = URLEncoder.encode("목적지", "UTF-8")

        Log.d("MapFragment", "Search route: ($startX, $startY) -> ($endX, $endY)")

        val request = PedestrianRouteRequest(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            startName = startName,
            endName = endName
        )

        Log.d("MapFragment", "Request: $request")

        apiService.findPedestrianRoute(
            appKey = BuildConfig.TMAP_API_KEY,
            requestBody = request
        ).enqueue(object : retrofit2.Callback<PedestrianRouteResponse> {
            override fun onResponse(
                call: Call<PedestrianRouteResponse>,
                response: retrofit2.Response<PedestrianRouteResponse>
            ) {

                if (response.isSuccessful) {
                    val routeResponse = response.body()
                    val points = mutableListOf<TMapPoint>()

                    Log.d("findPedestrianRoute" , "Response: $response")

                    routeResponse?.features?.forEach { feature ->
                        when (feature.geometry.type) {
                            "LineString" -> {
                                val coordinates = feature.geometry.coordinates
                                if (coordinates is List<*>) {
                                    coordinates.forEach { coord ->
                                        if (coord is List<*> && coord.size >= 2) {
                                            val latitude = coord[0] as Double
                                            val longitude = coord[1] as Double
                                            val lngLat = TMapPoint(longitude, latitude)
                                            points.add(lngLat)
                                        }
                                    }
                                }
                            }
                            "Point" -> {
                                val coordinates = feature.geometry.coordinates
                                if (coordinates is List<*> && coordinates.size == 2) {
                                    val latitude = coordinates[0] as Double
                                    val longitude = coordinates[1] as Double
                                    val lngLat = TMapPoint(longitude, latitude)
                                    points.add(lngLat)
                                }
                            }
                            else -> {
                                Log.e("MapFragment", "Unknown geometry type: ${feature.geometry.type}")
                            }
                        }
                    }

                    routePoints = points
                    drawRoute(points)
                } else {
                    Log.e("MapFragment", "API 호출 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<PedestrianRouteResponse>, t: Throwable) {
                Log.e("MapFragment", "API 호출 실패: ${t.message}")
            }
        })
    }

    private fun drawRoute(points: List<TMapPoint>) {
        Log.d("drawRoute", "Drawing route with ${points.size} points")
        val pointList = ArrayList(points)

        // TMapPolyLine 객체 생성
        val polyLine = TMapPolyLine("routeLine",pointList).apply {
            // 색상 및 두께 설정 (선택)
            lineColor = android.graphics.Color.BLUE
            lineWidth = 2f
        }

        // TMapView에 PolyLine 추가
        tMapView.addTMapPolyLine(polyLine)

        // 지도 중심을 경로의 첫 번째 포인트로 설정
        if (points.isNotEmpty()) {
            tMapView.setCenterPoint(points.first().latitude, points.first().longitude, true)
            tMapView.zoomLevel = 14
        }
    }
}

