package com.example.abren.repositories

import com.example.abren.models.Request
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface RequestRepository : ReactiveMongoRepository<Request?, String?> {
    fun findByStatus(status: String): Flux<Request?>
}