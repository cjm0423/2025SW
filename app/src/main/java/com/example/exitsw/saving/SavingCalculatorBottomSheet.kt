package com.example.exitsw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import kotlin.math.pow

class SavingCalculatorBottomSheet : BottomSheetDialogFragment() {

    private var baseRate: Double? = null   // 연 %
    private var preferRate: Double? = null // 연 %

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseRate = arguments?.getDouble(ARG_BASE_RATE)?.takeIf { it > 0.0 }
        preferRate = arguments?.getDouble(ARG_PREFER_RATE)?.takeIf { it > 0.0 }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.bottomsheet_saving_calculator, container, false)

        val tilMonthly = v.findViewById<TextInputLayout>(R.id.tilMonthly)
        val inputMonthly = v.findViewById<TextInputEditText>(R.id.inputMonthly)
        val tilMonths = v.findViewById<TextInputLayout>(R.id.tilMonths)
        val inputMonths = v.findViewById<TextInputEditText>(R.id.inputMonths)

        val radioGroup = v.findViewById<RadioGroup>(R.id.radioRateGroup)
        //val radioBase = v.findViewById<MaterialRadioButton>(R.id.radioBase)
        //val radioPrefer = v.findViewById<MaterialRadioButton>(R.id.radioPrefer)
        val radioCustom = v.findViewById<MaterialRadioButton>(R.id.radioCustom)

        val tilCustomRate = v.findViewById<TextInputLayout>(R.id.tilCustomRate)
        val inputCustomRate = v.findViewById<TextInputEditText>(R.id.inputCustomRate)

        val spinnerTiming = v.findViewById<Spinner>(R.id.spinnerTiming)
        val spinnerTax = v.findViewById<Spinner>(R.id.spinnerTax)
        val tilCustomTax = v.findViewById<TextInputLayout>(R.id.tilCustomTax)
        val inputCustomTax = v.findViewById<TextInputEditText>(R.id.inputCustomTax)

        val btnCalc = v.findViewById<MaterialButton>(R.id.btnCalc)
        val txtResult = v.findViewById<TextView>(R.id.txtResult)

        // 라벨에 금리 노출
        /*
        radioBase.text = "기본금리" + (baseRate?.let { " (${trim(it)}%)" } ?: " (미제공)")
        radioPrefer.text = "우대금리" + (preferRate?.let { " (${trim(it)}%)" } ?: " (미제공)")

        // 금리 옵션 활성/비활성
        radioBase.isEnabled = baseRate != null
        radioPrefer.isEnabled = preferRate != null
        // 기본 선택: 우대 있으면 우대, 없으면 기본, 없으면 직접입력
        when {
            preferRate != null -> radioPrefer.isChecked = true
            baseRate != null -> radioBase.isChecked = true
            else -> {
                radioCustom.isChecked = true
                tilCustomRate.visibility = View.VISIBLE
            }
        }

         */

        // 라디오 변경 시 직접입력란 표시/숨김
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            tilCustomRate.visibility = if (checkedId == R.id.radioCustom) View.VISIBLE else View.GONE
        }

        // 스피너 셋업
        spinnerTiming.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("후불(월말 납입)", "선불(월초 납입)")
        )

        val taxItems = listOf("일반과세(15.4%)", "세금우대(9.5%)", "비과세(0%)", "사용자 지정")
        spinnerTax.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, taxItems)
        spinnerTax.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                tilCustomTax.visibility = if (taxItems[pos] == "사용자 지정") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        btnCalc.setOnClickListener {
            // 입력값 검증
            val monthly = inputMonthly.text?.toString()?.replace(",", "")?.toDoubleOrNull()
            val months = inputMonths.text?.toString()?.toIntOrNull()
            if (monthly == null || monthly <= 0) {
                tilMonthly.error = "월 납입액을 입력하세요"
                return@setOnClickListener
            } else tilMonthly.error = null

            if (months == null || months <= 0) {
                tilMonths.error = "가입 개월 수를 입력하세요"
                return@setOnClickListener
            } else tilMonths.error = null

            val selAnnualRate = when (radioGroup.checkedRadioButtonId) {
                //ㅂㅈㄷR.id.radioBase -> baseRate
                //R.id.radioPrefer -> preferRate
                else -> inputCustomRate.text?.toString()?.toDoubleOrNull()
            }
            if (selAnnualRate == null || selAnnualRate <= 0.0) {
                tilCustomRate.error = "금리를 확인하세요"
                return@setOnClickListener
            } else tilCustomRate.error = null

            val taxRate = when (spinnerTax.selectedItem as String) {
                "일반과세(15.4%)" -> 15.4
                "세금우대(9.5%)" -> 9.5
                "비과세(0%)" -> 0.0
                else -> inputCustomTax.text?.toString()?.toDoubleOrNull() ?: run {
                    tilCustomTax.error = "세율을 입력하세요"
                    return@setOnClickListener
                }
            }

            val timingIsDueBeginning = (spinnerTiming.selectedItem as String).startsWith("선불") // 월초 납입=선불(Annuity Due)

            val result = calcInstallmentSaving(
                monthly = monthly.toDouble(),
                months = months,
                annualRatePercent = selAnnualRate,
                taxPercent = taxRate,
                isAnnuityDue = timingIsDueBeginning
            )

            txtResult.text = buildString {
                appendLine("총 납입원금: ${won(monthly * months)}")
                appendLine("세전 이자: ${won(result.grossInterest)}")
                appendLine("세후 이자: ${won(result.netInterest)}")
                appendLine("만기 수령액(세후): ${won(result.maturityAfterTax)}")
            }
        }

        return v



    }

    // 계산 로직
    data class SavingCalcResult(
        val grossInterest: Double,
        val netInterest: Double,
        val maturityAfterTax: Double
    )

    /**
     * 적금(자유x, 정액) 월불입, 연 이율(%) → 월복리 가정
     * isAnnuityDue=true 이면 월초 납입(선불, annuity-due), false면 월말 납입(후불).
     */
    private fun calcInstallmentSaving(
        monthly: Double,
        months: Int,
        annualRatePercent: Double,
        taxPercent: Double,
        isAnnuityDue: Boolean
    ): SavingCalcResult {
        val i = (annualRatePercent / 100.0) / 12.0 // 월이율
        val principal = monthly * months

        val fvFactor = if (i == 0.0) {
            months.toDouble()
        } else {
            ((1 + i).pow(months) - 1) / i
        }

        val dueFactor = if (isAnnuityDue) (1 + i) else 1.0
        val futureValue = monthly * fvFactor * dueFactor  // 세전 만기금액
        val grossInterest = (futureValue - principal).coerceAtLeast(0.0)

        val netInterest = grossInterest * (1 - taxPercent / 100.0)
        val maturityAfterTax = principal + netInterest

        return SavingCalcResult(
            grossInterest = grossInterest,
            netInterest = netInterest,
            maturityAfterTax = maturityAfterTax
        )
    }

    // 유틸
    private fun won(v: Double): String =
        NumberFormat.getInstance().format(kotlin.math.round(v)) + "원"

    private fun trim(rate: Double): String {
        // 3.5 처럼 깔끔하게
        val s = String.format("%.3f", rate).trimEnd('0').trimEnd('.')
        return s
    }

    companion object {
        private const val ARG_BASE_RATE = "base_rate"
        private const val ARG_PREFER_RATE = "prefer_rate"

        fun newInstance(baseRate: Double?, preferRate: Double?): SavingCalculatorBottomSheet {
            return SavingCalculatorBottomSheet().apply {
                arguments = Bundle().apply {
                    if (baseRate != null) putDouble(ARG_BASE_RATE, baseRate)
                    if (preferRate != null) putDouble(ARG_PREFER_RATE, preferRate)
                }
            }
        }
    }
}