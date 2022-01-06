package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.*

final class DataBindingUsagesExclusionsProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion
  private final boolean excludeDataBinderMapper

  DataBindingUsagesExclusionsProject(String agpVersion, boolean excludeDataBinderMapper) {
    this.agpVersion = agpVersion
    this.excludeDataBinderMapper = excludeDataBinderMapper
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)

        if (excludeDataBinderMapper) {
          bs.additions = """\
            dependencyAnalysis {
              usages {
                exclusions {
                  excludeClasses(".*\\\\.DataBinderMapperImpl\\\$")
                }
              }
            }
          """.stripIndent()
        }
      }
    }
    builder.withAndroidSubproject('app') { app ->
      app.withBuildScript { bs ->
        bs.plugins = [Plugin.androidAppPlugin, Plugin.kotlinAndroidPlugin, Plugin.kaptPlugin]
        bs.android = AndroidBlock.defaultAndroidAppBlock(true)
        bs.dependencies = appDependencies
        bs.additions = "android.buildFeatures.dataBinding true"
      }
      app.manifest = AndroidManifest.defaultLib("com.example.app")
      app.sources = appSources
    }

    builder.withAndroidSubproject('lib') { lib ->
      lib.withBuildScript { bs ->
        bs.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin, Plugin.kaptPlugin]
        bs.android = AndroidBlock.defaultAndroidLibBlock(true)
        bs.dependencies = libDependencies
        bs.additions = "android.buildFeatures.dataBinding true"
      }
      lib.manifest = AndroidManifest.defaultLib("com.example.lib")
      lib.sources = libSources
      lib.withFile('src/main/res/layout/hello.xml', """\
          <?xml version="1.0" encoding="utf-8"?>
          <layout
            xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:binding="http://schemas.android.com/apk/res-auto">

            <TextView
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              binding:text="@{`Hello`}" />

          </layout>
        """.stripIndent()
      )
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private appSources = [
    new Source(
      SourceType.KOTLIN, 'MainActivity', 'com/example/app',
      """
        package com.example.app
        
        import androidx.appcompat.app.AppCompatActivity
        
        class MainActivity : AppCompatActivity() {
        }
      """.stripIndent()
    )
  ]

  private List<Dependency> appDependencies = [
    Dependency.kotlinStdLib("implementation"),
    Dependency.appcompat("implementation"),
    Dependency.project("implementation", ":lib"),
  ]

  private libSources = [
    new Source(
      SourceType.KOTLIN, 'Bindings', 'com/example/lib',
      """
        package com.example.lib
        
        import android.widget.TextView
        import androidx.databinding.BindingAdapter
        
        @BindingAdapter("text")
        fun setText(view: TextView, text: String) {
          view.text = text
        }
      """.stripIndent()
    )
  ]

  private List<Dependency> libDependencies = [
    Dependency.kotlinStdLib("api")
  ]

  private final List<ComprehensiveAdvice> expectedBuildHealthWithExclusions = [
    AdviceHelper.emptyCompAdviceFor(':'),
    AdviceHelper.compAdviceForDependencies(':app', [
      Advice.ofRemove(AdviceHelper.dependency(":lib", null, "implementation"))
    ] as Set<Advice>),
    AdviceHelper.emptyCompAdviceFor(':lib'),
  ]

  private final List<ComprehensiveAdvice> expectedBuildHealthWithoutExclusions =
    AdviceHelper.emptyBuildHealthFor(':', ':app', ':lib')

  final List<ComprehensiveAdvice> expectedBuildHealth = excludeDataBinderMapper
    ? expectedBuildHealthWithExclusions
    : expectedBuildHealthWithoutExclusions

  List<ComprehensiveAdvice> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }
}
