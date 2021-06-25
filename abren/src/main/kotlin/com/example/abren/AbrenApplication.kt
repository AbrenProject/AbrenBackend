package com.example.abren

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.*
import java.util.stream.Collectors


@SpringBootApplication
@EnableReactiveMongoRepositories
class AbrenApplication

fun main(args: Array<String>) {
	runApplication<AbrenApplication>(*args)
}

@Bean
fun mongoClient(): MongoClient? {
	return MongoClients.create()
}

fun getDatabaseName(): String? {
	return "abrendatabase"
}
// Trial handler (will be removed)
@RestController
class TestHandler(private val userRepository: UserRepository) {

	@GetMapping(value = ["/try_python"])
	fun tryPython () : ResponseEntity<List<String>> {
		val processBuilder = ProcessBuilder("python", resolvePythonScriptPath("try.py"))
		processBuilder.redirectErrorStream(true)

		val process = processBuilder.start()
		val results: List<String> = readProcessOutput(process.inputStream)

		val exitCode = process.waitFor()
		return ResponseEntity(results, HttpStatus.OK);
	}

	@Throws(IOException::class)
	private fun readProcessOutput(inputStream: InputStream): List<String> {
		BufferedReader(InputStreamReader(inputStream)).use { output ->
			return output.lines()
				.collect(Collectors.toList())
		}
	}

	private fun resolvePythonScriptPath(filename: String): String {
		val file = File("src/main/resources/scripts/$filename")
		return file.absolutePath
	}

	@GetMapping(value = ["/try_database"])
	fun tryDatabase(): Flux<User?> {
		return userRepository.findAll();
	}
}

@Document
data class User (var _id: String,
	var name: String)


@Repository
interface UserRepository : ReactiveMongoRepository<User?, String?>