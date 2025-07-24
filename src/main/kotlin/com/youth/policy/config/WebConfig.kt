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

        // 최소 로깅 인터셉터 추가 (업무환경 수준)
        restTemplate.interceptors.add(SimpleLoggingInterceptor())

        return restTemplate
    }

    // 업무환경에 맞는 최소 로깅
    class SimpleLoggingInterceptor : ClientHttpRequestInterceptor {
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            // 핵심 정보만 로깅 (이모지, 바디 출력 X)
            println("Request: [${request.method}] ${request.uri}")
            val response = execution.execute(request, body)
            println("Response: ${response.statusCode} for ${request.uri}")
            return response
        }
    }
}
