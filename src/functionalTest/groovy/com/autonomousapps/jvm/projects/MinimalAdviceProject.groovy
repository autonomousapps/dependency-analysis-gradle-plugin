package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.*

abstract class MinimalAdviceProject extends AbstractProject {

  final GradleProject gradleProject

  MinimalAdviceProject(boolean isStrictMode) {
    this.gradleProject = build(isStrictMode)
  }

  private GradleProject build(boolean isStrictMode) {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = """
        dependencyAnalysis {
          strictMode($isStrictMode)
        }
        """.stripMargin()
      }
    }
    builder.withSubproject('app') { s ->
      s.sources = appSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [
          project('implementation', ':lib')
        ]
      }
    }
    builder.withSubproject('lib') { s ->
      s.sources = libSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [
          kotlinStdlibJdk7('api'),
          moshiKotlin('api'),
          moshiAdapters('api')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  abstract List<Source> appSources
  abstract List<Source> libSources

  final static class Changes extends MinimalAdviceProject {

    Changes(boolean isStrictMode) {
      super(isStrictMode)
    }

    @Override
    List<Source> getAppSources() {
      [
        new Source(
          SourceType.KOTLIN, "App", "com/example",
          """\
          package com.example
          
          import com.squareup.moshi.Moshi
          import okio.Buffer
          
          class App {
            // Implementation dependencies
            private val moshi = Moshi.Builder().build()
            private val buffer = Buffer()
            
            fun foo() {
              // We use Lib, so we can also use its API dependencies without declaring them
              Lib()
            }
          }
          """.stripIndent()
        )
      ]
    }

    @Override
    List<Source> getLibSources() {
      [
        new Source(
          SourceType.KOTLIN, "Lib", "com/example",
          """\
          package com.example
          
          import com.squareup.moshi.Moshi
          import okio.Buffer
          
          class Lib {
            // Both Moshi and Buffer are part of Lib's ABI
            fun moshi(buffer: Buffer): Moshi {
              TODO("Unnecessary for TestKit test")
            }
          }
          """.stripIndent()
        )
      ]
    }

    final List<ComprehensiveAdvice> expectedAdvice = [
      emptyCompAdviceFor(':'),
      compAdviceForDependencies(':app', [] as Set<Advice>),
      compAdviceForDependencies(':lib', [
        Advice.
          ofRemove(
            dependency(identifier: 'com.squareup.moshi:moshi-kotlin', configurationName: 'api')),
        Advice.
          ofRemove(
            dependency(identifier: 'com.squareup.moshi:moshi-adapters', configurationName: 'api')),
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.moshi:moshi'), 'api'),
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.okio:okio'), 'api')
      ] as Set<Advice>)
    ]
  }

  final static class SomeChanges extends MinimalAdviceProject {

    SomeChanges(boolean isStrictMode) {
      super(isStrictMode)
    }

    @Override
    List<Source> getAppSources() {
      [
        new Source(
          SourceType.KOTLIN, "App", "com/example",
          """\
          package com.example
          
          import com.squareup.moshi.Moshi
          import okio.Buffer
          
          class App {
            private val moshi = Moshi.Builder().build()
            private val buffer = Buffer()
            
            // Moshi is part of the ABI and must be declared.
            // Okio is an implementation detail and doesn't need to be declared; it can be picked up
            // from the transitive graph.
            fun foo(): Moshi {
              Lib()
              TODO("Unnecessary for TestKit test")
            }
          }
          """.stripIndent()
        )
      ]
    }

    @Override
    List<Source> getLibSources() {
      [
        new Source(
          SourceType.KOTLIN, "Lib", "com/example",
          """\
          package com.example
          
          import com.squareup.moshi.Moshi
          import okio.Buffer
          
          class Lib {
            // Both Moshi and Buffer are part of Lib's ABI
            fun moshi(buffer: Buffer): Moshi {
              TODO("Unnecessary for TestKit test")
            }
          }
          """.stripIndent()
        )
      ]
    }

    final List<ComprehensiveAdvice> expectedAdvice = [
      emptyCompAdviceFor(':'),
      compAdviceForDependencies(':app', [
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.moshi:moshi'), 'api'),
      ] as Set<Advice>),
      compAdviceForDependencies(':lib', [
        Advice.ofRemove(dependency(identifier: 'com.squareup.moshi:moshi-kotlin', configurationName: 'api')),
        Advice.ofRemove(dependency(identifier: 'com.squareup.moshi:moshi-adapters', configurationName: 'api')),
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.moshi:moshi'), 'api'),
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.okio:okio'), 'api')
      ] as Set<Advice>)
    ]
  }

  final static class NoChanges extends MinimalAdviceProject {

    NoChanges(boolean isStrictMode) {
      super(isStrictMode)
    }

    @Override
    List<Source> getAppSources() {
      [
        new Source(
          SourceType.KOTLIN, "App", "com/example",
          """\
          package com.example
          
          import com.squareup.moshi.Moshi
          import okio.Buffer
          
          class App {
            val moshi = Moshi.Builder().build()
            val buffer = Buffer()
            
            fun foo() {
              Lib()
            }
          }
          """.stripIndent()
        )
      ]
    }

    @Override
    List<Source> getLibSources() {
      [
        new Source(
          SourceType.KOTLIN, "Lib", "com/example",
          """\
          package com.example
          
          import com.squareup.moshi.Moshi
          import okio.Buffer
          
          class Lib {
            // Both Moshi and Buffer are part of Lib's ABI
            fun moshi(buffer: Buffer): Moshi {
              TODO("Unnecessary for TestKit test")
            }
          }
          """.stripIndent()
        )
      ]
    }

    final List<ComprehensiveAdvice> expectedAdvice = [
      emptyCompAdviceFor(':'),
      compAdviceForDependencies(':app', [
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.moshi:moshi'), 'api'),
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.okio:okio'), 'api'),
      ] as Set<Advice>),
      compAdviceForDependencies(':lib', [
        Advice.ofRemove(dependency(identifier: 'com.squareup.moshi:moshi-kotlin', configurationName: 'api')),
        Advice.ofRemove(dependency(identifier: 'com.squareup.moshi:moshi-adapters', configurationName: 'api')),
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.moshi:moshi'), 'api'),
        Advice.ofAdd(transitiveDependency(dependency: 'com.squareup.okio:okio'), 'api')
      ] as Set<Advice>)
    ]
  }
}
