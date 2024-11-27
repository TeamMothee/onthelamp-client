package com.example.onthelamp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.example.onthelamp.BuildConfig
import com.example.onthelamp.data.model.PedestrianRouteRequest
import com.example.onthelamp.data.model.PedestrianRouteResponse
import com.skt.tmap.TMapPoint


import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
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

        // TMapView 초기화 완료 시 작업 수행
        tMapView.setOnMapReadyListener {
            Log.d("MapFragment", "TMapView initialized successfully")
//            addMarker(POI("서울시청", 37.5662952, 126.97794509999999, "서울", "중구", "태평로1가"))
        }


        // 전달된 데이터 받기
        val selectedPOI = arguments?.getSerializable("selectedPOI") as? POI
        selectedPOI?.let {
            Log.d("MapFragment", "Received POI: ${it.name}, Lat: ${it.frontLat}, Lon: ${it.frontLon}")

            //TODO:  현재 위치 (임의 설정) GPS로 변경
            val startLat = 37.566477
            val startLon = 126.985022

            // 최단 경로 탐색
            searchRoute(startLat.toString(), startLon.toString(), it.frontLat.toString(), it.frontLon.toString())
        }


        return view
    }

    private fun searchRoute(startX: String, startY: String, endX: String, endY: String) {
        val startName = URLEncoder.encode("출발지", "UTF-8") // 출발지 이름 URL 인코딩
        val endName = URLEncoder.encode("목적지", "UTF-8")   // 목적지 이름 URL 인코딩

        val request = PedestrianRouteRequest(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            startName = startName,
            endName = endName
        )

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

                    // 좌표 추출
                    routeResponse?.features?.forEach { feature ->
                        if (feature.geometry.type == "LineString") {
                            feature.geometry.coordinates.forEach { coord ->
                                val latLng = TMapPoint(coord[1], coord[0])
                                points.add(latLng)
                            }
                        }
                    }

                    Log.d("MapFragment", "Route points: $points")
                    Log.d("MapFragment", "Request: $request")

                    // 지도에 경로 표시
                    drawRoute(points)
                } else {
                    Log.d("MapFragment", "Response: $response")
                    Log.e("MapFragment", "API 호출 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<PedestrianRouteResponse>, t: Throwable) {
                Log.d("MapFragment", "Request: $request")
                Log.e("MapFragment", "API 호출 실패: ${t.message}")
            }
        })
    }

    private fun drawRoute(points: List<TMapPoint>) {
        val polyline = TMapPolyLine().apply {
            lineColor = android.graphics.Color.BLUE // 경로 색상 설정
            lineWidth = 5f // 경로 두께 설정
        }

        polyline.setID("Line123") // Polyline 식별자 설정

        points.forEach { point ->
            polyline.addLinePoint(point) // 좌표를 경로에 추가
        }

        // 지도에 Polyline 추가
        tMapView.addTMapPolyLine(polyline)

        tMapView.setCenterPoint(points.first().longitude, points.first().latitude, true)

//        if (points.isNotEmpty()) {
//            tMapView.setCenterPoint(points.first().longitude, points.first().latitude, true)
//        }
    }



}

