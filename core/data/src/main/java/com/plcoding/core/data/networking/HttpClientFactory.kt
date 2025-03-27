package com.plcoding.core.data.networking

import android.util.Log
import com.plcoding.core.data.BuildConfig
import com.plcoding.core.domain.AuthInfo
import com.plcoding.core.domain.SessionStorage
import com.plcoding.core.domain.util.Result
import com.sougata.core.data.networking.refreshApi.RefreshApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class HttpClientFactory(
    private val sessionStorage: SessionStorage
) {

    fun build(): HttpClient {

        val retrofit = Retrofit.Builder()
            .baseUrl("https://e-store-user-service.onrender.com/") // Set your base URL
            .addConverterFactory(GsonConverterFactory.create()) // Add Gson converter
            .build()

        // Create an instance of the ApiService
        val apiService = retrofit.create(RefreshApiService::class.java)
        
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    json = Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.d(message)
                    }
                }
                level = LogLevel.ALL
            }
            defaultRequest {
                contentType(ContentType.Application.Json)

                val authInfo = runBlocking {
                    sessionStorage.get()
                }

                if (authInfo?.accessToken != null && authInfo.accessToken.isNotEmpty() && authInfo.refreshToken.isNotEmpty()) {
                    headers.append("Authorization", "Bearer ${authInfo.accessToken}")
                }
            }
            install(Auth) {
                bearer {
//                    loadTokens {
//                        val info = sessionStorage.get()
//                        BearerTokens(
//                            accessToken = info?.accessToken ?: "",
//                            refreshToken = info?.refreshToken ?: ""
//                        )
//                    }
                    refreshTokens {
                        val authInfo = sessionStorage.get()
                        val response = try {
                            // Perform the POST request with Retrofit
                            val tokenResponse = apiService.getAccessToken(
                                AccessTokenRequest(authInfo?.refreshToken ?: "")
                            )
                            println("This is successful ${tokenResponse.isSuccessful}")
                            if (tokenResponse.isSuccessful) {
                                val data = tokenResponse.body()?.data
                                if (data != null) {
                                    println("This is data ${data.accessToken}")
                                    sessionStorage.set(
                                        AuthInfo(
                                            accessToken = data.accessToken,
                                            refreshToken = data.refreshToken,
                                            userId = "jabcka"
                                        )
                                    )
                                    BearerTokens(
                                        accessToken = data.accessToken,
                                        refreshToken = data.refreshToken
                                    )
                                } else {
                                    BearerTokens(accessToken = "", refreshToken = "")
                                }
                            } else {
                                BearerTokens(accessToken = "", refreshToken = "")
                            }
                        } catch (e: Exception) {
                            BearerTokens(accessToken = "", refreshToken = "")
                        }
                        
                        response
                    }
                }
            }
        }
    }
}