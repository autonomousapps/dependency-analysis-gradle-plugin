//file:noinspection DuplicatedCode
package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*

/**
 * Basic structure is a normal Android project, with some variant-specific source. A dependency will
 * use `implementation` but should be on `debugImplementation` and `releaseImplementation`, and a
 * `debugImplementation` dependency will not be seen as unused.
 */
final class VariantProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  VariantProject(String agpVersion) {
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
    // This is used, but only in the debug variant and so should be `debugImplementation`
    Dependency.commonsCollections("implementation"),
    // This is correctly declared
    Dependency.commonsIO("debugImplementation"),
    // This isn't used and should be recommended for removal
    Dependency.commonsMath("debugImplementation")
  ]

  private List<Source> sources = [
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

  private Set<Advice> appAdvice = [
    Advice.ofChange(
      moduleCoordinates('org.apache.commons:commons-collections4', '4.4'), 'implementation', 'debugImplementation'
    ),
    Advice.ofRemove(
      moduleCoordinates('org.apache.commons:commons-math3', '3.6.1'), 'debugImplementation'
    )
  ]

  private ProjectAdvice getAppAdvice() {
    projectAdviceForDependencies(':app', appAdvice)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    getAppAdvice()
  ]
}
