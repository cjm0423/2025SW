package com.example.exitsw.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.FragmentPolicyListBinding

class PolicyListFragment : Fragment() {

    private var _binding: FragmentPolicyListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var policyAdapter: PolicyAdapter

    private var selectedRegion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedRegion = it.getString("regionName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPolicyListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupToolbar()

        // 1. API 응답을 기다리는 동안 플레이스홀더를 먼저 보여줍니다.
        showPlaceholderData()

        // 2. ViewModel의 그룹핑된 데이터 전체를 관찰
        viewModel.groupedWelfareData.observe(viewLifecycleOwner) { groupedData ->
            val policyList = groupedData[selectedRegion]
            if (policyList != null) {
                // 실제 데이터가 있으면 플레이схолдер를 대체
                policyAdapter.submitList(policyList)
            }
            // 데이터가 없다면 (API 로딩 전이거나 실패 시) 플레이схолдер가 계속 보임
        }
    }

    // 플레이схолдер 데이터를 보여주는 함수
    private fun showPlaceholderData() {
        val placeholder = LocalWelfareServiceDto("LOADING", "로딩 중...", "데이터를 불러오고 있습니다.", null, null, null, null)
        policyAdapter.submitList(listOf(placeholder, placeholder, placeholder, placeholder, placeholder))
    }

    private fun setupToolbar() {
        binding.toolbar.title = selectedRegion
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        policyAdapter = PolicyAdapter()
        binding.policyRecyclerView.adapter = policyAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(regionName: String) =
            PolicyListFragment().apply {
                arguments = Bundle().apply {
                    putString("regionName", regionName)
                }
            }
    }
}
