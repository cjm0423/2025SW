package com.example.exitsw

/**
 * 채팅 아이템 종류 정의
 */
sealed class ChatItem {
    data class UserMessage(val text: String) : ChatItem()
    data class BotMessage(val text: String) : ChatItem()
    data class PolicyMessage(
        val title: String,
        val desc: String,
        val link: String
    ) : ChatItem()
}
