package com.autonomousapps.fixtures

import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromJsonSet

class DataBindingProject(
  private val agpVersion: String
) {

  val appSpec = AppSpec(
    sources = mapOf("MainActivity.kt" to """
                import androidx.appcompat.app.AppCompatActivity
                
                class MainActivity : AppCompatActivity() {
                }
            """.trimIndent()),
    dependencies = listOf(
      "implementation" to KOTLIN_STDLIB_ID,
      "implementation" to APPCOMPAT
    ),
    buildAdditions = "android.dataBinding.enabled = true"
  )

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  val expectedAdviceForApp = "[]".fromJsonSet<Advice>()
}
