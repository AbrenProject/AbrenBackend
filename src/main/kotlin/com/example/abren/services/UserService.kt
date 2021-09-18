package com.example.abren.services

import com.example.abren.models.Name
import com.example.abren.models.User
import com.example.abren.repositories.UserRepository
import com.example.abren.requests.DocumentVerifierRequest
import com.example.abren.responses.DocumentVerifierResponse
import com.example.abren.security.SecurityContextRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.*


@Service
class UserService(private val userRepository: UserRepository, private val webClientBuilder: WebClient.Builder) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)
    fun findAll(): Flux<User?> {
        return userRepository.findAll();
    }

    fun findOne(id: String): Mono<User?> {
        return userRepository.findById(id)
    }

    fun findByPhoneNumber(phoneNumber: String): Mono<User?> {
        return userRepository.findByPhoneNumber(phoneNumber)
    }

    fun update(user: User): Mono<User> {
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

    @Throws(IllegalArgumentException::class)
    fun register(user: User): Mono<User> {
        val idResultMono = verifyDocument("ID", user.idCardUrl, user.profilePictureUrl)
        return idResultMono.flatMap { idResult ->
            logger.info("ID: $idResult")
            if (!idResult.isLogoVerified) {
                throw IllegalArgumentException("ID_CARD: The document could not be validated.")
            }

            if (!idResult.isFaceVerified) {
                throw IllegalArgumentException("ID_CARD: Profile picture doesn't match picture from document.")
            }

            if (!idResult.isTextVerified) {
                throw IllegalArgumentException("ID_CARD: The text in the document could not be validated.")
            }

            if(idResult.data.name != null){
                val nameArr = idResult.data.name.split(" ")
                user.name = Name(nameArr[0], nameArr[1], nameArr[2])
            }

            if(idResult.data.sex != null){
                when {
                    setOf("F", "f", "T", "t", "E", "e").contains(idResult.data.sex) -> {
                        user.gender =  "Female"
                    }
                    setOf("M", "m", "N", "n", "W", "w").contains(idResult.data.sex) -> {
                        user.gender =  "Male"
                    }
                    else -> {
                        throw IllegalArgumentException("ID_CARD: The text in the document could not be validated.")
                    }
                }
            }

            if(idResult.data.dateOfBirth != null) {
                val formatter: DateTimeFormatter = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM dd, yyyy").toFormatter()
                val date = LocalDate.parse(idResult.data.dateOfBirth, formatter)
                val age = Period.between(date, LocalDate.now()).years
                when {
                    age in 18..24 -> {
                        user.ageGroup = "18 - 25"
                    }
                    age in 25..40 -> {
                        user.ageGroup = "25 - 40"
                    }
                    age in 40..60 -> {
                        user.ageGroup = "40 - 60"
                    }
                    age >= 60 -> {
                        user.ageGroup = "> 60"
                    }
                }
            }

            if(user.role == "DRIVER"){
                logger.info("DL")
                val dlResultMono = verifyDocument("DL", user.vehicleInformation?.licenseUrl!!, user.profilePictureUrl!!)
                return@flatMap dlResultMono.flatMap { dlResult ->
                    logger.info("DL: $dlResult")
                    if (!dlResult.isLogoVerified) {
                        throw IllegalArgumentException("DRIVING_LICENSE: The document could not be validated.")
                    }

                    if (!dlResult.isFaceVerified) {
                        throw IllegalArgumentException("DRIVING_LICENSE: Profile picture doesn't match picture from document.")
                    }

                    if (!dlResult.isTextVerified) {
                        throw IllegalArgumentException("DRIVING_LICENSE: The text in the document could not be validated.")
                    }

                    user.isVerified = true
                    userRepository.save(user)
                }
            }
            user.isVerified = true
            userRepository.save(user)
        }
    }

    fun verifyDocument(documentType: String, imagePath: String, profileImagePath: String): Mono<DocumentVerifierResponse> {
        val webClient = webClientBuilder.baseUrl("https://abren-project-scripts.herokuapp.com").build()

        val documentVerifierRequest = DocumentVerifierRequest(documentType, imagePath, profileImagePath)
        return webClient.post()
            .uri("/verify-document")
            .body(Mono.just(documentVerifierRequest), DocumentVerifierRequest::class.java)
            .retrieve()
            .bodyToMono(DocumentVerifierResponse::class.java)
    }
}