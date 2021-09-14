package com.example.abren.handlers

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.abren.configurations.Constants
import com.example.abren.models.Preference
import com.example.abren.models.User
import com.example.abren.models.VehicleInformation
import com.example.abren.requests.LoginRequest
import com.example.abren.responses.AuthResponse
import com.example.abren.responses.BadRequestResponse
import com.example.abren.responses.DocumentVerifierResponse
import com.example.abren.security.SecurityContextRepository
import com.example.abren.security.TokenProvider
import com.example.abren.services.UserService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.lang.Integer.parseInt


@Component
class UserHandler(private val userService: UserService, private val tokenProvider: TokenProvider, private val env: Environment) {
    val constants = Constants()
    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)

    fun login(r: ServerRequest): Mono<ServerResponse> {
        val loginMono = r.bodyToMono(LoginRequest::class.java)
        return loginMono.flatMap { login ->
            userService.findByPhoneNumber(login.phoneNumber)
                .flatMap second@{ user ->
                    if (user != null && BCryptPasswordEncoder().matches(login.password, user.password)) {
                        return@second ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(AuthResponse(user, tokenProvider.generateToken(user))))
                    } else {
                        return@second ServerResponse.badRequest()
                            .body(BodyInserters.fromValue(BadRequestResponse("Invalid Credentials")))
                    }
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("User does not exist")))
                )
        }.switchIfEmpty(
            ServerResponse.badRequest()
                .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required: ${constants.REQUIRED_LOGIN_FIELDS}")))
        ).onErrorResume {
            ServerResponse.badRequest()
                .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required: ${constants.REQUIRED_LOGIN_FIELDS}")))
        }
    }

    fun signup(r: ServerRequest): Mono<ServerResponse> {
        val userMono = r.bodyToMono(User::class.java)
        return userMono.flatMap first@{ user ->
            logger.info("User: $user")
            if (user.phoneNumber.length != 12) {
                logger.info("PhoneNum")
                return@first ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("Invalid Input for phoneNumber")))
            }

            if (user.emergencyPhoneNumber.length != 12) {
                logger.info("Emer")
                return@first ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("Invalid Input for emergencyPhoneNumber")))
            }

            val retrievedUser = userService.findByPhoneNumber(user.phoneNumber)
            retrievedUser.flatMap {
                logger.info("Exists")
                return@flatMap ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("User already exists")))
            }.switchIfEmpty {
                if (user.role != "RIDER" && user.role != "DRIVER") {
                    logger.info("Role")
                    return@switchIfEmpty ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("The role field must be one of the following values: ['RIDER', 'DRIVER']")))
                }

                if (user.role == "DRIVER" && user.vehicleInformation == null) {
                    logger.info("vehicleInfo")
                    return@switchIfEmpty ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required for DRIVER role: ${constants.REQUIRED_DRIVER_FIELDS}")))
                }

                user.password = BCryptPasswordEncoder().encode(user.password)

                val savedUserMono = userService.register(user)
                savedUserMono.flatMap { savedUser ->
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                        BodyInserters.fromValue(AuthResponse(savedUser, tokenProvider.generateToken(savedUser)))
                    )
                }.onErrorResume { e ->
                    ServerResponse.badRequest().body(
                        BodyInserters.fromValue(BadRequestResponse("Document Verification Error: " + e.message))
                    )
                }
            }
        }.switchIfEmpty(
            ServerResponse.badRequest()
                .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required: ${constants.REQUIRED_FIELDS}")))
        ).onErrorResume {
            logger.info("Error: ${it.message}")
            if("VehicleInformation".toRegex().find(it.message.toString()) != null){
                ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required for DRIVER role: ${constants.REQUIRED_DRIVER_FIELDS}")))
            }else {
                ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required: ${constants.REQUIRED_FIELDS}")))
            }
        }
    }

    fun getProfile(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val user: Mono<User?> = userService.findByPhoneNumber(securityContext.authentication.principal as String)
            return@flatMap ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromProducer(user, User::class.java))
        }
    }


//    fun editUser(r: ServerRequest): Mono<ServerResponse> {
//        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
//            val userMono: Mono<User?> =
//                userService.findByPhoneNumber(securityContext.authentication.principal as String)
//            userMono.flatMap second@{ user ->
//                r.body(BodyExtractors.toMultipartData()).flatMap third@{ parts ->
//                    val map: Map<String, Part> = parts.toSingleValueMap()
//
//                    if (map.containsKey("preferences")) {
//                        val mapper = jacksonObjectMapper()
//                        val reader = mapper.readerFor(object : TypeReference<List<Preference>>() {})
//                        val preferencesString: String = (map["preferences"]!! as FormFieldPart).value()
//                        val preferences: List<Preference> = reader.readValue(preferencesString)
//                        user?.preference = preferences
//                    }
//
//                    if (map.containsKey("emergencyPhoneNumber")) {
//                        user?.emergencyPhoneNumber = (map["emergencyPhoneNumber"]!! as FormFieldPart).value()
//                    }
//
//                    if (map.containsKey("creditsBought")) {
//                        user?.creditsBought = (map["creditsBought"]!! as FormFieldPart).value().toDouble()
//                    }
//
//                    if (map.containsKey("role") && (map["role"]!! as FormFieldPart).value() == "DRIVER" && user?.role == "RIDER") {
//                        if (!constants.REQUIRED_DRIVER_FIELDS.all { map.containsKey(it) }) {
//                            return@third ServerResponse.badRequest()
//                                .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required for DRIVER role: ${constants.REQUIRED_DRIVER_FIELDS}")))
//                        }
//
//                        val drivingLicensePicture: FilePart = map["drivingLicensePicture"]!! as FilePart
//                        val ownershipDocPicture: FilePart = map["ownershipDocPicture"]!! as FilePart
//                        val insuranceDocPicture: FilePart = map["insuranceDocPicture"]!! as FilePart
//                        val vehiclePicture: FilePart = map["vehiclePicture"]!! as FilePart
//                        val licensePlateNumber: String = (map["licensePlateNumber"]!! as FormFieldPart).value()
//                        val year: String = (map["year"]!! as FormFieldPart).value()
//                        val make: String = (map["make"]!! as FormFieldPart).value()
//                        val model: String = (map["model"]!! as FormFieldPart).value()
//                        val kml: Double = (map["kml"]!! as FormFieldPart).value().toDouble()
//
//                        val vehicleInformation = VehicleInformation(
//                            year,
//                            make,
//                            model,
//                            licensePlateNumber,
//                            saveFile(drivingLicensePicture, "driving_licenses"),
//                            saveFile(ownershipDocPicture, "ownership_docs"),
//                            saveFile(insuranceDocPicture, "insurance_docs"),
//                            saveFile(vehiclePicture, "vehicles"),
//                            kml
//                        )
//                        user.vehicleInformation = vehicleInformation
//                    }
//
//                    var savedUserMono: Mono<User> = Mono.empty()
//                    if (user != null) {
//                        savedUserMono = userService.update(user)
//                    }
//
//                    savedUserMono.flatMap { savedUser ->
//                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
//                            BodyInserters.fromValue(savedUser)
//                        )
//                    }
//                }
//            }
//        }
//    }


    fun rate(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> = userService.findOne(r.pathVariable("id"))
            userMono.flatMap second@{ user ->
                r.body(BodyExtractors.toMultipartData()).flatMap third@{ parts ->
                    val map: Map<String, Part> = parts.toSingleValueMap()

                    if (user != null && map.containsKey("rating") && user.phoneNumber != securityContext.authentication.principal as String) {
                        try {
                            val rating = parseInt((map["rating"]!! as FormFieldPart).value())
                            if (rating < 1 || rating > 5) {
                                throw NumberFormatException()
                            }
                            user.rating[rating - 1]++
                        } catch (e: NumberFormatException) {
                            return@third ServerResponse.badRequest()
                                .body(BodyInserters.fromValue(BadRequestResponse("Invalid value for rating.")))
                        }

                    } else {
                        return@third ServerResponse.badRequest()
                            .body(BodyInserters.fromValue(BadRequestResponse("Can not rate logged in user.")))
                    }

                    userService.update(user).flatMap { savedUser ->
                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromValue(savedUser)
                        )
                    }
                }
            }.switchIfEmpty {
                ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("User not found.")))
            }
        }
    }
}