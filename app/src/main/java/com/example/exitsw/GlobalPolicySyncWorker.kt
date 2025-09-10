package com.example.exitsw

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WelfareSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // sigunguCd = null → 서버가 전체 반환 허용 시 전체 저장
            WelfareApiToFirebase.sync(
                context = applicationContext,
                sigunguCd = null,
                pageSize = 100
            )
            Result.success()
        } catch (_: Throwable) {
            // 재시도는 WorkManager 백오프로 넘어가도 충분하니 실패만 반환
            Result.retry()
        }
    }
}
