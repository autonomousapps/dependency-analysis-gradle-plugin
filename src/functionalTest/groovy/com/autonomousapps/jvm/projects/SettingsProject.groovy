package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

abstract class SettingsProject extends AbstractProject {

  static final class ScalaProject extends SettingsProject {
    final GradleProject gradleProject

    ScalaProject() {
      this.gradleProject = build()
    }

    @SuppressWarnings('DuplicatedCode')
    private GradleProject build() {
      return newSettingsProjectBuilder()
        .withSubproject('app') { s ->
          s.sources = applicationSources
          s.withBuildScript { bs ->
            bs.plugins = scalaApplication
            bs.dependencies = [
              project('implementation', ':lib'), // ok
              commonsCollections('implementation'), // unused
              scalaStdlib('implementation'), // ok
            ]
          }
        }
        .withSubproject('lib') { s ->
          s.sources = librarySources
          s.withBuildScript { bs ->
            bs.plugins = scalaLibrary
            bs.dependencies = [
              commonsCollections('implementation'), // should be api
              commonsIO('api'), // unused
              scalaStdlib('implementation'), // should be api
            ]
          }
        }
        .write()
    }

    private final List<Plugin> scalaLibrary = [Plugin.scala, Plugin.javaLibrary]
    private final List<Plugin> scalaApplication = [Plugin.scala, Plugin.application]

    private final List<Source> applicationSources = [
      new Source(
        SourceType.SCALA, 'Main', 'com/example/app',
        """\
        package com.example.app
        
        import com.example.lib.Library
        import java.util.Optional
        
        object App {
           def main(args: Array[String]): Unit = {
             val library = Optional.of(new Library())
           }
        }
      """.stripIndent()
      )
    ]
    private final List<Source> librarySources = [
      new Source(
        SourceType.SCALA, 'Library', 'com/example/lib',
        """\
        package com.example.lib
        
        import org.apache.commons.collections4.Bag
        import org.apache.commons.collections4.bag.HashBag
        
        class Library {
          def getBag(): Bag[String] = {
            new HashBag[String]
          }
        }
      """.stripIndent()
      )
    ]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    private final Set<Advice> appAdvice = [
      Advice.ofRemove(moduleCoordinates('org.apache.commons:commons-collections4:4.4'), 'implementation'),
    ]

    private final Set<Advice> libAdvice = [
      Advice.ofRemove(moduleCoordinates('commons-io:commons-io:2.6'), 'api'),
      Advice.ofChange(
        moduleCoordinates('org.apache.commons:commons-collections4:4.4'),
        'implementation',
        'api'
      ),
      Advice.ofChange(
        moduleCoordinates('org.scala-lang:scala-library:2.13.1'),
        'implementation',
        'api'
      )
    ]

    final Set<ProjectAdvice> expectedBuildHealth = [
      projectAdviceForDependencies(':app', appAdvice),
      projectAdviceForDependencies(':lib', libAdvice),
    ]
  }

  static final class KotlinProject extends SettingsProject {
    final GradleProject gradleProject

    KotlinProject() {
      this.gradleProject = build()
    }

    @SuppressWarnings('DuplicatedCode')
    private GradleProject build() {
      return newSettingsProjectBuilder(withKotlin: true)
        .withSubproject('app') { s ->
          s.sources = applicationSources
          s.withBuildScript { bs ->
            bs.plugins = kotlinApplication
            bs.dependencies = [
              project('implementation', ':lib'), // ok
              commonsCollections('implementation'), // unused
            ]
          }
        }
        .withSubproject('lib') { s ->
          s.sources = librarySources
          s.withBuildScript { bs ->
            bs.plugins = kotlinLibrary
            bs.dependencies = [
              commonsCollections('implementation'), // should be api
              commonsIO('api'), // unused
            ]
          }
        }
        .write()
    }

    private final List<Plugin> kotlinLibrary = [Plugins.kotlinNoVersion, Plugin.javaLibrary]
    private final List<Plugin> kotlinApplication = [Plugins.kotlinNoVersion, Plugin.application]

    private final List<Source> applicationSources = [
      Source.kotlin(
        '''\
          package com.example.app
          
          import com.example.lib.Library
          import java.util.Optional
          
          object App {
            fun main() {
              val library = Optional.of(Library())
            }
          }
        '''
      )
        .withPath('com.example.main', 'Main')
        .build()
    ]
    private final List<Source> librarySources = [
      Source.kotlin(
        '''\
          package com.example.lib
        
          import org.apache.commons.collections4.Bag
          import org.apache.commons.collections4.bag.HashBag
          
          class Library {
            fun getBag(): Bag<String> = HashBag<String>()
          }
        '''
      )
        .withPath('com.example.lib', 'Library')
        .build()
    ]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    private final Set<Advice> appAdvice = [
      Advice.ofRemove(moduleCoordinates('org.apache.commons:commons-collections4:4.4'), 'implementation'),
    ] + downgradeKotlinStdlib()

    private final Set<Advice> libAdvice = [
      Advice.ofRemove(moduleCoordinates('commons-io:commons-io:2.6'), 'api'),
      Advice.ofChange(
        moduleCoordinates('org.apache.commons:commons-collections4:4.4'),
        'implementation',
        'api'
      ),
    ]

    final Set<ProjectAdvice> expectedBuildHealth = [
      projectAdviceForDependencies(':app', appAdvice),
      projectAdviceForDependencies(':lib', libAdvice),
    ]
  }

  static final class KgpMissingProject extends SettingsProject {
    final GradleProject gradleProject

    KgpMissingProject() {
      this.gradleProject = build()
    }

    @SuppressWarnings('DuplicatedCode')
    private GradleProject build() {
      return newSettingsProjectBuilder(withKotlin: false)
        .withSubproject('app') { s ->
          s.withBuildScript { bs ->
            bs.plugins = kotlinApplication
          }
        }
        .write()
    }

    private final List<Plugin> kotlinApplication = [
      new Plugin("org.jetbrains.kotlin.jvm", Plugins.KOTLIN_VERSION),
      Plugin.application
    ]
  }
}
