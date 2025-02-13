package com.plcoding.run.network

import com.plcoding.core.data.networking.constructRoute
import com.plcoding.core.data.networking.delete
import com.plcoding.core.data.networking.get
import com.plcoding.core.data.networking.safeCall
import com.plcoding.core.domain.run.RemoteRunDataSource
import com.plcoding.core.domain.run.Run
import com.plcoding.core.domain.util.DataError
import com.plcoding.core.domain.util.EmptyResult
import com.plcoding.core.domain.util.Result
import com.plcoding.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

class KtorRemoteRunDataSource(
    private val httpClient: HttpClient
): RemoteRunDataSource {

    override suspend fun getRuns(): Result<List<Run>, DataError.Network> {
        return httpClient.get<List<RunDto>>(
            baseUrl = "https://runrequest-service.onrender.com",
            route = "/api/run/get-all-run",
        ).map { runDtos ->
            runDtos.map { it.toRun() }
        }
    }

    override suspend fun postRun(run: Run, mapPicture: ByteArray): Result<Run, DataError.Network> {
        val createRunRequestJson = Json.encodeToString(run.toCreateRunRequest())

        val result = safeCall<RunDto> {
//            httpClient.submitFormWithBinaryData(
//                url = constructRoute("/api/run/create-run", baseUrl = "https://runrequest-service.onrender.com"),
//                formData = formData {
//                    append("MAP_PICTURE", mapPicture, Headers.build {
//                        append(HttpHeaders.ContentType, "image/jpeg")
//                        append(HttpHeaders.ContentDisposition, "filename=mappicture.jpg")
//                    })
//                    append("RUN_DATA", createRunRequestJson, Headers.build {
//                        append(HttpHeaders.ContentType, "text/plain")
//                        append(HttpHeaders.ContentDisposition, "form-data; name=\"RUN_DATA\"")
//                    })
//                }
//            ) {
//                method = HttpMethod.Post
//            }

            httpClient.submitFormWithBinaryData(
                url = "https://runrequest-service.onrender.com/api/run/create-run",
                formData = formData {
                    // Append image file as byte array
                    append("MAP_PICTURE", mapPicture, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=mappicture.jpg")
                    })

                    // Append JSON as a text field
                    append("RUN_DATA", createRunRequestJson, Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    })
                }
            ){
                method = HttpMethod.Post
            }
        }
        return result.map {
            it.toRun()
        }
    }

    override suspend fun deleteRun(id: String): EmptyResult<DataError.Network> {
        return httpClient.delete(
            baseUrl = "https://runrequest-service.onrender.com",
            route = "/api/run/delete/$id"
        )
    }
}



fun generateCurlCommand(url: String, mapPicture: ByteArray, runDataJson: String): String {
    val boundary = "----KtorBoundary"

    val curlCommand = buildString {
        append("curl -X POST \"$url\" \\\n")
        append("  -H \"Content-Type: multipart/form-data; boundary=$boundary\" \\\n")

        // Add MAP_PICTURE
        append("  -F \"MAP_PICTURE=@mappicture.jpg;type=image/jpeg\" \\\n")

        // Add RUN_DATA (escaped properly)
        val escapedJson = URLEncoder.encode(runDataJson, "UTF-8").replace("+", "%20")
        append("  -F \"RUN_DATA=$escapedJson\" \n")
    }

    return curlCommand
}