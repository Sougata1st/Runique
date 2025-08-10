import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.plcoding.convention.configureAndroidFirebase
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class FirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val extension = extensions.getByType<LibraryExtension>()
            configureAndroidFirebase(extension)
        }
    }

}