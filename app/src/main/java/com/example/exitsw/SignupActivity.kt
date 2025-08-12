package com.example.exitsw

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.exitsw.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 카카오에서 전달된 닉네임
        val kakaoNick = intent.getStringExtra("nickname")
        if (!kakaoNick.isNullOrEmpty()) {
            binding.etNickname.setText(kakaoNick)
        } else {
            // 닉네임이 없는 경우 힌트 유지
            binding.etNickname.hint = "닉네임"
        }

        binding.btnSignUp.setOnClickListener {
            val nicknameInput = binding.etNickname.text.toString().trim()
            val birthInput = binding.etBirth.text.toString().trim()
            val addressInput = binding.etAddress.text.toString().trim()
            val incomeInput = binding.etIncome.text.toString().trim()
            val keywordInput = binding.actvKeyword.text.toString().trim()
            val agreeInfo = binding.cbAgreeInfo.isChecked
            val agreeNotify = binding.cbAgreeNotify.isChecked

            if (nicknameInput.isEmpty() || birthInput.isEmpty() || addressInput.isEmpty() || incomeInput.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!agreeInfo) {
                Toast.makeText(this, "개인 정보 수집에 동의해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 서버 전송 로직

            // 가입 완료 후 MainActivity 이동
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
