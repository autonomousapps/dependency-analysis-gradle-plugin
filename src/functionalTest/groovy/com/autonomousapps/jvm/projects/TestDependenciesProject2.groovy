// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsCollections
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.junit

// https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/pull/553
final class TestDependenciesProject2 extends AbstractProject {

  final GradleProject gradleProject

  TestDependenciesProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('a') { s ->
        s.sources = sourcesA
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [
            project('implementation', ':b'),
            commonsCollections('testImplementation'),
            junit('testImplementation'),
          ]
        }
      }
      .withSubproject('b') { s ->
        s.sources = sourcesB
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [commonsCollections('api')]
        }
      }
      .write()
  }

  private List<Source> sourcesA = [
    new Source(
      SourceType.JAVA, "A", "com/example/a",
      """\
        package com.example.a;
        
        import com.example.b.B;

        public class A {
          // consistent with `implementation project(':b')`
          private B b;
        }""".stripIndent()
    ),
    new Source(
      SourceType.JAVA, "Spec", "com/example/a",
      """\
        package com.example.a;
        
        import org.apache.commons.collections4.Bag;
        import org.apache.commons.collections4.bag.HashBag;
        import org.junit.Test;
        
        public class Spec {
          @Test
          public void test() {
            // consistent with `testImplementation commonsCollections` 
            Bag<String> bag = new HashBag<>();
          }
        }""".stripIndent(),
      'test'
    )
  ]

  private List<Source> sourcesB = [
    new Source(
      SourceType.JAVA, "B", "com/example/b",
      """\
        package com.example.b;

        import org.apache.commons.collections4.Bag;

        public abstract class B {
          // consistent with `api commonsCollections`
          public abstract Bag<String> bagOfStrings();
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':a'),
    emptyProjectAdviceFor(':b'),
  ]
}
