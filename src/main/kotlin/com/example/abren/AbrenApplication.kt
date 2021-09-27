package com.example.abren

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.reactive.config.EnableWebFlux


@SpringBootApplication
@EnableReactiveMongoRepositories
@EnableWebFlux
@EnableScheduling
class AbrenApplication

fun main(args: Array<String>) {
    runApplication<AbrenApplication>(*args)
}

@Bean
fun mongoClient(): MongoClient? {
    return MongoClients.create()
}

fun getDatabaseName(): String? {
    return "abrendatabase"
}