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
import java.util.Calendar
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

    // ✅ LoginActivity에서 넘긴 Firebase Auth UID 사용
    private val uid: String by lazy {
        intent.getStringExtra(LoginActivity.EXTRA_UID)?.trim().orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 닉네임 프리필 (LoginActivity에서 전달한 displayName 등)
        intent.getStringExtra(LoginActivity.EXTRA_NICKNAME)
            ?.takeIf { it.isNotBlank() }
            ?.let { binding.etNickname.setText(it) }

        // ✅ UID 필수 검사 (없으면 회원가입 진행 불가)
        if (uid.isBlank()) {
            Toast.makeText(this, "로그인 정보가 유효하지 않습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
            return
        }

        // 1) 생년월일 DatePicker
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

        // 2) 드롭다운 연결 (성별/지역/소득/키워드)
        binding.actvGender.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                resources.getStringArray(R.array.gender_options).toList()
            )
        )
        binding.actvRegion.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                resources.getStringArray(R.array.kor_regions).toList()
            )
        )
        binding.actvIncome.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                resources.getStringArray(R.array.income_brackets).toList()
            )
        )
        binding.actvKeyword.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                resources.getStringArray(R.array.interest_keywords).toList()
            )
        )

        // 드롭다운 즉시 펼침 설정
        binding.actvGender.threshold = 0
        binding.actvRegion.threshold = 0
        binding.actvIncome.threshold = 0
        binding.actvKeyword.threshold = 0

        binding.actvGender.setOnClickListener { binding.actvGender.showDropDown() }
        binding.actvRegion.setOnClickListener { binding.actvRegion.showDropDown() }
        binding.actvIncome.setOnClickListener { binding.actvIncome.showDropDown() }
        binding.actvKeyword.setOnClickListener { binding.actvKeyword.showDropDown() }

        binding.actvGender.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (hasFocus) binding.actvGender.showDropDown()
        }
        binding.actvRegion.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (hasFocus) binding.actvRegion.showDropDown()
        }
        binding.actvIncome.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (hasFocus) binding.actvIncome.showDropDown()
        }
        binding.actvKeyword.setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            if (hasFocus) binding.actvKeyword.showDropDown()
        }

        // 3) 가입 저장
        binding.btnSignUp.setOnClickListener {
            val nickname = binding.etNickname.text?.toString()?.trim().orEmpty()
            val gender   = binding.actvGender.text?.toString()?.trim().orEmpty()
            val birthStr = binding.etBirth.text?.toString()?.trim().orEmpty()
            val region   = binding.actvRegion.text?.toString()?.trim().orEmpty()
            val income   = binding.actvIncome.text?.toString()?.trim().orEmpty()
            val keyword  = binding.actvKeyword.text?.toString()?.trim().orEmpty()
            val agreeInfo   = binding.cbAgreeInfo.isChecked
            val agreeNotify = binding.cbAgreeNotify.isChecked

            if (nickname.isEmpty() || gender.isEmpty() || birthStr.isEmpty() ||
                region.isEmpty() || income.isEmpty()
            ) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!agreeInfo) {
                Toast.makeText(this, "개인 정보 수집에 동의해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (uid.isBlank()) {
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

            // ✅ 만 나이 계산
            val ageYears = calculateAge(birthDate)

            val data = hashMapOf(
                "nickname"     to nickname,
                "gender"       to gender,                 // ✅ 성별 저장
                "birth"        to birthDate,              // Firestore Timestamp로 저장
                "age_years"    to ageYears,               // ✅ 만 나이 저장
                "region_label" to region,
                "income"       to income,
                "interest"     to keyword,
                "agree_info"   to agreeInfo,
                "agree_notify" to agreeNotify,
                "auth_uid"     to uid,                    // (선택) 추후 디버깅 편의
                "updatedAt"    to FieldValue.serverTimestamp()
            )

            FirebaseFirestore.getInstance()
                .collection("user")
                .document(uid)                             // ✅ 문서 ID = Firebase UID
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

    private fun calculateAge(birth: Date, now: Date = Date()): Int {
        val calBirth = Calendar.getInstance().apply { time = birth }
        val calNow   = Calendar.getInstance().apply { time = now }

        var age = calNow.get(Calendar.YEAR) - calBirth.get(Calendar.YEAR)

        val currMonth = calNow.get(Calendar.MONTH)
        val birthMonth = calBirth.get(Calendar.MONTH)
        val currDay = calNow.get(Calendar.DAY_OF_MONTH)
        val birthDay = calBirth.get(Calendar.DAY_OF_MONTH)

        if (currMonth < birthMonth || (currMonth == birthMonth && currDay < birthDay)) {
            age--
        }
        return age.coerceAtLeast(0)
    }
}
