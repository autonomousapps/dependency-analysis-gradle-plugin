package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.dependency
import static com.autonomousapps.AdviceHelper.transitiveDependency

final class ServiceLoaderProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  ServiceLoaderProject(String agpVersion) {
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('app') { a ->
      a.sources = sources
      a.layouts = layouts
      a.withBuildScript { bs ->
        bs.plugins = plugins
        bs.android = androidBlock
        bs.dependencies = dependencies
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Plugin> plugins = [
    Plugin.androidAppPlugin,
    Plugin.kotlinAndroidPlugin
  ]

  private AndroidBlock androidBlock = AndroidBlock.defaultAndroidAppBlock(true)

  private List<Dependency> dependencies = [
    Dependency.kotlinStdLib("implementation"),
    Dependency.appcompat("implementation"),
    Dependency.constraintLayout("implementation"),
    Dependency.kotlinxCoroutines("implementation")
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

  List<Advice> actualAdvice() {
    return AdviceHelper.actualAdviceForFirstSubproject(gradleProject)
  }

  final List<Advice> expectedAdvice = [
    Advice.ofAdd(transitiveDependency(
      dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core", "1.3.5"),
      [dependency("org.jetbrains.kotlinx:kotlinx-coroutines-android")]
    ), "implementation")
  ]
}
