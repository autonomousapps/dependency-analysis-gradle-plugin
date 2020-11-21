package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.kit.Dependency.kotlinStdLib
import static com.autonomousapps.kit.Dependency.project

final class AnnotationsAbiProject extends AbstractProject {

  enum Target {
    CLASS, METHOD, PARAMETER
  }

  final GradleProject gradleProject
  private final Target target

  AnnotationsAbiProject(Target target) {
    this.target = target
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = projSources()
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [kotlinStdLib('api'), project('api', ':annos')]
      }
    }
    builder.withSubproject('annos') { s ->
      s.sources = annosSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [kotlinStdLib('api')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  def projSources() {
    if (target == Target.CLASS) return classTarget
    else if (target == Target.METHOD) return methodTarget
    else if (target == Target.PARAMETER) return paramTarget

    throw new IllegalStateException("No source available for target=$target")
  }

  def classTarget = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example
        
        @Anno
        class Main {
          fun magic() = 42
        }
      """.stripIndent()
    )
  ]

  def methodTarget = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example
        
        class Main {
          @Anno
          fun magic() = 42
        }
      """.stripIndent()
    )
  ]

  def paramTarget = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example
        
        class Main {
          fun magic(@Anno i: Int) = 42
        }
      """.stripIndent()
    )
  ]

  def annosSources = [
    new Source(
      SourceType.KOTLIN, "Anno", "com/example",
      """\
        package com.example
        
        @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
        @Retention(AnnotationRetention.RUNTIME)
        @MustBeDocumented
        annotation class Anno
      """.stripIndent()
    )
  ]

  List<ComprehensiveAdvice> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    new ComprehensiveAdvice(':proj', [] as Set<Advice>, [] as Set<PluginAdvice>, false),
    new ComprehensiveAdvice(':annos', [] as Set<Advice>, [] as Set<PluginAdvice>, false),
    new ComprehensiveAdvice(':', [] as Set<Advice>, [] as Set<PluginAdvice>, false)
  ]
}
