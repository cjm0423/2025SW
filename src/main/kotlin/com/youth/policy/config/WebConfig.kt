package com.youth.policy.config // ⛔️ 이 패키지 이름은 실제 프로젝트에 맞게 확인하세요.

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer { // 👈 1. WebMvcConfigurer 인터페이스 구현

    // ===================================================
    // 👇 외부 API 호출을 위한 설정 (기존 코드와 동일)
    // ===================================================
    @Bean
    fun xmlMapper(): XmlMapper {
        val xmlMapper = XmlMapper()
        xmlMapper.registerModule(KotlinModule())
        xmlMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return xmlMapper
    }

    @Bean
    fun restTemplate(xmlMapper: XmlMapper): RestTemplate {
        val restTemplate = RestTemplate()
        restTemplate.messageConverters.add(MappingJackson2XmlHttpMessageConverter(xmlMapper))
        restTemplate.interceptors = listOf(
            UserAgentInterceptor(),
            SimpleLoggingInterceptor()
        )
        return restTemplate
    }

    class UserAgentInterceptor : ClientHttpRequestInterceptor {
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            request.headers.add(
                HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"
            )
            return execution.execute(request, body)
        }
    }

    class SimpleLoggingInterceptor : ClientHttpRequestInterceptor {
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            val response = execution.execute(request, body)
            println("API Call -> [${request.method}] ${request.uri} | Status: ${response.statusCode}")
            return response
        }
    }

    // ===================================================
    // 👇 2. 안드로이드 앱 응답을 JSON으로 강제하는 설정
    //    이전 코드를 이 코드로 교체합니다.
    // ===================================================
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // 새로운 JSON 컨버터를 생성해서
        val jsonConverter = MappingJackson2HttpMessageConverter()
        // 컨버터 목록의 맨 앞에 추가하여 최우선 순위를 부여합니다.
        converters.add(0, jsonConverter)
    }
}