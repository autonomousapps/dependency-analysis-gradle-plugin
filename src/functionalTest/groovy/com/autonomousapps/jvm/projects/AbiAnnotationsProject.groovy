package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.kotlinStdLib
import static com.autonomousapps.kit.Dependency.project

final class AbiAnnotationsProject extends AbstractProject {

  enum Target {
    CLASS, METHOD, PARAMETER, WITH_PROPERTY, TYPE_PARAMETER
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
        bs.dependencies = projDeps()
      }
    }
    builder.withSubproject('annos') { s ->
      s.sources = annosSources()
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [kotlinStdLib('api')]
      }
    }
    builder.withSubproject('property') { s ->
      s.sources = withPropertySources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [kotlinStdLib('api')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private projDeps() {
    def deps = [kotlinStdLib('api'), project('api', ':annos')]
    if (target == Target.WITH_PROPERTY) {
      deps += project('api', ':property')
    }
    return deps
  }

  def projSources() {
    if (target == Target.CLASS) return classTarget
    else if (target == Target.METHOD) return methodTarget
    else if (target == Target.PARAMETER) return paramTarget
    else if (target == Target.WITH_PROPERTY) return withPropertyTarget
    else if (target == Target.TYPE_PARAMETER) return typeTarget

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

  def withPropertyTarget = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example
        
        @WithProperty(TheProperty::class)
        class Main {
          fun magic() = 42
        }
      """.stripIndent()
    )
  ]

  def typeTarget = [
    // Kotlin currently has no support for type parameter annotations
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        import java.util.*;
        
        public class Main {
          public List<@TypeAnno Object> magic() {
            return new ArrayList<Object>();
          }
        }
      """.stripIndent()
    ),
    // The only purpose of this is so there's no advice to remove the kotlin stdlib
    new Source(
      SourceType.KOTLIN, "Dummy", "com/example",
      """\
        package com.example
        
        class Dummy
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
      ),
      new Source(
        SourceType.JAVA, "TypeAnno", "com/example",
        """\
        package com.example;
        
        import java.lang.annotation.*;
        
        @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
        @Retention(RetentionPolicy.RUNTIME)
        @interface TypeAnno {
        }
      """.stripIndent()
      ),
      new Source(
        SourceType.KOTLIN, "WithProperty", "com/example",
        """\
        package com.example
        
        import kotlin.reflect.KClass
        
        @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
        @Retention(${retention()})
        @MustBeDocumented
        annotation class WithProperty(val arg: KClass<*>)
      """.stripIndent()
      )
    ]
  }

  private final withPropertySources = [
    new Source(
      SourceType.KOTLIN, "TheProperty", "com/example",
      """\
        package com.example
        
        class TheProperty
      """.stripIndent()
    )
  ]

  private retention() {
    if (visible) return "AnnotationRetention.RUNTIME"
    else return "AnnotationRetention.SOURCE"
  }

  List<ComprehensiveAdvice> actualBuildHealth() {
    return actualBuildHealth(gradleProject)
  }

  List<ComprehensiveAdvice> expectedBuildHealth() {
    if (visible) {
      return expectedBuildHealthForRuntimeRetention
    } else {
      return expectedBuildHealthForSourceRetention
    }
  }

  private final expectedBuildHealthForRuntimeRetention = emptyBuildHealthFor(
    ':proj', ':annos', ':property', ':'
  )

  private final Set<Advice> toCompileOnly = [Advice.ofChange(
    new Dependency(':annos', null, 'api'),
    'compileOnly'
  )] as Set<Advice>

  private final List<ComprehensiveAdvice> expectedBuildHealthForSourceRetention = [
    emptyCompAdviceFor(':'),
    emptyCompAdviceFor(':annos'),
    compAdviceForDependencies(':proj', toCompileOnly),
    emptyCompAdviceFor(':property'),
  ]
}
