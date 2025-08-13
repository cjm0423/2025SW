package com.example.exitsw.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.FragmentPolicyDetailBinding

class PolicyDetailFragment : Fragment() {

    private var _binding: FragmentPolicyDetailBinding? = null
    private val binding get() = _binding!!

    private var policy: LocalWelfareServiceDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 데이터를 전달받는 부분
        arguments?.let {
            policy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable("policy", LocalWelfareServiceDto::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable("policy")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolicyDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전달받은 policy 데이터가 null이 아닐 때 UI 업데이트
        policy?.let { policyData ->
            binding.toolbar.title = policyData.serviceName
            binding.textPolicyName.text = policyData.serviceName
            binding.textPolicyAgency.text = policyData.department
            binding.textPolicySummary.text = policyData.summary

            // "사이트로 이동" 버튼 클릭 리스너
            binding.btnGoToSite.setOnClickListener {
                // [수정된 부분] it.detailLink가 아닌 policyData.detailLink로 올바르게 참조
                policyData.detailLink?.let { url ->
                    if (url.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
                }
            }
        }

        // 툴바의 뒤로가기 버튼
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(policy: LocalWelfareServiceDto) =
            PolicyDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("policy", policy)
                }
            }
    }
}