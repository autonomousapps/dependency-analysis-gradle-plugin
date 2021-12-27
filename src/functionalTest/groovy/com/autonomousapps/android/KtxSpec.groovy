package com.autonomousapps.android

import com.autonomousapps.fixtures.KtxProject
import com.autonomousapps.internal.android.AgpVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class KtxSpec extends AbstractAndroidSpec {

  def "ktx dependencies are treated per user configuration (#gradleVersion, AGP #agpVersion, ignoreKtx=#ignoreKtx, useKtx=#useKtx)"() {
    given:
    def project = new KtxProject(agpVersion, ignoreKtx, useKtx)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    assertThat(androidProject.adviceFor(project.appSpec)).containsExactlyElementsIn(project.expectedAdviceForApp)

    // This test is too expensive, so we're only going to test against the latest AGP
    where:
    [gradleVersion, agpVersion, useKtx, ignoreKtx] << gradleAgpMatrixPlus(AgpVersion.version('4.1'), [true, false], [true, false])
  }
}
