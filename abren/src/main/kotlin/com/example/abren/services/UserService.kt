package com.example.abren.services

import com.example.abren.models.User
import com.example.abren.repositories.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@Service
class UserService(private val userRepository: UserRepository) {

    fun findAll(): Flux<User?> {
        return userRepository.findAll();
    }

    fun findOne(id: String): Mono<User?> {
        return userRepository.findById(id)
    }

    fun findByPhoneNumber(phoneNumber: String): Mono<User?> {
        return userRepository.findByPhoneNumber(phoneNumber)
    }

    fun update(id: String, user: User): Mono<User> {
        return userRepository.save(user) //TODO: Make better
    }

    fun delete(id: String): Mono<User> {
        return userRepository
            .findById(id)
            .flatMap { u ->
                u?._id?.let { userRepository.deleteById(it).thenReturn(u) }
            }
    }

    fun count(): Mono<Long> {
        return userRepository.count()
    }

    fun register(user: User): Mono<User> {
        return userRepository.save(user)
    }

//    fun verifyAccount(user: User): Mono<Boolean> {
//        return userRepository.save(user)
//    }

//    fun upgradeAccount(user: User): Mono<User?> {
//        return userRepository.save(user)
//    }
//
//    fun rate(user: User): Mono<User?> {
//        return userRepository.save(user)
//    }
}