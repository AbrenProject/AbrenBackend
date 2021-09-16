package com.example.abren.handlers

import com.example.abren.models.*
import com.example.abren.responses.RidesResponse
import com.example.abren.security.SecurityContextRepository
import com.example.abren.services.RequestService
import com.example.abren.services.RideService
import com.example.abren.services.UserService
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
import java.lang.Boolean.FALSE
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap


@Component
class RideHandler(
    private val rideService: RideService,
    private val userService: UserService,
    private val requestService: RequestService
) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)

    var startNeighbors: MutableMap<String, MutableSet<String>> = HashMap()
    var destinationNeighbors: MutableMap<String, MutableSet<String>> = HashMap()

    //TODO: UPDATE THE STATUS OF REQUEST TO ACCEPTED
    fun acceptRequest(r: ServerRequest): Mono<ServerResponse> {
        val rideMono = rideService.findOne(r.pathVariable("id"))
        return rideMono.flatMap { ride->
                r.queryParam("requestId").map{  requestId->
                ride?.acceptedRequests?.add(requestId)
                ride?.requests?.remove(requestId)
              }
            val updatedRide = rideService.update(ride!!)
            ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                    BodyInserters.fromProducer(updatedRide, Ride::class.java))
        }.switchIfEmpty(
                ServerResponse.badRequest()
                        .body(BodyInserters.fromValue("Ride not found."))
        )
        }

    fun getRideRequests(r:ServerRequest): Mono<ServerResponse>{
        val rideMono = rideService.findOne(r.pathVariable("id"))
        return rideMono.flatMap { ride ->
            val rideRequestsId = ride?.requests!!
            val rideRequests = requestService.findAllByIds(rideRequestsId)
            ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromProducer(rideRequests, Request::class.java))
        }.switchIfEmpty(
                ServerResponse.badRequest()
                        .body(BodyInserters.fromValue("Ride not found."))
        )


    }

    fun createRide(r:ServerRequest): Mono<ServerResponse>{
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            val userMono: Mono<User?> =
                    userService.findByPhoneNumber(securityContext.authentication.principal as String)
            userMono.flatMap { user ->
                val rideMono = r.bodyToMono(Ride::class.java)
                rideMono.flatMap { ride ->
                    ride.driverId = user?._id
                    ride.status="NOT STARTED" //TODO: Check status options
                    ride.createdAt= LocalDateTime.now()
                    val otpCode = ((Math.random() * 900000).toInt() + 100000).toString()
                    val otp = Otp(otpCode,LocalDateTime.now(),FALSE)
                    ride.otp=otp
                    val savedRide = rideService.create(ride)
                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                            BodyInserters.fromProducer(savedRide, Request::class.java)
                    )
                }
            }


        }
    }

    fun getRides(r: ServerRequest): Mono<ServerResponse> {
        return ReactiveSecurityContextHolder.getContext().flatMap first@ { securityContext ->
            val userMono: Mono<User?> =
                userService.findByPhoneNumber(securityContext.authentication.principal as String)

            userMono.flatMap second@ { user ->
                val requestMono = requestService.findOne(r.pathVariable("id"))
                requestMono.flatMap third@{ request ->
                    if (user?._id != request?.riderId) {
                        return@third ServerResponse.status(401)
                            .body(BodyInserters.fromValue("Request doesn't belong to logged in user."))
                    } else {
                        val requestedFlux = rideService.findAllById(request?.requestedRides!!)
                        val neighborsStart = startNeighbors[request._id]
                        val neighborsDest = destinationNeighbors[request._id]
                        if (neighborsStart != null && neighborsDest != null) { //TODO: Make sure this handles everything
                            val nearbyIds = neighborsStart intersect neighborsDest
                            logger.info("Cluster: $nearbyIds")
                            return@third rideService.findAllById(nearbyIds).collectList().flatMap fourth@ { nearby -> //TODO: Handle Duplicates
                               requestedFlux.collectList().flatMap { requested ->
                                    ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromValue(RidesResponse(requested, nearby)))
                                }
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

    @Scheduled(fixedDelay = 10000)
    fun prepareRides() { //TODO: Run in thread?
        logger.info("Preparing Rides")
        val activeRides = rideService.findByStatus("ACTIVE") //TODO: Check how to pass this to python
        val activeRequests = requestService.findByStatus("PENDING")

//        val requestsList = activeRequests.collectList().block()
//        val newRequestList = requestsList?.stream()?.map { elt ->  }?.collect(Collectors.toList())
//
//        logger.info("Requests: $requests")

//        val mapper = ObjectMapper()
//        mapper.writeValue(Paths.get("src/main/resources/ClusteringInputRequests.json").toFile(), activeRequests.collectList().block())

//        File("src/main/resources/ClusteringInputRides.json").writeText(activeRides.collectList().block().toString())
//        File("src/main/resources/ClusteringInputRequests.json").writeText(activeRequests.collectList().block().toString())

//        val file = File("src/main/resources/scripts/LocationClustering.py")
//        val processBuilder = ProcessBuilder("python", file.absolutePath)
//        processBuilder.redirectErrorStream(true)
//
//        val process = processBuilder.start()
//        val results: List<String> = readProcessOutput(process.inputStream)
//        logger.info(results.toString())
//
//        process.waitFor()

//        val mapper = jacksonObjectMapper()
//        val reader = mapper.readerFor(object : TypeReference<MutableMap<Any, Any>>() {})
//        val map = reader.readValue<MutableMap<Any, Any>>(
//            Paths.get("src/main/resources/LocationClusteringResult.json").toFile()
//        )
//
//        startNeighbors = map["startNeighbors"] as MutableMap<String, MutableSet<String>> //TODO: Make sure this is fine
//        logger.info("Start Neighbors: $startNeighbors")
//
//        destinationNeighbors = map["destinationNeighbors"] as MutableMap<String, MutableSet<String>>
//        logger.info("Destination Neighbors: $destinationNeighbors")

    }

    @Throws(IOException::class)
    private fun readProcessOutput(inputStream: InputStream): List<String> {
        BufferedReader(InputStreamReader(inputStream)).use { output ->
            return output.lines()
                .collect(Collectors.toList())
        }
    }

}