package com.example.demo1

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicLong

@SpringBootApplication
class Demo1Application

fun main(args: Array<String>) {
	runApplication<Demo1Application>(*args)
}

@RestController
class GreetingController {
	val counter = AtomicLong()

	@GetMapping("/greeting")
	fun greeting(@RequestParam(value = "name", defaultValue = "World") name: String) =
			Greeting(counter.incrementAndGet(), "Hello, $name")

	data class Greeting(val id: Long, val content: String)
}