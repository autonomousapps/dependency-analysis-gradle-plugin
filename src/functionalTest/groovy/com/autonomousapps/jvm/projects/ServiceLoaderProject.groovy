package com.autonomousapps.jvm.projects

import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.*

import static com.autonomousapps.AdviceHelper.dependency
import static com.autonomousapps.AdviceHelper.transitiveDependency

final class ServiceLoaderProject {

  final GradleProject gradleProject
  private final String agpVersion

  ServiceLoaderProject(String agpVersion) {
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = new GradleProject.Builder()

    builder.gradleProperties = new GradleProperties([
      GradleProperties.JVM_ARGS, GradleProperties.USE_ANDROID_X
    ])
    builder.agpVersion = agpVersion
    def plugins = [Plugin.androidAppPlugin(), Plugin.kotlinAndroidPlugin()]
    def dependencies = [
      Dependency.kotlinStdlib("implementation"),
      Dependency.appcompat("implementation"),
      Dependency.constraintLayout("implementation"),
      Dependency.kotlinxCoroutines("implementation")
    ]
    def source = new Source(
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

    def layouts = [
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

    def manifest = AndroidManifest.DEFAULT_MANIFEST

    builder.addAndroidSubproject(
      manifest, plugins, AndroidBlock.defaultAndroidBlock(true), dependencies,
      [source], layouts, 'debug', ''
    )

    def project = builder.build()
    project.writer().write()
    return project
  }

  List<Advice> actualAdvice() {
    return AdviceHelper.actualAdvice(gradleProject)
  }

  final List<Advice> expectedAdvice = [
    Advice.ofAdd(transitiveDependency(
      dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core", "1.3.5"),
      [dependency("org.jetbrains.kotlinx:kotlinx-coroutines-android")]
    ), "implementation")
  ]
}
