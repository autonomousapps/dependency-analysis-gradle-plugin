package com.autonomousapps.android

import com.autonomousapps.android.projects.CouldBeAndroidProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.android.projects.CouldBeAndroidProject.ExpectedResult
import static com.autonomousapps.kit.truth.BuildResultSubject.buildResults
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertAbout

final class CouldBeAndroidSpec extends AbstractAndroidSpec {

  def "warning that android module could be jvm module (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new CouldBeAndroidProject(agpVersion as String, ExpectedResult.WARN)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '-Pdependency.analysis.print.build.health=true'
    )

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .containsExactlyModuleAdviceIn(project.expectedModuleAdvice)
    assertAbout(buildResults())
      .that(result)
      .output()
      .contains('''\
        Module structure advice
        This project uses limited Android features and could be a JVM project.
        * Includes BuildConfig.
      '''.stripIndent())

    where:
    [gradleVersion, agpVersion] << [gradleAgpMatrix().last()]
  }

  def "failure because android module could be jvm module (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new CouldBeAndroidProject(agpVersion as String, ExpectedResult.FAIL)
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '-Pdependency.analysis.print.build.health=true'
    )

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .containsExactlyModuleAdviceIn(project.expectedModuleAdvice)
    assertAbout(buildResults())
      .that(result)
      .output()
      .contains('''\
        Module structure advice
        This project uses limited Android features and could be a JVM project.
        * Includes BuildConfig.
      '''.stripIndent())

    where:
    [gradleVersion, agpVersion] << [gradleAgpMatrix().last()]
  }

  def "don't tell me android module could be jvm module (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new CouldBeAndroidProject(agpVersion as String, ExpectedResult.IGNORE)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '-Pdependency.analysis.print.build.health=true'
    )

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .containsExactlyModuleAdviceIn(project.expectedModuleAdviceForIgnore)
    assertAbout(buildResults())
      .that(result)
      .output()
      .doesNotContain('Module structure advice')

    where:
    [gradleVersion, agpVersion] << [gradleAgpMatrix().last()]
  }

  def "specifically don't tell me android module could be jvm module (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new CouldBeAndroidProject(agpVersion as String, ExpectedResult.IGNORE_ANDROID)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '-Pdependency.analysis.print.build.health=true'
    )

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .containsExactlyModuleAdviceIn(project.expectedModuleAdviceForIgnore)
    assertAbout(buildResults())
      .that(result)
      .output()
      .doesNotContain('Module structure advice')

    where:
    [gradleVersion, agpVersion] << [gradleAgpMatrix().last()]
  }
}
