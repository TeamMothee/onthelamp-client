package com.example.onthelamp

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var rightButton: Button;
    lateinit var leftButton: Button;

//    private val frame: FrameLayout by lazy { // activity_main의 화면 부분
//        findViewById(R.id.frame_container)
//    }
    private val bottomNagivationView: BottomNavigationView by lazy { // 하단 네비게이션 바
        findViewById(R.id.bottom_navigation)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rightButton = findViewById(R.id.right_button)
        leftButton = findViewById(R.id.left_button)

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment? ?: return

        val navController = host.navController

        setupBottomNavMenu(navController)

//        // 애플리케이션 실행 후 첫 화면 설정
//        supportFragmentManager.beginTransaction().add(frame.id, MapFragment()).commit()
//
//        // 하단 네비게이션 바 클릭 이벤트 설정
//        bottomNagivationView.setOnNavigationItemSelectedListener {item ->
//            when(item.itemId) {
//                R.id.nav_map -> {
//                    replaceFragment(MapFragment())
//                    true
//                }
//                R.id.nav_camera -> {
//                    replaceFragment(NavigationFragment())
//                    true
//                }
//                R.id.nav_settings -> {
//                    replaceFragment(SettingsFragment())
//                    true
//                }
//                else -> false
//            }
//        }
    }

    // 화면 전환 구현 메소드
//    fun replaceFragment(fragment: Fragment) {
//        supportFragmentManager.beginTransaction().replace(frame.id, fragment).commit()
//    }

    fun setRightButtonAction(action: () -> Unit) {
        rightButton.setOnClickListener {
            action()
        }
    }

    fun setLeftButtonAction(action: () -> Unit) {
        leftButton.setOnClickListener {
            action()
        }
    }

    fun updateRightButtonText(newText: String) {
        rightButton.text = newText
    }

    fun updateLeftButtonText(newText: String) {
        leftButton.text = newText
    }

    fun hideRightButtonText() {
        rightButton.visibility = View.GONE
    }

    fun hideLeftButtonText() {
        leftButton.visibility = View.GONE
    }

    fun showRightButtonText() {
        rightButton.visibility = View.VISIBLE
    }

    fun showLeftButtonText() {
        leftButton.visibility = View.VISIBLE
    }

    fun setRightButtonColor(backgroundColor: Int) {
        ViewCompat.setBackgroundTintList(rightButton, ContextCompat.getColorStateList(this, backgroundColor))
//        rightButton.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    fun setLeftButtonColor(backgroundColor: Int) {
        ViewCompat.setBackgroundTintList(leftButton, ContextCompat.getColorStateList(this, backgroundColor))
//        leftButton.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    private fun setupBottomNavMenu(navController: NavController) {
        // TODO STEP 9.3 - Use NavigationUI to set up Bottom Nav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setupWithNavController(navController)
        // TODO END STEP 9.3
    }
}