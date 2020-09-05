package com.autonomousapps

import com.autonomousapps.android.AbstractAndroidSpec
import com.autonomousapps.android.projects.AndroidThreeTenProject
import com.autonomousapps.android.projects.KotlinStdlibProject
import org.gradle.util.GradleVersion
import spock.lang.Unroll

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DependenciesSpec extends AbstractAndroidSpec {

  @Unroll
  def "kotlin stdlib is a dependency bundle by default (#gradleVersion)"() {
    given:
    def project = new KotlinStdlibProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'advice to change stdlib deps'
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedBundleAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "threetenbp should be declared when not part of a dependency bundle (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidThreeTenProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'should add core three-ten-bp lib'
    assertThat(actualAdvice()).containsExactlyElementsIn(project.expectedAdvice())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  @Unroll
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
    assertThat(actualAdvice()).containsExactlyElementsIn([])

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
