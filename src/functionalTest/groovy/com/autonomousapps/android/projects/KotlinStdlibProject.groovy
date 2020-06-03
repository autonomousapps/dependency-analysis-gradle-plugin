package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.kit.Dependency.kotlinStdlibJdk7

final class KotlinStdlibProject extends AbstractProject {

  private static final STDLIB = new Dependency('org.jetbrains.kotlin:kotlin-stdlib', Plugin.KOTLIN_VERSION, null)
  private static final STDLIB7 = new Dependency('org.jetbrains.kotlin:kotlin-stdlib-jdk7', Plugin.KOTLIN_VERSION, 'implementation')

  final GradleProject gradleProject
  private final String additions

  KotlinStdlibProject(String additions = '') {
    this.additions = additions
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = additions
      }
    }
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPlugin(null, true)]
        bs.dependencies = [kotlinStdlibJdk7('implementation')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, 'Library', 'com/example',
      """\
        package com.example
      
        class Library {
          fun magic() = 42
        }
      """
    )
  ]

  @SuppressWarnings("GrMethodMayBeStatic")
  Set<Advice> expectedNoFacadeAdvice() {
    return [addCoreStdlib(), removeStdlib7()] as Set<Advice>
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  Set<Advice> expectedFacadeAdvice() {
    return [] as Set<Advice>
  }

  private static Advice addCoreStdlib() {
    return Advice.ofAdd(
      new TransitiveDependency(STDLIB, [STDLIB7] as Set<Dependency>, ["main"] as Set<String>),
      'implementation'
    )
  }

  private static Advice removeStdlib7() {
    return Advice.ofRemove(new ComponentWithTransitives(STDLIB7, [STDLIB] as Set<Dependency>))
  }
}
