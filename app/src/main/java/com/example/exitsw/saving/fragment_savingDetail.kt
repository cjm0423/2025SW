package com.example.exitsw.saving

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.exitsw.R
import com.example.exitsw.SavingCalculatorBottomSheet
import com.example.exitsw.saving.BankLogoUtil

// 기존 템플릿에서 쓰던 파라미터 키 유지
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * Saving 상세 화면
 * - 리스트에서 전달한 상품명/은행명/비고, 금리(base/prefer)를 표시
 * - 계산하기 버튼 클릭 시 SavingCalculatorBottomSheet 호출
 */
class fragment_savingDetail : Fragment() {

    // 템플릿에서 유지하던 필드(필요 없으면 제거 가능)
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_saving_detail, container, false)

        // 리스트에서 전달받은 데이터 꺼내기
        val productName = arguments?.getString("product_name")
        val bankName = arguments?.getString("bank_name")
        val note = arguments?.getString("note")

        // 금리(Double) 꺼내기 (NaN 방지 처리)
        val baseRateRaw = arguments?.getDouble("base_rate", Double.NaN) ?: Double.NaN
        val preferRateRaw = arguments?.getDouble("prefer_rate", Double.NaN) ?: Double.NaN
        val baseRate: Double? = baseRateRaw.takeIf { it.isFinite() && it > 0.0 }
        val preferRate: Double? = preferRateRaw.takeIf { it.isFinite() && it > 0.0 }

        // UI 바인딩
        val nameTextView = view.findViewById<TextView>(R.id.text_saving_name)
        val bankTextView = view.findViewById<TextView>(R.id.text_bank_name)
        val noteTextView = view.findViewById<TextView>(R.id.text_note)
        // ** 추가: 은행 로고를 표시할 ImageView 바인딩 **
        val bankLogoImageView = view.findViewById<ImageView>(R.id.imageDetailBankLogo)

        nameTextView.text = productName ?: "-"
        bankTextView.text = bankName ?: "-"
        noteTextView.text = note ?: "-"

        // ** 추가: BankLogoUtil을 사용하여 로고 이미지 설정 **
        val logoResId = BankLogoUtil().getBankLogoRes(bankName)
        bankLogoImageView.setImageResource(logoResId)

        // 계산하기 버튼 → 바텀시트 호출
        view.findViewById<View>(R.id.btn_calculate)?.setOnClickListener {
            SavingCalculatorBottomSheet
                .newInstance(baseRate = baseRate, preferRate = preferRate)
                .show(parentFragmentManager, "saving_calc")
        }
        // 신청하기 버튼 → homp_url로 외부 브라우저 열기
        view.findViewById<View>(R.id.btnSave)?.setOnClickListener {
            val rawUrl = arguments?.getString("apply_url")?.trim()
            val finalUrl = rawUrl?.takeIf { it.isNotBlank() }?.let { url ->
                if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            }

            if (finalUrl.isNullOrBlank()) {
                android.widget.Toast.makeText(requireContext(), "신청 URL이 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(finalUrl)
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(requireContext(), "URL을 열 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    companion object {
        /**
         * (옵션) 템플릿에서 유지하던 팩토리 메서드
         * 필요 시 외부에서 param1/param2를 넣어 생성할 때 사용하세요.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            fragment_savingDetail().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
        }
    }