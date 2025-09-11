package com.example.exitsw

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.exitsw.databinding.ActivityMainBinding
import com.example.exitsw.main.MainFragment
import com.example.exitsw.mypage.MypageFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            replaceFragment(MainFragment())
            runWelfareSyncOnce()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    replaceFragment(MainFragment()); true
                }
                R.id.menu_policy -> {
                    replaceFragment(MypageFragment()); true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun runWelfareSyncOnce() {
        lifecycleScope.launch {
            try {
                // ★ 우선 특정 시군구 코드로 성공 여부 확인
                val saved = WelfareApiToFirebase.sync(
                    context = this@MainActivity,
                    sigunguCd = "11680",
                    pageSize = 100
                )
                Toast.makeText(
                    this@MainActivity,
                    "정책 동기화 완료(저장 $saved)",
                    Toast.LENGTH_LONG
                ).show()
            } catch (t: Throwable) {
                Log.e("WelfareSync", "동기화 실패", t)
                Toast.makeText(
                    this@MainActivity,
                    "정책 동기화 실패: ${t.message ?: t::class.java.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
