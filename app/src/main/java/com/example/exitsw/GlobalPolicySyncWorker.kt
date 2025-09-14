package com.example.exitsw

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager에 의해 주기적으로 실행되는 백그라운드 작업자입니다.
 * 로컬 API 서버에서 전체 정책 데이터를 가져와 Firestore를 최신 상태로 업데이트하는 역할을 합니다.
 */
class GlobalPolicySyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // 수정된 sync 함수를 호출하여 모든 지역의 데이터를 가져와 Firestore에 덮어씁니다.
            // pageSize를 10으로 설정하여, 여러 지역에서 데이터를 가져오더라도 총 10개를 넘지 않도록 제한합니다.
            WelfareApiToFirebase.sync(
                context = applicationContext,
                pageSize = 10
            )
            // 작업이 성공적으로 완료되었음을 알립니다.
            Result.success()
        } catch (e: Throwable) {
            // 작업 중 오류 발생 시 (예: 네트워크 문제) 실패를 알리고,
            // WorkManager의 재시도 정책에 따라 나중에 다시 시도하도록 합니다.
            Result.retry()
        }
    }
}