package com.example.exitsw.ai

import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gemini API 클라이언트
 * - generateEndpoint: 예) https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
 * - API 키/엔드포인트는 BuildConfig에서 주입
 */
class GeminiClient(
    private val apiKey: String,
    private val generateEndpoint: String
) {
    private val JSON = "application/json".toMediaType()

    // 타임아웃 여유 있게
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    /**
     * LLM에 프롬프트 전달 → **JSON 배열** 수신
     * - response_mime_type=application/json 로 JSON만 오도록 유도
     * - 실패 시 빈 배열 반환
     */
    fun generateJsonArrayPrompt(prompt: String): JSONArray {
        val payload = JSONObject()
            .put(
                "contents", JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                )
            )
            .put("generationConfig", JSONObject().put("response_mime_type", "application/json"))

        val url = "$generateEndpoint?key=$apiKey"
        val req = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON))
            .build()

        client.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Gemini HTTP ${resp.code}: $bodyStr")

            val res = JSONObject(bodyStr)
            val cand = res.optJSONArray("candidates")?.optJSONObject(0) ?: return JSONArray()
            val content = cand.optJSONObject("content") ?: return JSONArray()
            val parts = content.optJSONArray("parts") ?: return JSONArray()
            val jsonText = parts.optJSONObject(0)?.optString("text", "[]") ?: "[]"

            return try { JSONArray(jsonText) } catch (_: Throwable) { JSONArray() }
        }
    }
}
