package com.example.exitsw

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.exitsw.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EditProfileActivity : AppCompatActivity() {

    private val TAG = "EditProfile"

    // 초기값(변경 비교용)
    private var initNickname: String? = null
    private var initBirthYmd: String? = null
    private var initRegionLabel: String? = null
    private var initIncome: String? = null
    private var initInterest: String? = null

    // 뷰
    private lateinit var editNickname: TextInputEditText
    private lateinit var etBirth: TextInputEditText
    private lateinit var actvGender: MaterialAutoCompleteTextView
    private lateinit var actvRegion: MaterialAutoCompleteTextView
    private lateinit var actvIncome: MaterialAutoCompleteTextView
    private lateinit var actvKeyword: MaterialAutoCompleteTextView
    private lateinit var tilGender: TextInputLayout
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnGoWithdraw: MaterialButton   // ✅ 추가

    // 포맷터
    private val ymd by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
            timeZone = TimeZone.getDefault()
            isLenient = false
        }
    }

    // Firebase
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val user by lazy { FirebaseAuth.getInstance().currentUser }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()
        setupDropdowns()     // 드롭다운 어댑터 연결 & 자동 펼치기
        lockGenderField()    // 성별은 읽기 전용

        // 생년월일: 달력 선택 전용
        etBirth.inputType = InputType.TYPE_NULL
        etBirth.setOnClickListener { showDatePicker() }

        // Firestore에서 기본값 채우기
        loadProfileAndFill()

        // 저장
        btnEditProfile.setOnClickListener { saveProfile() }

        // ✅ 회원 탈퇴 화면으로 이동
        btnGoWithdraw.setOnClickListener {
            startActivity(Intent(this, WithdrawActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun bindViews() {
        editNickname   = findViewById(R.id.editNickname)
        etBirth        = findViewById(R.id.etBirth)
        actvGender     = findViewById(R.id.actvGender)
        actvRegion     = findViewById(R.id.actvRegion)
        actvIncome     = findViewById(R.id.actvIncome)
        actvKeyword    = findViewById(R.id.actvKeyword)
        tilGender      = findViewById(R.id.tilGender)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnGoWithdraw  = findViewById(R.id.btnGoWithdraw) // ✅ 추가
    }

    private fun setupDropdowns() {
        val regions  = resources.getStringArray(R.array.kor_regions)
        val incomes  = resources.getStringArray(R.array.income_brackets)
        val keywords = resources.getStringArray(R.array.interest_keywords)

        actvRegion.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, regions))
        actvIncome.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, incomes))
        actvKeyword.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, keywords))

        listOf(actvRegion, actvIncome, actvKeyword).forEach { tv ->
            tv.inputType = InputType.TYPE_NULL
            tv.keyListener = null
            tv.threshold = 0
            tv.setOnClickListener { tv.showDropDown() }
            tv.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) tv.showDropDown() }
        }
    }

    private fun lockGenderField() {
        tilGender.isEnabled = false
        actvGender.isEnabled = false
        actvGender.isFocusable = false
        actvGender.isClickable = false
        actvGender.inputType = InputType.TYPE_NULL
        actvGender.keyListener = null
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _: DatePicker, y: Int, m: Int, d: Int ->
                val mm = String.format("%02d", m + 1)
                val dd = String.format("%02d", d)
                etBirth.setText("$y-$mm-$dd")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /** Any → "YYYY-MM-DD" */
    private fun anyBirthToYmd(v: Any?): String? = when (v) {
        null -> null
        is String -> v
        is Timestamp -> ymd.format(v.toDate())
        is Date -> ymd.format(v)
        is Number -> {
            val ms = if (v.toLong() < 10_000_000_000L) v.toLong() * 1000 else v.toLong()
            ymd.format(Date(ms))
        }
        is Map<*, *> -> {
            val sec = (v["seconds"] as? Number)?.toLong()
            if (sec != null) ymd.format(Date(sec * 1000)) else null
        }
        else -> null
    }

    /** "YYYY-MM-DD" → Date (파싱 실패 시 null) */
    private fun parseYmdToDate(s: String): Date? = try { ymd.parse(s) } catch (_: Exception) { null }

    /** 만 나이 계산 */
    private fun calculateAge(birth: Date, now: Date = Date()): Int {
        val b = Calendar.getInstance().apply { time = birth }
        val n = Calendar.getInstance().apply { time = now }
        var age = n.get(Calendar.YEAR) - b.get(Calendar.YEAR)
        val mOk = n.get(Calendar.MONTH) > b.get(Calendar.MONTH)
        val dOk = n.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
                n.get(Calendar.DAY_OF_MONTH) >= b.get(Calendar.DAY_OF_MONTH)
        if (!(mOk || dOk)) age--
        return age.coerceAtLeast(0)
    }

    /** Firestore → 입력칸 기본값 세팅 */
    private fun loadProfileAndFill() {
        val u = user
        if (u == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "loadProfileAndFill: user is null")
            return
        }

        val projectId = FirebaseApp.getInstance().options.projectId
        val path = "user/${u.uid}"
        Log.d(TAG, "Firestore Project: $projectId, DocPath: $path")

        db.collection("user").document(u.uid).get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Log.w(TAG, "Document not found at $path")
                    FirebaseAuth.getInstance().currentUser?.displayName?.let { editNickname.setText(it) }
                    return@addOnSuccessListener
                }

                val nickname = snap.getString("nickname")
                val gender   = snap.getString("gender")
                val birthRaw = snap.get("birth")
                val region   = snap.getString("region_label")
                val income   = snap.getString("income")
                val interest = snap.getString("interest")

                val birthYmd = anyBirthToYmd(birthRaw)

                // 화면 채우기
                nickname?.let { editNickname.setText(it) }
                birthYmd?.let { etBirth.setText(it) }
                gender?.let   { actvGender.setText(it, false) }
                region?.let   { actvRegion.setText(it, false) }
                income?.let   { actvIncome.setText(it, false) }
                interest?.let { actvKeyword.setText(it, false) }

                // 초기값 저장(변경 비교용)
                initNickname    = nickname
                initBirthYmd    = birthYmd
                initRegionLabel = region
                initIncome      = income
                initInterest    = interest
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "loadProfileAndFill failed for $path", e)
                Toast.makeText(this, "프로필 불러오기에 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveProfile() {
        val u = user
        if (u == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "saveProfile: user is null")
            return
        }

        val path = "user/${u.uid}"
        val updates = mutableMapOf<String, Any>()

        fun putIfChanged(key: String, old: String?, new: String) {
            if (new.isNotBlank() && new != old) updates[key] = new
        }

        val newNickname = editNickname.text?.toString()?.trim().orEmpty()
        val newBirthStr = etBirth.text?.toString()?.trim().orEmpty()
        val newRegion   = actvRegion.text?.toString()?.trim().orEmpty()
        val newIncome   = actvIncome.text?.toString()?.trim().orEmpty()
        val newInterest = actvKeyword.text?.toString()?.trim().orEmpty()
        // gender는 읽기 전용

        putIfChanged("nickname",     initNickname,    newNickname)
        putIfChanged("region_label", initRegionLabel, newRegion)
        putIfChanged("income",       initIncome,      newIncome)
        putIfChanged("interest",     initInterest,    newInterest)

        // birth는 Date/Timestamp로 저장하고 age_years도 재계산
        if (newBirthStr.isNotBlank() && newBirthStr != initBirthYmd) {
            val birthDate = parseYmdToDate(newBirthStr)
            if (birthDate != null) {
                updates["birth"] = birthDate
                updates["age_years"] = calculateAge(birthDate)
            } else {
                Toast.makeText(this, "생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD)", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (updates.isEmpty()) {
            Toast.makeText(this, "변경된 내용이 없어요.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "No changes to update for $path")
            return
        }

        updates["updatedAt"] = FieldValue.serverTimestamp()

        Log.d(TAG, "Upserting $path with $updates")
        db.collection("user").document(u.uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "프로필을 수정했어요.", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK, Intent().putExtra("updated", true))
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "set(merge) failed for $path", e)
                Toast.makeText(this, "수정 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}
