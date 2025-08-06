package com.youth.policy.service

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.common.util.concurrent.RateLimiter
import java.util.Properties
import java.util.concurrent.TimeUnit

// --- 요청 DTO 정의 ---
/**
 * 요청 바디: contents → parts → text
 */
data class GenerateRequest(val contents: List<Content>)
data class Content(val parts: List<PartRequest>)
data class PartRequest(val text: String)

// --- 응답 DTO 실제 스키마 반영 ---
data class GenerateResponse(val candidates: List<Candidate>?)
data class Candidate(val content: ContentContainer?)
data class ContentContainer(val parts: List<Part>?)
data class Part(val text: String?)

/**
 * GeminiService: Google Generative Language API(Gemini) 호출
 * 분당 최대 15회, Free Tier 한도를 초과하지 않도록 RateLimiter 적용
 */
class GeminiService {
    companion object {
        // 분당 15회 제한 → 초당 0.25회
        private val CALL_LIMITER: RateLimiter = RateLimiter.create(15.0 / 60.0)
    }

    private val client: OkHttpClient
    private val apiKey: String
    private val endpoint: String
    private val maxRetries: Int
    private val retryDelayMs: Long

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val reqAdapter = moshi.adapter(GenerateRequest::class.java)
    private val respAdapter = moshi.adapter(GenerateResponse::class.java)

    init {
        val props = Properties().apply {
            Thread.currentThread().contextClassLoader
                .getResourceAsStream("config.properties")
                ?.use { load(it) }
        }
        apiKey      = System.getenv("GEMINI_API_KEY")
            ?: props.getProperty("gemini.api.key", "").trim()
        endpoint    = props.getProperty("gemini.api.endpoint", "").trim()
        maxRetries  = props.getProperty("gemini.api.max-retries", "3").toInt()
        retryDelayMs= props.getProperty("gemini.api.retry-delay-ms", "30000").toLong()
        val timeout = props.getProperty("gemini.api.timeout-ms", "15000").toLong()

        require(apiKey.isNotBlank()) { "GEMINI_API_KEY가 설정되지 않았습니다!" }

        client = OkHttpClient.Builder()
            .callTimeout(timeout, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * prompt 하나로 API 호출 후 첫 번째 텍스트 리턴
     * RateLimiter로 분당 호출 횟수 조절
     */
    @Throws(InterruptedException::class)
    fun generate(prompt: String): String {
        // 분당 15회 제한 적용
        CALL_LIMITER.acquire()

        val requestBody = GenerateRequest(
            contents = listOf(
                Content(parts = listOf(PartRequest(text = prompt)))
            )
        )
        val jsonBody = reqAdapter.toJson(requestBody)
        val mediaType = "application/json".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$endpoint?key=$apiKey")
            .post(body)
            .build()

        repeat(maxRetries) { attempt ->
            client.newCall(request).execute().use { resp ->
                val respJson = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val gen = respAdapter.fromJson(respJson)
                    val text = gen
                        ?.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text
                    return text?.trim() ?: "응답 포맷이 예상과 다릅니다."
                }
                if (resp.code == 429 && attempt < maxRetries - 1) {
                    Thread.sleep(retryDelayMs)
                } else {
                    return "API 오류: HTTP ${resp.code}"
                }
            }
        }
        return "최대 재시도 횟수를 초과했습니다."
    }
}
