package com.example.exitsw.main

import android.os.Bundle
import android.util.Log
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
        // activityViewModels()를 사용해 MainActivity의 ViewModel을 공유
        private val viewModel: MainViewModel by activityViewModels()

        // 3개의 목록을 위한 어댑터 변수 선언
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

        // ViewModel의 데이터 변화를 관찰하는 함수
        private fun observeViewModel() {
                // 1. 추천 상품 데이터 관찰 및 어댑터에 전달
                viewModel.recommendList.observe(viewLifecycleOwner) { recommendList ->
                        recommendAdapter.submitList(recommendList)
                }

                // 2. 인기 상품 데이터 관찰 및 어댑터에 전달
                viewModel.popularList.observe(viewLifecycleOwner) { popularList ->
                        popularAdapter.submitList(popularList)
                }

                // 3. 지역별 지원 정책 데이터 관찰 및 어댑터에 전달
                viewModel.welfareList.observe(viewLifecycleOwner) { welfareList ->
                        policyAdapter.submitList(welfareList)
                }
        }

        // 클릭 리스너를 설정하는 함수
        private fun setupClickListeners() {
                binding.textPolicyTitle.setOnClickListener {
                        parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, RegionListFragment())
                                .addToBackStack(null) // 뒤로가기 버튼으로 돌아올 수 있도록 스택에 추가
                                .commit()
                }
        }

        // 3개의 RecyclerView를 모두 설정하는 함수
        private fun setupRecyclerViews() {
                // 어댑터 인스턴스 생성
                recommendAdapter = HomeCardAdapter()
                popularAdapter = HomeCardAdapter()
                policyAdapter = HomeCardAdapter()

                // 추천 상품 RecyclerView 설정
                binding.recommendRecyclerView.apply {
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                        adapter = recommendAdapter
                }

                // 인기 상품 RecyclerView 설정
                binding.popularRecyclerView.apply {
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                        adapter = popularAdapter
                }

                // 지역별 지원 정책 RecyclerView 설정
                binding.policyRecyclerView.apply {
                        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                        adapter = policyAdapter
                }
        }

        // Fragment가 파괴될 때 메모리 누수를 방지하기 위해 binding을 null로 설정
        override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
        }
}
