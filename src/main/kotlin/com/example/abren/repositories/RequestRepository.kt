package com.example.abren.repositories

import com.example.abren.models.Request
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface RequestRepository  : ReactiveMongoRepository<Request?, String?> {
}