package com.example.exitsw.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.R
import com.example.exitsw.data.LocalWelfareServiceDto
import com.example.exitsw.databinding.FragmentPolicyListBinding

class PolicyListFragment : Fragment() {

    private var _binding: FragmentPolicyListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var policyAdapter: PolicyAdapter

    private var selectedRegion: String? = null
    private var filterLink: String? = null
    private var filterTitle: String? = null
    private var autoOpenDetail: Boolean = false
    private var handledDeepFocus = false

    private var currentList: List<LocalWelfareServiceDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedRegion   = it.getString("regionName")
            filterLink       = it.getString("filterLink")
            filterTitle      = it.getString("filterTitle")
            autoOpenDetail   = it.getBoolean("autoOpenDetail", false)
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

        viewModel.groupedWelfareData.observe(viewLifecycleOwner) { grouped ->
            val list = if (!selectedRegion.isNullOrBlank()) {
                grouped[selectedRegion].orEmpty()
            } else {
                grouped.values
                    .flatten()
                    .distinctBy { it.servId ?: ((it.servNm ?: "") + (it.servDtlLink ?: "")) }
                    .sortedBy { it.servNm ?: "" }
            }
            submitListAndMaybeFocus(list)
        }
    }

    private fun setupRecyclerView() {
        policyAdapter = PolicyAdapter { policy ->
            openDetail(policy)
        }
        binding.policyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = policyAdapter
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = selectedRegion ?: "전체 지역"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun showPlaceholderData() {
        val placeholder = LocalWelfareServiceDto(
            servId = "LOADING",
            servNm = "로딩 중...",
            bizChrDeptNm = "데이터를 불러오고 있습니다.",
            servDgst = null,
            servDtlLink = null,
            ctpvNm = null,
            intrsThemaNmArray = null
        )
        policyAdapter.submitList(listOf(placeholder, placeholder, placeholder, placeholder, placeholder))
    }

    private fun submitListAndMaybeFocus(list: List<LocalWelfareServiceDto>) {
        currentList = list
        policyAdapter.submitList(list)
        deeplinkFocusIfNeeded()
    }

    private fun deeplinkFocusIfNeeded() {
        if (handledDeepFocus) return
        if (currentList.isEmpty()) return
        if (filterLink.isNullOrBlank() && filterTitle.isNullOrBlank()) return

        var idx = -1
        filterLink?.trim()?.takeIf { it.isNotEmpty() }?.let { link ->
            idx = currentList.indexOfFirst { (it.servDtlLink ?: "").equals(link, ignoreCase = true) }
        }
        if (idx < 0) {
            filterTitle?.trim()?.takeIf { it.isNotEmpty() }?.let { title ->
                idx = currentList.indexOfFirst { (it.servNm ?: "").contains(title, ignoreCase = true) }
            }
        }
        if (idx < 0) return

        handledDeepFocus = true
        binding.policyRecyclerView.post {
            binding.policyRecyclerView.scrollToPosition(idx)
            if (autoOpenDetail) {
                currentList.getOrNull(idx)?.let { openDetail(it) }
            }
        }
    }

    private fun openDetail(item: LocalWelfareServiceDto) {
        val detail = PolicyDetailFragment.newInstance(item)
        // ✅ replace → add + hide(this): 리스트 스크롤 상태 유지
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.fragment_container, detail, "PolicyDetail")
            .hide(this)
            .addToBackStack("PolicyDetail")
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(regionName: String) =
            PolicyListFragment().apply {
                arguments = Bundle().apply { putString("regionName", regionName) }
            }

        fun newInstanceForDeeplink(
            regionName: String? = null,
            filterLink: String? = null,
            filterTitle: String? = null,
            autoOpenDetail: Boolean = true
        ) = PolicyListFragment().apply {
            arguments = Bundle().apply {
                putString("regionName", regionName)
                putString("filterLink", filterLink)
                putString("filterTitle", filterTitle)
                putBoolean("autoOpenDetail", autoOpenDetail)
            }
        }
    }
}
