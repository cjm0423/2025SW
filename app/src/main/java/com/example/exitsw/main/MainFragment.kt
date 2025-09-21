package com.example.exitsw.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.R
import com.example.exitsw.databinding.FragmentMainBinding

class MainFragment : Fragment() {

        private var _binding: FragmentMainBinding? = null
        private val binding get() = _binding!!
        private val viewModel: MainViewModel by activityViewModels()

        private lateinit var recommendAdapter: HomeCardAdapter
        private lateinit var popularAdapter: HomeCardAdapter
        private lateinit var policyAdapter: HomeCardAdapter

        override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View {
                _binding = FragmentMainBinding.inflate(inflater, container, false)
                return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                setupRecyclerViews()
                setupClickListeners()
                observeViewModel()
        }

        private fun observeViewModel() {
                viewModel.recommendList.observe(viewLifecycleOwner) { list ->
                        recommendAdapter.submitList(list)
                }
                viewModel.popularList.observe(viewLifecycleOwner) { list ->
                        popularAdapter.submitList(list)
                }
                viewModel.homeRegionList.observe(viewLifecycleOwner) { list ->
                        policyAdapter.submitList(list)
                }
        }

        private fun setupClickListeners() {
                binding.textPolicyTitle.setOnClickListener {
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, RegionListFragment())
                                .addToBackStack(null)
                                .commit()
                }

                binding.textPopularTitle.setOnClickListener {
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, PopularListFragment())
                                .addToBackStack(null)
                                .commit()
                }
        }

        private fun setupRecyclerViews() {
                recommendAdapter = HomeCardAdapter { /* TODO: 추천 상품 클릭 시 동작 */ }

                popularAdapter = HomeCardAdapter { item ->
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, PolicyDetailFragment.newInstance(item))
                                .addToBackStack(null)
                                .commit()}

                policyAdapter = HomeCardAdapter { regionDto ->
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, PolicyListFragment.newInstance(regionDto.servNm ?: ""))
                                .addToBackStack(null)
                                .commit()
                }

                binding.recommendRecyclerView.adapter = recommendAdapter
                binding.popularRecyclerView.adapter = popularAdapter
                binding.policyRecyclerView.adapter = policyAdapter

                binding.recommendRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.popularRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.policyRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
        }
}