package com.example.exitsw

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 처음 실행 시 기본 프래그먼트로 MainFragment 설정
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment())
                .commit()
        }

        // BottomNavigation 메뉴 클릭 시 프래그먼트 교체
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.menu_home -> MainFragment()
                R.id.menu_ai -> fragment_chatbot()
                R.id.menu_saving -> FragmentSavingList()
                R.id.menu_policy -> fragment_savingDetail()
                // 계산기 메뉴는 아직 Fragment 없으므로 생략하거나 나중에 추가
                else -> null
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
                true
            } ?: false
        }
    }
}