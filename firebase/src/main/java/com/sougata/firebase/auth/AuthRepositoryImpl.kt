package com.sougata.firebase.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.plcoding.auth.domain.AuthRepository
import com.plcoding.core.domain.AuthInfo
import com.plcoding.core.domain.SessionStorage
import com.plcoding.core.domain.util.DataError
import com.plcoding.core.domain.util.EmptyResult
import kotlinx.coroutines.tasks.await
import com.plcoding.core.domain.util.Result
import java.io.IOException
import java.net.SocketTimeoutException

class AuthRepositoryImpl (
    val firebaseAuth: FirebaseAuth,
    private val sessionStorage: SessionStorage
): AuthRepository {

    override suspend fun login(email: String, password: String): EmptyResult<DataError.Network> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            sessionStorage.set(
                AuthInfo(
                    userId = result.user?.uid.orEmpty()
                )
            )
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(t.toNetworkError())
        }
    }

    override suspend fun register(email: String, password: String): EmptyResult<DataError.Network> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(t.toNetworkError())
        }
    }
}

fun Throwable.toNetworkError(): DataError.Network = when (this) {
    is FirebaseNetworkException -> DataError.Network.NO_INTERNET

    is FirebaseTooManyRequestsException -> DataError.Network.TOO_MANY_REQUESTS

    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> DataError.Network.UNAUTHORIZED

    is FirebaseAuthUserCollisionException -> DataError.Network.CONFLICT

    is FirebaseAuthException -> when (errorCode?.uppercase()) {
        "ERROR_QUOTA_EXCEEDED" -> DataError.Network.TOO_MANY_REQUESTS
        "ERROR_USER_DISABLED"  -> DataError.Network.UNAUTHORIZED
        else                   -> DataError.Network.UNKNOWN
    }

    is SocketTimeoutException -> DataError.Network.REQUEST_TIMEOUT
    is IOException             -> DataError.Network.SERIALIZATION

    else -> DataError.Network.UNKNOWN
}