package com.example.exitsw.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.R
import com.example.exitsw.databinding.FragmentPopularListBinding

class PopularListFragment : Fragment() {

    private var _binding: FragmentPopularListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var policyAdapter: PolicyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPopularListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        policyAdapter = PolicyAdapter { item ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PolicyDetailFragment.newInstance(item))
                .addToBackStack(null)
                .commit()
        }
        binding.popularListRecyclerView.adapter = policyAdapter
        binding.popularListRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun observeViewModel() {
        viewModel.popularList.observe(viewLifecycleOwner) { list ->
            val filteredList = list.filter { it.servId != "COMING_SOON" }
            policyAdapter.submitList(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}