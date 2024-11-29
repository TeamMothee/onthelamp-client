package com.example.onthelamp

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.example.onthelamp.data.model.PedestrianRouteRequest
import com.example.onthelamp.data.model.PedestrianRouteResponse
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapPolyLine
import retrofit2.Call
import java.net.URLEncoder

class MapFragment : Fragment() {
    private lateinit var tMapView: TMapView
    private val apiService = TMapRetrofitClient.getInstance().create(TMapService::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_map.xml 레이아웃을 설정
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // TMapView 동적 추가
        val tmapViewContainer = view.findViewById<FrameLayout>(R.id.tmapViewContainer)
        tMapView = TMapView(requireContext())
        tMapView.setSKTMapApiKey(BuildConfig.TMAP_API_KEY)
        tmapViewContainer.addView(tMapView)

        // 전달된 데이터 받기
        val selectedPOI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("selectedPOI", POI::class.java)
        } else {
            arguments?.getParcelable("selectedPOI")
        }


        selectedPOI?.let {
            Log.d("MapFragment", "Received POI: ${it.name}, Lat: ${it.frontLat}, Lon: ${it.frontLon}")

            //TODO:  현재 위치 (임의 설정) GPS로 변경 필요
            val startLat = 37.57592543
            val startLon = 126.98415913

            // RadioGroup 버튼 클릭 처리
            val riskRadioGroup = view.findViewById<RadioGroup>(R.id.riskRadioGroup)
            riskRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.highRisk -> { // "최단" 버튼
                        Log.d("MapFragment", "최단 경로 버튼 선택됨")
                        searchRoute(startLon, startLat, it.frontLon!!, it.frontLat!!)
                    }
                    R.id.lowRisk -> { // "안전" 버튼
                        Log.d("MapFragment", "안전 경로 버튼 선택됨 (아직 구현되지 않음)")
                        // TODO: 안전 경로 탐색 로직 추가 예정
                    }
                }
            }

        }


        return view
    }


    private fun searchRoute(startX: Double, startY: Double, endX: Double, endY: Double) {
        val startName = URLEncoder.encode("출발지", "UTF-8")
        val endName = URLEncoder.encode("목적지", "UTF-8")

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

