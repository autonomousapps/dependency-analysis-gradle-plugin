// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.VariantDeclarationProject
import spock.lang.PendingFeature

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

final class VariantDeclarationSpec extends AbstractAndroidSpec {

  // TODO(tsr): DAGP *wants* to do this:
  //  1. remove `debugImplementation(foo)`
  //  2. remove `releaseImplementation(foo)`
  //  3. add `implementation(foo-jvm)`
  //  But `Bundles` is eliminating all of this "natural" advice and leaving the build script as-is. What we *want* to
  //  happen is this:
  //  1. remove `debugImplementation(foo)`
  //  2. remove `releaseImplementation(foo)`
  //  3. add `implementation(foo)` (foo, not foo-jvm).
  //  It's important to note the code as-is works well when there's just a single declaration (e.g., `implementation(foo)`).
  //  It is breaking down when there are multiple declarations that *could be simplified*.
  //  On the plus side, the current situation doesn't break anything for end-users.
  @PendingFeature
  def "moves a dependency declared on every variant to implementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new VariantDeclarationProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
