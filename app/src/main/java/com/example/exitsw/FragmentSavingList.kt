package com.example.exitsw

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class FragmentSavingList : Fragment() {

    private var products: List<SavingProduct> = emptyList()
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_savinglist, container, false)
        fetchSavingProducts()

        rootView?.findViewById<FloatingActionButton>(R.id.btn_recommend)?.setOnClickListener {
            showRecommendationDialog()
        }
        return rootView!!
    }

    private fun fetchSavingProducts() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://finlife.fss.or.kr/finlifeapi/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(SavingApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getSavingProducts("2ade02d411b65c561f4dd75b9b9516d5")
                if (response.isSuccessful) {
                    products = response.body()?.result?.baseList ?: emptyList()
                    withContext(Dispatchers.Main) {
                        displayProducts(products)
                    }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "예외 발생: ${e.message}")
            }
        }
    }

    private fun displayProducts(list: List<SavingProduct>) {
        rootView?.findViewById<RecyclerView>(R.id.recycler_saving_products)?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SavingProductAdapter(list) { product ->
                val fragment = fragment_savingDetail().apply {
                    arguments = Bundle().apply {
                        putString("product_name", product.fin_prdt_nm)
                        putString("bank_name", product.kor_co_nm)
                        putString("note", product.etc_note)
                        // ⬇️ 금리 전달 (null이면 NaN으로 보냄)
                        putDouble("base_rate", product.intr_rate ?: Double.NaN)
                        putDouble("prefer_rate", product.intr_rate2 ?: Double.NaN)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun recommendSavingProducts(products: List<SavingProduct>, userInfo: UserInfo): List<SavingProduct> {
        return products.sortedByDescending { product ->
            var score = 0.0
            val note = product.etc_note?.lowercase() ?: ""

            if (userInfo.isSalaryTransferAvailable && note.contains("급여")) score += 1.5
            if (userInfo.isAutoTransferAvailable && note.contains("자동이체")) score += 1.2
            if (userInfo.isYouthBenefitEligible && note.contains("청년")) score += 1.3
            if (userInfo.hasCompletedFinancialEducation && note.contains("금융교육")) score += 1.1
            if (userInfo.isNonFaceToFaceAvailable && note.contains("비대면")) score += 1.0
            if (userInfo.isStudent && note.contains("대학생")) score += 1.0
            if (note.contains("만 ${userInfo.age}세") || note.contains("${userInfo.age}세")) score += 1.0

            score += (product.intr_rate2 ?: 0.0) * 2
            score
        }
    }

    private fun showRecommendationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_saving_recommend, null)

        // 필수 뷰를 먼저 안전하게 확보
        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_age_group)
        val monthlyInput = dialogView.findViewById<EditText>(R.id.edit_monthly_saving)

        // 스피너 셋업 (먼저 어댑터 세팅)
        val ageOptions = listOf("10대", "20대", "30대", "40대 이상")
        spinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ageOptions)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("추천 조건 입력")
            .setView(dialogView)
            .setPositiveButton("추천받기", null) // 일단 null로 두고 나중에 클릭 리스너 오버라이드
            .setNegativeButton("취소", null)
            .create()

        dialog.setOnShowListener {
            val okBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okBtn.setOnClickListener {
                try {
                    // 입력값 파싱 (null 안전)
                    val monthlySaving = monthlyInput?.text?.toString()?.toIntOrNull() ?: 0
                    val ageGroup = spinner?.selectedItem?.toString()
                    if (ageGroup == null) {
                        android.widget.Toast.makeText(requireContext(), "연령대를 선택하세요.", android.widget.Toast.LENGTH_SHORT).show()
                        Log.e("DIALOG", "spinner_age_group 가 null이거나 선택값이 없습니다.")
                        return@setOnClickListener
                    }

                    // 체크박스들 안전 수집
                    val checkedLabels = listOf(
                        R.id.check_auto_transfer to "자동이체",
                        R.id.check_non_face to "비대면",
                        R.id.check_salary_transfer to "급여이체",
                        R.id.check_youth to "청년",
                        R.id.check_financial_edu to "금융교육"
                    ).mapNotNull { (id, label) ->
                        val cb = dialogView.findViewById<CheckBox>(id)
                        if (cb == null) {
                            Log.e("DIALOG", "CheckBox id 를 찾지 못했습니다: $id (XML id 확인 필요)")
                            null
                        } else if (cb.isChecked) label else null
                    }

                    val userInfo = UserInfo(
                        age = when (ageGroup) {
                            "10대" -> 17
                            "20대" -> 25
                            "30대" -> 35
                            else -> 45
                        },
                        monthlySaving = monthlySaving,
                        isStudent = ageGroup == "10대" || ageGroup == "20대",
                        isSalaryTransferAvailable = "급여이체" in checkedLabels,
                        isAutoTransferAvailable = "자동이체" in checkedLabels,
                        isNonFaceToFaceAvailable = "비대면" in checkedLabels,
                        isYouthBenefitEligible = "청년" in checkedLabels,
                        hasCompletedFinancialEducation = "금융교육" in checkedLabels
                    )

                    val recommendedList = recommendSavingProducts(products, userInfo)
                    displayProducts(recommendedList)
                    dialog.dismiss()
                } catch (t: Throwable) {
                    Log.e("RECOMMEND", "추천 처리 중 오류", t)
                    android.widget.Toast.makeText(requireContext(), "입력 처리 중 오류가 발생했어요.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    // Retrofit API
    interface SavingApiService {
        @GET("savingProductsSearch.json")
        suspend fun getSavingProducts(
            @Query("auth") apiKey: String,
            @Query("topFinGrpNo") groupNo: String = "020000",
            @Query("pageNo") pageNo: Int = 1
        ): Response<SavingResponse>
    }

    data class SavingResponse(val result: ResultData)
    data class ResultData(val baseList: List<SavingProduct>)
    // ✅ SavingProduct는 외부 파일 SavingProduct.kt에서 import
}