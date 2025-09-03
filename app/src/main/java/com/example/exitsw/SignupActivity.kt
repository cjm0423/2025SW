package com.example.exitsw

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.exitsw.databinding.ActivitySignupBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    private val dateFormatter by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 닉네임 프리필
        intent.getStringExtra("nickname")
            ?.takeIf { it.isNotBlank() }
            ?.let { binding.etNickname.setText(it) }

        // 1) 생년월일: MaterialDatePicker
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("생년월일 선택")
            .setCalendarConstraints(constraints)
            .build()

        binding.etBirth.setOnClickListener {
            if (!datePicker.isAdded) {
                datePicker.show(supportFragmentManager, "birth_picker")
            }
        }
        datePicker.addOnPositiveButtonClickListener { millis ->
            binding.etBirth.setText(dateFormatter.format(Date(millis)))
        }

        // 2) 드롭다운 어댑터 명시적으로 연결 (가장 확실한 방법)
        val regions = resources.getStringArray(R.array.kor_regions).toList()
        val incomes = resources.getStringArray(R.array.income_brackets).toList()
        val keywords = resources.getStringArray(R.array.interest_keywords).toList()

        binding.actvRegion.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, regions)
        )
        binding.actvIncome.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, incomes)
        )
        binding.actvKeyword.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, keywords)
        )

        // 입력 없이도 열리게 (threshold=0) + 클릭/포커스 시 바로 펼치기
        binding.actvRegion.threshold = 0
        binding.actvIncome.threshold = 0
        binding.actvKeyword.threshold = 0

        binding.actvRegion.setOnClickListener { binding.actvRegion.showDropDown() }
        binding.actvIncome.setOnClickListener { binding.actvIncome.showDropDown() }
        binding.actvKeyword.setOnClickListener { binding.actvKeyword.showDropDown() }

        binding.actvRegion.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) binding.actvRegion.showDropDown()
        }
        binding.actvIncome.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) binding.actvIncome.showDropDown()
        }
        binding.actvKeyword.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) binding.actvKeyword.showDropDown()
        }

        // 3) 가입 버튼
        binding.btnSignUp.setOnClickListener {
            val nickname = binding.etNickname.text?.toString()?.trim().orEmpty()
            val birth    = binding.etBirth.text?.toString()?.trim().orEmpty()
            val region   = binding.actvRegion.text?.toString()?.trim().orEmpty()
            val income   = binding.actvIncome.text?.toString()?.trim().orEmpty()
            val keyword  = binding.actvKeyword.text?.toString()?.trim().orEmpty()
            val agreeInfo   = binding.cbAgreeInfo.isChecked
            val agreeNotify = binding.cbAgreeNotify.isChecked

            if (nickname.isEmpty() || birth.isEmpty() || region.isEmpty() || income.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!agreeInfo) {
                Toast.makeText(this, "개인 정보 수집에 동의해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 서버 전송 (nickname, birth, region, income, keyword, agreeNotify)

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
