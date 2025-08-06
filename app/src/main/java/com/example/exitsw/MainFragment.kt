package com.example.exitsw;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainFragment : Fragment() {
        override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)

                // itemRecycler or recommendRecycler 뭐로 하지? 제어 흐름이 어떻게 되는지 회의 필요
                val itemRecycler = view.findViewById<RecyclerView>(R.id.itemRecycler)

                // LayoutManager 세팅 (this 대신 requireContext())
                itemRecycler.layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                // Adapter 세팅
                itemRecycler.adapter = RecommendAdapter(myRecommendList)
        }
}

