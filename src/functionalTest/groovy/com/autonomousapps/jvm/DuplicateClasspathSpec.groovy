package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.DuplicateClasspathProject
import org.gradle.testkit.runner.TaskOutcome

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class DuplicateClasspathSpec extends AbstractJvmSpec {

  def "duplicates on the classpath can lead to build failures (#gradleVersion)"() {
    given:
    def project = new DuplicateClasspathProject()
    gradleProject = project.gradleProject

    when:
    // This first invocation "fixes" the dependency declarations
    build(gradleVersion, gradleProject.rootDir, ':consumer:fixDependencies')
    // This second invocation fails because the fix (+ sort) causes the JVM to load the wrong class during compilation.
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(result.task(':consumer:compileJava').outcome).isEqualTo(TaskOutcome.FAILED)

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "buildHealth reports duplicates (#gradleVersion)"() {
    given:
    def project = new DuplicateClasspathProject()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(result.output).contains(
      '''\
        Warnings
        Some of your classpaths have duplicate classes, which means the compile and runtime behavior can be sensitive to the classpath order.
        
        Source set: main
        \\--- compile classpath
             +--- com/example/producer/Producer$Inner.class is provided by multiple dependencies: [:producer-1, :producer-2]
             \\--- com/example/producer/Producer.class is provided by multiple dependencies: [:producer-1, :producer-2]
        \\--- runtime classpath
             +--- com/example/producer/Producer$Inner.class is provided by multiple dependencies: [:producer-1, :producer-2]
             \\--- com/example/producer/Producer.class is provided by multiple dependencies: [:producer-1, :producer-2]'''
        .stripIndent()
    )

    and:
    assertAbout(buildHealth())
      .that(project.actualProjectAdvice())
      .isEquivalentIgnoringModuleAdvice(project.expectedProjectAdvice)

    where:
    gradleVersion << [GRADLE_LATEST]
  }
}
