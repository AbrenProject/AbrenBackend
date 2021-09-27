package com.example.abren.configurations

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class WebConfig: WebFluxConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val path = "file:///${System.getProperty("user.dir")}\\uploads\\"
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(path)
    }
}