package io.kraftsman.handlers

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import io.kraftsman.dtoc.News
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import tw.ktrssreader.generated.CustomChannelParser
import java.io.IOException
import java.net.HttpURLConnection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class RssListHandler: HttpFunction {

    private val rssSource = mapOf(
        "kotlin-blog" to "https://blog.jetbrains.com/kotlin/feed/",
        "andy-blog" to "https://andyludeveloper.medium.com/feed",
        "drawson-medium" to "https://drawson.medium.com/feed",
    )

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

        val client = OkHttpClient()

        val news = mutableListOf<News>()

        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.ENGLISH)


        rssSource.forEach { name, url ->
            val rssRequest = Request.Builder().url(url).build()
            val xmlString = client.newCall(rssRequest).execute().body?.string() ?: ""
            val rssChannel = CustomChannelParser.parse(xmlString)
            rssChannel.items.forEachIndexed { index, item ->
                val parsedPubDate = item.pubDate?.substringAfter(", ")
                    ?.replace(" +0000", "")
                    ?.replace(" GMT", "")
                    ?.trim()
                    .toString()

                news.add(
                    News(
                        id = "$name-${index + 1}",
                        title = item.title ?: "",
                        author = item.author ?: "",
                        content =  item.description ?: "",
                        coverUrl = item.featuredImage ?: "",
                        permalink = item.link ?: "",
                        publishedAt = LocalDateTime.parse(parsedPubDate, formatter),
                    )
                )
            }
        }
        val parsedNews = news.sortedByDescending { it.publishedAt }
            .take(limit)

        with(response) {
            with(response) {
                setStatusCode(HttpURLConnection.HTTP_OK)
                setContentType("application/json")
                writer.write(
                    Json.encodeToString(
                        mapOf("data" to parsedNews)
                    )
                )
            }

        }
    }
}
