package com.example.exitsw.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.R
import com.example.exitsw.databinding.FragmentRecommendListBinding

class RecommendListFragment : Fragment() {

    private var _binding: FragmentRecommendListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var policyAdapter: PolicyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        // 툴바 뒤로가기
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        // 타이틀
        binding.toolbar.title = getString(R.string.recommend_list_title) // strings.xml에 추가 권장
    }

    private fun setupRecyclerView() {
        policyAdapter = PolicyAdapter { item ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PolicyDetailFragment.newInstance(item))
                .addToBackStack(null)
                .commit()
        }
        binding.recommendListRecyclerView.adapter = policyAdapter
        binding.recommendListRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun observeViewModel() {
        viewModel.recommendList.observe(viewLifecycleOwner) { list ->
            // placeholder 제거
            val filtered = list.filter { it.servId != "COMING_SOON" }
            policyAdapter.submitList(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
