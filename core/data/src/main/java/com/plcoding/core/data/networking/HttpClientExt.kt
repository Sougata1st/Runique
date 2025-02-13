package com.plcoding.core.data.networking

import com.plcoding.core.data.BuildConfig
import com.plcoding.core.domain.util.DataError
import com.plcoding.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.content.TextContent
import io.ktor.http.HttpMethod
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

suspend inline fun <reified Response: Any> HttpClient.get(
    baseUrl: String = BuildConfig.BASE_URL,
    route: String,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> {
    return safeCall {
        val builder = HttpRequestBuilder().apply {
            url(constructRoute(route, baseUrl = baseUrl))
            method = HttpMethod.Get
            queryParameters.forEach { (key, value) ->
                parameter(key, value)
            }
        }
        println("The curl is \n ${toCurlCommand(builder)}") // Log cURL
        get(builder)
    }
}

suspend inline fun <reified Request, reified Response: Any> HttpClient.post(
    baseUrl: String = BuildConfig.BASE_URL,
    queryParameters: Map<String, Any?> = mapOf(),
    route: String,
    body: Request
): Result<Response, DataError.Network> {
    return safeCall {
        val builder = HttpRequestBuilder().apply {
            url(constructRoute(route, baseUrl = baseUrl))
            method = HttpMethod.Post
            queryParameters.forEach{(key, value) ->
                parameter(key, value)
            }
            setBody(body)
        }
        println("The curl is \n ${toCurlCommand(builder)}") // Log cURL
        post(builder)
    }
}

suspend inline fun <reified Response: Any> HttpClient.delete(
    baseUrl: String = BuildConfig.BASE_URL,
    route: String,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> {
    return safeCall {
        val builder = HttpRequestBuilder()
            .apply {
                url(constructRoute(route, baseUrl = baseUrl))
                method = HttpMethod.Delete
                queryParameters.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
        println("The curl is \n ${toCurlCommand(builder)}") // Log cURL
        delete(builder)
    }
}

suspend inline fun <reified T> safeCall(execute: () -> HttpResponse): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch(e: UnresolvedAddressException) {
        e.printStackTrace()
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: SerializationException) {
        e.printStackTrace()
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch(e: Exception) {
        if(e is CancellationException) throw e
        e.printStackTrace()
        return Result.Error(DataError.Network.UNKNOWN)
    }

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Network> {
    return when(response.status.value) {
        in 200..299 -> Result.Success(response.body<T>())
        401 -> Result.Error(DataError.Network.UNAUTHORIZED)
        408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        409 -> Result.Error(DataError.Network.CONFLICT)
        413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
        429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
        in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}

fun constructRoute(route: String,baseUrl: String = BuildConfig.BASE_URL): String {
    return when {
        route.contains(baseUrl) -> route
        route.startsWith("/") -> baseUrl + route
        else -> "$baseUrl/$route"
    }
}


fun toCurlCommand(builder: HttpRequestBuilder): String {
    val url = builder.url.buildString()
    val headers = builder.headers.entries().joinToString(" ") {
        """-H "${it.key}: ${it.value.joinToString(",")}""""
    }
    val data = (builder.body as? TextContent)?.text ?: ""

    return "curl -X ${builder.method.value} $headers '$url' -d '$data'"
}
