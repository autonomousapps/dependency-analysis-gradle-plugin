package com.autonomousapps.android

import com.autonomousapps.AdviceHelper
import com.autonomousapps.android.projects.AndroidThreeTenProject
import com.autonomousapps.android.projects.FirebaseProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DependenciesSpec extends AbstractAndroidSpec {

  def "threetenbp should be declared when not part of a dependency bundle (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidThreeTenProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'should add core three-ten-bp lib'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "jw threetenabp and threetenbp can be a dependency bundle (#gradleVersion AGP #agpVersion)"() {
    given:
    def additions = """\
      dependencyAnalysis {
        dependencies {
          bundle('three-ten') {
            includeDependency('org.threeten:threetenbp')
            includeDependency('com.jakewharton.threetenabp:threetenabp')
          }
        }
      }
    """
    def project = new AndroidThreeTenProject(agpVersion as String, additions)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBundleBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "firebase-analytics is a dependency bundle by default (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new FirebaseProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(AdviceHelper.actualBuildHealth(gradleProject))
      .containsExactlyElementsIn(AdviceHelper.emptyBuildHealthFor(':app', ':'))

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
