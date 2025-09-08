package com.example.exitsw;

import android.widget.ImageButton
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class MainFragment : Fragment() {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)

                val btnProfile = view.findViewById<ImageButton>(R.id.btn_profile)
                btnProfile.setOnClickListener {
                        parentFragmentManager.beginTransaction()// val transaction =
                                .replace(R.id.fragment_container, fragment_mypage())
                                .addToBackStack(null)
                                .commit()
                }
        }
}
