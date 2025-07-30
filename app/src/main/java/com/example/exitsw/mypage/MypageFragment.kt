package com.example.exitsw.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exitsw.databinding.FragmentMypageBinding

class MypageFragment : Fragment() {
    private lateinit var binding: FragmentMypageBinding
    private val viewModel: MypageViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentMypageBinding.inflate(inflater, container, false)

        return binding.root
    }
}
