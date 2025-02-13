package com.plcoding.auth.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val data: Data,
    val status: Int,
    val success: Boolean,
    val error: String?,
    val timeStamp: String
)

@Serializable
data class Data(
    val accessToken: String,
    val msg: String,
    val refreshToken: String
)