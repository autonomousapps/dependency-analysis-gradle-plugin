//file:noinspection DuplicatedCode
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidLayout
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

/**
 * Basic structure is a normal Android project, with some variant-specific source. A dependency will
 * use `implementation` but should be on `debugImplementation` and `releaseImplementation`, and a
 * `debugImplementation` dependency will not be seen as unused.
 */
abstract class AbstractVariantProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  protected final String agpVersion

  AbstractVariantProject(String agpVersion) {
    super(agpVersion)

    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = projectGradleProperties
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('app') { a ->
      a.sources = sources
      a.layouts = layouts
      a.withBuildScript { bs ->
        bs.plugins = plugins
        bs.android = defaultAndroidAppBlock()
        bs.dependencies = dependencies
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  protected final List<Plugin> plugins = [
    Plugins.androidApp,
    Plugins.kotlinAndroid
  ]

  protected final List<Dependency> dependencies = [
    kotlinStdLib("implementation"),
    appcompat("implementation"),
    constraintLayout("implementation"),
    // This is used, but only in the debug variant and so should be `debugImplementation`
    commonsCollections("implementation"),
    // This is correctly declared
    commonsIO("debugImplementation"),
    // This isn't used and should be recommended for removal
    commonsMath("debugImplementation")
  ]

  protected final List<Source> sources = [
    new Source(
      SourceType.KOTLIN, "MainActivity", "com/example",
      """\
        package com.example
        
        import android.os.Bundle
        import androidx.appcompat.app.AppCompatActivity
        
        class MainActivity : AppCompatActivity() {
          override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)            
          }        
        }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, "Debug", "com/example",
      """\
        package com.example
        
        import org.apache.commons.collections4.bag.HashBag
        import org.apache.commons.io.filefilter.EmptyFileFilter
        
        class Debug {
          private fun makeBag() {
            // This will be on implementation but should be debugImplementation
            val bag = HashBag<String>()
          }
          private fun makeFilter() {
            // This will be on debugImplementation and should pass by as-is
            val f = EmptyFileFilter.EMPTY
          }
        }
      """.stripIndent(),
      "debug"
    ),
    new Source(
      SourceType.KOTLIN, "Release", "com/example",
      """\
        package com.example

//        import org.apache.commons.collections4.bag.HashBag
        //import org.apache.commons.io.filefilter.EmptyFileFilter

        class Release {
//          private fun makeBag() {
//            // This will be on implementation but should be debugImplementation
//            val bag = HashBag<String>()
//          }
          //private fun makeFilter() {
            // This will be on debugImplementation and should pass by as-is
          //  val f = EmptyFileFilter.EMPTY
          //}
        }
      """.stripIndent(),
      "release"
    )
  ]

  protected final List<AndroidLayout> layouts = [
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

  protected GradleProperties getProjectGradleProperties() {
    return GradleProperties.minimalAndroidProperties()
  }

  final Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  abstract Set<ProjectAdvice> expectedBuildHealth()
}
