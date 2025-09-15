package com.example.exitsw

/**
 * 채팅 메시지 모델
 * @param text 메시지 내용 (링크 포함 가능)
 * @param isUser 사용자 메시지 여부 (true=사용자, false=봇)
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean = false
)
