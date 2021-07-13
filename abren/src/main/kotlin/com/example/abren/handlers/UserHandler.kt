package com.example.abren.handlers

import com.example.abren.models.LoginRequest
import com.example.abren.models.User
import com.example.abren.security.SecurityContextRepository
import com.example.abren.security.TokenProvider
import com.example.abren.services.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitFormData
import reactor.core.publisher.Mono
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path


@Component
class UserHandler(private val userService: UserService, private val tokenProvider: TokenProvider) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)
    fun login(request: ServerRequest): Mono<ServerResponse> {
        val loginRequestMono = request.bodyToMono(LoginRequest::class.java)

        return loginRequestMono.flatMap { loginRequest ->
            userService.findByPhoneNumber(loginRequest.phoneNumber)
                .flatMap SecondFlatMap@ { user ->
                    if (BCryptPasswordEncoder().matches(loginRequest.password, user?.password)) {
                        return@SecondFlatMap ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(tokenProvider.generateToken(user)))
                    } else {
                        return@SecondFlatMap ServerResponse.badRequest()
                            .body(BodyInserters.fromValue("Invalid Credentials"))
                    }
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue("User does not exist"))
                )
        }
    }

    fun signup(request: ServerRequest): Mono<ServerResponse> {
        return request.body(BodyExtractors.toMultipartData()).flatMap { parts ->
            val map: Map<String, Part> = parts.toSingleValueMap()
            val profilePicture : FilePart = map["profilePicture"]!! as FilePart
            val idCardPicture : FilePart = map["idCardPicture"]!! as FilePart
            val phoneNumber : FormFieldPart = map["phoneNumber"]!! as FormFieldPart

            saveFile(profilePicture, "profiles")
            saveFile(idCardPicture, "id_cards")



            ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                BodyInserters.fromValue(phoneNumber.value())
            )
        }

//        val userMono = request.bodyToMono(User::class.java)
//        return userMono.map { user ->
//            user.password = BCryptPasswordEncoder().encode(user.password)
//            user
//        }.flatMap SecondFlatMap@ { user ->
//            userService.findByPhoneNumber(user.phoneNumber).flatMap { retreivedUser ->
//                ServerResponse.badRequest()
//                    .body(BodyInserters.fromValue("User Already Exists"))
//            }.switchIfEmpty(
//                userService.register(user).flatMap { savedUser ->
//                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
//                        BodyInserters.fromValue(savedUser)
//                    )
//                }
//            )
//        }
    }

    fun getProfile(r: ServerRequest): Mono<ServerResponse> {
        val user : Mono<User?> = userService.findOne(r.pathVariable("id"))
        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromProducer(user, User::class.java))
    }

//    fun editUser(r: ServerRequest): Mono<ServerResponse> {
//
//    }
//
//    fun upgradeAccount(r: ServerRequest): Mono<ServerResponse> {
//
//    }
//
//    fun rate(r: ServerRequest): Mono<ServerResponse> {
//
//    }

    fun saveFile(filePart: FilePart, folder: String){
        val target = Paths.get("uploads/images/$folder").resolve(filePart.filename()) //TODO: Make sure this location works
        try {
            Files.deleteIfExists(target)
            val file = Files.createFile(target).toFile()

            filePart.transferTo(file)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}