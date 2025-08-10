package com.sougata.firebase.run

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.plcoding.core.domain.SessionStorage
import com.plcoding.core.domain.run.RemoteRunDataSource
import com.plcoding.core.domain.run.Run
import com.plcoding.core.domain.util.DataError
import com.plcoding.core.domain.util.EmptyResult
import com.plcoding.core.domain.util.Result
import com.plcoding.run.network.CreateRunRequest
import com.plcoding.run.network.RunDto
import com.plcoding.run.network.toCreateRunRequest
import com.plcoding.run.network.toRun
import com.sougata.firebase.auth.toNetworkError
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRemoteRunDataSource(
    private val storage: FirebaseStorage,
    private val db: FirebaseDatabase,
    private val sessionStorage: SessionStorage,
    private val firebaseAuth: FirebaseAuth
): RemoteRunDataSource {
    override suspend fun getRuns(): Result<List<Run>, DataError.Network> {
        val rawUserId = sessionStorage.get()?.userId
            ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        val userId = rawUserId.toRtdbKey()

        return try {
            val snapshot = db.reference.child(userId).child("Runs").get().await()
            val runs = snapshot.children.mapNotNull { it.getValue(CreateRunRequest::class.java)?.toRun() }
            android.util.Log.d("Sougata", "getRuns -> ${runs.size} runs")
            Result.Success(runs)
        } catch (t: Throwable) {
            android.util.Log.e("Sougata", "getRuns failed", t)
            Result.Error(t.toNetworkError())
        }
    }


    override suspend fun postRun(
        run: Run,
        mapPicture: ByteArray
    ): Result<Run, DataError.Network> {
        val rawUserId = sessionStorage.get()?.userId
            ?: return Result.Error(DataError.Network.UNAUTHORIZED)

        // Prefer the auth UID if you can store it in session:
        // val userId = firebaseAuth.currentUser?.uid ?: return Result.Error(DataError.Network.UNAUTHORIZED)

        // Otherwise sanitize keys for RTDB (avoid ., #, $, [, ], /)
        val userId = rawUserId.toRtdbKey()

        return try {
            val runsRef = db.reference.child(userId).child("Runs")

            val runId = if (run.id.isNullOrBlank()) {
                java.util.UUID.randomUUID().toString()
            } else run.id!!

            // upload image
            val imageRef = storage.reference
                .child("runs")
                .child(userId)
                .child("$runId.png")

            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/png")
                .build()

            imageRef.putBytes(mapPicture, metadata).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            // write dto
            val dto = run.toCreateRunRequest(mapPictureUrl = imageUrl).copy(id = runId)
            runsRef.child(runId).setValue(dto).await()

            Result.Success(dto.toRun() ?: run.copy(id = runId))
        } catch (t: Throwable) {
            android.util.Log.e("Sougata", "postRun failed", t)  // <- see real reason
            Result.Error(t.toNetworkError())
        }
    }

    // Helper: sanitize a key if you can’t guarantee it’s a UID
    private fun String.toRtdbKey(): String =
        this.replace(".", ",")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "(")
            .replace("]", ")")
            .replace("/", "_")



    override suspend fun deleteRun(id: String): EmptyResult<DataError.Network> {
        val userId = sessionStorage.get()?.userId
            ?: return Result.Error(DataError.Network.UNAUTHORIZED)

        return try {
            db.getReference(userId)
                .child("Runs")
                .child(id)
                .removeValue()
                .await()

            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(t.toNetworkError())
        }
    }

    override suspend fun logout(): EmptyResult<DataError.Network> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(DataError.Network.UNKNOWN)
        }
    }

}