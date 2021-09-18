package com.example.abren.services

import com.example.abren.models.Request
import com.example.abren.repositories.RequestRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class RequestService(private val requestRepository: RequestRepository) {

    fun findAllById(ids: Set<String>): Flux<Request?> {
        return requestRepository.findAllById(ids)
    }

    fun findAllById(ids: MutableList<String>): Flux<Request?> {
        return requestRepository.findAllById(ids)
    }

    fun findByStatus(status: String): Flux<Request?> {
        return requestRepository.findByStatus(status)
    }

    fun findOne(id: String): Mono<Request?> {
        return requestRepository.findById(id)
    }

    fun update(request: Request): Mono<Request> {
        return requestRepository.save(request) //TODO: Make better
    }

    fun delete(id: String): Mono<Request> {
        return requestRepository
            .findById(id)
            .flatMap { u ->
                u?._id?.let { requestRepository.deleteById(it).thenReturn(u) }
            }
    }

    fun count(): Mono<Long> {
        return requestRepository.count()
    }

    fun create(request: Request): Mono<Request> {
        return requestRepository.save(request)
    }


}