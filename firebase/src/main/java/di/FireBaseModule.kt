package di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.plcoding.auth.domain.AuthRepository
import com.plcoding.core.domain.run.RemoteRunDataSource
import com.sougata.firebase.auth.AuthRepositoryImpl
import com.sougata.firebase.run.FirebaseRemoteRunDataSource
import org.koin.dsl.module


val firebaseModule = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseDatabase> { FirebaseDatabase.getInstance() }
    single<FirebaseStorage> { FirebaseStorage.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single { get<FirebaseDatabase>().reference }
    single<RemoteRunDataSource> { FirebaseRemoteRunDataSource(get(), get(), get(), get()) }
}