package com.plcoding.run.network.di

import com.plcoding.core.domain.run.RemoteRunDataSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    //singleOf(::KtorRemoteRunDataSource).bind<RemoteRunDataSource>()
}