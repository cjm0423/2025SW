package com.example.exitsw.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.exitsw.databinding.FragmentMainBinding

class MainFragment : Fragment() {

        // ViewBinding과 ViewModel을 사용하기 위한 변수 선언
        private var _binding: FragmentMainBinding? = null
        private val binding get() = _binding!!
        private val viewModel: MainViewModel by viewModels()

        override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View {
                // ViewBinding을 사용하여 레이아웃을 인플레이트
                _binding = FragmentMainBinding.inflate(inflater, container, false)
                return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)

                // ViewModel의 welfareList(LiveData)에 변화가 생기는지 관찰(observe)
                viewModel.welfareList.observe(viewLifecycleOwner) { receivedList ->
                        // LiveData의 값이 변경될 때마다 이 부분이 실행됩니다.
                        Log.d("MainFragment", "성공: ${receivedList.size}개의 데이터를 ViewModel로부터 받았습니다.")

                        // TODO: 이 곳에서 받아온 receivedList를 지역별 지원 정책 RecyclerView 어댑터에 넘겨주면 화면에 표시됨
                }
        }

        // Fragment가 파괴될 때 메모리 누수를 방지하기 위해 binding을 null로 설정
        override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
        }
}
