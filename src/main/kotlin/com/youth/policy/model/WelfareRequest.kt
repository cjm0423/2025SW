package com.youth.policy.model

data class WelfareListRequest(
    val pageNo: Int = 1,
    val numOfRows: Int = 10,
    val srchKeyCode: String = "001",
    val searchWrd: String? = null,
    val lifeArray: String? = null,
    val trgterIndvdlArray: String? = null,
    val intrsThemaArray: String? = null,
    val age: String? = null,
    val onapPsbltYn: String? = null,
    val orderBy: String? = null
)

data class WelfareDetailRequest(
    val servId: String
)