package com.example.abren

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime

@SpringBootApplication
class AbrenApplication

fun main(args: Array<String>) {
	runApplication<AbrenApplication>(*args)
}

// Trial handler (will be removed)
@RestController
class TestHandler {
	@GetMapping(value = ["/stocks/{symbol}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun prices (@PathVariable symbol: String) : Flux<StockPrice> {
		return Flux.interval(Duration.ofSeconds(1))
				.map { StockPrice(symbol, 50.0, LocalDateTime.now())}
	}
}

data class StockPrice (var symbol: String,
						var price: Double,
						var time: LocalDateTime)
