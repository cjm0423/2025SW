package com.example.exitsw.saving

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exitsw.R
import com.example.exitsw.SavingProduct
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.Toast
import com.example.exitsw.SavingProductAdapter

class fragment_savinglist : Fragment() {

    private var products: List<SavingProduct> = emptyList()
    private var rootView: View? = null
    private var bankUrlMap: Map<String, String> = emptyMap()

    private val onProductClick: (SavingProduct) -> Unit = { product ->
        val bankName = product.kor_co_nm?.trim().orEmpty()
        val matchedUrl = bankUrlMap[bankName]  // 회사 API로부터 매칭된 URL

        Log.d("SavingList", "clicked: ${product.fin_prdt_nm}, bank=${bankName}, matchedUrl=$matchedUrl, raw_homp=${product.homp_url}")

        val fragment = fragment_savingDetail().apply {
            arguments = Bundle().apply {
                putString("product_name", product.fin_prdt_nm)
                putString("bank_name", product.kor_co_nm)
                putString("note", product.etc_note)
                putDouble("base_rate", product.intr_rate ?: Double.NaN)
                putDouble("prefer_rate", product.intr_rate2 ?: Double.NaN)

                // ✅ 핵심: 매칭된 URL을 apply_url로 전달
                putString("apply_url", matchedUrl)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_savinglist, container, false)

        // 리스트 처음 띄우기
        setupRecycler()
        fetchSavingProducts()

        // 추천(FAB)
        rootView?.findViewById<FloatingActionButton>(R.id.btn_recommend)?.setOnClickListener {
            showRecommendationDialog()
        }
        return rootView!!
    }

    // ───────── RecyclerView 준비 ─────────
    private fun setupRecycler() {
        rootView?.findViewById<RecyclerView>(R.id.recycler_saving_products)?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SavingProductAdapter(products, onProductClick)
        }
    }

    private fun refreshRecycler(newList: List<SavingProduct>) {
        val rv = rootView?.findViewById<RecyclerView>(R.id.recycler_saving_products) ?: return
        rv.adapter = SavingProductAdapter(newList, onProductClick)
    }

    // ───────── API 호출 ─────────
    private fun fetchSavingProducts() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://finlife.fss.or.kr/finlifeapi/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(SavingApiService::class.java)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("SavingList", "API 호출 시작: getSavingProducts()")
                val response = apiService.getSavingProducts("2ade02d411b65c561f4dd75b9b9516d5")
                Log.d("SavingList", "HTTP status=${response.code()} success=${response.isSuccessful}")

                if (!response.isSuccessful) {
                    Log.e("API_ERROR", "실패 코드: ${response.code()} / ${response.message()}")
                    return@launch
                }

                val productList = response.body()?.result?.baseList.orEmpty()
                Log.i("SavingList", "불러온 상품 수=${productList.size}")

                // ✅ 2단계: 회사 목록 조회 (회사 homp_url 확보)
                val companyRes = apiService.getCompanies("2ade02d411b65c561f4dd75b9b9516d5")
                val companyList = companyRes.body()?.result?.baseList.orEmpty()
                // 은행명 -> homp_url 매핑
                val urlByBank: Map<String, String> =
                    companyList
                        .mapNotNull { it.kor_co_nm?.trim()?.let { name -> name to (it.homp_url?.trim() ?: "") } }
                        .filter { it.second.isNotBlank() }
                        .toMap()

                Log.d("SavingList", "회사 수=${companyList.size}, URL 보유 회사 수=${urlByBank.size}")

                // ✅ 3단계: 상품에 URL 매칭 (상품의 homp_url이 null이므로, 은행명으로 채움)
                //  - 현재 SavingProduct에는 homp_url 필드가 있지만 응답에서 null이므로,
                //    상세 화면 인자로 넘길 때 이 매칭 URL을 사용합니다.
                //  - RecyclerView 표시용 리스트는 그대로 products에 저장.
                products = productList

                // 검증 로그: 샘플 3건 매칭 결과
                val sample = productList.take(3).joinToString {
                    val bank = it.kor_co_nm?.trim().orEmpty()
                    val u = urlByBank[bank]
                    "[${it.fin_prdt_nm} / ${bank} / matched_url=${u}]"
                }
                Log.d("SavingList", "샘플 매칭: $sample")

                withContext(Dispatchers.Main) {
                    refreshRecycler(products)

                    // 클릭 시 사용할 매핑을 onProductClick에서 접근할 수 있도록 태워두는 방법 2가지
                    // (1) 전역 프로퍼티로 저장
                    bankUrlMap = urlByBank
                    // (2) 또는 어댑터에 넘겨서 ViewHolder에서 바로 사용하도록 전달 (현재 구조는 (1)로 진행)
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "예외 발생: ${e.message}", e)
            }
        }
    }

    // ───────── 추천 로직 ─────────
    private fun recommendSavingProducts(list: List<SavingProduct>, userInfo: UserInfo): List<SavingProduct> {
        return list.sortedByDescending { product ->
            var score = 0.0
            val note = product.etc_note?.lowercase() ?: ""
            if (userInfo.isSalaryTransferAvailable && note.contains("급여")) score += 1.5
            if (userInfo.isAutoTransferAvailable && note.contains("자동이체")) score += 1.2
            if (userInfo.isYouthBenefitEligible && note.contains("청년")) score += 1.3
            if (userInfo.hasCompletedFinancialEducation && note.contains("금융교육")) score += 1.1
            if (userInfo.isNonFaceToFaceAvailable && note.contains("비대면")) score += 1.0
            if (userInfo.isStudent && note.contains("대학생")) score += 1.0
            if (note.contains("만 ${userInfo.age}세") || note.contains("${userInfo.age}세")) score += 1.0
            score += (product.intr_rate2 ?: 0.0) * 2 // 우대금리 가중
            score
        }
    }

    // ───────── 추천 다이얼로그 ─────────
    private fun showRecommendationDialog() {
        // 네가 만든 다이얼로그 레이아웃 id가 다르면 맞춰서 수정
        val dialogView = layoutInflater.inflate(R.layout.dialog_saving_recommend, null)

        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_age_group)
        val monthlyInput = dialogView.findViewById<EditText>(R.id.edit_monthly_saving)

        val ageOptions = listOf("10대", "20대", "30대", "40대 이상")
        spinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ageOptions)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("추천 조건 입력")
            .setView(dialogView)
            .setPositiveButton("추천받기", null)
            .setNegativeButton("취소", null)
            .create()

        dialog.setOnShowListener {
            val okBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okBtn.setOnClickListener {
                try {
                    val monthlySaving = monthlyInput?.text?.toString()?.toIntOrNull() ?: 0
                    val ageGroup = spinner?.selectedItem?.toString()
                    if (ageGroup == null) {
                        Toast.makeText(requireContext(), "연령대를 선택하세요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val checkedLabels = listOf(
                        R.id.check_auto_transfer to "자동이체",
                        R.id.check_non_face to "비대면",
                        R.id.check_salary_transfer to "급여이체",
                        R.id.check_youth to "청년",
                        R.id.check_financial_edu to "금융교육"
                    ).mapNotNull { (id, label) ->
                        val cb = dialogView.findViewById<CheckBox>(id)
                        if (cb?.isChecked == true) label else null
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

                    val recommended = recommendSavingProducts(products, userInfo)
                    refreshRecycler(recommended)
                    dialog.dismiss()
                } catch (t: Throwable) {
                    Log.e("RECOMMEND", "추천 처리 오류", t)
                    Toast.makeText(requireContext(), "입력 처리 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    // ───────── Retrofit 정의 ─────────
    interface SavingApiService {
        @GET("savingProductsSearch.json")
        suspend fun getSavingProducts(
            @Query("auth") apiKey: String,
            @Query("topFinGrpNo") groupNo: String = "020000",
            @Query("pageNo") pageNo: Int = 1
        ): Response<SavingResponse>

        // ✅ 추가: 금융회사 조회 (회사 homp_url 제공)
        @GET("companySearch.json")
        suspend fun getCompanies(
            @Query("auth") apiKey: String,
            @Query("topFinGrpNo") groupNo: String = "020000",
            @Query("pageNo") pageNo: Int = 1
        ): Response<CompanyResponse>
    }

    data class SavingResponse(val result: ResultData)
    data class ResultData(val baseList: List<SavingProduct>)

    // ✅ 추가: 회사 응답 DTO
    data class CompanyResponse(val result: CompanyResult)
    data class CompanyResult(val baseList: List<CompanyItem>)
    data class CompanyItem(
        val dcls_month: String?,
        val fin_co_no: String?,
        val kor_co_nm: String?,     // 은행/금융사 이름
        val homp_url: String?,      // ★ 우리가 필요한 URL
        val cal_tel: String?
    )

    // ───────── 사용자 입력 상태 ─────────
    data class UserInfo(
        val age: Int,
        val monthlySaving: Int,
        val isStudent: Boolean,
        val isSalaryTransferAvailable: Boolean,
        val isAutoTransferAvailable: Boolean,
        val isNonFaceToFaceAvailable: Boolean,
        val isYouthBenefitEligible: Boolean,
        val hasCompletedFinancialEducation: Boolean
    )
}