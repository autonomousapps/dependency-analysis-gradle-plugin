package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice

class LeakCanaryProject(private val agpVersion: String) {

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  private val sources = mapOf("App.kt" to """
    package $DEFAULT_PACKAGE_NAME
    
    import androidx.appcompat.app.AppCompatActivity
    
    class MainActivity : AppCompatActivity() {
    }
  """.trimIndent())

  val appSpec = AppSpec(
    sources = sources,
    dependencies = listOf(
      "implementation" to "org.jetbrains.kotlin:kotlin-stdlib:1.4.21",
      "implementation" to APPCOMPAT,
      "debugImplementation" to "com.squareup.leakcanary:leakcanary-android:2.2"
    )
  )

  val expectedAdviceForApp: Set<Advice> = setOf()
}
