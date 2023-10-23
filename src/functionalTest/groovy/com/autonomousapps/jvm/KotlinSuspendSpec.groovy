package com.autonomousapps.jvm

import com.autonomousapps.android.AbstractAndroidSpec
import com.autonomousapps.jvm.projects.KotlinSuspendProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings('GroovyAssignabilityCheck')
final class KotlinSuspendSpec extends AbstractJvmSpec {

  def "can detect functions that take suspend parameters (#gradleVersion)"() {
    given:
    def project = new KotlinSuspendProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
