package com.example.abren.repositories

import com.example.abren.models.User
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface UserRepository : ReactiveMongoRepository<User?, String?> {
    fun findByPhoneNumber(phoneNumber: String): Mono<User?>
}