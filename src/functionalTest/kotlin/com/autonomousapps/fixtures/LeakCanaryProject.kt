package com.autonomousapps.fixtures

import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates

class LeakCanaryProject(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf(
    "App.kt" to """
    package $DEFAULT_PACKAGE_NAME
    
    import androidx.appcompat.app.AppCompatActivity
    
    class MainActivity : AppCompatActivity() {
    }
  """.trimIndent()
  )

  val appSpec = AppSpec(
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:${Plugin.KOTLIN_VERSION}",
      "implementation" to APPCOMPAT,
      "debugImplementation" to "com.squareup.leakcanary:leakcanary-android:2.2"
    )
  )

  val expectedAdviceForApp = setOf(
    Advice.ofChange(
      ModuleCoordinates("com.squareup.leakcanary:leakcanary-android", "2.2", GradleVariantIdentification.EMPTY),
      "debugImplementation", "debugRuntimeOnly"
    )
  )
}
