package com.example.onthelamp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import com.skt.tmap.TMapView
import androidx.fragment.app.Fragment

class MainFragment : Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_map.xml 레이아웃을 설정
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        // TMapView 동적 추가
        val tmapViewContainer = view.findViewById<FrameLayout>(R.id.tmapViewContainer)
        val tMapView = TMapView(requireContext())
        tMapView.setSKTMapApiKey(BuildConfig.TMAP_API_KEY)
        tmapViewContainer.addView(tMapView)

        // 버튼 클릭 시 MapFragment로 전환
        val button = view.findViewById<Button>(R.id.startButton)

        button.setOnClickListener {
            (activity as MainActivity).replaceFragment(MapFragment())
        }



        return view
    }

}