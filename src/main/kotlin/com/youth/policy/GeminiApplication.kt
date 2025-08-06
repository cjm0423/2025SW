package com.youth.policy

import com.youth.policy.service.GeminiService

/**
 * Gemini API를 콘솔에서 간단히 테스트할 수 있는 애플리케이션
 * 
 * 실행 방법:
 *   ./gradlew run --args="원하는 프롬프트 내용"
 * 또는 인터랙티브 모드로 실행 후 프롬프트 입력
 */
fun main(args: Array<String>) {
    val gemini = GeminiService()
    // 1) 커맨드라인 인수로 프롬프트 받기
    val prompt = if (args.isNotEmpty()) {
        args.joinToString(" ")
    } else {
        // 2) 아니면 콘솔에서 입력
        print("프롬프트 입력: ")
        val input = readLine()
        if (input.isNullOrBlank()) {
            println("프롬프트가 비어 있습니다. 종료합니다.")
            return
        }
        input
    }

    println("\n--- 생성 중... 잠시만 기다려주세요 ---\n")

    // 3) Gemini API 호출
    val response = try {
        gemini.generate(prompt)
    } catch (e: InterruptedException) {
        "오류 발생: ${e.message}"
    }

    // 4) 결과 출력
    println("=== Gemini 응답 ===")
    println(response)
}
