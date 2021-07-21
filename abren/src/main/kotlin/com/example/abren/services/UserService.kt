package com.example.abren.services

import com.example.abren.models.Preference
import com.example.abren.models.User
import com.example.abren.repositories.UserRepository
import com.example.abren.security.SecurityContextRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.*
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors


@Service
class UserService(private val userRepository: UserRepository) {

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
        val idCardMap = verifyDocument(user.idCardUrl, "ID_CARD")
        val drivingLicenseMap = verifyDocument(user.vehicleInformation?.licenseUrl, "DRIVING_LICENSE")

        if(idCardMap["isVerified"] as Boolean && drivingLicenseMap["isVerified"] as Boolean){
            return userRepository.save(user)
        }

        return Mono.empty()
    }

    @Throws(IllegalArgumentException::class)
    fun verifyDocument(url: String?, type: String): MutableMap<Any, Any> {
        val file = File("src/main/resources/scripts/DocumentVerifier.py")
        val processBuilder = ProcessBuilder("python", file.absolutePath)
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val results: List<String> = readProcessOutput(process.inputStream)
        logger.info(results.toString())

        process.waitFor()

        val mapper = jacksonObjectMapper()
        val reader = mapper.readerFor(object : TypeReference<MutableMap<Any, Any>>() {})
        val map = reader.readValue<MutableMap<Any, Any>>(Paths.get("src/main/resources/DocumentVerifierResult.json").toFile())
        logger.info(map.toString())

        val idCardData: MutableMap<*, *> = map["idCardData"] as MutableMap<*, *>
        val drivingLicenseData: MutableMap<*, *> = map["idCardData"] as MutableMap<*, *>

        map.replace("idCardData", idCardData);
        map.replace("drivingLicenseData", drivingLicenseData);

        if(!(map["isFaceVerified"] as Boolean)){
            throw IllegalArgumentException("$type: Profile picture doesn't match picture from document.")
        }

        if(!(map["isLogoVerified"] as Boolean)){
            throw IllegalArgumentException("$type: The document could not be validated.")
        }

        if(!(map["isTextVerified"] as Boolean)){
            throw IllegalArgumentException("$type: The text in the document could not be extracted.")
        }

        if(type == "ID_CARD"){ //TODO: Handle for driving license (ethiopian date)
            val expiryDate = idCardData["expiryDate"] as String
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
            val date = LocalDate.parse(expiryDate, formatter)

            if(date > LocalDate.now()){
                throw IllegalArgumentException("$type: The document has expired.")
            }
        }

        return map;
    }

    @Throws(IOException::class)
    private fun readProcessOutput(inputStream: InputStream): List<String> {
        BufferedReader(InputStreamReader(inputStream)).use { output ->
            return output.lines()
                .collect(Collectors.toList())
        }
    }

//    fun upgradeAccount(user: User): Mono<User?> {
//        return userRepository.save(user)
//    }
//
//    fun rate(user: User): Mono<User?> {
//        return userRepository.save(user)
//    }
}