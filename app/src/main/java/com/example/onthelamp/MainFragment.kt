package com.example.onthelamp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.skt.tmap.TMapView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onthelamp.ui.DividerItemDecoration
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainFragment : Fragment(){
    val tmapApiKey = BuildConfig.TMAP_API_KEY
    val topPoiLimit = 4

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_map.xml 레이아웃을 설정
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        // TMapView 동적 추가
        val tmapViewContainer = view.findViewById<FrameLayout>(R.id.tmapViewContainer)
        val tMapView = TMapView(requireContext())
        tMapView.setSKTMapApiKey(tmapApiKey)
        tmapViewContainer.addView(tMapView)

        // 버튼 클릭 시 MapFragment로 전환
        val startButton = view.findViewById<Button>(R.id.startButton)
        val startInput = view.findViewById<EditText>(R.id.startInput)

        startButton.setOnClickListener {
            val keyword = startInput.text.toString().trim()
            if (keyword.isNotBlank()) {
                searchPOI(keyword)
            } else {
//                TODO: 더 큰 alert dialog로 변경
                Toast.makeText(requireContext(  ), "검색 키워드를 입력하세요", Toast.LENGTH_SHORT).show()
            }
//            (activity as MainActivity).replaceFragment(MapFragment())
        }

        return view
    }

    private fun updatePOIList(topPois: List<POI>) {
        val poiAdapter = POIAdapter(topPois) // RecyclerView 어댑터 초기화
        val recyclerView = view?.findViewById<RecyclerView>(R.id.poiRecyclerView)

        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext()) // 세로 리스트로 표시

            // ItemDecoration 추가 (구분선)
            if (itemDecorationCount == 0) { // 중복 추가 방지
                val dividerColor = requireContext().getColor(android.R.color.white) // 구분선 색상
                val dividerHeight = 2
                addItemDecoration(DividerItemDecoration(dividerColor, dividerHeight))
            }

            adapter = poiAdapter
            visibility = View.VISIBLE // 검색 결과가 있으면 표시
        }
    }


    private fun searchPOI(keyword: String) {
        val apiService = TMapRetrofitClient.getInstance().create(TMapService::class.java)
        val appKey = tmapApiKey
        val call = apiService.searchPOI(keyword = keyword, appKey = appKey)

        call.enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                if (response.isSuccessful) {
                    val pois = response.body()?.searchPoiInfo?.pois?.poi

                    if (!pois.isNullOrEmpty()) {
                        // 상위 4개의 POI 가져오기
                        val topPois = pois.take(topPoiLimit)

                        // RecyclerView에 데이터를 전달 (별도 메서드에서 처리)
                        updatePOIList(topPois)
                    } else {
                        Toast.makeText(requireContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "API 응답 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "API 호출 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

