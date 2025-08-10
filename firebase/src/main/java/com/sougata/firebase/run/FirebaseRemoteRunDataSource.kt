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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

class FirebaseRemoteRunDataSource(
    private val storage: FirebaseStorage,
    private val db: FirebaseDatabase,
    private val sessionStorage: SessionStorage,
    private val firebaseAuth: FirebaseAuth
): RemoteRunDataSource {
    override suspend fun getRuns(): Result<List<Run>, DataError.Network> {
        val userId = sessionStorage.get()?.userId
            ?: return Result.Error(DataError.Network.UNAUTHORIZED)

        return try {
            val snapshot = db
                .getReference(userId)
                .child("Runs")
                .get()
                .await()

            val runs = snapshot.children.mapNotNull { snap ->
                snap.getValue(RunDto::class.java)?.toRun()
            }

            Result.Success(runs)
        } catch (t: Throwable) {
            Result.Error(t.toNetworkError())
        }
    }

    override suspend fun postRun(
        run: Run,
        mapPicture: ByteArray
    ): Result<Run, DataError.Network> = withContext(Dispatchers.IO) {
        val userId = sessionStorage.get()?.userId
            ?: return@withContext Result.Error(DataError.Network.UNAUTHORIZED)

        try {
            withTimeout(5_000L) { // ⏱️ 5 seconds
                val runsRef = db.getReference(userId).child("Runs")

                val runId = if (run.id.isNullOrBlank()) {
                    java.util.UUID.randomUUID().toString()
                } else run.id!!

                val imageRef = storage.reference
                    .child("runs")
                    .child(userId)
                    .child("$runId.png")

                val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                    .setContentType("image/png")
                    .build()

                imageRef.putBytes(mapPicture, metadata).await()
                val imageUrl = imageRef.downloadUrl.await().toString()

                val dto: CreateRunRequest = run.toCreateRunRequest(mapPictureUrl = imageUrl).copy(id = runId)
                runsRef.child(runId).setValue(dto).await()

                Result.Success(dto.toRun() ?: run.copy(id = runId))
            }
        } catch (e: TimeoutCancellationException) {
            Result.Error(DataError.Network.REQUEST_TIMEOUT)
        } catch (t: Throwable) {
            Result.Error(t.toNetworkError())
        }
    }


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