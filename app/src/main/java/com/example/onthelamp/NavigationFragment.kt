package com.example.onthelamp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class NavigationFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_navigation.xml 레이아웃을 사용하여 화면을 구성합니다.
        return inflater.inflate(R.layout.fragment_navigation, container, false)
    }
}