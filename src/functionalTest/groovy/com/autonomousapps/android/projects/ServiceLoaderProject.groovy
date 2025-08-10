// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidLayout
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class ServiceLoaderProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  private final kotlinStdLib = kotlinStdLib("implementation")
  private final appcompat = appcompat("implementation")
  private final constraintLayout = constraintLayout("implementation")
  private final kotlinxCoroutinesAndroid = kotlinxCoroutinesAndroid("implementation")
  private final kotlinxCoroutinesCore = kotlinxCoroutinesCore("implementation")

  ServiceLoaderProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  @SuppressWarnings('DuplicatedCode')
  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { a ->
        a.sources = sources
        a.styles = AndroidStyleRes.DEFAULT
        a.colors = AndroidColorRes.DEFAULT
        a.layouts = layouts
        a.withBuildScript { bs ->
          bs.plugins = androidAppWithKotlin
          bs.android = defaultAndroidAppBlock()
          bs.dependencies = dependencies
        }
      }
      .write()
  }

  private List<Dependency> dependencies = [
    kotlinStdLib,
    appcompat,
    constraintLayout,
    kotlinxCoroutinesAndroid,
  ]

  private List<Source> sources = [
    new Source(
      SourceType.KOTLIN, "MainActivity", "com/example",
      """\
        package com.example
        
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
                  view.text = "Countdown ${'\$'}i ..." // update text
                  delay(500) // wait half a second
                }
                view.text = "Done!"
              }
            }
          }        
        }
      """.stripIndent()
    )
  ]

  private List<AndroidLayout> layouts = [
    new AndroidLayout("activity_main.xml", """\
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
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> appAdvice = [
    Advice.ofChange(moduleCoordinates(kotlinxCoroutinesAndroid), kotlinxCoroutinesAndroid.configuration, 'runtimeOnly'),
    Advice.ofAdd(moduleCoordinates(kotlinxCoroutinesCore), 'implementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [projectAdviceForDependencies(':app', appAdvice)]
}
