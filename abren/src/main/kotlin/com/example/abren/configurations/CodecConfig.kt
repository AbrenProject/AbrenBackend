package com.example.abren.configurations

import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader
import org.springframework.http.codec.multipart.MultipartHttpMessageReader
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class CodecConfig : WebFluxConfigurer {
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        val partReader = DefaultPartHttpMessageReader()
        partReader.setMaxHeadersSize(700000) //TODO: Make sure this is okay
        partReader.isEnableLoggingRequestDetails = true
        val multipartReader = MultipartHttpMessageReader(partReader)
        multipartReader.isEnableLoggingRequestDetails = true
        configurer.defaultCodecs().multipartReader(multipartReader)
    }
}