package com.example.exitsw.mypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MypageViewModel : ViewModel() {
    private val _productList = MutableLiveData<List<ProductItem>>()
    val productList: LiveData<List<ProductItem>> get() = _productList

    init {
        loadProducts()
    }

    private fun loadProducts() {
        // DB or API에서 로딩했다고 가정
        _productList.value = listOf(
            ProductItem("청년적금", "국민은행", "...", 4.5f, true),
            ProductItem("내일채움공제", "IBK", "...", 3.5f, false)
        )
    }
}
