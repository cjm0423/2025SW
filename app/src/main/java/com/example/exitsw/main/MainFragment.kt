package com.example.exitsw.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.R
import com.example.exitsw.databinding.FragmentMainBinding
import com.example.exitsw.EditProfileActivity   // ✅ 프로필 수정 화면 import

class MainFragment : Fragment() {

        private var _binding: FragmentMainBinding? = null
        private val binding get() = _binding!!
        private val viewModel: MainViewModel by activityViewModels()

        private lateinit var recommendAdapter: HomeCardAdapter
        private lateinit var popularAdapter: HomeCardAdapter
        private lateinit var policyAdapter: HomeCardAdapter

        // ✅ EditProfileActivity 결과를 받는 런처
        private val editProfileLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
        ) { result ->
                if (result.resultCode == Activity.RESULT_OK &&
                        result.data?.getBooleanExtra("updated", false) == true
                ) {
                        // 프로필이 변경됨 → 추천 다시 계산
                        viewModel.refreshRecommendations()
                }
        }

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

        override fun onResume() {
                super.onResume()
                 viewModel.refreshRecommendations()
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

                binding.textRecommendTitle.setOnClickListener {
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, RecommendListFragment())
                                .addToBackStack(null)
                                .commit()
                }

        }

        private fun setupRecyclerViews() {
                recommendAdapter = HomeCardAdapter { item ->
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, PolicyDetailFragment.newInstance(item))
                                .addToBackStack(null)
                                .commit()
                }

                popularAdapter = HomeCardAdapter { item ->
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, PolicyDetailFragment.newInstance(item))
                                .addToBackStack(null)
                                .commit()
                }

                policyAdapter = HomeCardAdapter { regionDto ->
                        parentFragmentManager.beginTransaction()
                                .replace(
                                        R.id.fragment_container,
                                        PolicyListFragment.newInstance(regionDto.servNm ?: "")
                                )
                                .addToBackStack(null)
                                .commit()
                }

                binding.recommendRecyclerView.apply {
                        adapter = recommendAdapter
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }
                binding.popularRecyclerView.apply {
                        adapter = popularAdapter
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }
                binding.policyRecyclerView.apply {
                        adapter = policyAdapter
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }
        }

        override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
        }
}
