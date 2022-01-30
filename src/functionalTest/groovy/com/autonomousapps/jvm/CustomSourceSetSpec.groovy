package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.IntegrationTestProject
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.IgnoreIf

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@IgnoreIf({ PreconditionContext it -> it.sys.'dependency.analysis.old.model' == 'true' })
final class CustomSourceSetSpec extends AbstractJvmSpec {

  def "transitive dependency for main but declared on custom source set should not prevent advice (#gradleVersion)"() {
    given:
    def project = new IntegrationTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

}
