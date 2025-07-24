package com.youth.policy.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
        return restTemplate
    }
}
