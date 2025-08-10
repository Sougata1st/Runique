package com.plcoding.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidFirebase(
    commonExtension: CommonExtension<*, *, *, *, *>
){
    commonExtension.run {
        dependencies{
            val bom = libs.findLibrary("firebase.bom").get()
            "implementation"(platform(bom))
            "implementation"(libs.findLibrary("firebase.auth").get())
            "implementation"(libs.findLibrary("firebase.database").get())
            "implementation"(libs.findLibrary("firebase.storage").get())
        }
    }
}