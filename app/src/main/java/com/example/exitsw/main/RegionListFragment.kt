package com.example.exitsw.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.exitsw.databinding.FragmentRegionListBinding

class RegionListFragment : Fragment() {

    private var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!
    // activityViewModels()를 사용해 MainActivity의 ViewModel을 공유
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var regionAdapter: RegionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // ViewModel의 지역 목록(regionList)을 관찰
        viewModel.regionList.observe(viewLifecycleOwner) { regionList ->
            regionAdapter.submitList(regionList)
        }

        // 툴바 뒤로가기 버튼 클릭 리스너
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        regionAdapter = RegionAdapter()
        binding.regionRecyclerView.adapter = regionAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
