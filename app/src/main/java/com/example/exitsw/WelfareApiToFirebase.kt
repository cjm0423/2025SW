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
    private const val BASE_URL = "http://172.30.1.60:8080/"
    private const val PATH_LOCAL_WELFARE = "/api/welfare/local/services"

    private val KOREA_SIGUNGU_CODES = listOf(
        "11000", "26000", "27000", "28000", "29000", "30000", "31000", "36000",
        "41000", "42000", "43000", "44000", "45000", "46000", "47000", "48000", "50000"
    )

    private interface Api {
        @GET(PATH_LOCAL_WELFARE)
        suspend fun getLocalWelfareList(
            @Query("sigunguCd") sigunguCd: String?,
            @Query("pageNo") pageNo: Int = 1,
            @Query("numOfRows") numOfRows: Int = 10
        ): Response<JsonObject>
    }

    private fun buildOkHttp(context: Context): OkHttpClient {
        val cacheSize = 10L * 1024 * 1024
        val cache = Cache(File(context.cacheDir, "http-cache"), cacheSize)
        val httpLog = HttpLoggingInterceptor { msg -> Log.d("OkHttp", msg) }
            .setLevel(HttpLoggingInterceptor.Level.BODY)

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
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(context: Context): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(buildOkHttp(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private fun api(context: Context): Api = buildRetrofit(context).create(Api::class.java)

    suspend fun sync(
        context: Context,
        pageSize: Int = 10
    ): Int = withContext(Dispatchers.IO) {
        val service = api(context)
        val db = FirebaseFirestore.getInstance()
        val gson = Gson()
        val allJsonObjects = mutableListOf<JsonObject>()

        for (sigunguCode in KOREA_SIGUNGU_CODES) {
            Log.d(TAG, "[시작] 지역 코드: $sigunguCode 데이터 수집 (1페이지)")

            val resp = service.getLocalWelfareList(sigunguCode, 1, pageSize)

            if (!resp.isSuccessful) {
                Log.e(TAG, "API 요청 실패 (코드: ${resp.code()}). 다음 지역으로 넘어갑니다.")
                continue
            }

            val root = resp.body()
            if (root != null) {
                val items = findFirstArrayByName(root, "servList") ?: JsonArray()
                val jsonObjects = items.filter { it.isJsonObject }.map { it.asJsonObject }
                allJsonObjects.addAll(jsonObjects)
                Log.d(TAG, "($sigunguCode): ${jsonObjects.size}건 수집 (누적: ${allJsonObjects.size}건)")
            }

            // [수정점] 수집한 데이터의 총 개수가 10개를 넘으면 즉시 반복을 중단합니다.
            if (allJsonObjects.size >= 10) {
                Log.i(TAG, "목표 수집량(10개)에 도달하여 데이터 수집을 중단합니다.")
                break
            }
        }

        // 만약 마지막 API 호출로 10개를 초과해서 수집했을 경우, 정확히 10개만 잘라냅니다.
        val finalObjects = allJsonObjects.take(10)

        Log.i(TAG, "모든 지역 데이터 수집 완료. 총 ${finalObjects.size}건. Firestore 저장을 시작합니다.")

        if (finalObjects.isEmpty()) {
            Log.w(TAG, "저장할 데이터가 없습니다. 동기화를 종료합니다.")
            return@withContext 0
        }

        var totalSaved = 0
        finalObjects.distinctBy { it["servId"]?.asString }
            .chunked(400).forEach { chunk ->
                db.runBatch { batch ->
                    chunk.forEach { jo ->
                        val map = jsonToPlainMap(gson, jo).toMutableMap()
                        val docId = (map["servId"] ?: map["svcId"] ?: map["id"] ?: map["serviceId"] ?: sha1(gson.toJson(jo))).toString()

                        map["source"] = "api"
                        map["syncedAt"] = FieldValue.serverTimestamp()

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
                Log.d(TAG, "Firestore 저장: +${chunk.size}건 (누적: $totalSaved 건)")
            }

        Log.i(TAG, "동기화 완료. 총 저장된 데이터: $totalSaved 건")
        totalSaved
    }

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