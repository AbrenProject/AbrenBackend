package com.example.abren.configurations

import com.example.abren.security.SecurityContextRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class WebConfig: WebFluxConfigurer {
    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val path = "file:///${System.getProperty("user.dir")}\\uploads\\"
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(path)
    }
}