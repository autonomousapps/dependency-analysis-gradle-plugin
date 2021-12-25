package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.RedundantPluginsProject
import org.spockframework.runtime.extension.builtin.PreconditionContext
import spock.lang.PendingFeatureIf

import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

// TODO V2: support has not yet been added to v2
final class RedundantPluginsSpec extends AbstractJvmSpec {

  @PendingFeatureIf({ PreconditionContext it -> it.sys.v == '2' })
  def "kotlin-jvm plugin is redundant (#gradleVersion)"() {
    given:
    def project = new RedundantPluginsProject()
    gradleProject = project.gradleProject

    when:
    buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:

    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
