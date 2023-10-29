package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.junit

final class CustomTestSourceSetProject extends AbstractProject {

  /** Inherited by our functionalTest source set. */
  private static final commonsCollections = commonsCollections('testImplementation')
  private static final junit = junit('testImplementation')

  private final SourceType sourceType
  final GradleProject gradleProject

  CustomTestSourceSetProject(SourceType sourceType) {
    this.sourceType = sourceType
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = source()
      s.withBuildScript { bs ->
        bs.plugins = plugins()
        bs.dependencies = [commonsCollections, junit]
        bs.withGroovy('''\
          sourceSets {
            functionalTest {
              compileClasspath += main.output + configurations.testRuntimeClasspath
              runtimeClasspath += output + compileClasspath
            }
          }
          configurations {
            functionalTestImplementation.extendsFrom testImplementation
          }''')
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Plugin> plugins() {
    if (sourceType == SourceType.JAVA) {
      return [Plugin.javaLibrary]
    } else if (sourceType == SourceType.KOTLIN) {
      return [Plugins.kotlinNoVersion]
    } else {
      throw new IllegalArgumentException("Only Java and Kotlin supported. Was '${sourceType}'.")
    }
  }

  private List<Source> source() {
    if (sourceType == SourceType.JAVA) {
      return javaSource
    } else if (sourceType == SourceType.KOTLIN) {
      return kotlinSource
    } else {
      throw new IllegalArgumentException("Only Java and Kotlin supported. Was '${sourceType}'.")
    }
  }

  private List<Source> javaSource = [
    new Source(
      SourceType.JAVA, 'FunctionalTest', 'com/example/func',
      """\
        package com.example.func;

        import org.apache.commons.collections4.Bag;
        import org.apache.commons.collections4.bag.HashBag;

        public class FunctionalTest {
          // part of the "API" (but this is a test, no API)
          public Bag<String> magic() {
            return new HashBag<>();
          }
        }""".stripIndent(),
      'functionalTest'
    ),
    new Source(
      SourceType.JAVA, 'Spec', 'com/example/test',
      """\
        package com.example.test;
        
        import org.apache.commons.collections4.bag.HashBag;
        import org.junit.Test;
        
        public class Spec {
          @Test public void test() {
            // part of the implementation for this source set
            HashBag<String> bag = new HashBag<>();
          }
        }""".stripIndent(),
      'test'
    )
  ]

  private List<Source> kotlinSource = [
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
      SourceType.KOTLIN, 'Spec', 'com/example/test',
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
