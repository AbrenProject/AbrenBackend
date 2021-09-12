package com.example.abren.services

import com.example.abren.models.Request
import com.example.abren.models.Ride
import com.example.abren.repositories.RideRepository
import com.example.abren.requests.DocumentVerifierRequest
import com.example.abren.requests.NearbyRidesRequest
import com.example.abren.responses.DocumentVerifierResponse
import com.example.abren.responses.NearbyRidesResponse
import com.example.abren.security.SecurityContextRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class RideService(private val rideRepository: RideRepository, private val requestService: RequestService, private val webClientBuilder: WebClient.Builder) {

    private val logger: Logger = LoggerFactory.getLogger(SecurityContextRepository::class.java)
    fun findAll(): Flux<Ride?> {
        return rideRepository.findAll();
    }

    fun findAllById(ids: Set<String>): Flux<Ride?> {
        return rideRepository.findAllById(ids);
    }

    fun findAllById(ids: MutableList<String>): Flux<Ride?> {
        return rideRepository.findAllById(ids);
    }

    fun findByStatus(status: String): Flux<Ride?> {
        return rideRepository.findByStatus(status);
    }

    fun findOne(id: String): Mono<Ride?> {
        return rideRepository.findById(id)
    }

    fun update(ride: Ride): Mono<Ride> {
        return rideRepository.save(ride) //TODO: Make better
    }

    fun delete(id: String): Mono<Ride> {
        return rideRepository
            .findById(id)
            .flatMap { u ->
                u?._id?.let { rideRepository.deleteById(it).thenReturn(u) }
            }
    }

    fun count(): Mono<Long> {
        return rideRepository.count()
    }

    fun create(ride: Ride): Mono<Ride> {
        return rideRepository.save(ride)
    }

    @Scheduled(fixedDelay = 20000)
    fun prepareRides() : NearbyRidesResponse? { //TODO: Run in thread?
        logger.info("Preparing Rides")
        val activeRides = findByStatus("ACTIVE") //TODO: Check how to pass this to python
        val activeRequests = requestService.findByStatus("PENDING")

        val requestsList = activeRequests.collectList().block()
        val ridesList = activeRides.collectList().block()
//
        logger.info("Requests: $requestsList")
        logger.info("Rides: $ridesList")

        return getNearby(ridesList, requestsList).block()



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

    fun getNearby(rides: MutableList<Ride?>?, requests: MutableList<Request?>?): Mono<NearbyRidesResponse> {
        val webClient = webClientBuilder.baseUrl("http://localhost:5000").build()

        val nearbyRidesRequest = NearbyRidesRequest(rides, requests)
        return webClient.post()
            .uri("/nearest-rides")
            .body(Mono.just(nearbyRidesRequest), NearbyRidesRequest::class.java)
            .retrieve()
            .bodyToMono(NearbyRidesResponse::class.java)
    }
}