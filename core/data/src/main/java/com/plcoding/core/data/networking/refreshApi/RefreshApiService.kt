package com.sougata.core.data.networking.refreshApi


import com.plcoding.core.data.networking.AccessTokenRequest
import com.plcoding.core.data.networking.AccessTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RefreshApiService {
    @POST("auth/refresh")  // Use the correct endpoint URL
    suspend fun getAccessToken(
        @Body request: AccessTokenRequest
    ): Response<AccessTokenResponse>
}