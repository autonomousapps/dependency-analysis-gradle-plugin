package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.Dependency.commonsCollections
import static com.autonomousapps.kit.Dependency.junit

final class CustomTestSourceSetProject extends AbstractProject {

  /** Inherited by our functionalTest source set. */
  private static final commonsCollections = commonsCollections('testImplementation')
  private static final junit = junit('testImplementation')

  final GradleProject gradleProject

  CustomTestSourceSetProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin, Plugin.kotlinPluginNoVersion]
        bs.dependencies = [commonsCollections, junit]
        bs.additions = '''\
          sourceSets {
            functionalTest {
              compileClasspath += main.output + configurations.testRuntimeClasspath
              runtimeClasspath += output + compileClasspath
            }
          }
          configurations {
            functionalTestImplementation.extendsFrom testImplementation
          }'''.stripIndent()
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.KOTLIN, 'FunctionalTest', 'com/example/func',
      """\
        package com.example.func

        import org.apache.commons.collections4.Bag

        class FunctionalTest {
          // part of the "API" (but this is a test, no API)
          fun magic(): Bag<String> {
            TODO()
          }
        }""".stripIndent(),
      'functionalTest'
    ),
    new Source(
      SourceType.KOTLIN, "Spec", "com/example/test",
      """\
        package com.example.test
        
        import org.apache.commons.collections4.bag.HashBag
        import org.junit.Test
        
        class Spec {
          @Test fun test() {
            // part of the implementation for this source set
            val bag = HashBag<String>();
          }
        }""".stripIndent(),
      'test'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':proj', [] as Set<Advice>)
  ]
}
