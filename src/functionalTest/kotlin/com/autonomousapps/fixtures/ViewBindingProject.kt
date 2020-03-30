package com.autonomousapps.fixtures

import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.utils.fromJsonList

class ViewBindingProject(
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
    buildAdditions = "android.viewBinding.enabled = true"
  )

  fun newProject() = AndroidProject(
    agpVersion = agpVersion,
    appSpec = appSpec
  )

  val expectedAdviceForApp: Set<Advice> =
    """[]"""
      .fromJsonList<Advice>()
      .toSet()
}
