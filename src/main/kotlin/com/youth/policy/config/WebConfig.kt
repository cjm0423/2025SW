package com.youth.policy.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Configuration
class WebConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        val restTemplate = RestTemplate()

        // XML 메시지 컨버터 설정
        val xmlMapper = XmlMapper()
        xmlMapper.registerModule(KotlinModule())
        xmlMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val xmlConverter: HttpMessageConverter<*> = MappingJackson2XmlHttpMessageConverter(xmlMapper)

        restTemplate.messageConverters.add(0, xmlConverter)

        // 로깅 인터셉터 추가
        restTemplate.interceptors.add(LoggingInterceptor())

        return restTemplate
    }

    class LoggingInterceptor : ClientHttpRequestInterceptor {
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            println("🔽 Request URI: ${request.uri}")
            println("🔽 Request Method: ${request.method}")
            println("🔽 Request Headers: ${request.headers}")
            if (body.isNotEmpty()) {
                println("🔽 Request Body: ${String(body, StandardCharsets.UTF_8)}")
            }

            val response = execution.execute(request, body)

            println("✅ Response Status: ${response.statusCode}")
            println("✅ Response Headers: ${response.headers}")

            val responseBody = BufferedReader(InputStreamReader(response.body, StandardCharsets.UTF_8)).use { it.readText() }
            println("✅ Response Body (500자 미리보기): ${responseBody.take(500)}")

            return ClientHttpResponseWrapper(response, responseBody.toByteArray(StandardCharsets.UTF_8))
        }
    }

    class ClientHttpResponseWrapper(
        private val response: ClientHttpResponse,
        private val body: ByteArray
    ) : ClientHttpResponse by response {
        override fun getBody() = body.inputStream()
    }
}
