package com.autonomousapps.android

import com.autonomousapps.android.projects.DoubleDeclarationsProject
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.IgnoreIf

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// TODO V2: Delete spec after migrating to v2??
@IgnoreIf({ PreconditionContext it -> it.sys.v == '2' })
final class DoubleDeclarationSpec extends AbstractAndroidSpec {

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "doesn't advise to move to api if on api and implementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DoubleDeclarationsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
