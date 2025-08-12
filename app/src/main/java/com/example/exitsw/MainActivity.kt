package com.example.exitsw

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.exitsw.databinding.ActivityMainBinding
import com.example.exitsw.main.MainFragment
import com.example.exitsw.mypage.MypageFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 처음 실행 시 MainFragment를 보여줌
        if (savedInstanceState == null) {
            replaceFragment(MainFragment())
        }

        // 하단 네비게이션 탭 바의 아이템 클릭 리스너 설정
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    replaceFragment(MainFragment())
                    true
                }
                // ✨ '정책' 탭을 누르면 MypageFragment를 보여주도록 설정
                R.id.menu_policy -> {
                    replaceFragment(MypageFragment())
                    true
                }
                // TODO: 다른 메뉴 아이템에 대한 프래그먼트 연결
                // R.id.menu_calculator -> replaceFragment(CalculatorFragment())
                // R.id.menu_ai -> replaceFragment(AiFragment())
                // R.id.menu_saving -> replaceFragment(SavingFragment())
                else -> false
            }
        }
    }

    // 화면(Fragment)을 교체하는 함수
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
