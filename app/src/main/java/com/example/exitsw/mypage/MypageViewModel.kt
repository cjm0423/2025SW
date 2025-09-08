package com.example.exitsw.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MypageViewModel : ViewModel() {
    private val _productList = MutableLiveData<List<ProductItem>>()
    val productList: LiveData<List<ProductItem>> get() = _productList

    init {
        _productList.value = listOf(
            ProductItem("001", "청년적금", "국민은행", "...", 4.5f, true),
            ProductItem("002", "내일채움공제", "IBK", "...", 3.5f, false)
        )
    }
}