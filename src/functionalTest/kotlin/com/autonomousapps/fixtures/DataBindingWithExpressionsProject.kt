package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice

class DataBindingWithExpressionsProject(
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
    layouts = setOf(
      AndroidLayout(
        fileName = "main_activity.xml",
        content = """
          <?xml version="1.0" encoding="utf-8"?>
          <layout
            xmlns:android="http://schemas.android.com/apk/res/android">

            <TextView
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:text="@{`Text`}" />

          </layout>
        """.trimIndent()
      )
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
