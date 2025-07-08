package com.youth.policy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@SpringBootApplication
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}

@Component
class ServerStartupListener : ApplicationListener<WebServerInitializedEvent> {
    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port
        println("\n애플리케이션이 시작되었습니다.")
        println("✅ API 서버 주소: http://localhost:$port")
        println("📌 사용 가능한 API 목록:")
        println("- 청년정책 API:           http://localhost:$port/api/youth/policies")
        println("- 환율 API:              http://localhost:$port/api/exchange-rates")
        println("- 중앙부처복지서비스 목록: http://localhost:$port/api/welfare/services")
        println("- 지자체복지서비스 목록:    http://localhost:$port/api/local/welfare/services")
        println()
    }
}
