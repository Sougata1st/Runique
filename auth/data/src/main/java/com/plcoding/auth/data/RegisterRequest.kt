package com.plcoding.auth.data

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name : String,
    val email: String,
    val password: String
)
