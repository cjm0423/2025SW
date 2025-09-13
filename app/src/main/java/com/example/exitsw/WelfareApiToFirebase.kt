package com.example.exitsw

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

object WelfareApiToFirebase {

    private const val TAG = "WelfareSync"

    // ※ 에뮬레이터: 10.0.2.2 / 물리 디바이스: PC 로컬 IP(예: 192.168.x.x)로 반드시 변경
    //private const val BASE_URL = "http://10.0.2.2:8080/"
    private const val BASE_URL = "http://172.30.1.60:8080/"
    private const val PATH_LOCAL_WELFARE = "/api/welfare/local/services"

    private interface Api {
        @GET(PATH_LOCAL_WELFARE)
        suspend fun getLocalWelfareList(
            @Query("sigunguCd") sigunguCd: String? = null,
            @Query("pageNo") pageNo: Int = 1,
            @Query("numOfRows") numOfRows: Int = 100
        ): Response<JsonObject>
    }

    private fun buildOkHttp(context: Context): OkHttpClient {
        val cacheSize = 10L * 1024 * 1024
        val cache = Cache(File(context.cacheDir, "http-cache"), cacheSize)

        val httpLog = HttpLoggingInterceptor { msg -> Log.d("OkHttp", msg) }
            .setLevel(HttpLoggingInterceptor.Level.BODY) // BODY로 올려서 응답 바디까지 확인

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val online = isNetworkAvailable(context)
                val req = chain.request().newBuilder().apply {
                    if (online) {
                        header("Cache-Control", "public, max-age=60")
                    } else {
                        header("Cache-Control", "public, only-if-cached, max-stale=${60 * 60 * 24 * 7}")
                    }
                }.build()
                chain.proceed(req)
            }
            .addInterceptor(httpLog)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(context: Context): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(buildOkHttp(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private fun api(context: Context): Api = buildRetrofit(context).create(Api::class.java)

    /**
     * 정책 수집 → Firestore 저장
     * @param sigunguCd 대부분 서버가 필수로 요구. 전체 조회가 안 되면 400/422가 뜹니다.
     */
    suspend fun sync(
        context: Context,
        sigunguCd: String? = null,
        pageSize: Int = 100
    ): Int = withContext(Dispatchers.IO) {
        val service = api(context)
        val db = FirebaseFirestore.getInstance()
        val gson = Gson()

        var page = 1
        var totalSaved = 0
        var totalCount: Int? = null

        while (true) {
            val resp = service.getLocalWelfareList(sigunguCd, page, pageSize)

            if (!resp.isSuccessful) {
                val code = resp.code()
                val errBody = resp.errorBody()?.string().orEmpty()
                val msg = "HTTP $code, body=$errBody"
                Log.e(TAG, msg)

                if (sigunguCd == null && code in listOf(400, 422)) {
                    throw IllegalStateException("서버가 sigunguCd(시군구 코드) 파라미터를 요구합니다.")
                } else {
                    throw IllegalStateException("정책 API 요청 실패: $msg")
                }
            }

            val root = resp.body() ?: throw IllegalStateException("응답 바디가 없습니다.")
            // items/data 우선 → 없으면 JSON 트리에서 가장 큰 배열 자동 탐색(스키마 방어)
            val items = findFirstArrayByName(root, "items")
                ?: findFirstArrayByName(root, "data")
                ?: findLargestArray(root)
                ?: JsonArray()

            if (totalCount == null) {
                totalCount = findFirstNumberByName(root, "totalCount")
                    ?: findFirstNumberByName(root, "count")
            }

            if (items.size() == 0) {
                Log.d(TAG, "아이템 0건(page=$page). 종료.")
                break
            }

            val jsonObjects = items.filter { it.isJsonObject }.map { it.asJsonObject }
            jsonObjects.chunked(400).forEach { chunk ->
                db.runBatch { batch ->
                    chunk.forEach { jo ->
                        val map = jsonToPlainMap(gson, jo).toMutableMap()

                        val docId = (
                                map["svcId"] ?: map["id"] ?: map["serviceId"] ?: map["service_id"] ?: map["no"]
                                ?: sha1(gson.toJson(jo))
                                ).toString()

                        val resolvedSigungu = (map["sigunguCd"] ?: sigunguCd ?: "ALL").toString()

                        map["sigunguCd"] = resolvedSigungu
                        map["source"] = "api"
                        map["syncedAt"] = FieldValue.serverTimestamp()

                        // 전역 카탈로그
                        batch.set(
                            db.collection("policies")
                                .document("all")
                                .collection("items")
                                .document(docId),
                            map,
                            SetOptions.merge()
                        )
                    }
                }.await()
                totalSaved += chunk.size
                Log.d(TAG, "Firestore 저장: +${chunk.size} (누적=$totalSaved)")
            }

            val more = if (totalCount != null) totalSaved < totalCount!! else items.size() >= pageSize
            if (!more) break
            page += 1
        }

        Log.i(TAG, "동기화 완료. 총 저장: $totalSaved")
        totalSaved
    }

    // ------- Utilities -------

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun findFirstArrayByName(root: JsonElement?, name: String): JsonArray? {
        if (root == null || root.isJsonNull) return null
        if (root.isJsonObject) {
            val obj = root.asJsonObject
            obj.entrySet().forEach { (k, v) ->
                if (k.equals(name, ignoreCase = true) && v.isJsonArray) return v.asJsonArray
            }
            obj.entrySet().forEach { (_, v) ->
                val found = findFirstArrayByName(v, name)
                if (found != null) return found
            }
        } else if (root.isJsonArray) {
            root.asJsonArray.forEach { el ->
                val found = findFirstArrayByName(el, name)
                if (found != null) return found
            }
        }
        return null
    }

    // JSON 트리 전체에서 "가장 큰 배열"을 찾아서 반환(스키마가 불안정할 때 유용)
    private fun findLargestArray(root: JsonElement?): JsonArray? {
        var best: JsonArray? = null
        fun dfs(el: JsonElement?) {
            if (el == null || el.isJsonNull) return
            if (el.isJsonArray) {
                val arr = el.asJsonArray
                if (best == null || arr.size() > best!!.size()) best = arr
                arr.forEach { dfs(it) }
            } else if (el.isJsonObject) {
                el.asJsonObject.entrySet().forEach { (_, v) -> dfs(v) }
            }
        }
        dfs(root)
        return best
    }

    private fun findFirstNumberByName(root: JsonElement?, name: String): Int? {
        if (root == null || root.isJsonNull) return null
        if (root.isJsonObject) {
            val obj = root.asJsonObject
            obj.entrySet().forEach { (k, v) ->
                if (k.equals(name, ignoreCase = true) &&
                    v.isJsonPrimitive && v.asJsonPrimitive.isNumber
                ) {
                    return try { v.asInt } catch (_: Exception) { v.asDouble.toInt() }
                }
            }
            obj.entrySet().forEach { (_, v) ->
                val found = findFirstNumberByName(v, name)
                if (found != null) return found
            }
        } else if (root.isJsonArray) {
            root.asJsonArray.forEach { el ->
                val found = findFirstNumberByName(el, name)
                if (found != null) return found
            }
        }
        return null
    }

    private fun jsonToPlainMap(gson: Gson, jo: JsonObject): Map<String, Any?> {
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(gson.toJson(jo), type)
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.lowercase(Locale.US)
    }
}
