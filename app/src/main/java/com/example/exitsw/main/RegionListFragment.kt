package com.example.exitsw.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.exitsw.R
import com.example.exitsw.databinding.FragmentRegionListBinding

class RegionListFragment : Fragment() {

    private var _binding: FragmentRegionListBinding? = null
    private val binding get() = _binding!!
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

        // ViewModel의 지역 정보 목록(regionInfoList)을 관찰
        viewModel.regionInfoList.observe(viewLifecycleOwner) { regionInfoList ->
            regionAdapter.submitList(regionInfoList)
        }

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        // 어댑터를 생성할 때, 클릭 시 동작할 람다 함수를 전달
        regionAdapter = RegionAdapter { regionInfo ->
            // 클릭된 지역의 이름을 담아 PolicyListFragment로 전환
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PolicyListFragment.newInstance(regionInfo.name))
                .addToBackStack(null)
                .commit()
        }
        binding.regionRecyclerView.adapter = regionAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
