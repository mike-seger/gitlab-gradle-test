package com.net128.app.gitlab.test

import com.fasterxml.jackson.module.kotlin.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@SpringBootTest
class ApplicationTest {
    @Value("\${gitlab-gradle-test.remote-port}")
    private val remotePort: Int = 8080

    @Value("\${gitlab-gradle-test.hello-name}")
    private val helloName: String = "World"

    val mapper = jacksonObjectMapper()

    @Test
    fun whenAdding1and3_thenAnswerIs4() {
        val client = HttpClient.newBuilder().build()
        val content = "Hello, $helloName"
        val url = "http://localhost:${remotePort}/greeting?name=" +
            URLEncoder.encode(helloName, Charsets.UTF_8)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(10000))
            .build()
        val response: HttpResponse<String> = client.send(request,
            HttpResponse.BodyHandlers.ofString())
        assertEquals(HttpStatus.OK.value(), response.statusCode())
        val greeting = mapper.readValue<Greeting>(response.body())
        assertEquals(content, greeting.content)
    }

    private data class Greeting(val id: Long, val content: String)
}