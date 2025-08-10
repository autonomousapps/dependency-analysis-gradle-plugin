// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.Dependency.implementation

/**
 * A dependency declared on {@code implementation} is visible to {@code testImplementation}, and so we don't typically
 * advise to add a declaration on the latter. However, in the case where the dependency is not used in main source, and
 * that dependency also provides runtime capabilities (such as a service loader), the algorithm must not suggest moving
 * the declaration to {@code runtimeOnly} without also adding it to {@code testImplementation} (since the former is not
 * visible to the latter). So, this is a case where a single declaration, combined with two usages (runtimeOnly and
 * testImplementation), must result in two pieces of advice: <strong>change</strong> {@code implementation} ->
 * {@code runtimeOnly} and <strong>add</strong> to {@code testImplementation}.
 *
 * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html#resolvable_configurations">Java configurations</a>
 */
final class ImplRuntimeTestImplConfusionProject extends AbstractProject {

  final GradleProject gradleProject

  static final SPARK_SQL_ID = "org.apache.spark:spark-sql_2.12"
  private final spark = implementation("$SPARK_SQL_ID:3.5.0")

  ImplRuntimeTestImplConfusionProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('lib') { s ->
        s.sources = SOURCES
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(spark)
        }
      }
      .write()
  }

  private static final List<Source> SOURCES = [
    Source.java(
      '''\
        package com.example.lib;
        
        public class Lib {}
      '''
    )
      .withPath('com.example.lib', 'Lib')
      .build(),
    Source.java(
      '''\
        package com.example.lib;
        
        import org.apache.spark.sql.SparkSession;
        
        public class LibTest {
          private void test() {
            SparkSession.builder().getOrCreate();
          }
        }
      '''
    )
      .withPath('com.example.lib', 'LibTest')
      .withSourceSet('test')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> libAdvice = [
    Advice.ofChange(moduleCoordinates(spark), 'implementation', 'runtimeOnly'),
    Advice.ofAdd(moduleCoordinates(spark), 'testImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':lib', libAdvice),
  ]
}
