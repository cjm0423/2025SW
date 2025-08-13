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
                // 홈 화면 미리보기용 지역 목록을 관찰
                viewModel.homeRegionList.observe(viewLifecycleOwner) { list ->
                        policyAdapter.submitList(list)
                }
        }

        private fun setupClickListeners() {
                // '지역별 지원 정책' 제목을 누르면 전체 지역 목록으로 이동
                binding.textPolicyTitle.setOnClickListener {
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, RegionListFragment())
                                .addToBackStack(null)
                                .commit()
                }
        }

        private fun setupRecyclerViews() {
                // 추천/인기 상품 어댑터 (클릭 시 동작 없음)
                recommendAdapter = HomeCardAdapter { /* TODO: 추천 상품 클릭 시 동작 */ }
                popularAdapter = HomeCardAdapter { /* TODO: 인기 상품 클릭 시 동작 */ }

                // 지역별 정책 어댑터 (클릭 시 화면 전환)
                policyAdapter = HomeCardAdapter { regionDto ->
                        // 클릭된 카드의 serviceName (지역 이름)을 PolicyListFragment로 전달
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, PolicyListFragment.newInstance(regionDto.serviceName ?: ""))
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
