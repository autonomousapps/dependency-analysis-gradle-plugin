package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice

class ViewBindingProject(private val agpVersion: String) {
  val appSpec = AppSpec(
    sources = mapOf("MainActivity.kt" to """
      package $DEFAULT_PACKAGE_NAME
      
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
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  val expectedAdviceForApp: Set<Advice> = emptySet()
}
