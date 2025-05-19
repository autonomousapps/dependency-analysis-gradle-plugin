// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.LeakCanaryProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings("GroovyAssignabilityCheck")
final class LeakCanarySpec extends AbstractAndroidSpec {

  def "leakcanary is not reported as unused (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new LeakCanaryProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      // TODO(tsr): something isn't quite right here, look at the fixture
      'app:reason', '--id', "com.squareup.leakcanary:leakcanary-android:${LeakCanaryProject.LEAK_CANARY_VERSION}",
    )
    // TOD(tsr): delete when done iterating
    build(
      gradleVersion, gradleProject.rootDir,
      'app:reason', '--id', "com.squareup.leakcanary:leakcanary-android-core:${LeakCanaryProject.LEAK_CANARY_VERSION}"
    )

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
