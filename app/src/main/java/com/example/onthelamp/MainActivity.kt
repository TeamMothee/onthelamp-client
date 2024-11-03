package com.example.onthelamp

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 지도 이동 버튼 클릭 시 MapFragment 로드
        val mapButton = findViewById<Button>(R.id.btn_navigate_map)
        mapButton.setOnClickListener {
            loadFragment(MapFragment())
        }

        // 네비게이션 이동 버튼 클릭 시 NavigationFragment 로드
        val navigationButton = findViewById<Button>(R.id.btn_navigate_navigation)
        navigationButton.setOnClickListener {
            loadFragment(NavigationFragment())
        }

        // 설정 이동 버튼 클릭 시 SettingsFragment 로드
        val settingsButton = findViewById<Button>(R.id.btn_navigate_settings)
        settingsButton.setOnClickListener {
            loadFragment(SettingsFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
