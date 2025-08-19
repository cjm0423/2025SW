package com.youth.policy.config

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
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.client.RestTemplate

@Configuration
class WebConfig {

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
        val xmlConverter = MappingJackson2XmlHttpMessageConverter(xmlMapper)

        restTemplate.messageConverters.add(0, xmlConverter)

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

    // ✅ 로그 출력 방식을 한 줄로 깔끔하게 수정
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
}