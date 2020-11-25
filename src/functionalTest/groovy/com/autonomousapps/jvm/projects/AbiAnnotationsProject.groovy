package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.compAdviceForDependencies
import static com.autonomousapps.AdviceHelper.emptyBuildHealthFor
import static com.autonomousapps.AdviceHelper.emptyCompAdviceFor
import static com.autonomousapps.kit.Dependency.kotlinStdLib
import static com.autonomousapps.kit.Dependency.project

final class AbiAnnotationsProject extends AbstractProject {

  enum Target {
    CLASS, METHOD, PARAMETER
  }

  final GradleProject gradleProject
  private final Target target
  private final boolean visible

  AbiAnnotationsProject(Target target, boolean visible = true) {
    this.target = target
    this.visible = visible
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
      s.sources = annosSources()
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

  private annosSources() {
    return [
      new Source(
        SourceType.KOTLIN, "Anno", "com/example",
        """\
        package com.example
        
        @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
        @Retention(${retention()})
        @MustBeDocumented
        annotation class Anno
      """.stripIndent()
      )
    ]
  }

  private retention() {
    if (visible) return "AnnotationRetention.RUNTIME"
    else return "AnnotationRetention.SOURCE"
  }

  List<ComprehensiveAdvice> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  List<ComprehensiveAdvice> expectedBuildHealth() {
    if (visible) {
      return expectedBuildHealthForRuntimeRetention
    } else {
      return expectedBuildHealthForSourceRetention
    }
  }

  private final List<ComprehensiveAdvice> expectedBuildHealthForRuntimeRetention = emptyBuildHealthFor(':proj', ':annos', ':')

  private final Set<Advice> toCompileOnly = [Advice.ofChange(
    new Dependency(':annos', '', 'api'),
    'compileOnly'
  )] as Set<Advice>

  private final List<ComprehensiveAdvice> expectedBuildHealthForSourceRetention = [
    compAdviceForDependencies(':proj', toCompileOnly),
    emptyCompAdviceFor(':annos'),
    emptyCompAdviceFor(':')
  ]
}
