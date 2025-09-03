package com.example.exitsw

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.exitsw.databinding.ActivitySignupBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    private var selectedBirthMillis: Long? = null
    private val dateFormatter by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    private val kakaoId: String by lazy {
        intent.getStringExtra(LoginActivity.EXTRA_KAKAO_ID)?.trim().orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 닉네임 프리필
        intent.getStringExtra(LoginActivity.EXTRA_NICKNAME)
            ?.takeIf { it.isNotBlank() }
            ?.let { binding.etNickname.setText(it) }

        // kakaoId 필수 검사 (없으면 회원가입 진행 못 하게)
        if (kakaoId.isBlank()) {
            Toast.makeText(this, "로그인 정보가 유효하지 않습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
            return
        }

        // 생년월일
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("생년월일 선택")
            .setCalendarConstraints(constraints)
            .build()
        binding.etBirth.setOnClickListener {
            if (!datePicker.isAdded) datePicker.show(supportFragmentManager, "birth_picker")
        }
        datePicker.addOnPositiveButtonClickListener { millis ->
            selectedBirthMillis = millis
            binding.etBirth.setText(dateFormatter.format(Date(millis)))
        }

        // 드롭다운
        binding.actvRegion.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.kor_regions).toList()))
        binding.actvIncome.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.income_brackets).toList()))
        binding.actvKeyword.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.interest_keywords).toList()))
        binding.actvRegion.threshold = 0
        binding.actvIncome.threshold = 0
        binding.actvKeyword.threshold = 0
        binding.actvRegion.setOnClickListener { binding.actvRegion.showDropDown() }
        binding.actvIncome.setOnClickListener { binding.actvIncome.showDropDown() }
        binding.actvKeyword.setOnClickListener { binding.actvKeyword.showDropDown() }
        binding.actvRegion.setOnFocusChangeListener { _: View, hasFocus: Boolean -> if (hasFocus) binding.actvRegion.showDropDown() }
        binding.actvIncome.setOnFocusChangeListener { _: View, hasFocus: Boolean -> if (hasFocus) binding.actvIncome.showDropDown() }
        binding.actvKeyword.setOnFocusChangeListener { _: View, hasFocus: Boolean -> if (hasFocus) binding.actvKeyword.showDropDown() }

        // 가입 저장
        binding.btnSignUp.setOnClickListener {
            val nickname = binding.etNickname.text?.toString()?.trim().orEmpty()
            val birthStr = binding.etBirth.text?.toString()?.trim().orEmpty()
            val region   = binding.actvRegion.text?.toString()?.trim().orEmpty()
            val income   = binding.actvIncome.text?.toString()?.trim().orEmpty()
            val keyword  = binding.actvKeyword.text?.toString()?.trim().orEmpty()
            val agreeInfo   = binding.cbAgreeInfo.isChecked
            val agreeNotify = binding.cbAgreeNotify.isChecked

            if (nickname.isEmpty() || birthStr.isEmpty() || region.isEmpty() || income.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (!agreeInfo) {
                Toast.makeText(this, "개인 정보 수집에 동의해주세요.", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            // kakaoId 최종 확인
            if (kakaoId.isBlank()) {
                Toast.makeText(this, "저장 실패: 로그인 정보가 없습니다.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val birthDate: Date? = selectedBirthMillis?.let { Date(it) } ?: run {
                try { dateFormatter.parse(birthStr) } catch (_: Exception) { null }
            }
            if (birthDate == null) {
                Toast.makeText(this, "생년월일 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = hashMapOf(
                "nickname"     to nickname,
                "birth"        to birthDate,
                "region_label" to region,
                "income"       to income,
                "interest"     to keyword,
                "agree_info"   to agreeInfo,
                "agree_notify" to agreeNotify,
                "updatedAt"    to FieldValue.serverTimestamp()
            )

            FirebaseFirestore.getInstance()
                .collection("user")
                .document(kakaoId)            // ✅ Default로 저장하지 않음
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "저장 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
