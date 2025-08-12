package com.example.exitsw.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.databinding.FragmentMainBinding

class MainFragment : Fragment() {

        private var _binding: FragmentMainBinding? = null
        private val binding get() = _binding!!
        private val viewModel: MainViewModel by viewModels()

        // ✨ [변경] 어댑터를 3개 선언
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

                setupRecyclerViews() // RecyclerView 설정 함수 호출

                // 1. 지역별 지원 정책 데이터 관찰 및 어댑터에 전달
                viewModel.welfareList.observe(viewLifecycleOwner) { welfareList ->
                        policyAdapter.submitList(welfareList)
                }

                // ✨ [추가] 2. 추천 상품 데이터 관찰 및 어댑터에 전달
                viewModel.recommendList.observe(viewLifecycleOwner) { recommendList ->
                        recommendAdapter.submitList(recommendList)
                }

                // ✨ [추가] 3. 인기 상품 데이터 관찰 및 어댑터에 전달
                viewModel.popularList.observe(viewLifecycleOwner) { popularList ->
                        popularAdapter.submitList(popularList)
                }
        }

        // ✨ [변경] 3개의 RecyclerView를 모두 설정하는 함수
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

        override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
        }
}
