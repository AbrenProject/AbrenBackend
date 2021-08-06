package com.example.abren.handlers

import com.example.abren.models.User
import com.example.abren.responses.RidesResponse
import com.example.abren.security.SecurityContextRepository
import com.example.abren.services.RequestService
import com.example.abren.services.RideService
import com.example.abren.services.UserService
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.io.*
import java.nio.file.Paths
import java.util.stream.Collectors

@Component
class RideHandler(
    private val rideService: RideService,
    private val userService: UserService,
    private val requestService: RequestService
) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)

    var objectToDestinationCluster: MutableMap<String, String> = HashMap()
    var objectToStartCluster: MutableMap<String, String> = HashMap()
    var clusterToObjects: MutableMap<String, MutableSet<String>> = HashMap()


    fun getRides(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)

            userMono.flatMap { user ->
                val requestMono = requestService.findOne(r.pathVariable("id"))
                requestMono.flatMap third@{ request ->
                    if (user?._id != request?.riderId) {
                        return@third ServerResponse.status(401)
                            .body(BodyInserters.fromValue("Request doesn't belong to logged in user."))
                    } else {
                        val requested = request?.requestedRides
                        val clusterStart = clusterToObjects[objectToStartCluster[request?._id]]
                        val clusterDest = clusterToObjects[objectToDestinationCluster[request?._id]]
                        if (clusterStart != null && clusterDest != null) { //TODO: Make sure this handles everything
                            val cluster = clusterStart intersect clusterDest
                            logger.info("Cluster: $cluster")
                            return@third rideService.findAllById(cluster).collectList().flatMap { nearby ->
                                ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(RidesResponse(requested, nearby)))
                            }
                        }
                    }
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue("There are no nearby rides that match your destination."))
                }.switchIfEmpty(
                    ServerResponse.badRequest()
                        .body(BodyInserters.fromValue("Request not found."))
                )
            }
        }
    }

    @Scheduled(fixedRate = 10000)
    fun getRides() {
        logger.info("Getting Rides")
        val activeRides = rideService.findByStatus("ACTIVE") //TODO: Check how to pass this to python
        val activeRequests = requestService.findByStatus("PENDING")

        val file = File("src/main/resources/scripts/LocationClustering.py")
        val processBuilder = ProcessBuilder("python", file.absolutePath)
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val results: List<String> = readProcessOutput(process.inputStream)
        logger.info(results.toString())

        process.waitFor()

        val mapper = jacksonObjectMapper()
        val reader = mapper.readerFor(object : TypeReference<MutableMap<Any, Any>>() {})
        val map = reader.readValue<MutableMap<Any, Any>>(
            Paths.get("src/main/resources/LocationClusteringResult.json").toFile()
        )
        logger.info(map.toString())

        val startClusters: List<*> = map["startClusters"] as List<*>
        val destinationClusters: List<*> = map["destinationClusters"] as List<*>

        startClusters.forEach { item -> //TODO: Check if this is better in python
            if (item is MutableMap<*, *>) {
                val itemObject = item["object"] as String
                val itemCluster = item["cluster"] as String
                objectToStartCluster[itemObject] = itemCluster

                if (!clusterToObjects.containsKey(itemCluster)) {
                    clusterToObjects[itemCluster] = HashSet()
                }

                clusterToObjects[itemCluster]?.add(itemObject)
            }
        }

        destinationClusters.forEach { item ->
            if (item is MutableMap<*, *>) {
                logger.info("destClusters: $destinationClusters")
                val itemObject = item["object"] as String
                val itemCluster = item["cluster"] as String
                objectToDestinationCluster[itemObject] = itemCluster

                if (!clusterToObjects.containsKey(itemCluster)) {
                    clusterToObjects[itemCluster] = HashSet()
                }

                clusterToObjects[itemCluster]?.add(itemObject)
            }
        }
    }

    @Throws(IOException::class)
    private fun readProcessOutput(inputStream: InputStream): List<String> {
        BufferedReader(InputStreamReader(inputStream)).use { output ->
            return output.lines()
                .collect(Collectors.toList())
        }
    }

}