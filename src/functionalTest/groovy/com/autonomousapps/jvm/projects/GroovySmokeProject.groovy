package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class GroovySmokeProject extends AbstractProject {

  final GradleProject gradleProject

  GroovySmokeProject() {
    this.gradleProject = build()
  }

  @SuppressWarnings('DuplicatedCode')
  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('app') { s ->
      s.sources = applicationSources
      s.withBuildScript { bs ->
        bs.plugins = groovyApplication
        bs.dependencies = [
          project('implementation', ':lib'), // ok
          commonsCollections('implementation'), // unused
          groovyStdlib('implementation'), // ok
        ]
      }
    }
    builder.withSubproject('lib') { s ->
      s.sources = librarySources
      s.withBuildScript { bs ->
        bs.plugins = groovyLibrary
        bs.dependencies = [
          commonsCollections('implementation'), // should be api
          commonsIO('api'), // unused
          groovyStdlib('implementation'), // should be api
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private final List<Plugin> groovyLibrary = [Plugin.groovy, Plugin.javaLibrary]
  private final List<Plugin> groovyApplication = [Plugin.groovy, Plugin.application]

  private final List<Source> applicationSources = [
    new Source(
      SourceType.GROOVY, 'Main', 'com/example/app',
      """\
        package com.example.app
        
        import groovy.transform.CompileStatic
        import com.example.lib.Library
        import java.util.Optional

        @CompileStatic
        public class Main {
          public static void main(String... args) {
            Optional<Library> library = Optional.of(new Library())
          }
        }""".stripIndent()
    )
  ]
  private final List<Source> librarySources = [
    new Source(
      SourceType.GROOVY, 'Library', 'com/example/lib',
      """\
        package com.example.lib
        
        import groovy.transform.CompileStatic
        import org.apache.commons.collections4.Bag
        import org.apache.commons.collections4.bag.HashBag
        
        @CompileStatic
        class Library {
          Bag<String> getBag() {
            return new HashBag<>()
          }
        }""".stripIndent()
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
      moduleCoordinates('org.codehaus.groovy:groovy-all:2.4.15'),
      'implementation',
      'api'
    )
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice),
    projectAdviceForDependencies(':lib', libAdvice),
  ]
}
