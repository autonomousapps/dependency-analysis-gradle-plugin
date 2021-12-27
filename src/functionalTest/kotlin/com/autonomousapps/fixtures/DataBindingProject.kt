package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice

class DataBindingProject(
  private val agpVersion: String
) {

  val appSpec = AppSpec(
    sources = mapOf(
      "MainActivity.kt" to """
                import androidx.appcompat.app.AppCompatActivity
                
                class MainActivity : AppCompatActivity() {
                }
            """.trimIndent()
    ),
    dependencies = listOf(
      "implementation" to KOTLIN_STDLIB_ID,
      "implementation" to APPCOMPAT
    ),
    buildAdditions = "android.buildFeatures.dataBinding true"
  )

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  val expectedAdviceForApp = emptySet<Advice>()
}
