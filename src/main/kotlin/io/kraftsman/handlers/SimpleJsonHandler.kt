package io.kraftsman.handlers

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import io.kraftsman.dtoc.News
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.time.LocalDateTime

class SimpleJsonHandler: HttpFunction {

    @Throws(IOException::class)
    override fun service(request: HttpRequest, response: HttpResponse) {

        if ("GET" != request.method) {

            with(response) {
                setStatusCode(HttpURLConnection.HTTP_BAD_METHOD)
                writer.write("Bad method")
            }

            return
        }

        val limit = request.queryParameters["limit"]?.first()?.toIntOrNull() ?: 10

        val news = (1..limit).map { id ->
            News(
                id = id.toString(),
                title = id.toString(),
                author = "Title: $id",
                content = "Author: $id",
                coverUrl = "https://www.example.com",
                permalink = "https://www.example.com",
                publishedAt = LocalDateTime.now(),
            )
        }


        with(response) {
            with(response) {
                setStatusCode(HttpURLConnection.HTTP_OK)
                setContentType("application/json")
                writer.write(
                    Json.encodeToString(
                        mapOf("data" to news)
                    )
                )
            }

        }
    }
}
