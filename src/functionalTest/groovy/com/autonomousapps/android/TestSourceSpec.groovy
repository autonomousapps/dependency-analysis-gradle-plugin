package com.autonomousapps.android

import com.autonomousapps.android.projects.TestSourceProject
import org.gradle.util.GradleVersion
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.PendingFeatureIf

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// TODO V2: add support for tests
final class TestSourceSpec extends AbstractAndroidSpec {

  @PendingFeatureIf({ PreconditionContext it -> it.sys.v == '2' })
  def "test dependencies should be on testImplementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new TestSourceProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
