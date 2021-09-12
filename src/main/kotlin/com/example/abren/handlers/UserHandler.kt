package com.example.abren.handlers

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.abren.configurations.Constants
import com.example.abren.models.Preference
import com.example.abren.models.User
import com.example.abren.models.VehicleInformation
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
import java.io.File
import java.io.IOException
import java.lang.Integer.parseInt
import java.nio.file.Paths
import java.time.Instant


@Component
class UserHandler(private val userService: UserService, private val tokenProvider: TokenProvider, private val env: Environment) {
    val constants = Constants()
    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)

    fun login(r: ServerRequest): Mono<ServerResponse> {
        return r.body(BodyExtractors.toMultipartData()).flatMap { parts ->
            val map: MutableMap<String, Part> = parts.toSingleValueMap()
            logger.info(map.values.toString())
            val requiredKeys = arrayListOf("phoneNumber", "password")
            if (!requiredKeys.all { map.containsKey(it) }) {
                return@flatMap ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required: $requiredKeys")))
            }

            val phoneNumber = (map["phoneNumber"] as FormFieldPart).value()
            val password = (map["password"] as FormFieldPart).value()

            userService.findByPhoneNumber(phoneNumber)
                .flatMap second@{ user ->
                    if (user != null && BCryptPasswordEncoder().matches(password, user.password)) {
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
        }
    }

    fun signup(r: ServerRequest): Mono<ServerResponse> {
        return r.body(BodyExtractors.toMultipartData()).flatMap first@{ parts ->
            val map: Map<String, Part> = parts.toSingleValueMap()
            logger.info(map.toString())
            if (!constants.REQUIRED_FIELDS.all { map.containsKey(it) }) {
                logger.info("Rider Keys")
                return@first ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required: ${constants.REQUIRED_FIELDS}")))
            }

            val profilePicture: FilePart = map["profilePicture"]!! as FilePart
            val idCardPicture: FilePart = map["idCardPicture"]!! as FilePart
            val idCardBackPicture: FilePart = map["idCardBackPicture"]!! as FilePart
            val phoneNumber: String = (map["phoneNumber"]!! as FormFieldPart).value()
            val emergencyPhoneNumber: String = (map["emergencyPhoneNumber"]!! as FormFieldPart).value()
            val password: String = (map["password"]!! as FormFieldPart).value()



            if (phoneNumber.length != 12) {
                logger.info("PhoneNum")
                return@first ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("Invalid Input for phoneNumber")))
            }

            if (emergencyPhoneNumber.length != 12) {
                logger.info("Emer")
                return@first ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("Invalid Input for emergencyPhoneNumber")))
            }

            val retrievedUser: Mono<User?> = userService.findByPhoneNumber(phoneNumber)
            retrievedUser.flatMap {
                logger.info("Exist")
                return@flatMap ServerResponse.badRequest()
                    .body(BodyInserters.fromValue(BadRequestResponse("User already exists")))
            }.switchIfEmpty {
                val role: String = (map["role"]!! as FormFieldPart).value()
                if (role != "RIDER" && role != "DRIVER") {
                    logger.info("Role")
                    return@switchIfEmpty ServerResponse.badRequest()
                        .body(BodyInserters.fromValue(BadRequestResponse("The role field must be one of the following values: ['RIDER', 'DRIVER']")))
                }

                val user = User(
                    password = BCryptPasswordEncoder().encode(password),
                    phoneNumber = phoneNumber,
                    emergencyPhoneNumber = emergencyPhoneNumber,
                    role = role,
                    idCardUrl = saveFile(idCardPicture, "id_cards"),
                    idCardBackUrl = saveFile(idCardBackPicture, "id_card_backs"),
                    profilePictureUrl = saveFile(profilePicture, "profiles")
                )

                if (role == "DRIVER") {
                    if (!constants.REQUIRED_DRIVER_FIELDS.all { map.containsKey(it) }) {
                        logger.info("Driver Keys")
                        return@switchIfEmpty ServerResponse.badRequest()
                            .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required for DRIVER role: ${constants.REQUIRED_DRIVER_FIELDS}")))
                    }

                    val drivingLicensePicture: FilePart = map["drivingLicensePicture"]!! as FilePart
                    val ownershipDocPicture: FilePart = map["ownershipDocPicture"]!! as FilePart
                    val insuranceDocPicture: FilePart = map["insuranceDocPicture"]!! as FilePart
                    val vehiclePicture: FilePart = map["vehiclePicture"]!! as FilePart
                    val licensePlateNumber: String = (map["licensePlateNumber"]!! as FormFieldPart).value()
                    val year: String = (map["year"]!! as FormFieldPart).value()
                    val make: String = (map["make"]!! as FormFieldPart).value()
                    val model: String = (map["model"]!! as FormFieldPart).value()
                    val kml: Double = (map["kml"]!! as FormFieldPart).value().toDouble()

                    val vehicleInformation = VehicleInformation(
                        year,
                        make,
                        model,
                        licensePlateNumber,
                        saveFile(drivingLicensePicture, "driving_licenses"),
                        saveFile(ownershipDocPicture, "ownership_docs"),
                        saveFile(insuranceDocPicture, "insurance_docs"),
                        saveFile(vehiclePicture, "vehicles"),
                        kml
                    )
                    user.vehicleInformation = vehicleInformation
                }

                if (map.containsKey("preferences")) {
                    val mapper = jacksonObjectMapper()
                    val reader = mapper.readerFor(object : TypeReference<List<Preference>>() {})
                    val preferencesString: String = (map["preferences"]!! as FormFieldPart).value()
                    val preferences: List<Preference> = reader.readValue(preferencesString)
                    user.preference = preferences
                }

//                val savedUserMono: Mono<User>
//                try {
//
//                } catch (e: IllegalArgumentException) {
//                    return@switchIfEmpty ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
//                        BodyInserters.fromValue("Document Verification Error: " + e.message)
//                    )
//                }
                val savedUserMono = userService.register(user)
                savedUserMono.flatMap { savedUser ->
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                        BodyInserters.fromValue(AuthResponse(savedUser, tokenProvider.generateToken(savedUser)))
                    )
                }.onErrorResume { e ->
                    ServerResponse.badRequest().body(
                        BodyInserters.fromValue(BadRequestResponse("Document Verification Error: " + e.message)))
                }
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


    fun editUser(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)
            userMono.flatMap second@{ user ->
                r.body(BodyExtractors.toMultipartData()).flatMap third@{ parts ->
                    val map: Map<String, Part> = parts.toSingleValueMap()

                    if (map.containsKey("preferences")) {
                        val mapper = jacksonObjectMapper()
                        val reader = mapper.readerFor(object : TypeReference<List<Preference>>() {})
                        val preferencesString: String = (map["preferences"]!! as FormFieldPart).value()
                        val preferences: List<Preference> = reader.readValue(preferencesString)
                        user?.preference = preferences
                    }

                    if (map.containsKey("emergencyPhoneNumber")) {
                        user?.emergencyPhoneNumber = (map["emergencyPhoneNumber"]!! as FormFieldPart).value()
                    }

                    if (map.containsKey("creditsBought")) {
                        user?.creditsBought = (map["creditsBought"]!! as FormFieldPart).value().toDouble()
                    }

                    if (map.containsKey("role") && (map["role"]!! as FormFieldPart).value() == "DRIVER" && user?.role == "RIDER") {
                        if (!constants.REQUIRED_DRIVER_FIELDS.all { map.containsKey(it) }) {
                            return@third ServerResponse.badRequest()
                                .body(BodyInserters.fromValue(BadRequestResponse("The following fields are required for DRIVER role: ${constants.REQUIRED_DRIVER_FIELDS}")))
                        }

                        val drivingLicensePicture: FilePart = map["drivingLicensePicture"]!! as FilePart
                        val ownershipDocPicture: FilePart = map["ownershipDocPicture"]!! as FilePart
                        val insuranceDocPicture: FilePart = map["insuranceDocPicture"]!! as FilePart
                        val vehiclePicture: FilePart = map["vehiclePicture"]!! as FilePart
                        val licensePlateNumber: String = (map["licensePlateNumber"]!! as FormFieldPart).value()
                        val year: String = (map["year"]!! as FormFieldPart).value()
                        val make: String = (map["make"]!! as FormFieldPart).value()
                        val model: String = (map["model"]!! as FormFieldPart).value()
                        val kml: Double = (map["kml"]!! as FormFieldPart).value().toDouble()

                        val vehicleInformation = VehicleInformation(
                            year,
                            make,
                            model,
                            licensePlateNumber,
                            saveFile(drivingLicensePicture, "driving_licenses"),
                            saveFile(ownershipDocPicture, "ownership_docs"),
                            saveFile(insuranceDocPicture, "insurance_docs"),
                            saveFile(vehiclePicture, "vehicles"),
                            kml
                        )
                        user.vehicleInformation = vehicleInformation
                    }

                    var savedUserMono: Mono<User> = Mono.empty()
                    if (user != null) {
                        savedUserMono = userService.update(user)
                    }

                    savedUserMono.flatMap { savedUser ->
                        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromValue(savedUser)
                        )
                    }
                }
            }
        }
    }


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

    fun saveFile(filePart: FilePart, folder: String): String {
//        val target = Paths.get("uploads/images/$folder")
//            .resolve(Instant.now().toEpochMilli().toString() + ".jpg") //TODO: Make sure this location works
//        try {
//            Files.deleteIfExists(target)
//            val file = Files.createFile(target).toFile()
//            val content = filePart.content().blockFirst() //TODO: Try not blocking or run in thread
//            val buffer = ByteArray(content?.readableByteCount()!!) //TODO: Be careful
//            content.read(buffer);
//            file.writeBytes(buffer)
//            return target.toString()
//        } catch (e: IOException) {
//            throw RuntimeException(e)
//        }

        val fileName = "${Instant.now().toEpochMilli()}"

        val cloudinary = Cloudinary(ObjectUtils.asMap(
            "cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME"),
            "api_key", System.getenv("CLOUDINARY_API_KEY"),
            "api_secret", System.getenv("CLOUDINARY_API_SECRET"),
            "secure", true))


        val params: Map<*, *> = ObjectUtils.asMap(
            "public_id", "abren/uploads/$folder/$fileName",
            "overwrite", true,
            "resource_type", "image"
        )

        try {
            val convFile = File(fileName)
            convFile.createNewFile()
            val content = filePart.content().blockFirst() //TODO: Try not blocking or run in thread
            val buffer = ByteArray(content?.readableByteCount()!!) //TODO: Be careful
            content.read(buffer);
            convFile.writeBytes(buffer)

            val uploadResult = cloudinary.uploader().upload(convFile, params)
            return uploadResult["secure_url"] as String
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}