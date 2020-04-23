package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency

class ServiceLoaderProject(private val agpVersion: String) {

  val appSpec = AppSpec(
    sources = mapOf("MainActivity.kt" to """
      package $DEFAULT_PACKAGE_NAME
      
      import android.os.Bundle
      import android.widget.Button
      import androidx.appcompat.app.AppCompatActivity
      import kotlinx.coroutines.Dispatchers
      import kotlinx.coroutines.GlobalScope
      import kotlinx.coroutines.delay
      import kotlinx.coroutines.launch
      
      class MainActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContentView(R.layout.activity_main)
  
          findViewById<Button>(R.id.btn).setOnClickListener { view ->
            view as Button
            GlobalScope.launch(Dispatchers.Main) { // launch coroutine in the main thread
              for (i in 10 downTo 1) { // countdown from 10 to 1 
                view.text = "Countdown ${'$'}i ..." // update text
                delay(500) // wait half a second
              }
              view.text = "Done!"
            }
          }
        }
      }""".trimIndent()),
    layouts = setOf(
      AndroidLayout(
        fileName = "activity_main.xml",
        content = """
          <?xml version="1.0" encoding="utf-8"?>
          <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            xmlns:tools="http://schemas.android.com/tools"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            tools:context=".MainActivity"
            >
            <Button
              android:id="@+id/btn"
              android:layout_width="wrap_content"
              android:layout_height="wrap_content"
              android:text="Hello!"
              app:layout_constraintBottom_toBottomOf="parent"
              app:layout_constraintEnd_toEndOf="parent"
              app:layout_constraintStart_toStartOf="parent"
              app:layout_constraintTop_toTopOf="parent"
              />
          </androidx.constraintlayout.widget.ConstraintLayout>
          """.trimIndent()
      )
    ),
    dependencies = listOf(
      "implementation" to KOTLIN_STDLIB_ID,
      "implementation" to APPCOMPAT,
      "implementation" to "androidx.constraintlayout:constraintlayout:1.1.3",
      "implementation" to "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.5"
    )
  )

  fun newProject() = AndroidProject(
    rootSpec = RootSpec(agpVersion = agpVersion),
    appSpec = appSpec
  )

  val expectedAdviceForApp: Set<Advice> = setOf(
    Advice(
      dependency = Dependency(
        identifier = "org.jetbrains.kotlinx:kotlinx-coroutines-core",
        resolvedVersion = "1.3.5"
      ),
      toConfiguration = "implementation"
    )
  )
}
