package com.example.exitsw.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.exitsw.R
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

        showPlaceholderData()

        viewModel.groupedWelfareData.observe(viewLifecycleOwner) { groupedData ->
            val policyList = groupedData[selectedRegion]
            if (policyList != null) {
                policyAdapter.submitList(policyList)
            }
        }
    }

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
        // ✨ [핵심 수정] 어댑터를 생성할 때, 클릭 시 동작할 람다 함수를 전달
        policyAdapter = PolicyAdapter { policy ->
            // 클릭된 정책 데이터를 담아 PolicyDetailFragment로 전환
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PolicyDetailFragment.newInstance(policy))
                .addToBackStack(null)
                .commit()
        }
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
