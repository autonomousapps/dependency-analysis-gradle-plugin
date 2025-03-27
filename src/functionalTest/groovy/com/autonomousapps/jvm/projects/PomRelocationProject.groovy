package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.Dependency.runtimeOnly

final class PomRelocationProject extends AbstractProject {

  /*
   * `mysql:mysql-connector-java` pom file:
   *
   *  <groupId>mysql</groupId>
   *  <artifactId>mysql-connector-java</artifactId>
   *  <version>8.0.33</version>
   *  <distributionManagement>
   *    <relocation>
   *      <groupId>com.mysql</groupId>
   *      <artifactId>mysql-connector-j</artifactId>
   *      <message>MySQL Connector/J artifacts moved to reverse-DNS compliant Maven 2+ coordinates.</message>
   *    </relocation>
   *  </distributionManagement>
   */

  // This is a shim that redirects to `com.mysql:mysql-connector-j:8.0.33` via `distributionManagement.relocation`.
  // Also, this should be runtimeOnly since it's not used at compile- time.
  private final mySqlShim = implementation('mysql:mysql-connector-java:8.0.33')
  private final mySql = runtimeOnly('com.mysql:mysql-connector-j:8.0.33')

  final GradleProject gradleProject

  PomRelocationProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { c ->
        c.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(mySqlShim)
        }
      }
      .write()
  }

  private final Set<Advice> projAdvice = [
    Advice.ofRemove(moduleCoordinates(mySqlShim), 'implementation'),
    Advice.ofAdd(moduleCoordinates(mySql), 'runtimeOnly'),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':proj', projAdvice),
  ]
}
