package com.autonomousapps.android.projects

import com.autonomousapps.kit.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.Dependency.*

final class DaggerProject extends AbstractAndroidProject {

  private final String agpVersion
  private final String projectName
  final GradleProject gradleProject

  DaggerProject(String agpVersion, String projectName) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.projectName = projectName
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.gradleProperties = GradleProperties.minimalAndroidProperties()
      r.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject(projectName) { s ->
      s.manifest = AndroidManifest.defaultLib('com.example.lib')
      // TODO: should invert the defaults to be null rather than have dummy values
      s.styles = null
      s.strings = null
      s.colors = null
      s.sources = sources
      s.withBuildScript { bs ->
        bs.android = androidLibBlock(true)
        bs.plugins = [Plugin.androidLibPlugin, Plugin.kotlinAndroidPlugin, Plugin.kaptPlugin]
        bs.dependencies = [
          javaxInject('api'),
          dagger('api'),
          // Using two annotation processors triggers a LinkageError with a faulty `FirstClassLoader` (now resolved).
          daggerCompiler('kapt'),
          daggerAndroidCompiler('kapt'),
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.KOTLIN, "DaggerUser", "com/example",
      """\
        package com.example
        
        import javax.inject.Inject

        class DaggerUser @Inject constructor(val s: String)
      """.stripIndent()
    )
  ]

  final ProjectAdvice expectedAdvice = projectAdviceForDependencies(
    ":$projectName",
    [Advice.ofRemove(moduleCoordinates('com.google.dagger:dagger-android-processor', '2.44.2'), 'kapt')] as Set<Advice>
  )
}
