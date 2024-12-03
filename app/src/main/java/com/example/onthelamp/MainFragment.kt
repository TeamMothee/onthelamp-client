package com.example.onthelamp

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.Manifest

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.skt.tmap.TMapView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onthelamp.ui.DividerItemDecoration
import com.example.onthelamp.utils.SpeechRecognizerHelper
import com.example.onthelamp.utils.TTSHelper
import com.skt.tmap.TMapPoint
import com.skt.tmap.overlay.TMapMarkerItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private val handler = Handler(Looper.getMainLooper())
private var searchRunnable: Runnable? = null

class MainFragment : Fragment(), OnMicButtonClickListener {

    val tmapApiKey = BuildConfig.TMAP_API_KEY
    val topPoiLimit = 4

    private var selectedPOI: POI? = null // 선택된 위치 데이터

    private var textWatcher: TextWatcher? = null // TextWatcher 참조 저장

    private lateinit var tMapView: TMapView // TMapView 객체

    private lateinit var speechRecognizerHelper: SpeechRecognizerHelper
    private lateinit var ttsHelper: TTSHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_map.xml 레이아웃을 설정
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        // TMapView 동적 추가
        val tmapViewContainer = view.findViewById<FrameLayout>(R.id.tmapViewContainer)
        tMapView = TMapView(requireContext())
        tMapView.setSKTMapApiKey(tmapApiKey)
        tmapViewContainer.addView(tMapView)

        ttsHelper = TTSHelper(requireContext())
        speechRecognizerHelper = SpeechRecognizerHelper(requireContext()) { recognizedText ->
            // STT 결과 로그 출력
            Log.d("STT Result", "인식된 텍스트: $recognizedText")
            handleSpeechResult(recognizedText)
        }

//      TODO: gps로 현재 위치 받아오기


        // 버튼 클릭 시 MapFragment로 전환
        val startInput = view.findViewById<EditText>(R.id.startInput)

        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().trim()

                // 이전 검색 작업 취소
                searchRunnable?.let { handler.removeCallbacks(it) }

                // 새로운 검색 작업 설정
                searchRunnable = Runnable {
                    if (keyword.isNotBlank()) {
                        searchPOI(keyword)
                    } else {
                        hideRecyclerView()
                    }
                }
                handler.postDelayed(searchRunnable!!, 150)
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        }

        startInput.addTextChangedListener(textWatcher) // TextWatcher 등록

        // 버튼 클릭 이벤트 처리
        val startButton = view.findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener {
            navigateToMapFragment() // 버튼 클릭 시 MapFragment로 이동
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsHelper.destroy()
    }

    private fun handleSpeechResultForSelection(recognizedText: String, topPois: List<POI>) {
        val index = when (recognizedText.trim()) {
            "일", "1" -> 0
            "이", "2" -> 1
            "삼", "3" -> 2
            "사", "4" -> 3
            else -> -1
        }

        if (index in topPois.indices) {
            val selectedPOI = topPois[index]
            handlePOISelection(selectedPOI)
            Toast.makeText(requireContext(), "선택된 위치: ${selectedPOI.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "올바른 선택이 아닙니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun promptUserToSelectOption(topPois: List<POI>) {
        val poiDescriptions = topPois.mapIndexed { index, poi -> "${index + 1}번 ${poi.name}" }
        val promptMessage = "다음 중 원하는 위치를 선택하세요: ${poiDescriptions.joinToString(", ")}"

        ttsHelper.speak(promptMessage)

        // STT를 시작하여 선택지를 입력받음
        speechRecognizerHelper = SpeechRecognizerHelper(requireContext()) { recognizedText ->
            handleSpeechResultForSelection(recognizedText, topPois)
        }
        speechRecognizerHelper.startListening()
    }



    private fun handleSpeechResult(recognizedText: String) {
        searchPOIBySTT(recognizedText)
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
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ActivityResultLauncher for Permission Request
    private val requestPermissionLauncher =
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



    private fun handlePOISelection(selectedItem: POI) {
        // 선택된 POI 저장
        selectedPOI = selectedItem

        // TextWatcher 제거 -> 텍스트 업데이트 -> 다시 추가
        val startInput = view?.findViewById<EditText>(R.id.startInput)
        startInput?.removeTextChangedListener(textWatcher)
        startInput?.setText(selectedItem.name)
        startInput?.addTextChangedListener(textWatcher)

        // RecyclerView 숨기기
        hideRecyclerView()
        // 지도에 마커 추가 및 이동
        addMarker(selectedItem)
    }

    private fun navigateToMapFragment() {
        if (selectedPOI != null) {
            val bundle = Bundle().apply {
                putParcelable("selectedPOI", selectedPOI) // Serializable로 데이터 전달
            }
            findNavController().navigate(R.id.action_mainFragment_to_mapFragment, bundle)
        } else {
//            TODO: Toast 메세지 수정
            Toast.makeText(requireContext(), "위치를 선택해주세요.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun hideRecyclerView() {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.poiRecyclerView)
        val outsideTouchView = view?.findViewById<View>(R.id.outsideTouchView)
        recyclerView?.visibility = View.GONE
        outsideTouchView?.visibility = View.GONE
    }

    private fun updatePOIList(topPois: List<POI>) {
        // TTS로 선택지 읽어주기

        val poiAdapter = POIAdapter(topPois) { selectedItem ->
            // 아이템 클릭 이벤트 처리
            handlePOISelection(selectedItem)
        }
        val recyclerView = view?.findViewById<RecyclerView>(R.id.poiRecyclerView)
        val outsideTouchView = view?.findViewById<View>(R.id.outsideTouchView)

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

        // 외부 클릭 감지 View 활성화
        outsideTouchView?.visibility = View.VISIBLE

        // 외부 클릭 시 RecyclerView 숨기기
        outsideTouchView?.setOnClickListener {
            recyclerView?.visibility = View.GONE
            outsideTouchView.visibility = View.GONE
        }
    }

    private fun updatePOIListSTT(topPois: List<POI>) {
        // TTS로 선택지 읽어주기

        val poiAdapter = POIAdapter(topPois) { selectedItem ->
            // 아이템 클릭 이벤트 처리
            handlePOISelection(selectedItem)
        }
        val recyclerView = view?.findViewById<RecyclerView>(R.id.poiRecyclerView)
        val outsideTouchView = view?.findViewById<View>(R.id.outsideTouchView)

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

        // 외부 클릭 감지 View 활성화
        outsideTouchView?.visibility = View.VISIBLE

        // 외부 클릭 시 RecyclerView 숨기기
        outsideTouchView?.setOnClickListener {
            recyclerView?.visibility = View.GONE
            outsideTouchView.visibility = View.GONE
        }

        // TTS로 선택지 읽고 선택받기
        promptUserToSelectOption(topPois)
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
                        val topPois = pois.take(topPoiLimit)

                        // RecyclerView에 데이터를 전달 (별도 메서드에서 처리)
                        updatePOIList(topPois)
                    } else {
//                        TODO: 큰 dialog로 수정
                        Toast.makeText(requireContext(), "검색 결과가 없습니다. 다시 마이크로 말씀해주세요.", Toast.LENGTH_SHORT).show()
                    }
                } else {
//                    TODO: Toast 메세지 수정
                    Toast.makeText(requireContext(), "API 응답 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
//                    TODO: Toast 메세지 수정
                Toast.makeText(requireContext(), "API 호출 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchPOIBySTT(keyword: String) {
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
                        updatePOIListSTT(topPois)
                    } else {
//                        TODO: 큰 dialog로 수정
                        Toast.makeText(requireContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
//                    TODO: Toast 메세지 수정
                    Toast.makeText(requireContext(), "API 응답 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
//                    TODO: Toast 메세지 수정
                Toast.makeText(requireContext(), "API 호출 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMarker(poi: POI) {
        val markerPosition = TMapPoint(poi.frontLat!!, poi.frontLon!!) // 마커 위치

        val markerItem = TMapMarkerItem().apply {
            id = "marker_1" // 마커 식별자
            tMapPoint = markerPosition // 마커 위치 설정
            name = poi.name // 마커 이름
            canShowCallout = true // 마커 클릭 시 이름 표시
            calloutTitle = poi.name // 표시할 이름
            calloutSubTitle = "${poi.upperAddrName} ${poi.middleAddrName} ${poi.lowerAddrName}" // 부제목
            icon = BitmapFactory.decodeStream(requireContext().assets.open("marker_icon.png")) // 마커 아이콘
        }


        tMapView.addTMapMarkerItem(markerItem) // 지도에 마커 추가
        tMapView.zoomLevel = 16 // 줌 레벨 설정
        tMapView.setCenterPoint( markerPosition.latitude, markerPosition.longitude) // 마커 중심으로 지도 이동

    }
}

