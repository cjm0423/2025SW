package com.example.exitsw

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.exitsw.databinding.ActivityMainBinding
import com.example.exitsw.main.MainFragment
import com.example.exitsw.mypage.MypageFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Firestore 오프라인 캐시 기능 활성화
        // 스마트폰 내부에 데이터를 저장하여 앱 로딩 속도를 높이고 오프라인을 지원합니다.
        val firestore = Firebase.firestore
        val settings = firestoreSettings {
            isPersistenceEnabled = true
        }
        firestore.firestoreSettings = settings

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            replaceFragment(MainFragment())

            // 2. 주기적인 백그라운드 동기화 작업 예약
            // 앱이 처음 시작될 때 앞으로 7일에 한 번씩 데이터를 자동 업데이트하도록 예약합니다.
            // 이 작업은 이제 Firestore의 데이터를 최신 상태로 유지하는 유일한 방법입니다.
            schedulePeriodicSync()
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

    /**
     * WorkManager를 사용하여 7일 주기로 백그라운드 동기화 작업을 예약합니다.
     * 이 작업은 사용자가 앱을 사용하지 않더라도 OS가 최적의 시간에 실행시켜 줍니다.
     */
    private fun schedulePeriodicSync() {
        val syncRequest = PeriodicWorkRequestBuilder<GlobalPolicySyncWorker>(7, TimeUnit.DAYS)
            .build()

        // "policySync"라는 고유한 이름으로 작업을 예약하여 중복을 방지합니다.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "policySync",
            ExistingPeriodicWorkPolicy.KEEP, // 이미 예약된 작업이 있다면 유지
            syncRequest
        )
        Log.d("WorkManager", "7일 주기의 데이터 동기화 작업이 성공적으로 예약되었습니다.")
    }
}