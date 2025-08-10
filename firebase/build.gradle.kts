plugins {
    alias(libs.plugins.runique.android.library)
    alias(libs.plugins.runique.firebase.android)
}

android {
    namespace = "com.sougata.firebase"
}
dependencies{
    implementation(libs.bundles.koin)
    implementation(projects.core.domain)
    implementation(projects.auth.domain)
    implementation(libs.firebase.auth)
    implementation(projects.run.domain)
    implementation(projects.run.data)
    implementation(projects.run.network)
}
